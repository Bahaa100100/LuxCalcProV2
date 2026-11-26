package com.luxcalc.pro.domain.engine

import com.luxcalc.pro.domain.model.RecommendationSource
import com.luxcalc.pro.domain.model.SpaceCategory
import com.luxcalc.pro.domain.model.SpaceRecognitionResult
import com.luxcalc.pro.domain.model.SpaceTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional Gemini AI integration for advanced unfamiliar custom space recognition.
 * - Strictly optional; core app works 100% offline.
 * - Never embeds any API key in source code; user provides their own key in Settings.
 */
object GeminiSpaceAdvisor {

    suspend fun analyzeCustomSpaceWithGemini(
        apiKey: String,
        spaceName: String,
        additionalContext: String = ""
    ): Result<SpaceRecognitionResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("لم يتم إدخال مفتاح Gemini API في الإعدادات."))
        }
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey.trim()}"
            val prompt = """
                أنت مهندس تصميم إضاءة معماري محترف. حلل الفراغ التالي: "$spaceName".
                سياق إضافي: "$additionalContext".
                أعد JSON فقط بالصيغة التالية بدون أي نص خارج JSON:
                {
                  "minLux": 200,
                  "defaultLux": 300,
                  "maxLux": 450,
                  "defaultCctK": 4000,
                  "cctAr": "4000K محايد",
                  "minCri": 90,
                  "workplaneM": 0.80,
                  "visualTasksAr": "وصف المهام البصرية بالعربية",
                  "lightingRequirementsAr": "متطلبات الإضاءة والوحدات المقترحة بالعربية",
                  "explanationAr": "شرح هندسي واضح للتوصية بالعربية مع التوضيح أنها تقدير ذكي"
                }
                لا تخترع أرقام معايير قياسية غير موجودة.
            """.trimIndent()

            val body = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                ))
            }

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val rawResponse = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                return@withContext Result.failure(RuntimeException("خطأ من خدمة Gemini (رمز $code)"))
            }

            val root = JSONObject(rawResponse)
            val text = root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val jsonStart = text.indexOf('{')
            val jsonEnd = text.lastIndexOf('}')
            val parsed = JSONObject(text.substring(jsonStart, jsonEnd + 1))

            val template = SpaceTemplate(
                id = "ai_${System.currentTimeMillis()}",
                nameAr = spaceName.trim(),
                category = SpaceCategory.CUSTOM,
                minLux = parsed.optInt("minLux", 200),
                defaultLux = parsed.optInt("defaultLux", 300),
                maxLux = parsed.optInt("maxLux", 450),
                recommendedCctAr = parsed.optString("cctAr", "4000K محايد"),
                defaultCctK = parsed.optInt("defaultCctK", 4000),
                minCri = parsed.optInt("minCri", 85),
                workplaneHeightM = parsed.optDouble("workplaneM", 0.80),
                lightingRequirementsAr = parsed.optString("lightingRequirementsAr", "توزيع متوازن"),
                suggestedFixtureIds = listOf("spot_anti_glare", "panel_led", "cove_light"),
                source = RecommendationSource.AI_ESTIMATE,
                referenceAr = "تقدير ذكاء اصطناعي (Gemini AI) — يُراجع هندسياً قبل الاعتماد",
                isCustom = true
            )

            Result.success(
                SpaceRecognitionResult(
                    inputName = spaceName,
                    matchedTemplate = template,
                    explanationAr = parsed.optString("explanationAr", "تقدير مولد بواسطة Gemini AI."),
                    visualTasksAr = parsed.optString("visualTasksAr", "مهام بصرية مخصصة"),
                    needsClarification = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
