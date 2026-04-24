package game.level

import game.ecs.EnemyType
import game.ecs.PowerupType

data class EnemyConfig(
    val type: String,   // "spider", "cockroach", "wasp", "snail"
    val count: Int,
    val speed: Float
) {
    fun toEnemyType(): EnemyType = when (type.lowercase()) {
        "spider"    -> EnemyType.SPIDER
        "cockroach" -> EnemyType.COCKROACH
        "wasp"      -> EnemyType.WASP
        "snail"     -> EnemyType.SNAIL
        else        -> error("Unknown enemy type: $type")
    }
}

data class LevelData(
    val id: Int,
    val background: String,
    val timeSeconds: Int,
    val targetPercent: Int,
    val enemies: List<EnemyConfig>,
    val powerups: List<String>
) {
    fun powerupTypes(): List<PowerupType> = powerups.map { p ->
        when (p.lowercase()) {
            "time"   -> PowerupType.TIME
            "freeze" -> PowerupType.FREEZE
            "speed"  -> PowerupType.SPEED
            "shield" -> PowerupType.SHIELD
            else     -> error("Unknown powerup: $p")
        }
    }
}
