package com.juani.careervault

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.roundToInt
import kotlin.random.Random

private val PitchBg = Color(0xFF343F45)
private val PitchLine = Color(0xFFFF6A13)

private fun tagColor(tag: String): Color? = when (tag) {
    "FUTURO" -> CvBlue
    "CESION" -> CvOrange
    else -> null
}

/* ───────────────────────── LISTA DE PLANTILLAS ───────────────────────── */

@Composable
fun SquadsTab(vm: AppVm, nav: NavController) {
    val squads by remember { vm.dao.squads() }.collectAsState(emptyList())
    val players by remember { vm.dao.allSquadPlayers() }.collectAsState(emptyList())
    var open by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var formation by remember { mutableStateOf("4-3-3") }

    ScreenScaffold(
        title = "Plantillas",
        subtitle = "Armá tus formaciones",
        fab = { Fab("+ NUEVA PLANTILLA") { name = ""; formation = "4-3-3"; open = true } }
    ) {
        if (squads.isEmpty()) {
            item {
                NoteCard {
                    Text("Todavía no armaste plantillas", fontWeight = FontWeight.Bold)
                    Text("Creá una, elegí el esquema y mové a los jugadores por la cancha.", color = CvMuted)
                }
            }
        }
        items(squads, key = { it.id }) { s ->
            val mine = players.filter { it.squadId == s.id }
            NoteCard(onClick = { nav.navigate("squad/${s.id}") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${s.formation} · ${mine.count { it.starter }} titulares · ${mine.count { !it.starter }} suplentes", color = CvMuted, fontSize = 13.sp)
                    }
                    Text("›", color = CvMuted, fontSize = 24.sp)
                }
            }
        }
    }

    if (open) {
        FormDialog(
            title = "Nueva plantilla",
            canSave = name.isNotBlank(),
            onDismiss = { open = false },
            onSave = { vm.createSquad(name.trim(), formation); open = false }
        ) {
            Field("Nombre de la plantilla", name) { name = it }
            Text("ESQUEMA", color = CvMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            ChipRow(FORMATIONS.keys.toList(), formation) { formation = it }
        }
    }
}

/* ───────────────────────── CANCHA ───────────────────────── */

private fun DrawScope.drawPitchLines() {
    val w = size.width
    val h = size.height
    val sw = 2.dp.toPx()
    val st = Stroke(sw)
    val m = w * 0.045f
    val left = m
    val top = m
    val right = w - m
    val bottom = h - m
    val fw = right - left
    val fh = bottom - top
    drawRect(PitchLine, Offset(left, top), Size(fw, fh), style = st)
    drawLine(PitchLine, Offset(left, h / 2), Offset(right, h / 2), strokeWidth = sw)
    drawCircle(PitchLine, w * 0.13f, Offset(w / 2, h / 2), style = st)
    drawCircle(PitchLine, sw * 1.5f, Offset(w / 2, h / 2))
    val pw = fw * 0.60f
    val ph = fh * 0.16f
    drawRect(PitchLine, Offset(w / 2 - pw / 2, top), Size(pw, ph), style = st)
    drawRect(PitchLine, Offset(w / 2 - pw / 2, bottom - ph), Size(pw, ph), style = st)
    val gw = fw * 0.30f
    val gh = fh * 0.06f
    drawRect(PitchLine, Offset(w / 2 - gw / 2, top), Size(gw, gh), style = st)
    drawRect(PitchLine, Offset(w / 2 - gw / 2, bottom - gh), Size(gw, gh), style = st)
    val pgw = fw * 0.14f
    val pgh = m * 0.55f
    drawRect(PitchLine, Offset(w / 2 - pgw / 2, top - pgh), Size(pgw, pgh), style = st)
    drawRect(PitchLine, Offset(w / 2 - pgw / 2, bottom), Size(pgw, pgh), style = st)
    drawCircle(PitchLine, sw, Offset(w / 2, top + fh * 0.11f))
    drawCircle(PitchLine, sw, Offset(w / 2, bottom - fh * 0.11f))
    val r = m * 0.6f
    drawArc(PitchLine, 0f, 90f, false, Offset(left - r, top - r), Size(2 * r, 2 * r), style = st)
    drawArc(PitchLine, 90f, 90f, false, Offset(right - r, top - r), Size(2 * r, 2 * r), style = st)
    drawArc(PitchLine, 270f, 90f, false, Offset(left - r, bottom - r), Size(2 * r, 2 * r), style = st)
    drawArc(PitchLine, 180f, 90f, false, Offset(right - r, bottom - r), Size(2 * r, 2 * r), style = st)
}

