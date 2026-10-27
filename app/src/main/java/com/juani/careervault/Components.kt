package com.juani.careervault

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

val CvBg = Color(0xFF000000)
val CvSurface = Color(0xFF0E1411)
val CvSurface2 = Color(0xFF16201B)
val CvBorder = Color(0xFF243229)
val CvGreen = Color(0xFF2EE07A)
val CvText = Color(0xFFF2F5F3)
val CvMuted = Color(0xFF8A9A92)
val CvRed = Color(0xFFFF5A5F)
val CvOrange = Color(0xFFFF9F43)
val CvBlue = Color(0xFF4DA3FF)
val CvGold = Color(0xFFFFC94D)

@Composable
fun CvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CvGreen, onPrimary = Color.Black,
            secondary = CvGold, background = CvBg, onBackground = CvText,
            surface = CvSurface, onSurface = CvText,
            surfaceVariant = CvSurface2, onSurfaceVariant = CvMuted,
            outline = CvBorder, surfaceContainerHigh = CvSurface2, surfaceContainer = CvSurface2
        ),
        content = content
    )
}

@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    fab: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit
) {
    Scaffold(containerColor = CvBg, floatingActionButton = fab) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    Text(
                        "‹", fontSize = 36.sp, color = CvGreen,
                        modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(horizontal = 14.dp)
                    )
                } else Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) Text(subtitle, color = CvMuted, style = MaterialTheme.typography.bodyMedium)
                }
                actions()
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
fun Fab(label: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick, containerColor = CvGreen, contentColor = Color.Black) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NoteCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    var m = modifier.fillMaxWidth().clip(shape)
        .background(if (accent != null) accent.copy(alpha = 0.14f) else CvSurface)
        .border(1.dp, if (accent != null) accent.copy(alpha = 0.55f) else CvBorder, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(16.dp), content = content)
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = CvMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

@Composable
fun Pill(text: String, color: Color = CvGreen) {
    Text(
        text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

@Composable
fun InfoLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = CvMuted, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
fun Tiles(items: List<Triple<String, String, Color>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value, color) ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(CvSurface2).padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
                Text(label, fontSize = 11.sp, color = CvMuted, maxLines = 1)
            }
        }
    }
}

@Composable
fun DeleteIcon(what: String = "¿Borrar este registro?", onConfirm: () -> Unit) {
    var ask by remember { mutableStateOf(false) }
    Text("✕", color = CvMuted, fontSize = 16.sp, modifier = Modifier.clip(CircleShape).clickable { ask = true }.padding(12.dp))
    if (ask) {
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text("¿Borrar?") },
            text = { Text(what) },
            confirmButton = { TextButton(onClick = { ask = false; onConfirm() }) { Text("Borrar", color = CvRed) } },
            dismissButton = { TextButton(onClick = { ask = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun EntryRow(
    accent: Color? = null,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(shape)
            .background(if (accent != null) accent.copy(alpha = 0.16f) else CvSurface2)
            .border(1.dp, if (accent != null) accent.copy(alpha = 0.55f) else CvBorder, shape)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(10.dp)) }
        Column(Modifier.weight(1f), content = content)
        if (trailing != null) trailing()
        DeleteIcon(onConfirm = onDelete)
    }
}

@Composable
fun SeasonGroup(season: String, summary: String, count: Int, open: Boolean, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember(season) { mutableStateOf(open) }
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(CvSurface).border(1.dp, CvBorder, shape)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(season, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = CvGreen)
                if (summary.isNotBlank()) Text(summary, color = CvMuted, fontSize = 12.sp)
            }
            Pill("$count")
            Text(if (expanded) "▾" else "▸", color = CvMuted, modifier = Modifier.padding(start = 10.dp))
        }
        if (expanded) Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp), content = content)
    }
}

fun <T> LazyListScope.groupedBySeason(
    list: List<T>,
    seasonOf: (T) -> String,
    summary: (List<T>) -> String,
    row: @Composable (T) -> Unit
) {
    val groups = list.groupBy { seasonOf(it).ifBlank { "Sin temporada" } }
    groups.keys.sortedDescending().forEachIndexed { i, k ->
        item(key = "season_$k") {
            val items = groups.getValue(k)
            SeasonGroup(k, summary(items), items.size, open = i == 0) { items.forEach { row(it) } }
        }
    }
}

