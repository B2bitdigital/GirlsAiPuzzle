package game.ecs.systems

data class Explosion(
    val x: Float,
    val y: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    var elapsed: Float = 0f
) {
    companion object { const val DURATION = 0.6f }
    val progress: Float get() = elapsed / DURATION
}

class ExplosionSystem {
    private val _active = mutableListOf<Explosion>()
    val active: List<Explosion> get() = _active

    fun spawn(x: Float, y: Float, r: Float, g: Float, b: Float) {
        _active.add(Explosion(x, y, r, g, b))
    }

    fun update(delta: Float) {
        _active.forEach { it.elapsed += delta }
        _active.removeAll { it.progress >= 1f }
    }
}
