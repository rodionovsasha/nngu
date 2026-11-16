import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals

/** События проходят через реальную цепочку pointerInput, а не через функции вычисления координат. */
@OptIn(ExperimentalComposeUiApi::class)
class QuantumPlotTest {
    @Test fun tapAndDragBothWork() {
        var model by mutableStateOf(QuantumWellModel.defaults())
        val scene = ImageComposeScene(width = 1000, height = 560, density = Density(1f)) {
            MaterialTheme { QuantumPlot(model, false, false, { 0.0 }, Modifier.fillMaxSize()) { model = it } }
        }
        try {
            fun draw() { scene.render().close() }
            fun press(x: Float, y: Float) = scene.sendPointerEvent(PointerEventType.Press, Offset(x, y),
                buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            fun release(x: Float, y: Float) = scene.sendPointerEvent(PointerEventType.Release, Offset(x, y),
                buttons = PointerButtons(), button = PointerButton.Primary)
            draw()
            press(200f, 100f); release(200f, 100f); draw()
            assertEquals(5, model.level(), "Клик по спектру должен выбрать n=5")
            press(537.5f, 170f); release(537.5f, 170f); draw()
            assertEquals(0.25, model.xFraction(), 1e-6, "Клик по кривой должен установить x/L")
            press(510f, 170f)
            for (x in listOf(540f, 650f, 840f)) {
                scene.sendPointerEvent(PointerEventType.Move, Offset(x, 170f), buttons = PointerButtons(isPrimaryPressed = true))
                draw()
            }
            release(840f, 170f); draw()
            assertEquals(0.8, model.xFraction(), 1e-6, "Перетаскивание должно довести маркер до последней точки")
            press(675f, 400f); release(675f, 400f); draw()
            assertEquals(0.5, model.xFraction(), 1e-6, "После drag обычный клик должен продолжать работать")
        } finally { scene.close() }
    }
}
