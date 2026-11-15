import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Locale
import kotlin.math.*

private val Cyan = Color(0xff66e4ff)
private val Mint = Color(0xff6befbb)
private val Gold = Color(0xffffce82)
private val Muted = Color(0xff94a7c5)
private val Background = Color(0xff080f20)
private val Panel = Color(0xff101d33)
private val Scheme = darkColorScheme(
    primary = Cyan, onPrimary = Background, secondary = Mint,
    background = Background, surface = Panel, onSurface = Color(0xffeaf3ff),
    surfaceVariant = Color(0xff223550), onSurfaceVariant = Muted
)
private fun fmt(pattern: String, vararg args: Any) = String.format(Locale.US, pattern, *args)

fun main(args: Array<String>) {
    val demo = args.indexOf("--demo-png")
    if (demo >= 0) {
        require(demo + 1 < args.size) { "--demo-png требует путь к PNG" }
        val transition = "--transition" in args
        savePng(File(args[demo + 1]), QuantumWellModel.defaults(), transition, true, if (transition) 2.7 else 0.7)
        return
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ННГУ · Квантовая лаборатория · Compose Multiplatform",
            state = rememberWindowState(width = 1360.dp, height = 880.dp)
        ) {
            window.minimumSize = java.awt.Dimension(1100, 780)
            MaterialTheme(colorScheme = Scheme) { Laboratory(initialTransition = "--transition" in args) }
        }
    }
}

