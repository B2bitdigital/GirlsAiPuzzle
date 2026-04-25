package game.ecs.systems

import game.ecs.EnemyType
import kotlin.math.sqrt
import kotlin.random.Random

class EnemyAISystem(
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.PLAY_HEIGHT
) {
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
            EnemyType.COCKROACH -> moveBouncer(pos, dirX, dirY, speed, delta, grid, randomTurn = true)
            EnemyType.WASP      -> moveWasp(pos, dirX, dirY, speed, delta, playerX, playerY, grid)
            EnemyType.SNAIL     -> moveBouncer(pos, dirX, dirY, speed * 0.3f, delta, grid, randomTurn = false)
        }

        pos[0] = pos[0].coerceIn(0f, fieldWidth - 1f)
        pos[1] = pos[1].coerceIn(0f, fieldHeight - 1f)
    }

    private fun moveSpider(
        pos: FloatArray, dirX: FloatArray, dirY: FloatArray,
        speed: Float, delta: Float, grid: Array<BooleanArray>
    ) {
        val nx = pos[0] + dirX[0] * speed * delta
        val ny = pos[1] + dirY[0] * speed * delta

        val hitX = nx <= 0f || nx >= fieldWidth - 1f || isConquered(nx, pos[1], grid)
        val hitY = ny <= 0f || ny >= fieldHeight - 1f || isConquered(pos[0], ny, grid)

        if (hitX) {
            dirX[0] = -dirX[0]
            if (dirY[0] == 0f && Random.nextFloat() > 0.5f)
                dirY[0] = if (Random.nextBoolean()) 1f else -1f
        } else {
            pos[0] = nx
        }
        if (hitY) {
            dirY[0] = -dirY[0]
        } else {
            pos[1] = ny
        }
    }

    private fun moveBouncer(
        pos: FloatArray, dirX: FloatArray, dirY: FloatArray,
        speed: Float, delta: Float, grid: Array<BooleanArray>,
        randomTurn: Boolean
    ) {
        val nx = pos[0] + dirX[0] * speed * delta
        val ny = pos[1] + dirY[0] * speed * delta

        val hitX = nx <= cellSize || nx >= fieldWidth - cellSize || isConquered(nx, pos[1], grid)
        val hitY = ny <= cellSize || ny >= fieldHeight - cellSize || isConquered(pos[0], ny, grid)

        if (hitX) dirX[0] = -dirX[0] else pos[0] = nx
        if (hitY) dirY[0] = -dirY[0] else pos[1] = ny

        if (randomTurn && Random.nextFloat() < 0.01f) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            dirX[0] = kotlin.math.cos(angle)
            dirY[0] = kotlin.math.sin(angle)
        }
    }

    private fun moveWasp(
        pos: FloatArray, dirX: FloatArray, dirY: FloatArray,
        speed: Float, delta: Float,
        playerX: Float, playerY: Float,
        grid: Array<BooleanArray>
    ) {
        val dx = playerX - pos[0]
        val dy = playerY - pos[1]
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val nx = pos[0] + (dx / len) * speed * delta
        val ny = pos[1] + (dy / len) * speed * delta

        if (!isConquered(nx, pos[1], grid) && nx > 0f && nx < fieldWidth - 1f) {
            pos[0] = nx
            dirX[0] = dx / len
        } else {
            dirX[0] = -dirX[0]
        }
        if (!isConquered(pos[0], ny, grid) && ny > 0f && ny < fieldHeight - 1f) {
            pos[1] = ny
            dirY[0] = dy / len
        } else {
            dirY[0] = -dirY[0]
        }
    }

    private fun isConquered(x: Float, y: Float, grid: Array<BooleanArray>): Boolean {
        val col = (x / cellSize).toInt().coerceIn(0, grid.size - 1)
        val row = (y / cellSize).toInt().coerceIn(0, grid[0].size - 1)
        return grid[col][row]
    }
}
