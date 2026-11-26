package com.luxcalc.pro.data.local

import com.luxcalc.pro.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

object DesignJsonSerializer {

    fun encodeMixedEntries(entries: List<MixedFixtureEntry>): String {
        val arr = JSONArray()
        for (e in entries) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("fixtureId", e.fixture.id)
                put("fixtureNameAr", e.fixture.nameAr)
                put("category", e.fixture.category.name)
                put("layer", e.layer.name)
                put("wattage", e.fixture.wattage)
                put("lumens", e.fixture.lumens)
                put("beamAngleDeg", e.fixture.beamAngleDeg)
                put("cctK", e.fixture.cctK)
                put("cri", e.fixture.cri)
                put("ipRating", e.fixture.ipRating)
                put("mountingTypeAr", e.fixture.mountingTypeAr)
                put("quantity", e.quantity)
                put("switchingGroup", e.switchingGroup)
                put("dimmable", e.dimmable)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeMixedEntries(json: String): List<MixedFixtureEntry> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val cat = runCatching { FixtureCategory.valueOf(o.optString("category")) }
                    .getOrDefault(FixtureCategory.SPOTLIGHTS)
                val layer = runCatching { LightingLayer.valueOf(o.optString("layer")) }
                    .getOrDefault(LightingLayer.GENERAL)
                val fix = LightingFixture(
                    id = o.optString("fixtureId", "fix_$i"),
                    nameAr = o.optString("fixtureNameAr", "وحدة إضاءة"),
                    category = cat,
                    defaultLayer = layer,
                    wattage = o.optDouble("wattage", 10.0),
                    lumens = o.optDouble("lumens", 900.0),
                    beamAngleDeg = o.optInt("beamAngleDeg", 38),
                    cctK = o.optInt("cctK", 3000),
                    cri = o.optInt("cri", 90),
                    ipRating = o.optString("ipRating", "IP20"),
                    mountingTypeAr = o.optString("mountingTypeAr", "غاطس")
                )
                MixedFixtureEntry(
                    id = o.optString("id", "e_$i"),
                    fixture = fix,
                    quantity = o.optInt("quantity", 1),
                    layer = layer,
                    switchingGroup = o.optString("switchingGroup", "مجموعة 1"),
                    dimmable = o.optBoolean("dimmable", false)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun encodePlacements(items: List<PlacedFixture2D>): String {
        val arr = JSONArray()
        for (p in items) {
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("xM", p.xM)
                put("yM", p.yM)
                put("fixtureNameAr", p.fixtureNameAr)
                put("fixtureId", p.fixtureId)
                put("layer", p.layer.name)
                put("symbolCode", p.symbolCode)
                put("wattage", p.wattage)
                put("lumens", p.lumens)
            })
        }
        return arr.toString()
    }

    fun decodePlacements(json: String): List<PlacedFixture2D> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                PlacedFixture2D(
                    id = o.optString("id", "p_$i"),
                    xM = o.optDouble("xM", 1.0),
                    yM = o.optDouble("yM", 1.0),
                    fixtureNameAr = o.optString("fixtureNameAr", "سبوت"),
                    fixtureId = o.optString("fixtureId", "spot_single"),
                    layer = runCatching { LightingLayer.valueOf(o.optString("layer")) }.getOrDefault(LightingLayer.GENERAL),
                    symbolCode = o.optString("symbolCode", "SP"),
                    wattage = o.optDouble("wattage", 10.0),
                    lumens = o.optDouble("lumens", 900.0)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun encodeOpenings(items: List<WallOpening2D>): String {
        val arr = JSONArray()
        for (op in items) {
            arr.put(JSONObject().apply {
                put("id", op.id)
                put("type", op.type.name)
                put("wall", op.wall.name)
                put("offsetM", op.offsetM)
                put("widthM", op.widthM)
            })
        }
        return arr.toString()
    }

    fun decodeOpenings(json: String): List<WallOpening2D> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                WallOpening2D(
                    id = o.optString("id", "op_$i"),
                    type = runCatching { OpeningType.valueOf(o.optString("type")) }.getOrDefault(OpeningType.DOOR),
                    wall = runCatching { WallSide.valueOf(o.optString("wall")) }.getOrDefault(WallSide.BOTTOM),
                    offsetM = o.optDouble("offsetM", 1.0),
                    widthM = o.optDouble("widthM", 0.9)
                )
            }
        }.getOrDefault(emptyList())
    }
}
