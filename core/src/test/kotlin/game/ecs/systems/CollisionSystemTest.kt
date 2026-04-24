package game.ecs.systems

import org.junit.Assert.*
import org.junit.Test

class CollisionSystemTest {

    private val cs = CollisionSystem(cellSize = 10f)

    @Test
    fun `player and spider at same cell = collision`() {
        assertTrue(cs.playerHitByEnemy(px = 50f, py = 50f, ex = 50f, ey = 50f))
    }

    @Test
    fun `player and spider one cell apart = no collision`() {
        assertFalse(cs.playerHitByEnemy(px = 50f, py = 50f, ex = 60f, ey = 50f))
    }

    @Test
    fun `enemy overlapping line cell = line hit`() {
        val line = listOf(GridPoint(3, 3), GridPoint(3, 4), GridPoint(3, 5))
        // Enemy at pixel (35, 35) = grid (3, 3)
        assertTrue(cs.enemyHitsLine(ex = 35f, ey = 35f, line = line))
    }

    @Test
    fun `enemy not on any line cell = no line hit`() {
        val line = listOf(GridPoint(3, 3), GridPoint(3, 4))
        assertFalse(cs.enemyHitsLine(ex = 20f, ey = 20f, line = line))
    }

    @Test
    fun `player collects powerup within pickup radius`() {
        assertTrue(cs.playerCollectsPowerup(px = 50f, py = 50f, pwx = 54f, pwy = 54f))
    }

    @Test
    fun `player too far from powerup = no collect`() {
        assertFalse(cs.playerCollectsPowerup(px = 50f, py = 50f, pwx = 80f, pwy = 80f))
    }
}
