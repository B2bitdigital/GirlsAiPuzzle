package game.ecs

import com.badlogic.gdx.math.Vector2

data class PositionComponent(var x: Float, var y: Float) {
    fun toVec() = Vector2(x, y)
}

data class VelocityComponent(var vx: Float = 0f, var vy: Float = 0f)

data class PlayerComponent(
    var lives: Int,
    var score: Int = 0,
    var targetX: Float = -1f,
    var targetY: Float = -1f,
    var speedMultiplier: Float = 1f,
    var speedBoostTimer: Float = 0f,
    var shieldTimer: Float = 0f
)

data class EnemyComponent(
    var speed: Float,
    var freezeTimer: Float = 0f,
    var dirX: Float = 1f,
    var dirY: Float = 0f
)

data class PowerupComponent(
    var lifetime: Float,
    var active: Boolean = true
)

// All components for a single entity bundled for the world
data class EntityState(
    val entity: Entity,
    val position: PositionComponent,
    val velocity: VelocityComponent = VelocityComponent(),
    val playerComp: PlayerComponent? = null,
    val enemyComp: EnemyComponent? = null,
    val powerupComp: PowerupComponent? = null
)
