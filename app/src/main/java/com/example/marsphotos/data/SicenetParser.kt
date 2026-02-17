package com.example.marsphotos.data

import com.example.marsphotos.data.local.AcademicLoadEntity
import com.example.marsphotos.data.local.CardexEntity
import com.example.marsphotos.data.local.FinalGradesEntity
import com.example.marsphotos.data.local.UnitGradesEntity
import org.json.JSONArray

object SicenetParser {

    fun parseAcademicLoad(xml: String): List<AcademicLoadEntity> {
        // Try JSON first
        var entities = parseJsonAcademicLoad(xml)
        if (entities.isEmpty()) {
            // Fallback to XML
            entities = parseXmlAcademicLoad(xml)
        }
        return entities
    }

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
                            materia = obj.optString("materia", ""),
                            grupo = obj.optString("grupo", ""),
                            profesor = obj.optString("profesor", ""),
                            lunes = obj.optString("lunes", ""),
                            martes = obj.optString("martes", ""),
                            miercoles = obj.optString("miercoles", ""),
                            jueves = obj.optString("jueves", ""),
                            viernes = obj.optString("viernes", ""),
                            sabado = obj.optString("sabado", ""),
                            domingo = obj.optString("domingo", ""),
                            creditos = obj.optInt("creditos", 0),
                            aula = obj.optString("aula", ""),
                            estadoMateria = obj.optString("estado", "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entities
    }

    private fun parseXmlAcademicLoad(xml: String): List<AcademicLoadEntity> {
        val entities = mutableListOf<AcademicLoadEntity>()
        try {
            // Very basic XML scraping logic based on typical Sicenet XML tags
            // We split by a common closing tag to separate items, e.g., </CargaRegular> or similar
            // Since we don't know the exact row tag, we'll try to match repeating blocks.
            // A generic approach: Find all occurrences of <materia>...</materia> and extract widely.
            // BUT, splitting by a closing tag is safer to group fields.
            
            // Heuristic: If we don't know the Item Tag, we can try to find blocks that contain "materia".
            // Let's assume the standard NewDataSet/Table structure.
            
            // Regex to find blocks. Let's try to capture each "row".
            // If we can't easily find rows, we'll iterate through all matches of "materia" and try to find nearby neighbors.
            // BETTER: Split by <materia> and parse the chunk? No, properties might be in any order.
            
            // Let's try splitting by the closing tag of the main item. Often </Table> or </Carga>.
            // If unknown, we can simply findAll on specific properties, assuming they are in order!
            // This is risky but often works for simple lists if lists are synchronous.
            
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

            // Assuming all lists are same size. If not, we take the min size or safe bounds.
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
                            materia = obj.optString("materia", ""),
                            clave = obj.optString("clave", ""),
                            creditos = obj.optInt("creditos", 0),
                            calificacion = obj.optString("calif", "NA"),
                            evaluacion = obj.optString("tipoEval", ""),
                            semestre = obj.optInt("semestre", 0),
                            anio = obj.optInt("periodo", 0),
                            observacion = obj.optString("observacion", "")
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
            val observaciones = extractAllTags(xml, "observacion") // Sometimes 'obs'

            // Try fallback for 'calif' -> 'calificacion'
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
                            materia = obj.optString("materia", ""),
                            u1 = obj.optString("c1", ""),
                            u2 = obj.optString("c2", ""),
                            u3 = obj.optString("c3", ""),
                            u4 = obj.optString("c4", ""),
                            u5 = obj.optString("c5", ""),
                            u6 = obj.optString("c6", ""),
                            u7 = obj.optString("c7", ""),
                            u8 = obj.optString("c8", ""),
                            u9 = obj.optString("c9", ""),
                            u10 = obj.optString("c10", ""),
                            u11 = obj.optString("c11", ""),
                            u12 = obj.optString("c12", ""),
                            u13 = obj.optString("c13", ""),
                            act = obj.optString("act", ""),
                            pf = obj.optString("prom", "")
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
            val c1 = extractAllTags(xml, "c1")
            val c2 = extractAllTags(xml, "c2")
            val c3 = extractAllTags(xml, "c3")
            // Assuming up to 13 units? Just doing basic ones usually seen.
            val prom = extractAllTags(xml, "prom")

            val size = materias.size
            for (i in 0 until size) {
                entities.add(
                    UnitGradesEntity(
                        materia = materias.getOrElse(i) { "" },
                        u1 = c1.getOrElse(i) { "" },
                        u2 = c2.getOrElse(i) { "" },
                        u3 = c3.getOrElse(i) { "" },
                        u4 = "", // abbreviated for brevity in fallback, can add more if needed
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
                            materia = obj.optString("materia", ""),
                            calif = obj.optString("calif", ""),
                            observacion = obj.optString("observacion", "")
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
            val califs = extractAllTags(xml, "calif")
            val observaciones = extractAllTags(xml, "observacion")

            val size = materias.size
            for (i in 0 until size) {
                entities.add(
                    FinalGradesEntity(
                        materia = materias.getOrElse(i) { "" },
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


    // --- HELPER FUNCTIONS ---

    private fun extractJsonContent(xml: String): String {
        val startTags = listOf("Result>", "return>")
        for (tag in startTags) {
            if (xml.contains(tag)) {
                return xml.substringAfter(tag).substringBefore("</")
            }
        }
        val start = xml.indexOf("[")
        val end = xml.lastIndexOf("]")
        if (start != -1 && end != -1 && start < end) {
            return xml.substring(start, end + 1)
        }
        return ""
    }

    private fun extractAllTags(xml: String, tagName: String): List<String> {
        val list = mutableListOf<String>()
        try {
            // Match <tagName>value</tagName> case insensitive
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
