package com.juani.careervault

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

fun yearsText(s: Stint): String =
    if (s.yearFrom.isBlank()) "" else "${s.yearFrom} – ${s.yearTo.ifBlank { "actualidad" }}"

@Composable
fun CareersTab(vm: AppVm, nav: NavController) {
    val careers by remember { vm.dao.careers() }.collectAsState(emptyList())
    val stints by remember { vm.dao.allStints() }.collectAsState(emptyList())
    var showNew by remember { mutableStateOf(false) }
    var dt by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Mis carreras",
        subtitle = "Tu bóveda de modo carrera",
        fab = { Fab("+ NUEVA CARRERA") { dt = ""; showNew = true } }
    ) {
        item {
            Image(
                painterResource(R.drawable.logo_app), contentDescription = "CareerVault",
                modifier = Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Fit
            )
        }
        if (careers.isEmpty()) {
            item {
                NoteCard {
                    Text("Todavía no hay carreras", fontWeight = FontWeight.Bold)
                    Text("Tocá “+ NUEVA CARRERA”, poné el nombre del DT y empezá a agregar clubes.", color = CvMuted)
                }
            }
        }
        items(careers, key = { it.id }) { c ->
            val mine = stints.filter { it.careerId == c.id }
            val clubs = mine.count { it.kind == "CLUB" }
            val nts = mine.count { it.kind == "NT" }
            NoteCard(onClick = { nav.navigate("career/${c.id}") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.dt, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$clubs ${if (clubs == 1) "club" else "clubes"} · $nts ${if (nts == 1) "selección" else "selecciones"}", color = CvMuted, fontSize = 13.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        mine.take(3).forEach { Crest(it.crest, it.name, 38.dp) }
                    }
                }
            }
        }
    }

    if (showNew) {
        FormDialog(
            title = "Nueva carrera",
            canSave = dt.isNotBlank(),
            onDismiss = { showNew = false },
            onSave = { vm.addCareer(dt.trim()) { id -> nav.navigate("career/$id") }; showNew = false }
        ) {
            Text("Nombre del DT", color = CvMuted, fontSize = 12.sp)
            Field("Nombre del DT", dt) { dt = it }
        }
    }
}

@Composable
fun ActionTile(emoji: String, title: String, sub: String, color: Color, onClick: () -> Unit) {
    NoteCard(accent = color, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = color)
                Text(sub, color = CvMuted, fontSize = 12.sp)
            }
            Text("›", color = color, fontSize = 24.sp)
        }
    }
}

@Composable
fun StintDialog(careerId: Long, kind: String, initial: Stint?, onDismiss: () -> Unit, onSave: (Stint) -> Unit) {
    val club = kind == "CLUB"
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var from by remember { mutableStateOf(initial?.yearFrom ?: "") }
    var to by remember { mutableStateOf(initial?.yearTo ?: "") }
    var crest by remember { mutableStateOf(initial?.crest ?: "") }
    FormDialog(
        title = if (initial == null) (if (club) "Agregar club" else "Agregar selección") else "Editar",
        canSave = name.isNotBlank() && from.length == 4,
        onDismiss = onDismiss,
        onSave = {
            onSave((initial ?: Stint(careerId = careerId, kind = kind, name = "")).copy(name = name.trim(), yearFrom = from, yearTo = to, crest = crest))
        }
    ) {
        Field(if (club) "Nombre del club" else "Nombre de la selección", name) { name = it }
        Text(if (club) "AÑOS DE TRABAJO EN EL CLUB" else "AÑOS DIRIGIDOS", color = CvMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Desde", from, Modifier.weight(1f), KeyboardType.Number) { v -> from = v.filter { ch -> ch.isDigit() }.take(4) }
            Field("Hasta (después)", to, Modifier.weight(1f), KeyboardType.Number) { v -> to = v.filter { ch -> ch.isDigit() }.take(4) }
        }
        Text("Escudo", color = CvMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        CrestPicker(name, !club, crest) { crest = it }
        if (crest.isBlank()) Text("Si no elegís uno, lo busco solo en internet al guardar.", color = CvMuted, fontSize = 12.sp)
    }
}

@Composable
fun StintCard(s: Stint, onClick: () -> Unit) {
    NoteCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Crest(s.crest, s.name, 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(yearsText(s), color = CvMuted, fontSize = 13.sp)
            }
            Text("›", color = CvMuted, fontSize = 24.sp)
        }
    }
}