@Composable
fun Token(p: SquadPlayer, wPx: Float, hPx: Float, onTap: (Long) -> Unit, onMove: (Long, Float, Float) -> Unit) {
    var fx by remember(p.x) { mutableFloatStateOf(p.x) }
    var fy by remember(p.y) { mutableFloatStateOf(p.y) }
    val density = LocalDensity.current
    val tw = with(density) { 64.dp.toPx() }
    val cr = with(density) { 20.dp.toPx() }
    val color = tagColor(p.tag) ?: Color.White
    Column(
        Modifier
            .offset { IntOffset((fx * wPx - tw / 2).roundToInt(), (fy * hPx - cr).roundToInt()) }
            .width(64.dp)
            .pointerInput(p.id) { detectTapGestures(onTap = { onTap(p.id) }) }
            .pointerInput(p.id) {
                detectDragGestures(onDragEnd = { onMove(p.id, fx, fy) }) { change, drag ->
                    change.consume()
                    fx = (fx + drag.x / wPx).coerceIn(0.04f, 0.96f)
                    fy = (fy + drag.y / hPx).coerceIn(0.04f, 0.96f)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Text(p.pos, color = Color(0xFF263238), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        val label = "${p.number} ${p.name}".trim()
        if (label.isNotBlank()) {
            Text(label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun Pitch(starters: List<SquadPlayer>, onTap: (Long) -> Unit, onMove: (Long, Float, Float) -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(18.dp)).background(PitchBg)
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize()) { drawPitchLines() }
        starters.forEach { p -> key(p.id) { Token(p, wPx, hPx, onTap, onMove) } }
    }
}

/* ───────────────────────── EDITOR DE PLANTILLA ───────────────────────── */

@Composable
fun SquadPlayerDialog(
    initial: SquadPlayer?,
    squadId: Long,
    starterDefault: Boolean,
    onDismiss: () -> Unit,
    onSave: (SquadPlayer) -> Unit,
    onDelete: (SquadPlayer) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var number by remember { mutableStateOf(initial?.number ?: "") }
    var pos by remember { mutableStateOf(initial?.pos ?: "") }
    var role by remember { mutableStateOf(initial?.role ?: "") }
    var tag by remember { mutableStateOf(initial?.tag ?: "") }
    var starter by remember { mutableStateOf(initial?.starter ?: starterDefault) }
    val tagLabel = when (tag) { "FUTURO" -> "FUTURO FICHAJE"; "CESION" -> "VUELTA DE CESIÓN"; else -> "Normal" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Agregar jugador" else "Editar jugador", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Nombre", name) { name = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField("Número", number, Modifier.weight(1f)) { number = it }
                    Field("Posición", pos, Modifier.weight(1f)) { pos = it.uppercase() }
                }
                ChipRow(POSITIONS, pos) { pos = it }
                Field("Rol", role) { role = it }
                ChipRow(ROLES, role) { role = it }
                Text("ESTADO", color = CvMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                ChipRow(listOf("Normal", "FUTURO FICHAJE", "VUELTA DE CESIÓN"), tagLabel) {
                    tag = when (it) { "FUTURO FICHAJE" -> "FUTURO"; "VUELTA DE CESIÓN" -> "CESION"; else -> "" }
                }
                ChipRow(listOf("Titular", "Suplente"), if (starter) "Titular" else "Suplente") { starter = it == "Titular" }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val base = initial ?: SquadPlayer(
                    squadId = squadId,
                    x = 0.3f + Random.nextFloat() * 0.4f,
                    y = 0.35f + Random.nextFloat() * 0.3f
                )
                onSave(base.copy(name = name.trim(), number = number, pos = pos.trim(), role = role.trim(), tag = tag, starter = starter))
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { onDelete(initial) }) { Text("Borrar", color = CvRed) }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun SquadEditor(vm: AppVm, id: Long, nav: NavController) {
    val squad by remember(id) { vm.dao.squad(id) }.collectAsState(null)
    val players by remember(id) { vm.dao.squadPlayers(id) }.collectAsState(emptyList())
    var editingId by remember { mutableStateOf<Long?>(null) }
    var newStarter by remember { mutableStateOf(true) }
    var rename by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val sq = squad
    if (sq == null) { Box(Modifier.fillMaxSize()); return }
    val starters = players.filter { it.starter }
    val bench = players.filter { !it.starter }

    ScreenScaffold(
        title = sq.name,
        subtitle = "Esquema ${sq.formation}",
        onBack = { nav.popBackStack() },
        actions = {
            Text("✎", color = CvGreen, fontSize = 20.sp, modifier = Modifier.clickable { newName = sq.name; rename = true }.padding(10.dp))
            DeleteIcon("Se borra la plantilla completa.") { vm.launch { vm.dao.deleteSquad(sq) }; nav.popBackStack() }
        }
    ) {
        item { ChipRow(FORMATIONS.keys.toList(), sq.formation) { vm.applyFormation(sq, players, it) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Titulares ${starters.size}/11")
                Pill("Suplentes ${bench.size}/11", CvMuted)
                Pill("● Futuro fichaje", CvBlue)
                Pill("● Vuelta de cesión", CvOrange)
            }
        }
        item {
            Pitch(
                starters,
                onTap = { editingId = it },
                onMove = { pid, x, y -> vm.launch { vm.dao.moveSquadPlayer(pid, x, y) } }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { newStarter = true; editingId = 0L },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CvGreen, contentColor = Color.Black)
                ) { Text("+ TITULAR", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { newStarter = false; editingId = 0L },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CvSurface2, contentColor = CvGreen)
                ) { Text("+ SUPLENTE", fontWeight = FontWeight.Bold) }
            }
        }
        item { SectionTitle("SUPLENTES") }
        if (bench.isEmpty()) item { Text("Sin suplentes todavía.", color = CvMuted) }
        items(bench, key = { it.id }) { b ->
            EntryRow(
                accent = tagColor(b.tag),
                onClick = { editingId = b.id },
                onDelete = { vm.launch { vm.dao.deleteSquadPlayer(b) } },
                leading = {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(tagColor(b.tag) ?: Color.White), contentAlignment = Alignment.Center) {
                        Text(b.pos, color = Color(0xFF263238), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Text(listOf(b.number, b.name).filter { it.isNotBlank() }.joinToString("  ").ifBlank { "Sin nombre" }, fontWeight = FontWeight.Bold)
                Text(listOf(b.role, when (b.tag) { "FUTURO" -> "Futuro fichaje"; "CESION" -> "Vuelta de cesión"; else -> "" }).filter { it.isNotBlank() }.joinToString(" · "), color = CvMuted, fontSize = 12.sp)
            }
        }
    }

    editingId?.let { eid ->
        SquadPlayerDialog(
            initial = players.find { it.id == eid },
            squadId = id,
            starterDefault = newStarter,
            onDismiss = { editingId = null },
            onSave = { vm.saveSquadPlayer(it); editingId = null },
            onDelete = { vm.launch { vm.dao.deleteSquadPlayer(it) }; editingId = null }
        )
    }
    if (rename) {
        FormDialog(
            title = "Nombre de la plantilla",
            canSave = newName.isNotBlank(),
            onDismiss = { rename = false },
            onSave = { vm.launch { vm.dao.updateSquad(sq.copy(name = newName.trim())) }; rename = false }
        ) { Field("Nombre", newName) { newName = it } }
    }
}

/* ───────────────────────── GESTIÓN ───────────────────────── */

@Composable
fun PlanButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.18f), contentColor = color)
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}

@Composable
fun PlansTab(vm: AppVm) {
    val plans by remember { vm.dao.plans() }.collectAsState(emptyList())
    var kind by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Plan?>(null) }
    val fich = plans.filter { it.kind == "FICHAJE" }
    val ventas = plans.filter { it.kind == "VENTA" }

    ScreenScaffold(title = "Gestión", subtitle = "Futuros fichajes y futuras ventas") {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanButton("+ FUTURO FICHAJE", CvGreen, Modifier.weight(1f)) { editing = null; kind = "FICHAJE" }
                PlanButton("+ FUTURA VENTA", CvRed, Modifier.weight(1f)) { editing = null; kind = "VENTA" }
            }
        }
        item { SectionTitle("FUTUROS FICHAJES") }
        if (fich.isEmpty()) item { Text("Sin futuros fichajes.", color = CvMuted) }
        items(fich, key = { it.id }) { p ->
            EntryRow(accent = CvGreen, onClick = { editing = p; kind = p.kind }, onDelete = { vm.launch { vm.dao.deletePlan(p) } }) {
                Text(p.name, fontWeight = FontWeight.Bold)
                if (p.position.isNotBlank()) Text(p.position, color = CvMuted, fontSize = 12.sp)
            }
        }
        item { SectionTitle("FUTURAS VENTAS") }
        if (ventas.isEmpty()) item { Text("Sin futuras ventas.", color = CvMuted) }
        items(ventas, key = { it.id }) { p ->
            EntryRow(accent = CvRed, onClick = { editing = p; kind = p.kind }, onDelete = { vm.launch { vm.dao.deletePlan(p) } }) {
                Text(p.name, fontWeight = FontWeight.Bold)
                if (p.position.isNotBlank()) Text(p.position, color = CvMuted, fontSize = 12.sp)
            }
        }
    }

    kind?.let { k ->
        val init = editing
        var name by remember { mutableStateOf(init?.name ?: "") }
        var pos by remember { mutableStateOf(init?.position ?: "") }
        FormDialog(
            title = if (k == "FICHAJE") "Futuro fichaje" else "Futura venta",
            canSave = name.isNotBlank(),
            onDismiss = { kind = null },
            onSave = {
                val p = (init ?: Plan(kind = k, name = "")).copy(name = name.trim(), position = pos.trim())
                vm.launch { if (p.id == 0L) vm.dao.insertPlan(p) else vm.dao.updatePlan(p) }
                kind = null
            }
        ) {
            Field("Nombre", name) { name = it }
            Field("Posición", pos) { pos = it.uppercase() }
            ChipRow(POSITIONS, pos) { pos = it }
        }
    }
}
