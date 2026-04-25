package game.ecs.systems

import game.ecs.EnemyComponent
import game.ecs.PlayerComponent
import game.ecs.PowerupType
import kotlin.random.Random

data class SpawnedPowerup(val type: PowerupType, val x: Float, val y: Float)

class PowerupSystem(
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.PLAY_HEIGHT,
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val spawnInterval: Float = game.GameConstants.POWERUP_SPAWN_INTERVAL,
    private val lifetime: Float = game.GameConstants.POWERUP_LIFETIME
) {
    private var spawnTimer = 0f
    private var elapsedTime = 0f

    fun update(delta: Float, availableTypes: List<PowerupType>, grid: Array<BooleanArray>? = null): SpawnedPowerup? {
        elapsedTime += delta
        spawnTimer += delta
        if (spawnTimer < currentInterval() || availableTypes.isEmpty()) return null
        spawnTimer = 0f

        val type = availableTypes.random()
        val margin = cellSize * 2
        repeat(20) {
            val x = Random.nextFloat() * (fieldWidth - margin * 2) + margin
            val y = Random.nextFloat() * (fieldHeight - margin * 2) + margin
            if (grid == null || !isConquered(x, y, grid)) {
                return SpawnedPowerup(type, x, y)
            }
        }
        return null
    }

    fun applyEffect(
        type: PowerupType,
        timerRef: FloatArray? = null,
        playerComp: PlayerComponent? = null,
        enemyComps: List<EnemyComponent> = emptyList()
    ) {
        when (type) {
            PowerupType.TIME   -> timerRef?.let { it[0] += 10f }
            PowerupType.FREEZE -> enemyComps.forEach { it.freezeTimer = 3f }
            PowerupType.SPEED  -> playerComp?.let {
                it.speedMultiplier = 2f
                it.speedBoostTimer = 5f
            }
            PowerupType.SHIELD -> playerComp?.let { it.shieldTimer = 4f }
        }
    }

    private fun currentInterval(): Float {
        val reduction = (elapsedTime / 30f).toInt() * 1f
        return (spawnInterval - reduction).coerceAtLeast(5f)
    }

    private fun isConquered(x: Float, y: Float, grid: Array<BooleanArray>): Boolean {
        val col = (x / cellSize).toInt().coerceIn(0, grid.size - 1)
        val row = (y / cellSize).toInt().coerceIn(0, grid[0].size - 1)
        return grid[col][row]
    }
}
