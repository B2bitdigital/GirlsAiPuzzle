package game.ecs.systems

import game.ecs.EnemyType
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.random.Random

class EnemyAISystem(
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.PLAY_HEIGHT   // was FIELD_HEIGHT
) {
    /**
     * Update a single enemy's position. pos/dirX/dirY mutated in place.
     * @param freezeTimer if > 0 the enemy is frozen — skip movement.
     */
    fun updateEnemy(
        type: EnemyType,
        pos: FloatArray,
        dirX: FloatArray,
        dirY: FloatArray,
        speed: Float,
        freezeTimer: Float,
        delta: Float,
        grid: Array<BooleanArray>,
        playerX: Float,
        playerY: Float
    ) {
        if (freezeTimer > 0f) return

        when (type) {
            EnemyType.SPIDER    -> moveSpider(pos, dirX, dirY, speed, delta, grid)
            EnemyType.COCKROACH -> moveCockroach(pos, dirX, dirY, speed, delta)
            EnemyType.WASP      -> moveWasp(pos, speed, delta, playerX, playerY)
            EnemyType.SNAIL     -> moveSnail(pos, dirX, dirY, speed * 0.3f, delta)
        }

        pos[0] = pos[0].coerceIn(0f, fieldWidth - 1f)
        pos[1] = pos[1].coerceIn(0f, fieldHeight - 1f)
    }

    // Spider follows the perimeter of conquered territory (border-hugging)
    private fun moveSpider(
        pos: FloatArray, dirX: FloatArray, dirY: FloatArray,
        speed: Float, delta: Float, grid: Array<BooleanArray>
    ) {
        pos[0] += dirX[0] * speed * delta
        pos[1] += dirY[0] * speed * delta

        // Bounce on field boundary
        if (pos[0] <= 0f || pos[0] >= fieldWidth - 1f) {
            dirX[0] = -dirX[0]
            dirY[0] = if (dirY[0] == 0f && Random.nextFloat() > 0.5f) 1f else dirY[0]
        }
        if (pos[1] <= 0f || pos[1] >= fieldHeight - 1f) {
            dirY[0] = -dirY[0]
        }
    }

    private fun moveCockroach(
        pos: FloatArray, dirX: FloatArray, dirY: FloatArray, speed: Float, delta: Float
    ) {
        pos[0] += dirX[0] * speed * delta
        pos[1] += dirY[0] * speed * delta

        if (pos[0] <= cellSize || pos[0] >= fieldWidth - cellSize) {
            dirX[0] = -dirX[0]
        }
        if (pos[1] <= cellSize || pos[1] >= fieldHeight - cellSize) {
            dirY[0] = -dirY[0]
        }

        // Occasional random direction change
        if (Random.nextFloat() < 0.01f) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            dirX[0] = kotlin.math.cos(angle)
            dirY[0] = kotlin.math.sin(angle)
        }
    }

    private fun moveWasp(
        pos: FloatArray, speed: Float, delta: Float, playerX: Float, playerY: Float
    ) {
        val dx = playerX - pos[0]
        val dy = playerY - pos[1]
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        pos[0] += (dx / len) * speed * delta
        pos[1] += (dy / len) * speed * delta
    }

    private fun moveSnail(
        pos: FloatArray, dirX: FloatArray, dirY: FloatArray, speed: Float, delta: Float
    ) {
        pos[0] += dirX[0] * speed * delta
        pos[1] += dirY[0] * speed * delta

        if (pos[0] <= cellSize || pos[0] >= fieldWidth - cellSize) dirX[0] = -dirX[0]
        if (pos[1] <= cellSize || pos[1] >= fieldHeight - cellSize) dirY[0] = -dirY[0]
    }
}