@Composable
fun CareerScreen(vm: AppVm, id: Long, nav: NavController) {
    val career by remember(id) { vm.dao.career(id) }.collectAsState(null)
    val stints by remember(id) { vm.dao.stints(id) }.collectAsState(emptyList())
    var addKind by remember { mutableStateOf<String?>(null) }
    var rename by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val c = career
    if (c == null) { Box(Modifier.fillMaxSize()); return }

    ScreenScaffold(
        title = c.dt,
        subtitle = "Director técnico",
        onBack = { nav.popBackStack() },
        actions = {
            Text("✎", color = CvGreen, fontSize = 20.sp, modifier = Modifier.clickable { newName = c.dt; rename = true }.padding(10.dp))
            DeleteIcon("Se borra la carrera completa con todos sus clubes, selecciones y registros.") {
                vm.launch { vm.dao.deleteCareer(c) }
                nav.popBackStack()
            }
        }
    ) {
        item { ActionTile("➕", "AGREGAR CLUB", "Sumá un club con todos sus registros", CvGreen) { addKind = "CLUB" } }
        item { ActionTile("🌎", "AGREGAR SELECCIÓN", "Dirección de una selección", CvGreen) { addKind = "NT" } }
        item { ActionTile("🏅", "PREMIOS INDIVIDUALES DT", "Mejor DT, premios personales", CvGold) { nav.navigate("awards/$id") } }
        val clubs = stints.filter { it.kind == "CLUB" }
        val nts = stints.filter { it.kind == "NT" }
        if (clubs.isNotEmpty()) {
            item { SectionTitle("CLUBES") }
            items(clubs, key = { it.id }) { s -> StintCard(s) { nav.navigate("stint/${s.id}") } }
        }
        if (nts.isNotEmpty()) {
            item { SectionTitle("SELECCIONES") }
            items(nts, key = { it.id }) { s -> StintCard(s) { nav.navigate("stint/${s.id}") } }
        }
    }

    addKind?.let { kind ->
        StintDialog(id, kind, null, onDismiss = { addKind = null }) { vm.saveStint(it); addKind = null }
    }
    if (rename) {
        FormDialog(
            title = "Nombre del DT",
            canSave = newName.isNotBlank(),
            onDismiss = { rename = false },
            onSave = { vm.launch { vm.dao.updateCareer(c.copy(dt = newName.trim())) }; rename = false }
        ) { Field("Nombre del DT", newName) { newName = it } }
    }
}

private data class Sec(val route: String, val emoji: String, val title: String, val sub: String)

