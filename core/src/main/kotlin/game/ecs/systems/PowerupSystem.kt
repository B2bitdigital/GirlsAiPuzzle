package game.ecs.systems

import game.ecs.EnemyComponent
import game.ecs.PlayerComponent
import game.ecs.PowerupType
import kotlin.random.Random

data class SpawnedPowerup(val type: PowerupType, val x: Float, val y: Float)

class PowerupSystem(
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.FIELD_HEIGHT,
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val spawnInterval: Float = game.GameConstants.POWERUP_SPAWN_INTERVAL,
    private val lifetime: Float = game.GameConstants.POWERUP_LIFETIME
) {
    private var spawnTimer = 0f

    fun update(delta: Float, availableTypes: List<PowerupType>): SpawnedPowerup? {
        spawnTimer += delta
        if (spawnTimer < spawnInterval || availableTypes.isEmpty()) return null
        spawnTimer = 0f

        val type = availableTypes.random()
        val margin = cellSize * 2
        val x = Random.nextFloat() * (fieldWidth - margin * 2) + margin
        val y = Random.nextFloat() * (fieldHeight - margin * 2) + margin
        return SpawnedPowerup(type, x, y)
    }

    /**
     * Apply collected powerup effect.
     * @param timerRef float array [0] = current level time remaining (for TIME powerup)
     */
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
}
