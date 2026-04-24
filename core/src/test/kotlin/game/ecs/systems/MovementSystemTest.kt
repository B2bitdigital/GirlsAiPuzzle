package game.ecs.systems

import org.junit.Assert.*
import org.junit.Test

class MovementSystemTest {

    private val ms = MovementSystem(cellSize = 10f, fieldWidth = 100f, fieldHeight = 100f)

    @Test
    fun `player moves toward target on x axis`() {
        val pos = floatArrayOf(20f, 50f)
        ms.movePlayerToward(pos, targetX = 50f, targetY = 50f, speed = 100f, delta = 0.1f)
        assertTrue("should move right", pos[0] > 20f)
        assertEquals("y unchanged while moving horizontally", 50f, pos[1], 0.01f)
    }

    @Test
    fun `player stops at target`() {
        val pos = floatArrayOf(49f, 50f)
        ms.movePlayerToward(pos, targetX = 50f, targetY = 50f, speed = 100f, delta = 0.1f)
        assertEquals(50f, pos[0], 0.01f)
    }

    @Test
    fun `player clamps to field bounds`() {
        val pos = floatArrayOf(95f, 50f)
        ms.movePlayerToward(pos, targetX = 200f, targetY = 50f, speed = 100f, delta = 1f)
        assertTrue("x should not exceed field width", pos[0] <= 100f)
    }

    @Test
    fun `player moves y after reaching target x`() {
        val pos = floatArrayOf(50f, 20f)
        ms.movePlayerToward(pos, targetX = 50f, targetY = 50f, speed = 100f, delta = 0.1f)
        assertTrue("should move up", pos[1] > 20f)
    }
}