@Composable
private fun Laboratory(
    initialModel: QuantumWellModel = QuantumWellModel.defaults(),
    initialTransition: Boolean = false,
    initialEffects: Boolean = true,
    initialSeconds: Double = 0.0,
    initialPaused: Boolean = false,
    preview: Boolean = false
) {
    var model by remember { mutableStateOf(initialModel) }
    var transition by remember { mutableStateOf(initialTransition) }
    var effects by remember { mutableStateOf(initialEffects) }
    var paused by remember { mutableStateOf(initialPaused) }
    var seconds by remember { mutableDoubleStateOf(initialSeconds) }
    var message by remember { mutableStateOf("") }
    var exporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(paused) {
        if (paused || preview) return@LaunchedEffect
        var previous = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (previous != 0L) seconds += ((now - previous) / 1e9).coerceAtMost(0.1)
                previous = now
            }
        }
    }
    Column(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Background, Color(0xff15213a)))).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ННГУ  /  ЧАСТИЦА В ПОТЕНЦИАЛЬНОМ ЯЩИКЕ", color = Cyan, fontSize = 11.sp)
                Text("Квантовая лаборатория", color = Scheme.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Surface(color = Color(0xff18384a), shape = RoundedCornerShape(24.dp)) {
                Text("Compose Multiplatform  ·  Java 25", Modifier.padding(16.dp, 10.dp), color = Cyan, fontSize = 12.sp)
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Surface(Modifier.width(244.dp).fillMaxHeight(), shape = RoundedCornerShape(16.dp), color = Panel) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ПАРАМЕТРЫ МОДЕЛИ", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    ControlSlider("Ширина L", model.widthNm().toFloat(), 0.05f..10f, "%.2f нм") {
                        model = model.copy(width = it.toDouble())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (width in listOf(0.3, 0.6, 1.2)) {
                            OutlinedButton(onClick = { model = model.copy(width = width) }, contentPadding = PaddingValues(7.dp)) {
                                Text(fmt("%.1f", width), fontSize = 11.sp)
                            }
                        }
                    }
                    ControlSlider("Масса m / mₑ", model.massRatio().toFloat(), 0.05f..100f, "%.2f") {
                        model = model.copy(mass = it.toDouble())
                    }
                    ControlSlider("Число уровней N", model.count().toFloat(), 1f..10f, "%.0f", steps = 8) {
                        model = model.copy(count = it.roundToInt())
                    }
                    ControlSlider("Выбранный уровень n", model.level().toFloat(), 1f..max(2, model.count()).toFloat(), "%.0f",
                        steps = max(0, model.count() - 2), enabled = model.count() > 1) {
                        model = model.copy(level = it.roundToInt())
                    }
                    ControlSlider(fmt("x = %.3f нм", model.xNm()), model.xFraction().toFloat(), 0f..1f, "%.2f L") {
                        model = model.copy(fraction = it.toDouble())
                    }
                    HorizontalDivider(color = Color(0xff2a3b54))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(effects, onCheckedChange = { effects = it })
                        Text("Свечение и частицы", fontSize = 12.sp)
                    }
                    FilledTonalButton(onClick = { paused = !paused }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (paused) "▶  Продолжить" else "Ⅱ  Пауза")
                    }
                    OutlinedButton(onClick = { model = QuantumWellModel.defaults(); seconds = 0.0 }, modifier = Modifier.fillMaxWidth()) {
                        Text("Сбросить параметры", fontSize = 12.sp)
                    }
                    Button(enabled = !exporting, onClick = {
                        if (preview) return@Button
                        val file = chooseOutput(transition) ?: return@Button
                        val snapshot = model
                        val tab = transition
                        val glow = effects
                        val time = seconds
                        val frozen = paused
                        exporting = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.Default) { savePng(file, snapshot, tab, glow, time, frozen) }
                                message = "Сохранено: ${file.absolutePath}"
                            } catch (ex: Exception) {
                                message = "Ошибка экспорта: ${ex.message}"
                            } finally { exporting = false }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (exporting) "Сохранение…" else "↓  Сохранить PNG")
                    }
                    Text("Масса меняет только спектр. В переходе 2 → 1 используется mₑ.\n\nФаза замедлена: E/E₁ = n². Плотность стационарна.\n\nУровни и движение фотона показаны схематично.",
                        color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TabButton("01  Спектр и состояние", !transition) { transition = false; message = "" }
                    TabButton("02  Переход и фотон", transition) { transition = true; message = "" }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val metrics = if (transition) listOf(
                        "ЭНЕРГИЯ E₁" to fmt("%.4f эВ", model.electronEnergyEv(1)),
                        "ЭНЕРГИЯ E₂" to fmt("%.4f эВ", model.electronEnergyEv(2)),
                        "ФОТОН ΔE" to fmt("%.4f эВ", model.deltaEv()),
                        "ДЛИНА ВОЛНЫ λ" to fmt("%.2f нм", model.wavelengthNm())
                    ) else listOf(
                        "ЭНЕРГИЯ Eₙ" to fmt("%.4f эВ", model.energyEv(model.level())),
                        "ψₙ(x, 0), нм⁻½" to fmt("%+.4f", model.psi(model.xFraction())),
                        "|ψₙ(x)|², нм⁻¹" to fmt("%.4f", model.density(model.xFraction())),
                        "СОСТОЯНИЕ" to "n = ${model.level()}"
                    )
                    metrics.forEach { (name, value) ->
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = Panel, tonalElevation = 2.dp) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(name, color = Muted, fontSize = 11.sp, maxLines = 1)
                                Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
                // Время читается в Canvas: анимация перерисовывает график, а не все контролы.
                QuantumPlot(model, transition, effects, { seconds }, Modifier.weight(1f).fillMaxWidth()) { model = it }
                Text(message.ifEmpty {
                    if (transition) fmt("ΔE = hν = hc/λ  ·  ν = %.5e Гц  ·  m = mₑ  ·  время анимации условное", model.frequencyHz())
                    else "Выберите n на спектре и x на графике.  Re ψ = ψ₀ cos φ; Im ψ = −ψ₀ sin φ (пунктир); |ψ|² стационарна."
                }, color = Muted, fontSize = 11.sp, modifier = Modifier.heightIn(min = 38.dp))
            }
        }
    }
}

/** Обновление immutable record с ограничением n при уменьшении N. */
private fun QuantumWellModel.copy(
    width: Double = widthNm(), mass: Double = massRatio(), count: Int = count(),
    level: Int = level(), fraction: Double = xFraction()
) = QuantumWellModel(width, mass, count, level.coerceIn(1, count), fraction)

@Composable
private fun ControlSlider(
    name: String, value: Float, range: ClosedFloatingPointRange<Float>, format: String,
    steps: Int = 0, enabled: Boolean = true, onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 11.sp)
            Text(fmt(format, value), color = Cyan, fontSize = 12.sp)
        }
        Slider(value, onValueChange = onChange, valueRange = range, steps = steps, enabled = enabled,
            modifier = Modifier.height(30.dp).semantics { contentDescription = name })
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) Cyan else Color(0xff223550))
    Button(onClick, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(
        containerColor = color, contentColor = if (selected) Background else Scheme.onSurface
    )) { Text(text, fontSize = 13.sp) }
}

