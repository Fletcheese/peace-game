import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun InteractiveCircleScreenFireAndForget() {
    // 1. State to hold the component's size. Initialize to zero.
    var componentSize by remember { mutableStateOf<Offset>(Offset.Zero) }

    // 2. The target position for the animation. It's now non-nullable.
    var targetPosition by remember { mutableStateOf(Offset.Zero) }

    // This is the core of the animation. It will now always have a valid target.
    val animatedCirclePosition by animateOffsetAsState(
        targetValue = targetPosition,
        animationSpec = tween(durationMillis = 1000),
        label = "CirclePositionAnimation"
    )

    // 3. This side-effect runs once when the composable first appears.
    // We use it to set the initial target position to the center.
    LaunchedEffect(componentSize) {
        if (componentSize != Offset.Zero) { // Ensure we have a valid size
            targetPosition = Offset(componentSize.x / 2, componentSize.y / 2)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                // 4. Update the component's size when it's measured.
                componentSize = Offset(it.width.toFloat(), it.height.toFloat())
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        // Set the new target position on click.
                        targetPosition = offset
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // No more complex logic needed here, just draw at the animated position.
            drawCircle(
                color = Color.Red,
                radius = 50f,
                center = animatedCirclePosition
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun InteractiveCircleScreenFireAndForgetPreview() {
    InteractiveCircleScreenFireAndForget()
}
