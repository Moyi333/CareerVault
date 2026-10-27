package com.juani.careervault

val POSITIONS = listOf("PT", "LI", "LD", "CT", "CAI", "CAD", "MCD", "MC", "MI", "MD", "MP", "EI", "ED", "SD", "DL")
val ROLES = listOf("Clave", "Importante", "Rotación", "Suplente", "Promesa")

/** Esquemas del juego (de atrás hacia adelante). Cada fila son los códigos de posición. */
val FORMATIONS: LinkedHashMap<String, String> = linkedMapOf(
    "3-1-4-2" to "CT CT CT/MCD/MI MC MC MD/DL DL",
    "3-4-1-2" to "CT CT CT/MI MC MC MD/MP/DL DL",
    "3-4-2-1" to "CT CT CT/MI MC MC MD/MP MP/DL",
    "3-4-3" to "CT CT CT/MI MC MC MD/EI DL ED",
    "3-5-2" to "CT CT CT/CAI MCD MCD CAD/MP/DL DL",
    "4-1-2-1-2" to "LI CT CT LD/MCD/MC MC/MP/DL DL",
    "4-1-3-2" to "LI CT CT LD/MCD/MI MC MD/DL DL",
    "4-1-4-1" to "LI CT CT LD/MCD/MI MC MC MD/DL",
    "4-2-2-2" to "LI CT CT LD/MCD MCD/MP MP/DL DL",
    "4-2-3-1" to "LI CT CT LD/MCD MCD/MI MP MD/DL",
    "4-2-4" to "LI CT CT LD/MC MC/EI DL DL ED",
    "4-3-1-2" to "LI CT CT LD/MC MC MC/MP/DL DL",
    "4-3-2-1" to "LI CT CT LD/MC MC MC/MP MP/DL",
    "4-3-3" to "LI CT CT LD/MC MC MC/EI DL ED",
    "4-3-3 (2)" to "LI CT CT LD/MCD/MC MC/EI DL ED",
    "4-4-1-1" to "LI CT CT LD/MI MC MC MD/MP/DL",
    "4-4-2" to "LI CT CT LD/MI MC MC MD/DL DL",
    "4-4-2 (2)" to "LI CT CT LD/MI MCD MCD MD/DL DL",
    "4-5-1" to "LI CT CT LD/MI MC MC MC MD/DL",
    "5-2-1-2" to "CAI CT CT CT CAD/MC MC/MP/DL DL",
    "5-2-3" to "CAI CT CT CT CAD/MC MC/EI DL ED",
    "5-3-2" to "CAI CT CT CT CAD/MC MC MC/DL DL",
    "5-4-1" to "CAI CT CT CT CAD/MI MC MC MD/DL"
)

/** Devuelve (código, x, y) con x e y entre 0 y 1; el arco está abajo y el ataque arriba. */
fun layout(formation: String): List<Triple<String, Float, Float>> {
    val rows = (FORMATIONS[formation] ?: FORMATIONS.values.first()).split("/")
    val out = ArrayList<Triple<String, Float, Float>>()
    out.add(Triple("PT", 0.5f, 0.92f))
    val n = rows.size
    rows.forEachIndexed { i, r ->
        val codes = r.split(" ")
        val y = if (n == 1) 0.45f else 0.76f - i * (0.64f / (n - 1))
        codes.forEachIndexed { j, c -> out.add(Triple(c, (j + 1f) / (codes.size + 1f), y)) }
    }
    return out
}