@Composable
private fun QuantumPlot(
    model: QuantumWellModel, transition: Boolean, effects: Boolean, seconds: () -> Double,
    modifier: Modifier, onChange: (QuantumWellModel) -> Unit
) {
    val measurer = rememberTextMeasurer()
    val currentModel by rememberUpdatedState(model)
    val currentChange by rememberUpdatedState(onChange)
    fun select(point: Offset, size: IntSize) {
        if (transition) return
        val x = point.x / size.width * 1000
        val y = point.y / size.height * 560
        val state = currentModel
        if (x in 40f..310f && y in 78f..480f) {
            val n = ((470 - y) / 370 * max(1, state.count() - 1)).roundToInt() + 1
            currentChange(state.copy(level = n.coerceIn(1, state.count())))
        } else if (x in 400f..950f && y in 65f..503f) {
            currentChange(state.copy(fraction = ((x - 400) / 550.0).coerceIn(0.0, 1.0)))
        }
    }
    Canvas(modifier.clip(RoundedCornerShape(16.dp)).background(Panel)
        .semantics { contentDescription = if (transition) "Переход электрона 2 → 1" else "Спектр, волновая функция и плотность вероятности" }
        .pointerInput(transition) { detectTapGestures { select(it, size) } }
        .pointerInput(transition) { detectDragGestures { change, _ -> change.consume(); select(change.position, size) } }
    ) {
        val time = seconds()
        fun label(text: String, x: Float, y: Float, color: Color = Muted, fontSize: Int = 13) {
            drawText(measurer, text, topLeft = Offset(x, y - fontSize), size = Size(1000f - x, 100f),
                style = TextStyle(color = color, fontSize = fontSize.toSp()))
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float = 1f) {
            drawLine(color, Offset(x1, y1), Offset(x2, y2), width)
        }
        fun glowPath(path: Path, color: Color, width: Float = 2.5f) {
            if (effects) for (radius in listOf(16f, 10f, 6f)) {
                drawPath(path, color.copy(alpha = 0.045f), style = Stroke(radius, cap = StrokeCap.Round))
            }
            drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round))
        }
        fun particle(center: Offset, color: Color, radius: Float) {
            if (effects) drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.4f), Color.Transparent), center, radius * 4), radius * 4, center)
            drawCircle(color, radius, center)
        }
        withTransform({ scale(size.width / 1000, size.height / 560, Offset.Zero) }) {
            if (effects) repeat(36) { i ->
                drawCircle(Cyan.copy(alpha = 0.10f), (1 + i % 3).toFloat(), Offset(
                    ((i * 137.3 + time * (4 + i % 4)) % 1000).toFloat(), (i * 83.7 % 560).toFloat()))
            }
            if (!transition) {
                label("ЭНЕРГЕТИЧЕСКИЙ СПЕКТР", 30f, 38f, fontSize = 12)
                label("ψₙ(x, t)  ·  нм⁻½", 380f, 38f, Cyan, 15)
                label("Re —   /   Im ⋯", 785f, 38f, Color(0xffc1acff), 12)
                label("|ψₙ(x)|²  ·  нм⁻¹", 380f, 302f, Mint, 15)
                line(335f, 24f, 335f, 530f, Color(0xff26364d))
                for (n in 1..model.count()) {
                    val y = 470f - (n - 1f) / max(1, model.count() - 1) * 370
                    val selected = n == model.level()
                    val path = Path().apply { moveTo(40f, y); lineTo(305f, y) }
                    if (selected) glowPath(path, Cyan, 3f) else drawPath(path, Color(0xff3c526e), style = Stroke(1f))
                    label(fmt("n=%d     %.4f эВ", n, model.energyEv(n)), 50f, y - 11, if (selected) Cyan else Muted)
                }
                label("Eₙ = n²h² / (8mL²)", 40f, 520f)
                for (density in listOf(false, true)) {
                    val top = if (density) 328f else 65f
                    val bottom = if (density) 503f else 248f
                    val base = if (density) bottom else (top + bottom) / 2
                    val amplitude = (bottom - top) * if (density) 0.85f else 0.41f
                    val color = if (density) Mint else Cyan
                    val phase = cos(model.phase(time)).toFloat()
                    fun value(t: Float): Float {
                        val sine = sin(model.level() * PI * t).toFloat()
                        return base - amplitude * if (density) sine * sine else sine * phase
                    }
                    repeat(5) { i -> line(400f + 550f * i / 4, top, 400f + 550f * i / 4, bottom, Color(0xff24334a)) }
                    line(400f, base, 950f, base, Color(0xff435671))
                    val path = Path().apply {
                        moveTo(400f, value(0f))
                        for (i in 1..400) lineTo(400f + 550f * i / 400, value(i / 400f))
                    }
                    val fill = Path().apply {
                        addPath(path); lineTo(950f, base); lineTo(400f, base); close()
                    }
                    drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent), top, bottom))
                    if (!density) {
                        val imaginary = Path().apply {
                            for (i in 0..400) {
                                val t = i / 400f
                                val y = base + amplitude * sin(model.level() * PI * t).toFloat() * sin(model.phase(time)).toFloat()
                                if (i == 0) moveTo(400f, y) else lineTo(400f + 550 * t, y)
                            }
                        }
                        drawPath(imaginary, Color(0xffc1acff), style = Stroke(1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
                    }
                    glowPath(path, color)
                    val x = 400f + model.xFraction().toFloat() * 550
                    drawLine(Gold.copy(alpha = 0.6f), Offset(x, top), Offset(x, bottom), 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f)))
                    particle(Offset(x, value(model.xFraction().toFloat())), Gold, 5f)
                    label(fmt("%.2f", if (density) 2 / model.widthNm() else sqrt(2 / model.widthNm())), 350f, top + 12, fontSize = 11)
                    label("0", 400f, bottom + 20, fontSize = 11)
                    label(fmt("L = %.2f нм", model.widthNm()), 865f, bottom + 20, fontSize = 11)
                }
            } else {
                label("ИСПУСКАНИЕ ФОТОНА  /  2 → 1", 32f, 40f)
                label("Электрон отдаёт разность энергий электромагнитному полю", 32f, 68f)
                for (n in 1..2) {
                    val y = if (n == 2) 165f else 405f
                    val color = if (n == 2) Cyan else Mint
                    glowPath(Path().apply { moveTo(85f, y); lineTo(490f, y) }, color)
                    label(fmt("E%d = %.6f эВ", n, model.electronEnergyEv(n)), 95f, y - 22, color, 19)
                }
                line(280f, 190f, 280f, 375f, Gold, 2f)
                line(280f, 375f, 271f, 360f, Gold, 2f)
                line(280f, 375f, 289f, 360f, Gold, 2f)
                label(fmt("ΔE = %.6f эВ", model.deltaEv()), 300f, 294f, Gold, 17)
                val cycle = (time % 4 / 4).toFloat()
                val fall = (cycle / 0.4f).coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
                particle(Offset(280f, 165f + 240f * fall), Color.White, 11f)
                if (cycle > 0.4f) {
                    val center = 310 + (cycle - 0.4f) / 0.6f * 580
                    val path = Path().apply {
                        for (i in 0..160) {
                            val t = i / 160f
                            val x = center - 80 + 160 * t
                            val y = 405 + (sin(t * PI * 8 - time * 8) * sin(t * PI) * 22).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    glowPath(path, Gold, 3f)
                }
                label("ФОТОН", 650f, 160f, Gold)
                label(fmt("λ = %.3f нм", model.wavelengthNm()), 650f, 206f, Color.White, 28)
                label(fmt("ν = %.4e Гц", model.frequencyHz()), 650f, 244f, fontSize = 16)
                label("ΔE = hν = hc/λ", 650f, 292f, Cyan, 21)
                label("Ширина ящика определяет длину волны и энергию фотона", 85f, 510f, fontSize = 14)
            }
        }
    }
}

