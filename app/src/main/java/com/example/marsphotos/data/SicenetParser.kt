package com.example.marsphotos.data

import com.example.marsphotos.data.local.AcademicLoadEntity
import com.example.marsphotos.data.local.CardexEntity
import com.example.marsphotos.data.local.FinalGradesEntity
import com.example.marsphotos.data.local.UnitGradesEntity
import kotlinx.serialization.InternalSerializationApi
import org.json.JSONArray

/**
 * [CAPA DE DATOS - PARSER (Extractores de Texto)]
 * Objeto encargado de procesar las respuestas del servidor de Sicenet.
 */
@OptIn(InternalSerializationApi::class)
object SicenetParser {

    private fun org.json.JSONObject.optStringSafe(key: String): String {
        if (has(key)) return optString(key)
        val pascal = key.replaceFirstChar { it.uppercase() }
        if (has(pascal)) return optString(pascal)
        if (has(key.uppercase())) return optString(key.uppercase())
        val camel = key.replaceFirstChar { it.lowercase() }
        if (has(camel)) return optString(camel)
        if (has("str$pascal")) return optString("str$pascal")
        return ""
    }

    private fun org.json.JSONObject.optIntSafe(key: String): Int {
        if (has(key)) return optInt(key)
        val pascal = key.replaceFirstChar { it.uppercase() }
        if (has(pascal)) return optInt(pascal)
        return 0
    }

    @OptIn(InternalSerializationApi::class)
    fun parseAcademicLoad(xml: String): List<AcademicLoadEntity> {
        var entities = parseJsonAcademicLoad(xml)
        if (entities.isEmpty()) {
            entities = parseXmlAcademicLoad(xml)
        }
        return entities
    }

