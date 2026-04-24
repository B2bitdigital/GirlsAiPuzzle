package game.ecs.systems

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

class MovementSystem(
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.FIELD_HEIGHT
) {
    private val snapThreshold = cellSize * 0.5f

    /**
     * Move player position toward (targetX, targetY) at given speed.
     * Moves horizontally first, then vertically. pos = [x, y] (mutated in place).
     */
    fun movePlayerToward(
        pos: FloatArray,
        targetX: Float, targetY: Float,
        speed: Float, delta: Float
    ) {
        val dist = speed * delta
        val dx = targetX - pos[0]
        val dy = targetY - pos[1]

        when {
            abs(dx) > snapThreshold -> {
                val step = min(abs(dx), dist) * sign(dx)
                pos[0] = (pos[0] + step).coerceIn(0f, fieldWidth)
            }
            abs(dy) > snapThreshold -> {
                val step = min(abs(dy), dist) * sign(dy)
                pos[1] = (pos[1] + step).coerceIn(0f, fieldHeight)
            }
            else -> {
                pos[0] = targetX
                pos[1] = targetY
            }
        }
    }

    /** Snap a pixel coordinate to nearest grid cell center */
    fun snapToGrid(v: Float): Float {
        val cell = (v / cellSize).toInt()
        return cell * cellSize + cellSize / 2f
    }

    /** Convert pixel position to GridPoint */
    fun toGridPoint(x: Float, y: Float) = GridPoint(
        col = (x / cellSize).toInt().coerceIn(0, (fieldWidth / cellSize).toInt() - 1),
        row = (y / cellSize).toInt().coerceIn(0, (fieldHeight / cellSize).toInt() - 1)
    )
}
