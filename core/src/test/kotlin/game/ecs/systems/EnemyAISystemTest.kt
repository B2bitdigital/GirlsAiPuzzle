package game.ecs.systems

import game.ecs.EnemyType
import org.junit.Assert.*
import org.junit.Test

class EnemyAISystemTest {

    // Minimal 10x10 grid for testing
    private fun makeGrid(cols: Int = 10, rows: Int = 10): Array<BooleanArray> {
        val g = Array(cols) { BooleanArray(rows) }
        for (c in 0 until cols) { g[c][0] = true; g[c][rows - 1] = true }
        for (r in 0 until rows) { g[0][r] = true; g[cols - 1][r] = true }
        return g
    }

    private val ai = EnemyAISystem(cellSize = 10f, fieldWidth = 100f, fieldHeight = 100f)

    @Test
    fun `frozen enemy does not move`() {
        val pos = floatArrayOf(50f, 50f)
        ai.updateEnemy(
            type = EnemyType.SPIDER,
            pos = pos, dirX = floatArrayOf(1f), dirY = floatArrayOf(0f),
            speed = 60f, freezeTimer = 1f, delta = 0.1f,
            grid = makeGrid(), playerX = 20f, playerY = 20f
        )
        assertEquals(50f, pos[0], 0.01f)
        assertEquals(50f, pos[1], 0.01f)
    }

    @Test
    fun `cockroach bounces on field boundary`() {
        val pos = floatArrayOf(95f, 50f)
        val dirX = floatArrayOf(1f)
        val dirY = floatArrayOf(0f)
        ai.updateEnemy(
            type = EnemyType.COCKROACH,
            pos = pos, dirX = dirX, dirY = dirY,
            speed = 100f, freezeTimer = 0f, delta = 0.1f,
            grid = makeGrid(), playerX = 20f, playerY = 20f
        )
        assertEquals(-1f, dirX[0], 0.01f)
    }

    @Test
    fun `wasp moves toward player`() {
        val pos = floatArrayOf(20f, 20f)
        val dirX = floatArrayOf(0f)
        val dirY = floatArrayOf(0f)
        ai.updateEnemy(
            type = EnemyType.WASP,
            pos = pos, dirX = dirX, dirY = dirY,
            speed = 80f, freezeTimer = 0f, delta = 0.1f,
            grid = makeGrid(), playerX = 70f, playerY = 20f
        )
        assertTrue("wasp should move toward player (right)", pos[0] > 20f)
    }

    @Test
    fun `spider stays within field bounds`() {
        val pos = floatArrayOf(50f, 5f)
        val dirX = floatArrayOf(1f)
        val dirY = floatArrayOf(0f)
        repeat(100) {
            ai.updateEnemy(
                type = EnemyType.SPIDER,
                pos = pos, dirX = dirX, dirY = dirY,
                speed = 60f, freezeTimer = 0f, delta = 0.1f,
                grid = makeGrid(), playerX = 50f, playerY = 50f
            )
        }
        assertTrue(pos[0] in 0f..100f)
        assertTrue(pos[1] in 0f..100f)
    }
}
