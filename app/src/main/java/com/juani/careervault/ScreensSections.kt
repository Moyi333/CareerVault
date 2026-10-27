package com.juani.careervault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun NumField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    Field(label, value, modifier, KeyboardType.Number) { v -> onChange(v.filter { ch -> ch.isDigit() }.take(4)) }
}

private fun money(s: String): Long = ((s.replace(',', '.').toDoubleOrNull() ?: 0.0) * 1_000_000).toLong()
private fun moneyText(v: Long): String = if (v > 0) trimNum(v / 1_000_000.0) else ""

/* ───────────────────────── TRANSFERENCIAS ───────────────────────── */

val TRANSFER_TYPES = listOf("Alta: compra", "Alta: préstamo", "Alta: libre", "Baja: venta", "Baja: préstamo", "Baja: libre", "Baja: retiro")

@Composable
fun Highlight(title: String, e: Entry, color: Color) {
    NoteCard(accent = color) {
        Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.name, fontWeight = FontWeight.Bold)
                Text(listOf(e.other, e.season).filter { it.isNotBlank() }.joinToString(" · "), color = CvMuted, fontSize = 12.sp)
            }
            Text(fmtMoney(e.amount), color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
fun TransfersScreen(vm: AppVm, stintId: Long, nav: NavController) {
    val list by remember(stintId) { vm.dao.entries(stintId, "TRANSFER") }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    val altas = list.filter { it.type.startsWith("Alta") }
    val bajas = list.filter { it.type.startsWith("Baja") }
    val gastado = altas.sumOf { it.amount }
    val ingresado = bajas.sumOf { it.amount }
    val topBuy = altas.filter { it.type == "Alta: compra" && it.amount > 0 }.maxByOrNull { it.amount }
    val topSale = bajas.filter { it.type == "Baja: venta" && it.amount > 0 }.maxByOrNull { it.amount }

    ScreenScaffold(
        title = "Transferencias",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ AGREGAR") { editing = null; open = true } }
    ) {
        item {
            NoteCard {
                Text("RESUMEN · TODAS LAS TEMPORADAS", color = CvMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Tiles(listOf(
                    Triple("Gastado", fmtMoney(gastado), CvRed),
                    Triple("Ingresado", fmtMoney(ingresado), CvGreen),
                    Triple("Balance", fmtMoney(ingresado - gastado), if (ingresado >= gastado) CvGreen else CvRed)
                ))
                Spacer(Modifier.height(10.dp))
                TRANSFER_TYPES.forEach { t ->
                    val l = list.filter { it.type == t }
                    if (l.isNotEmpty()) InfoLine(t, "${l.size} · ${fmtMoney(l.sumOf { it.amount })}")
                }
            }
        }
        if (topBuy != null) item { Highlight("COMPRA MÁS CARA", topBuy, CvRed) }
        if (topSale != null) item { Highlight("VENTA MÁS CARA", topSale, CvGreen) }
        groupedBySeason(
            list, { it.season },
            { g ->
                val gs = g.filter { it.type.startsWith("Alta") }.sumOf { it.amount }
                val ig = g.filter { it.type.startsWith("Baja") }.sumOf { it.amount }
                "Gastado ${fmtMoney(gs)} · Ingresado ${fmtMoney(ig)} · Balance ${fmtMoney(ig - gs)}"
            }
        ) { e ->
            val alta = e.type.startsWith("Alta")
            EntryRow(
                accent = if (alta) CvRed else CvGreen,
                onClick = { editing = e; open = true },
                onDelete = { vm.deleteEntry(e) },
                trailing = { if (e.amount > 0) Text(fmtMoney(e.amount), fontWeight = FontWeight.Bold, color = if (alta) CvRed else CvGreen) }
            ) {
                Text(e.name, fontWeight = FontWeight.Bold)
                Text(e.type, color = CvMuted, fontSize = 12.sp)
                if (e.other.isNotBlank()) Text((if (alta) "Club procedente: " else "Club destino: ") + e.other, color = CvMuted, fontSize = 12.sp)
            }
        }
    }

    if (open) {
        val init = editing
        var season by remember { mutableStateOf(init?.season ?: "") }
        var name by remember { mutableStateOf(init?.name ?: "") }
        var type by remember { mutableStateOf(init?.type ?: TRANSFER_TYPES[0]) }
        var amount by remember { mutableStateOf(moneyText(init?.amount ?: 0)) }
        var club by remember { mutableStateOf(init?.other ?: "") }
        val retiro = type == "Baja: retiro"
        FormDialog(
            title = if (init == null) "Nueva transferencia" else "Editar transferencia",
            canSave = name.isNotBlank() && season.isNotBlank(),
            onDismiss = { open = false },
            onSave = {
                vm.saveEntry(
                    (init ?: Entry(stintId = stintId, category = "TRANSFER")).copy(
                        season = season.trim(), name = name.trim(), type = type,
                        amount = if (retiro) 0 else money(amount), other = if (retiro) "" else club.trim()
                    )
                )
                open = false
            }
        ) {
            SeasonField(season, list.map { it.season }) { season = it }
            Field("Jugador", name) { name = it }
            ChipRow(TRANSFER_TYPES, type) { type = it }
            if (!retiro) {
                Field("Valor (millones de €, ej. 12.5)", amount, kb = KeyboardType.Decimal) { amount = it }
                Field(if (type.startsWith("Alta")) "Club procedente" else "Club destino", club) { club = it }
            }
        }
    }
}

/* ───────────────────────── TÍTULOS ───────────────────────── */

@Composable
fun TrophyCount(league: Boolean, label: String, n: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TrophyIcon(league, 52.dp)
        Text("$n", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CvGold)
        Text(label, color = CvMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TitlesScreen(vm: AppVm, stintId: Long, nav: NavController) {
    val list by remember(stintId) { vm.dao.entries(stintId, "TITLE") }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }

    ScreenScaffold(
        title = "Títulos",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ AGREGAR") { editing = null; open = true } }
    ) {
        item {
            NoteCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TrophyCount(true, "LIGAS GANADAS", list.count { it.type == "LIGA" })
                    TrophyCount(false, "COPAS GANADAS", list.count { it.type == "COPA" })
                }
            }
        }
        groupedBySeason(list, { it.season }, { g -> "${g.count { it.type == "LIGA" }} ligas · ${g.count { it.type == "COPA" }} copas" }) { e ->
            EntryRow(
                onClick = { editing = e; open = true },
                onDelete = { vm.deleteEntry(e) },
                leading = { TrophyIcon(e.type == "LIGA", 36.dp) }
            ) {
                Text(e.name, fontWeight = FontWeight.Bold)
                Text(e.type, color = CvGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (open) {
        val init = editing
        var season by remember { mutableStateOf(init?.season ?: "") }
        var name by remember { mutableStateOf(init?.name ?: "") }
        var kind by remember { mutableStateOf(init?.type ?: "LIGA") }
        FormDialog(
            title = if (init == null) "Nuevo título" else "Editar título",
            canSave = name.isNotBlank() && season.isNotBlank(),
            onDismiss = { open = false },
            onSave = {
                vm.saveEntry((init ?: Entry(stintId = stintId, category = "TITLE")).copy(season = season.trim(), name = name.trim(), type = kind))
                open = false
            }
        ) {
            SeasonField(season, list.map { it.season }) { season = it }
            Field("Título / competición", name) { name = it }
            ChipRow(listOf("LIGA", "COPA"), kind) { kind = it }
        }
    }
}

/* ───────────────────────── FINALES ───────────────────────── */

@Composable
fun FinalsScreen(vm: AppVm, stintId: Long, nav: NavController) {
    val list by remember(stintId) { vm.dao.entries(stintId, "FINAL") }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    val won = list.count { it.type == "Ganada" }

    ScreenScaffold(
        title = "Finales",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ AGREGAR") { editing = null; open = true } }
    ) {
        item {
            NoteCard {
                Tiles(listOf(Triple("Jugadas", "${list.size}", CvText), Triple("Ganadas", "$won", CvGreen), Triple("Perdidas", "${list.size - won}", CvRed)))
            }
        }
        groupedBySeason(list, { it.season }, { g -> "${g.count { it.type == "Ganada" }} ganadas · ${g.count { it.type != "Ganada" }} perdidas" }) { e ->
            val w = e.type == "Ganada"
            EntryRow(
                accent = if (w) CvGreen else CvRed,
                onClick = { editing = e; open = true },
                onDelete = { vm.deleteEntry(e) },
                trailing = { Text("${e.gf} - ${e.ga}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (w) CvGreen else CvRed) }
            ) {
                Text(e.name, fontWeight = FontWeight.Bold)
                Text("vs ${e.other}", color = CvMuted, fontSize = 13.sp)
                if (e.note.isNotBlank()) Text(e.note, color = CvMuted, fontSize = 12.sp)
            }
        }
    }

    if (open) {
        val init = editing
        var season by remember { mutableStateOf(init?.season ?: "") }
        var name by remember { mutableStateOf(init?.name ?: "") }
        var rival by remember { mutableStateOf(init?.other ?: "") }
        var gf by remember { mutableStateOf(init?.gf?.toString() ?: "") }
        var ga by remember { mutableStateOf(init?.ga?.toString() ?: "") }
        var res by remember { mutableStateOf(init?.type ?: "Ganada") }
        var note by remember { mutableStateOf(init?.note ?: "") }
        FormDialog(
            title = if (init == null) "Nueva final" else "Editar final",
            canSave = name.isNotBlank() && season.isNotBlank(),
            onDismiss = { open = false },
            onSave = {
                vm.saveEntry(
                    (init ?: Entry(stintId = stintId, category = "FINAL")).copy(
                        season = season.trim(), name = name.trim(), other = rival.trim(),
                        gf = gf.n(), ga = ga.n(), type = res, note = note.trim()
                    )
                )
                open = false
            }
        ) {
            SeasonField(season, list.map { it.season }) { season = it }
            Field("Competición", name) { name = it }
            Field("Rival", rival) { rival = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Goles míos", gf, Modifier.weight(1f)) { gf = it }
                NumField("Goles rival", ga, Modifier.weight(1f)) { ga = it }
            }
            ChipRow(listOf("Ganada", "Perdida"), res) { res = it }
            Field("Nota (ej. penales 4-3)", note) { note = it }
        }
    }
}

/* ───────────────────────── CLÁSICOS ───────────────────────── */

data class Rec(val j: Int, val g: Int, val e: Int, val p: Int, val gf: Int, val gc: Int)

fun rec(l: List<Entry>) = Rec(
    l.size, l.count { it.gf > it.ga }, l.count { it.gf == it.ga }, l.count { it.gf < it.ga },
    l.sumOf { it.gf }, l.sumOf { it.ga }
)

@Composable
fun RecTiles(r: Rec) {
    Tiles(listOf(Triple("Jugados", "${r.j}", CvText), Triple("Ganados", "${r.g}", CvGreen), Triple("Empatados", "${r.e}", CvOrange), Triple("Perdidos", "${r.p}", CvRed)))
    Spacer(Modifier.height(6.dp))
    Text("Goles: ${r.gf} a favor · ${r.gc} en contra", color = CvMuted, fontSize = 12.sp)
}

@Composable
fun RivalDialog(initial: Rival?, stintId: Long, national: Boolean, onDismiss: () -> Unit, onSave: (Rival) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var crest by remember { mutableStateOf(initial?.crest ?: "") }
    FormDialog(
        title = if (initial == null) "Agregar clásico" else "Editar clásico",
        canSave = name.isNotBlank(),
        onDismiss = onDismiss,
        onSave = { onSave((initial ?: Rival(stintId = stintId, name = "")).copy(name = name.trim(), crest = crest)) }
    ) {
        Field(if (national) "Selección clásica" else "Club clásico", name) { name = it }
        CrestPicker(name, national, crest) { crest = it }
        if (crest.isBlank()) Text("Si no elegís escudo, lo busco solo en internet al guardar.", color = CvMuted, fontSize = 12.sp)
    }
}

@Composable
fun DerbiesScreen(vm: AppVm, stintId: Long, nav: NavController) {
    val stint by remember(stintId) { vm.dao.stint(stintId) }.collectAsState(null)
    val rivals by remember(stintId) { vm.dao.rivals(stintId) }.collectAsState(emptyList())
    val games by remember(stintId) { vm.dao.entries(stintId, "DERBY") }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    val national = stint?.kind == "NT"

    ScreenScaffold(
        title = "Clásicos",
        subtitle = "Recuento general",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ AGREGAR CLÁSICO") { open = true } }
    ) {
        item { NoteCard { RecTiles(rec(games)) } }
        if (rivals.isEmpty()) item { Text("Agregá tus clásicos (cada uno es una subnota con su propio recuento).", color = CvMuted) }
        items(rivals, key = { it.id }) { r ->
            val x = rec(games.filter { it.refId == r.id })
            NoteCard(onClick = { nav.navigate("rival/${r.id}") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Crest(r.crest, r.name, 52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("J ${x.j} · G ${x.g} · E ${x.e} · P ${x.p}", color = CvMuted, fontSize = 13.sp)
                    }
                    Text("›", color = CvMuted, fontSize = 24.sp)
                }
            }
        }
    }
    if (open) RivalDialog(null, stintId, national, onDismiss = { open = false }) { vm.saveRival(it, national); open = false }
}

@Composable
fun RivalScreen(vm: AppVm, rivalId: Long, nav: NavController) {
    val rival by remember(rivalId) { vm.dao.rival(rivalId) }.collectAsState(null)
    val list by remember(rivalId) { vm.dao.entriesByRef(rivalId, "DERBY") }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var editRival by remember { mutableStateOf(false) }
    val r = rival
    if (r == null) { Box(Modifier.fillMaxSize()); return }
    val stint by remember(r.stintId) { vm.dao.stint(r.stintId) }.collectAsState(null)
    val national = stint?.kind == "NT"

    ScreenScaffold(
        title = r.name,
        subtitle = "Clásico",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ AGREGAR PARTIDO") { editing = null; open = true } },
        actions = {
            Text("✎", color = CvGreen, fontSize = 20.sp, modifier = Modifier.clickable { editRival = true }.padding(10.dp))
            DeleteIcon("Se borra el clásico con todos sus partidos.") { vm.deleteRival(r); nav.popBackStack() }
        }
    ) {
        item {
            NoteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Crest(r.crest, r.name, 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("Recuento contra ${r.name}", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                RecTiles(rec(list))
            }
        }
        groupedBySeason(list, { it.season }, { g -> val x = rec(g); "G ${x.g} · E ${x.e} · P ${x.p}" }) { e ->
            val col = if (e.gf > e.ga) CvGreen else if (e.gf < e.ga) CvRed else CvOrange
            EntryRow(
                accent = col,
                onClick = { editing = e; open = true },
                onDelete = { vm.deleteEntry(e) },
                trailing = { Text("${e.gf} - ${e.ga}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = col) }
            ) {
                Text(e.name.ifBlank { "Partido" }, fontWeight = FontWeight.Bold)
                Text(if (e.gf > e.ga) "Victoria" else if (e.gf < e.ga) "Derrota" else "Empate", color = CvMuted, fontSize = 12.sp)
            }
        }
    }

    if (open) {
        val init = editing
        var season by remember { mutableStateOf(init?.season ?: "") }
        var comp by remember { mutableStateOf(init?.name ?: "") }
        var gf by remember { mutableStateOf(init?.gf?.toString() ?: "") }
        var ga by remember { mutableStateOf(init?.ga?.toString() ?: "") }
        FormDialog(
            title = if (init == null) "Nuevo partido" else "Editar partido",
            canSave = season.isNotBlank() && gf.isNotBlank() && ga.isNotBlank(),
            onDismiss = { open = false },
            onSave = {
                vm.saveEntry((init ?: Entry(stintId = r.stintId, category = "DERBY", refId = r.id)).copy(season = season.trim(), name = comp.trim(), gf = gf.n(), ga = ga.n()))
                open = false
            }
        ) {
            SeasonField(season, list.map { it.season }) { season = it }
            Field("Competición (opcional)", comp) { comp = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Goles míos", gf, Modifier.weight(1f)) { gf = it }
                NumField("Goles rival", ga, Modifier.weight(1f)) { ga = it }
            }
        }
    }
    if (editRival) RivalDialog(r, r.stintId, national, onDismiss = { editRival = false }) { vm.saveRival(it, national); editRival = false }
}

/* ───────────────────────── PIBES DEL CLUB ───────────────────────── */

@Composable
fun PromotedScreen(vm: AppVm, stintId: Long, nav: NavController) {
    val list by remember(stintId) { vm.dao.entries(stintId, "PROMOTED") }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }

    ScreenScaffold(
        title = "Pibes del club",
        subtitle = "Ascendidos de la cantera",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ AGREGAR") { editing = null; open = true } }
    ) {
        item { NoteCard { Tiles(listOf(Triple("Pibes ascendidos", "${list.size}", CvGreen))) } }
        groupedBySeason(list, { it.season }, { g -> "${g.size} ascendidos" }) { e ->
            EntryRow(
                onClick = { editing = e; open = true },
                onDelete = { vm.deleteEntry(e) },
                trailing = { if (e.position.isNotBlank()) Pill(e.position) }
            ) {
                Text(e.name, fontWeight = FontWeight.Bold)
                if (e.extra.isNotBlank()) Text("Nació: ${e.extra}", color = CvMuted, fontSize = 12.sp)
            }
        }
    }

    if (open) {
        val init = editing
        var season by remember { mutableStateOf(init?.season ?: "") }
        var name by remember { mutableStateOf(init?.name ?: "") }
        var birth by remember { mutableStateOf(init?.extra ?: "") }
        var pos by remember { mutableStateOf(init?.position ?: "") }
        FormDialog(
            title = if (init == null) "Nuevo pibe" else "Editar pibe",
            canSave = name.isNotBlank() && season.isNotBlank(),
            onDismiss = { open = false },
            onSave = {
                vm.saveEntry((init ?: Entry(stintId = stintId, category = "PROMOTED")).copy(season = season.trim(), name = name.trim(), extra = birth.trim(), position = pos.trim()))
                open = false
            }
        ) {
            SeasonField(season, list.map { it.season }) { season = it }
            Field("Jugador", name) { name = it }
            Field("Fecha de nacimiento (dd/mm/aaaa)", birth) { birth = it }
            Field("Posición", pos) { pos = it }
            ChipRow(POSITIONS, pos) { pos = it }
        }
    }
}

/* ───────────────────────── RETIROS ───────────────────────── */

@Composable
fun RetiredScreen(vm: AppVm, stintId: Long, nav: NavController) {
    val list by remember(stintId) { vm.dao.entries(stintId, "RETIRED") }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }

    ScreenScaffold(
        title = "Retiros",
        subtitle = "Jugadores retirados",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ AGREGAR") { editing = null; open = true } }
    ) {
        item { NoteCard { Tiles(listOf(Triple("Retirados", "${list.size}", CvGreen))) } }
        items(list, key = { it.id }) { e ->
            EntryRow(
                onClick = { editing = e; open = true },
                onDelete = { vm.deleteEntry(e) },
                trailing = { if (e.position.isNotBlank()) Pill(e.position) }
            ) {
                Text(e.name, fontWeight = FontWeight.Bold)
                if (e.extra.isNotBlank()) Text("Temporadas jugadas: ${e.extra}", color = CvMuted, fontSize = 12.sp)
                if (e.note.isNotBlank()) Text("Títulos: ${e.note}", color = CvGold, fontSize = 12.sp)
            }
        }
    }

    if (open) {
        val init = editing
        val parts = (init?.extra ?: "").split(" – ")
        var name by remember { mutableStateOf(init?.name ?: "") }
        var from by remember { mutableStateOf(parts.getOrElse(0) { "" }) }
        var to by remember { mutableStateOf(parts.getOrElse(1) { "" }) }
        var pos by remember { mutableStateOf(init?.position ?: "") }
        var titles by remember { mutableStateOf(init?.note ?: "") }
        FormDialog(
            title = if (init == null) "Nuevo retiro" else "Editar retiro",
            canSave = name.isNotBlank(),
            onDismiss = { open = false },
            onSave = {
                val span = if (from.isBlank() && to.isBlank()) "" else "${from.trim()} – ${to.trim()}"
                vm.saveEntry((init ?: Entry(stintId = stintId, category = "RETIRED")).copy(name = name.trim(), extra = span, position = pos.trim(), note = titles.trim()))
                open = false
            }
        ) {
            Field("Jugador", name) { name = it }
            Text("TEMPORADAS JUGADAS", color = CvMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Desde (2008/09)", from, Modifier.weight(1f)) { from = it }
                Field("Hasta (2024/25)", to, Modifier.weight(1f)) { to = it }
            }
            Field("Posición", pos) { pos = it }
            ChipRow(POSITIONS, pos) { pos = it }
            Field("Títulos (ej. 3 ligas, 2 copas)", titles) { titles = it }
        }
    }
}

/* ───────────────────────── ESTADÍSTICAS ───────────────────────── */

@Composable
fun StatPlayerDialog(initial: StatPlayer?, stintId: Long, onDismiss: () -> Unit, onSave: (StatPlayer) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var gk by remember { mutableStateOf(initial?.gk ?: false) }
    FormDialog(
        title = if (initial == null) "Nuevo jugador" else "Editar jugador",
        canSave = name.isNotBlank(),
        onDismiss = onDismiss,
        onSave = { onSave((initial ?: StatPlayer(stintId = stintId, name = "")).copy(name = name.trim(), gk = gk)) }
    ) {
        Field("Nombre del jugador", name) { name = it }
        ChipRow(listOf("JUGADOR DE CAMPO", "ARQUERO"), if (gk) "ARQUERO" else "JUGADOR DE CAMPO") { gk = it == "ARQUERO" }
    }
}

@Composable
fun StatsScreen(vm: AppVm, stintId: Long, nav: NavController) {
    val players by remember(stintId) { vm.dao.statPlayers(stintId) }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Estadísticas",
        subtitle = "Una subnota por jugador",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ AGREGAR JUGADOR") { open = true } }
    ) {
        if (players.isEmpty()) item { Text("Agregá un jugador para llevar sus estadísticas por temporada.", color = CvMuted) }
        items(players, key = { it.id }) { p ->
            NoteCard(onClick = { nav.navigate("stat/${p.id}") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Pill(if (p.gk) "ARQUERO" else "JUGADOR DE CAMPO", if (p.gk) CvOrange else CvGreen)
                    }
                    Text("›", color = CvMuted, fontSize = 24.sp)
                }
            }
        }
    }
    if (open) StatPlayerDialog(null, stintId, onDismiss = { open = false }) { p -> vm.launch { vm.dao.insertStatPlayer(p) }; open = false }
}

@Composable
fun StatPlayerScreen(vm: AppVm, playerId: Long, nav: NavController) {
    val player by remember(playerId) { vm.dao.statPlayer(playerId) }.collectAsState(null)
    val list by remember(playerId) { vm.dao.entriesByRef(playerId, "STAT") }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var editPlayer by remember { mutableStateOf(false) }
    val p = player
    if (p == null) { Box(Modifier.fillMaxSize()); return }

    fun line(l: List<Entry>): String =
        if (p.gk) "PJ ${l.sumOf { it.matches }} · Imbatidas ${l.sumOf { it.clean }}"
        else "PJ ${l.sumOf { it.matches }} · G ${l.sumOf { it.goals }} · A ${l.sumOf { it.assists }}"

    ScreenScaffold(
        title = p.name,
        subtitle = if (p.gk) "Arquero" else "Jugador de campo",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ TEMPORADA") { editing = null; open = true } },
        actions = {
            Text("✎", color = CvGreen, fontSize = 20.sp, modifier = Modifier.clickable { editPlayer = true }.padding(10.dp))
            DeleteIcon("Se borra el jugador con todas sus temporadas.") { vm.deleteStatPlayer(p); nav.popBackStack() }
        }
    ) {
        item {
            NoteCard {
                Text("TOTAL DE TODAS LAS TEMPORADAS", color = CvMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (p.gk) Tiles(listOf(Triple("Partidos", "${list.sumOf { it.matches }}", CvText), Triple("Porterías imbatidas", "${list.sumOf { it.clean }}", CvGreen)))
                else Tiles(listOf(Triple("Partidos", "${list.sumOf { it.matches }}", CvText), Triple("Goles", "${list.sumOf { it.goals }}", CvGreen), Triple("Asistencias", "${list.sumOf { it.assists }}", CvBlue)))
            }
        }
        groupedBySeason(list, { it.season }, { g -> line(g) }) { e ->
            EntryRow(onClick = { editing = e; open = true }, onDelete = { vm.deleteEntry(e) }) {
                Text(line(listOf(e)), fontWeight = FontWeight.Medium)
            }
        }
    }

    if (open) {
        val init = editing
        var season by remember { mutableStateOf(init?.season ?: "") }
        var pj by remember { mutableStateOf(init?.matches?.toString() ?: "") }
        var g by remember { mutableStateOf(init?.goals?.toString() ?: "") }
        var a by remember { mutableStateOf(init?.assists?.toString() ?: "") }
        var c by remember { mutableStateOf(init?.clean?.toString() ?: "") }
        FormDialog(
            title = if (init == null) "Nueva temporada" else "Editar temporada",
            canSave = season.isNotBlank(),
            onDismiss = { open = false },
            onSave = {
                vm.saveEntry(
                    (init ?: Entry(stintId = p.stintId, category = "STAT", refId = p.id)).copy(
                        season = season.trim(), matches = pj.n(),
                        goals = if (p.gk) 0 else g.n(), assists = if (p.gk) 0 else a.n(), clean = if (p.gk) c.n() else 0
                    )
                )
                open = false
            }
        ) {
            SeasonField(season, list.map { it.season }) { season = it }
            NumField("Partidos", pj) { pj = it }
            if (p.gk) NumField("Porterías imbatidas", c) { c = it }
            else {
                NumField("Goles", g) { g = it }
                NumField("Asistencias", a) { a = it }
            }
        }
    }
    if (editPlayer) StatPlayerDialog(p, p.stintId, onDismiss = { editPlayer = false }) { np -> vm.launch { vm.dao.updateStatPlayer(np) }; editPlayer = false }
}