private fun chooseOutput(transition: Boolean): File? {
    val dialog = FileDialog(null as Frame?, "Сохранить визуализацию", FileDialog.SAVE)
    return try {
        dialog.file = if (transition) "compose-transition.png" else "compose-spectrum.png"
        dialog.isVisible = true
        val name = dialog.file ?: return null
        File(dialog.directory, if (name.endsWith(".png", ignoreCase = true)) name else "$name.png")
    } finally { dialog.dispose() }
}

/** Тот же Compose UI рендерится в изображение, включая графики и текущую фазу. */
@OptIn(ExperimentalComposeUiApi::class)
private fun savePng(file: File, model: QuantumWellModel, transition: Boolean, effects: Boolean, seconds: Double, paused: Boolean = false) {
    val parent = file.absoluteFile.parentFile
    check(parent.isDirectory || parent.mkdirs()) { "Не удалось создать $parent" }
    val scene = ImageComposeScene(width = 1360, height = 880, density = Density(1f)) {
        MaterialTheme(colorScheme = Scheme) { Laboratory(model, transition, effects, seconds, paused, preview = true) }
    }
    try {
        val image = scene.render()
        try {
            val data = checkNotNull(image.encodeToData(EncodedImageFormat.PNG)) { "PNG encoder недоступен" }
            try { file.writeBytes(data.bytes) } finally { data.close() }
        } finally { image.close() }
    } finally { scene.close() }
    println("PNG: ${file.absolutePath}")
}
