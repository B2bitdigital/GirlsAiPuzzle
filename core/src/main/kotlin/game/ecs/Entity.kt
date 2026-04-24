package game.ecs

enum class EnemyType { SPIDER, COCKROACH, WASP, SNAIL }
enum class PowerupType { TIME, FREEZE, SPEED, SHIELD }

sealed class Entity(val id: Int) {
    class Player(id: Int) : Entity(id)
    class Enemy(id: Int, val type: EnemyType, val baseSpeed: Float) : Entity(id)
    class Powerup(id: Int, val type: PowerupType) : Entity(id)
}