    @OptIn(InternalSerializationApi::class)
    private fun parseJsonAcademicLoad(xml: String): List<AcademicLoadEntity> {
        val entities = mutableListOf<AcademicLoadEntity>()
        try {
            val jsonString = extractJsonContent(xml)
            if (jsonString.startsWith("[")) {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    entities.add(
                        AcademicLoadEntity(
                            materia = obj.optStringSafe("materia"),
                            grupo = obj.optStringSafe("grupo"),
                            profesor = obj.optStringSafe("profesor"),
                            lunes = obj.optStringSafe("lunes"),
                            martes = obj.optStringSafe("martes"),
                            miercoles = obj.optStringSafe("miercoles"),
                            jueves = obj.optStringSafe("jueves"),
                            viernes = obj.optStringSafe("viernes"),
                            sabado = obj.optStringSafe("sabado"),
                            domingo = obj.optStringSafe("domingo"),
                            creditos = obj.optIntSafe("creditos"),
                            aula = obj.optStringSafe("aula"),
                            estadoMateria = obj.optStringSafe("estado")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entities
    }

    @OptIn(InternalSerializationApi::class)
    private fun parseXmlAcademicLoad(xml: String): List<AcademicLoadEntity> {
        val entities = mutableListOf<AcademicLoadEntity>()
        try {
            val materias = extractAllTags(xml, "materia")
            val grupos = extractAllTags(xml, "grupo")
            val profesores = extractAllTags(xml, "profesor")
            val lunes = extractAllTags(xml, "lunes")
            val martes = extractAllTags(xml, "martes")
            val miercoles = extractAllTags(xml, "miercoles")
            val jueves = extractAllTags(xml, "jueves")
            val viernes = extractAllTags(xml, "viernes")
            val sabados = extractAllTags(xml, "sabado")
            val domingos = extractAllTags(xml, "domingo")
            val creditos = extractAllTags(xml, "creditos")
            val aulas = extractAllTags(xml, "aula")
            val estados = extractAllTags(xml, "estado")

            val size = materias.size
            for (i in 0 until size) {
                entities.add(
                    AcademicLoadEntity(
                        materia = materias.getOrElse(i) { "" },
                        grupo = grupos.getOrElse(i) { "" },
                        profesor = profesores.getOrElse(i) { "" },
                        lunes = lunes.getOrElse(i) { "" },
                        martes = martes.getOrElse(i) { "" },
                        miercoles = miercoles.getOrElse(i) { "" },
                        jueves = jueves.getOrElse(i) { "" },
                        viernes = viernes.getOrElse(i) { "" },
                        sabado = sabados.getOrElse(i) { "" },
                        domingo = domingos.getOrElse(i) { "" },
                        creditos = creditos.getOrElse(i) { "0" }.toIntOrNull() ?: 0,
                        aula = aulas.getOrElse(i) { "" },
                        estadoMateria = estados.getOrElse(i) { "" }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entities
    }

    fun parseCardex(xml: String): List<CardexEntity> {
        var entities = parseJsonCardex(xml)
        if (entities.isEmpty()) {
            entities = parseXmlCardex(xml)
        }
        return entities
    }

    private fun parseJsonCardex(xml: String): List<CardexEntity> {
        val entities = mutableListOf<CardexEntity>()
        try {
            val jsonString = extractJsonContent(xml)
            if (jsonString.startsWith("[")) {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    entities.add(
                        CardexEntity(
                            materia = obj.optStringSafe("materia"),
                            clave = obj.optStringSafe("clave"),
                            creditos = obj.optIntSafe("creditos"),
                            calificacion = obj.optStringSafe("calif"),
                            evaluacion = obj.optStringSafe("tipoEval"),
                            semestre = obj.optIntSafe("semestre"),
                            anio = obj.optIntSafe("periodo"),
                            observacion = obj.optStringSafe("observacion")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entities
    }

    private fun parseXmlCardex(xml: String): List<CardexEntity> {
        val entities = mutableListOf<CardexEntity>()
        try {
            val materias = extractAllTags(xml, "materia")
            val claves = extractAllTags(xml, "clave")
            val creditos = extractAllTags(xml, "creditos")
            val califs = extractAllTags(xml, "calif")
            val evaluacion = extractAllTags(xml, "tipoEval")
            val semestres = extractAllTags(xml, "semestre")
            val periodos = extractAllTags(xml, "periodo")
            val observaciones = extractAllTags(xml, "observacion") 

            val finalCalifs = if (califs.isEmpty()) extractAllTags(xml, "calificacion") else califs

            val size = materias.size
            for (i in 0 until size) {
                entities.add(
                    CardexEntity(
                        materia = materias.getOrElse(i) { "" },
                        clave = claves.getOrElse(i) { "" },
                        creditos = creditos.getOrElse(i) { "0" }.toIntOrNull() ?: 0,
                        calificacion = finalCalifs.getOrElse(i) { "NA" },
                        evaluacion = evaluacion.getOrElse(i) { "" },
                        semestre = semestres.getOrElse(i) { "0" }.toIntOrNull() ?: 0,
                        anio = periodos.getOrElse(i) { "0" }.toIntOrNull() ?: 0,
                        observacion = observaciones.getOrElse(i) { "" }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entities
    }

    fun parseUnitGrades(xml: String): List<UnitGradesEntity> {
        var entities = parseJsonUnitGrades(xml)
        if (entities.isEmpty()) {
            entities = parseXmlUnitGrades(xml)
        }
        return entities
    }

    private fun parseJsonUnitGrades(xml: String): List<UnitGradesEntity> {
        val entities = mutableListOf<UnitGradesEntity>()
        try {
            val jsonString = extractJsonContent(xml)
            if (jsonString.startsWith("[")) {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    entities.add(
                        UnitGradesEntity(
                            materia = obj.optStringSafe("materia"),
                            profesor = obj.optStringSafe("profesor").ifEmpty { obj.optStringSafe("docente") },
                            u1 = obj.optStringSafe("c1"),
                            u2 = obj.optStringSafe("c2"),
                            u3 = obj.optStringSafe("c3"),
                            u4 = obj.optStringSafe("c4"),
                            u5 = obj.optStringSafe("c5"),
                            u6 = obj.optStringSafe("c6"),
                            u7 = obj.optStringSafe("c7"),
                            u8 = obj.optStringSafe("c8"),
                            u9 = obj.optStringSafe("c9"),
                            u10 = obj.optStringSafe("c10"),
                            u11 = obj.optStringSafe("c11"),
                            u12 = obj.optStringSafe("c12"),
                            u13 = obj.optStringSafe("c13"),
                            act = obj.optStringSafe("act"),
                            pf = obj.optStringSafe("prom")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entities
    }

     private fun parseXmlUnitGrades(xml: String): List<UnitGradesEntity> {
        val entities = mutableListOf<UnitGradesEntity>()
        try {
            val materias = extractAllTags(xml, "materia")
            val profesores = extractAllTags(xml, "profesor").ifEmpty { extractAllTags(xml, "docente") }
            val c1 = extractAllTags(xml, "c1")
            val c2 = extractAllTags(xml, "c2")
            val c3 = extractAllTags(xml, "c3")
            val prom = extractAllTags(xml, "prom")

            val size = materias.size
            for (i in 0 until size) {
                entities.add(
                    UnitGradesEntity(
                        materia = materias.getOrElse(i) { "" },
                        profesor = profesores.getOrElse(i) { "" },
                        u1 = c1.getOrElse(i) { "" },
                        u2 = c2.getOrElse(i) { "" },
                        u3 = c3.getOrElse(i) { "" },
                        u4 = "", 
                        u5 = "",
                        u6 = "",
                        u7 = "",
                        u8 = "",
                        u9 = "",
                        u10 = "",
                        u11 = "",
                        u12 = "",
                        u13 = "",
                        act = "",
                        pf = prom.getOrElse(i) { "" }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entities
    }

    fun parseFinalGrades(xml: String): List<FinalGradesEntity> {
        var entities = parseJsonFinalGrades(xml)
        if (entities.isEmpty()) {
            entities = parseXmlFinalGrades(xml)
        }
        return entities
    }

    private fun parseJsonFinalGrades(xml: String): List<FinalGradesEntity> {
        val entities = mutableListOf<FinalGradesEntity>()
        try {
            val jsonString = extractJsonContent(xml)
            if (jsonString.startsWith("[")) {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    entities.add(
                        FinalGradesEntity(
                            materia = obj.optStringSafe("materia"),
                            profesor = obj.optStringSafe("profesor").ifEmpty { obj.optStringSafe("docente") },
                            calif = obj.optStringSafe("calif"),
                            observacion = obj.optStringSafe("observacion")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entities
    }

    private fun parseXmlFinalGrades(xml: String): List<FinalGradesEntity> {
        val entities = mutableListOf<FinalGradesEntity>()
        try {
            val materias = extractAllTags(xml, "materia")
            val profesores = extractAllTags(xml, "profesor").ifEmpty { extractAllTags(xml, "docente") }
            val califs = extractAllTags(xml, "calif")
            val observaciones = extractAllTags(xml, "observacion")

            val size = materias.size
            for (i in 0 until size) {
                entities.add(
                    FinalGradesEntity(
                        materia = materias.getOrElse(i) { "" },
                        profesor = profesores.getOrElse(i) { "" },
                        calif = califs.getOrElse(i) { "" },
                        observacion = observaciones.getOrElse(i) { "" }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entities
    }

    private fun extractJsonContent(xml: String): String {
        val start = xml.indexOf("[")
        val end = xml.lastIndexOf("]")
        if (start != -1 && end != -1 && start < end) {
             return xml.substring(start, end + 1)
        }
        val startTags = listOf("Result>", "return>")
        for (tag in startTags) {
            if (xml.contains(tag)) {
                return xml.substringAfter(tag).substringBefore("</")
            }
        }
        return ""
    }

    private fun extractAllTags(xml: String, tagName: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val pattern = "<(?:\\w+:)?$tagName>(.*?)</(?:\\w+:)?$tagName>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val matches = pattern.findAll(xml)
            matches.forEach { 
                list.add(it.groupValues[1].trim())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
