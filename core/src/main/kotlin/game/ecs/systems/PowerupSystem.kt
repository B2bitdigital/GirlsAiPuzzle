package game.ecs.systems

import game.ecs.EnemyComponent
import game.ecs.PlayerComponent
import game.ecs.PowerupType
import kotlin.random.Random

data class SpawnedPowerup(val type: PowerupType, val x: Float, val y: Float)

class PowerupSystem(
    private val offsetX: Float = game.GameConstants.FIELD_OFFSET_X,
    private val offsetY: Float = game.GameConstants.FIELD_OFFSET_Y,
    private val playWidth: Float = game.GameConstants.PLAY_WIDTH,
    private val playHeight: Float = game.GameConstants.PLAY_HEIGHT,
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val spawnInterval: Float = game.GameConstants.POWERUP_SPAWN_INTERVAL,
    private val lifetime: Float = game.GameConstants.POWERUP_LIFETIME
) {
    private var spawnTimer = 0f
    private var elapsedTime = 0f

    fun update(delta: Float, availableTypes: List<PowerupType>, cells: Array<Array<CellType>>? = null): SpawnedPowerup? {
        elapsedTime += delta
        spawnTimer += delta
        if (spawnTimer < currentInterval() || availableTypes.isEmpty()) return null
        spawnTimer = 0f

        val type = availableTypes.random()
        val margin = cellSize * 2
        repeat(20) {
            val x = offsetX + Random.nextFloat() * (playWidth - margin * 2) + margin
            val y = offsetY + Random.nextFloat() * (playHeight - margin * 2) + margin
            if (cells == null || !isConquered(x, y, cells)) {
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

    // FREE-cell check uses (x - offsetX) to align with grid coordinates.
    private fun isConquered(x: Float, y: Float, cells: Array<Array<CellType>>): Boolean {
        val col = ((x - offsetX) / cellSize).toInt().coerceIn(0, cells.size - 1)
        val row = ((y - offsetY) / cellSize).toInt().coerceIn(0, cells[0].size - 1)
        return cells[col][row] != CellType.FREE
    }
}
