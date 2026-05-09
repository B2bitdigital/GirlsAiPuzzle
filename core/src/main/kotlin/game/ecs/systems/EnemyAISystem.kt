package game.ecs.systems

import game.ecs.EnemyType
import kotlin.math.sqrt
import kotlin.random.Random

class EnemyAISystem(
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val offsetX: Float = game.GameConstants.FIELD_OFFSET_X,
    private val offsetY: Float = game.GameConstants.FIELD_OFFSET_Y,
    private val fieldWidth: Float = game.GameConstants.PLAY_WIDTH,
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
        cells: Array<Array<CellType>>,
        playerX: Float,
        playerY: Float
    ) {
        if (freezeTimer > 0f) return

        when (type) {
            EnemyType.SPIDER    -> moveSpider(pos, dirX, dirY, speed, delta, cells)
            EnemyType.COCKROACH -> moveBouncer(pos, dirX, dirY, speed, delta, cells, randomTurn = true)
            EnemyType.WASP      -> moveWasp(pos, dirX, dirY, speed, delta, playerX, playerY, cells)
            EnemyType.SNAIL     -> moveBouncer(pos, dirX, dirY, speed * 0.3f, delta, cells, randomTurn = false)
        }

        // clamp to field bounds in screen space
        pos[0] = pos[0].coerceIn(offsetX, offsetX + fieldWidth - 1f)
        pos[1] = pos[1].coerceIn(offsetY, offsetY + fieldHeight - 1f)
    }

    private fun moveSpider(
        pos: FloatArray, dirX: FloatArray, dirY: FloatArray,
        speed: Float, delta: Float, cells: Array<Array<CellType>>
    ) {
        val nx = pos[0] + dirX[0] * speed * delta
        val ny = pos[1] + dirY[0] * speed * delta

        val hitX = nx <= offsetX || nx >= offsetX + fieldWidth - 1f || isConquered(nx, pos[1], cells)
        val hitY = ny <= offsetY || ny >= offsetY + fieldHeight - 1f || isConquered(pos[0], ny, cells)

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
        speed: Float, delta: Float, cells: Array<Array<CellType>>,
        randomTurn: Boolean
    ) {
        val nx = pos[0] + dirX[0] * speed * delta
        val ny = pos[1] + dirY[0] * speed * delta

        val hitX = nx <= offsetX + cellSize || nx >= offsetX + fieldWidth - cellSize || isConquered(nx, pos[1], cells)
        val hitY = ny <= offsetY + cellSize || ny >= offsetY + fieldHeight - cellSize || isConquered(pos[0], ny, cells)

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
        cells: Array<Array<CellType>>
    ) {
        val dx = playerX - pos[0]
        val dy = playerY - pos[1]
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val nx = pos[0] + (dx / len) * speed * delta
        val ny = pos[1] + (dy / len) * speed * delta

        if (!isConquered(nx, pos[1], cells) && nx > offsetX && nx < offsetX + fieldWidth - 1f) {
            pos[0] = nx
            dirX[0] = dx / len
        } else {
            dirX[0] = -(dx / len)
        }
        if (!isConquered(pos[0], ny, cells) && ny > offsetY && ny < offsetY + fieldHeight - 1f) {
            pos[1] = ny
            dirY[0] = dy / len
        } else {
            dirY[0] = -(dy / len)
        }
    }

    // Enemies bounce off CONQUERED cells. LINE cells are traversable (hitting LINE
    // is a game-over event handled by CollisionSystem, not a movement boundary).
    private fun isConquered(x: Float, y: Float, cells: Array<Array<CellType>>): Boolean {
        val col = ((x - offsetX) / cellSize).toInt().coerceIn(0, cells.size - 1)
        val row = ((y - offsetY) / cellSize).toInt().coerceIn(0, cells[0].size - 1)
        return cells[col][row] == CellType.CONQUERED
    }
}
