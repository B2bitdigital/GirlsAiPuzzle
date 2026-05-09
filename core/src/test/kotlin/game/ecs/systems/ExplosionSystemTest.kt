package game.ecs.systems

import org.junit.Assert.*
import org.junit.Test

class ExplosionSystemTest {

    @Test
    fun `spawn adds an explosion`() {
        val sys = ExplosionSystem()
        sys.spawn(100f, 200f, 1f, 0f, 0f)
        assertEquals(1, sys.active.size)
        assertEquals(100f, sys.active[0].x, 0f)
        assertEquals(200f, sys.active[0].y, 0f)
    }

    @Test
    fun `progress starts at zero`() {
        val sys = ExplosionSystem()
        sys.spawn(0f, 0f, 1f, 1f, 1f)
        assertEquals(0f, sys.active[0].progress, 0.001f)
    }

    @Test
    fun `progress increases after update`() {
        val sys = ExplosionSystem()
        sys.spawn(0f, 0f, 1f, 1f, 1f)
        sys.update(0.3f)
        assertTrue("progress > 0.4", sys.active[0].progress > 0.4f)
    }

    @Test
    fun `expired explosion is removed`() {
        val sys = ExplosionSystem()
        sys.spawn(0f, 0f, 1f, 1f, 1f)
        sys.update(Explosion.DURATION + 0.1f)
        assertEquals(0, sys.active.size)
    }

    @Test
    fun `explosions tick independently`() {
        val sys = ExplosionSystem()
        sys.spawn(0f, 0f, 1f, 0f, 0f)
        sys.update(0.2f)
        sys.spawn(50f, 50f, 0f, 1f, 0f)
        // First elapsed=0.2, second elapsed=0 at this point.
        // Advance 0.5 more: first=0.7 > DURATION(0.6) → removed; second=0.5 → alive.
        sys.update(0.5f)
        assertEquals(1, sys.active.size)
        assertEquals(50f, sys.active[0].x, 0f)
    }
}
