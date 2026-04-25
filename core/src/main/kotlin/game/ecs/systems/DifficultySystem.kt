package game.ecs.systems

import game.ecs.EnemyType
import kotlin.math.floor
import kotlin.random.Random

data class SpawnedEnemy(
    val type: EnemyType,
    val x: Float,
    val y: Float,
    val speed: Float,
    val dirX: Float,
    val dirY: Float
)

class DifficultySystem(
    private val levelId: Int,
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.PLAY_HEIGHT
) {
    private var elapsedTime = 0f
    private var lastSpawnTime = 0f
    private val spawnInterval = 30f
    private val maxExtraEnemies = 8
    private var extraSpawned = 0

    /** Speed multiplier for existing enemies. Increases every 20s, capped at 2.5x. */
    fun speedMultiplier(): Float {
        val timeBonus = (elapsedTime / 20f) * 0.1f
        val levelBonus = (levelId - 1) * 0.05f
        return (1f + timeBonus + levelBonus).coerceAtMost(2.5f)
    }

    /**
     * Call each frame. Returns a new enemy to spawn, or null.
     * @param currentEnemyCount total enemies alive now
     */
    fun update(delta: Float, currentEnemyCount: Int): SpawnedEnemy? {
        elapsedTime += delta

        if (extraSpawned >= maxExtraEnemies) return null
        if (currentEnemyCount >= 12) return null
        if (elapsedTime - lastSpawnTime < spawnInterval) return null

        lastSpawnTime = elapsedTime
        extraSpawned++

        val type = when {
            levelId <= 10 -> if (Random.nextBoolean()) EnemyType.COCKROACH else EnemyType.SPIDER
            levelId <= 25 -> listOf(EnemyType.COCKROACH, EnemyType.SPIDER, EnemyType.WASP).random()
            else          -> listOf(EnemyType.COCKROACH, EnemyType.WASP, EnemyType.SPIDER, EnemyType.SNAIL).random()
        }
        val baseSpeed = (60f + levelId * 3f + (elapsedTime / 60f) * 10f).coerceAtMost(220f)
        val x = Random.nextFloat() * (fieldWidth * 0.6f) + fieldWidth * 0.2f
        val y = Random.nextFloat() * (fieldHeight * 0.6f) + fieldHeight * 0.2f
        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
        return SpawnedEnemy(
            type = type, x = x, y = y,
            speed = baseSpeed,
            dirX = kotlin.math.cos(angle),
            dirY = kotlin.math.sin(angle)
        )
    }

    fun elapsedSeconds() = elapsedTime
}