@Composable
fun StintScreen(vm: AppVm, id: Long, nav: NavController) {
    val stint by remember(id) { vm.dao.stint(id) }.collectAsState(null)
    val all by remember(id) { vm.dao.allEntries(id) }.collectAsState(emptyList())
    val rivals by remember(id) { vm.dao.rivals(id) }.collectAsState(emptyList())
    val players by remember(id) { vm.dao.statPlayers(id) }.collectAsState(emptyList())
    var edit by remember { mutableStateOf(false) }
    val s = stint
    if (s == null) { Box(Modifier.fillMaxSize()); return }
    val club = s.kind == "CLUB"
    fun cnt(cat: String) = all.count { it.category == cat }
    val titles = all.filter { it.category == "TITLE" }
    val tiles = buildList {
        if (club) add(Sec("transfers", "💸", "TRANSFERENCIAS", "${cnt("TRANSFER")} movimientos"))
        add(Sec("titles", "🏆", "TÍTULOS", "${titles.count { it.type == "LIGA" }} ligas · ${titles.count { it.type == "COPA" }} copas"))
        add(Sec("finals", "🥇", "FINALES", "${cnt("FINAL")} disputadas"))
        add(Sec("derbies", "🔥", "CLÁSICOS", "${rivals.size} rivales · ${cnt("DERBY")} partidos"))
        if (club) {
            add(Sec("promoted", "🌱", "PIBES DEL CLUB", "${cnt("PROMOTED")} ascendidos"))
            add(Sec("retired", "👴", "RETIROS", "${cnt("RETIRED")} retirados"))
            add(Sec("stats", "📊", "ESTADÍSTICAS", "${players.size} jugadores"))
        }
    }

    ScreenScaffold(
        title = s.name,
        subtitle = if (club) "Club" else "Selección",
        onBack = { nav.popBackStack() },
        actions = {
            Text("✎", color = CvGreen, fontSize = 20.sp, modifier = Modifier.clickable { edit = true }.padding(10.dp))
            DeleteIcon("Se borra ${s.name} con todos sus registros.") {
                vm.launch { vm.dao.deleteStint(s) }
                nav.popBackStack()
            }
        }
    ) {
        item {
            NoteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Crest(s.crest, s.name, 76.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(if (club) "AÑOS DE TRABAJO" else "AÑOS DIRIGIDOS", color = CvMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(yearsText(s), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CvGreen)
                        if (s.yearTo.isBlank()) Text("Tocá ✎ para cargar el año de fin", color = CvMuted, fontSize = 12.sp)
                    }
                }
            }
        }
        items(tiles, key = { it.route }) { t ->
            NoteCard(onClick = { nav.navigate("${t.route}/$id") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.emoji, fontSize = 26.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, fontWeight = FontWeight.Bold)
                        Text(t.sub, color = CvMuted, fontSize = 13.sp)
                    }
                    Text("›", color = CvMuted, fontSize = 24.sp)
                }
            }
        }
    }

    if (edit) StintDialog(s.careerId, s.kind, s, onDismiss = { edit = false }) { vm.saveStint(it); edit = false }
}

@Composable
fun AwardsScreen(vm: AppVm, careerId: Long, nav: NavController) {
    val list by remember(careerId) { vm.dao.awards(careerId) }.collectAsState(emptyList())
    val clubs by remember(careerId) { vm.dao.stints(careerId) }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Award?>(null) }

    ScreenScaffold(
        title = "Premios individuales",
        subtitle = "Del DT",
        onBack = { nav.popBackStack() },
        fab = { Fab("+ AGREGAR") { editing = null; open = true } }
    ) {
        if (list.isEmpty()) item { Text("Todavía no cargaste premios.", color = CvMuted) }
        items(list, key = { it.id }) { a ->
            EntryRow(
                accent = CvGold,
                onClick = { editing = a; open = true },
                onDelete = { vm.launch { vm.dao.deleteAward(a) } },
                leading = { Text("🏅", fontSize = 24.sp) }
            ) {
                Text(a.name, fontWeight = FontWeight.Bold)
                Text(listOf(a.club, a.season).filter { it.isNotBlank() }.joinToString(" · "), color = CvMuted, fontSize = 13.sp)
            }
        }
    }

    if (open) {
        val init = editing
        var name by remember { mutableStateOf(init?.name ?: "") }
        var club by remember { mutableStateOf(init?.club ?: "") }
        var season by remember { mutableStateOf(init?.season ?: "") }
        FormDialog(
            title = if (init == null) "Nuevo premio" else "Editar premio",
            canSave = name.isNotBlank(),
            onDismiss = { open = false },
            onSave = {
                val a = (init ?: Award(careerId = careerId, name = "")).copy(name = name.trim(), club = club.trim(), season = season.trim())
                vm.launch { if (a.id == 0L) vm.dao.insertAward(a) else vm.dao.updateAward(a) }
                open = false
            }
        ) {
            Field("Nombre del premio", name) { name = it }
            Field("Club", club) { club = it }
            val names = clubs.map { it.name }.distinct()
            if (names.isNotEmpty()) ChipRow(names, club) { club = it }
            SeasonField(season, list.map { it.season }) { season = it }
        }
    }
}