@Composable
fun FormDialog(
    title: String,
    canSave: Boolean = true,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = { TextButton(onClick = onSave, enabled = canSave) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    kb: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = kb,
            capitalization = if (kb == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
        )
    )
}

@Composable
fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = selected == o,
                onClick = { onSelect(o) },
                label = { Text(o) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CvGreen.copy(alpha = 0.22f),
                    selectedLabelColor = CvGreen
                )
            )
        }
    }
}

@Composable
fun SeasonField(value: String, existing: List<String>, onChange: (String) -> Unit) {
    Field("Temporada (ej. 2026/27)", value, onChange = onChange)
    val opts = existing.filter { it.isNotBlank() }.distinct().sortedDescending()
    if (opts.isNotEmpty()) ChipRow(opts, value, onChange)
}

@Composable
fun Crest(path: String, name: String, size: Dp = 44.dp) {
    val bmp = remember(path) { if (path.isBlank()) null else BitmapFactory.decodeFile(path)?.asImageBitmap() }
    Box(
        Modifier.size(size).clip(CircleShape).background(if (bmp == null) CvSurface2 else Color.White.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize().padding(size * 0.1f), contentScale = ContentScale.Fit)
        else Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = CvGreen, fontSize = (size.value * 0.4f).sp)
    }
}

@Composable
fun CrestPicker(name: String, national: Boolean, crest: String, onCrest: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { Crests.fromUri(ctx, uri)?.let { onCrest(it); msg = "" } }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Crest(crest, name, 64.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            TextButton(
                enabled = name.isNotBlank() && !busy,
                onClick = {
                    busy = true; msg = ""
                    scope.launch {
                        val p = Crests.search(ctx, name, national)
                        busy = false
                        if (p != null) onCrest(p) else msg = "No lo encontré. Probá con otro nombre o elegí la imagen a mano."
                    }
                }
            ) { Text(if (busy) "Buscando…" else "Buscar escudo en internet") }
            TextButton(onClick = { pick.launch("image/*") }) { Text("Elegir de la galería") }
        }
    }
    if (msg.isNotBlank()) Text(msg, color = CvOrange, fontSize = 12.sp)
}

/** Silueta genérica: copa (cáliz con asas) o liga (trofeo alto con esfera). */
@Composable
fun TrophyIcon(league: Boolean, size: Dp = 34.dp, tint: Color = CvGold) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        if (!league) {
            val bowl = Path().apply {
                moveTo(w * 0.28f, h * 0.10f); lineTo(w * 0.72f, h * 0.10f); lineTo(w * 0.66f, h * 0.50f)
                quadraticBezierTo(w * 0.5f, h * 0.70f, w * 0.34f, h * 0.50f); close()
            }
            drawPath(bowl, tint)
            drawArc(tint, 90f, 180f, false, Offset(w * 0.10f, h * 0.15f), Size(w * 0.30f, h * 0.30f), style = Stroke(w * 0.06f))
            drawArc(tint, 270f, 180f, false, Offset(w * 0.60f, h * 0.15f), Size(w * 0.30f, h * 0.30f), style = Stroke(w * 0.06f))
            drawRect(tint, Offset(w * 0.45f, h * 0.55f), Size(w * 0.10f, h * 0.25f))
            drawRect(tint, Offset(w * 0.30f, h * 0.80f), Size(w * 0.40f, h * 0.10f))
        } else {
            drawCircle(tint, w * 0.15f, Offset(w * 0.5f, h * 0.17f))
            val body = Path().apply {
                moveTo(w * 0.38f, h * 0.34f); lineTo(w * 0.62f, h * 0.34f); lineTo(w * 0.72f, h * 0.74f); lineTo(w * 0.28f, h * 0.74f); close()
            }
            drawPath(body, tint)
            drawRect(tint, Offset(w * 0.20f, h * 0.78f), Size(w * 0.60f, h * 0.12f))
        }
    }
}
