package game.ecs.systems

import org.junit.Assert.*
import org.junit.Test

class MovementSystemTest {

    // 10×10 field, cellSize=10 → 100×100 pixels
    private val ms = MovementSystem(cellSize = 10f, fieldWidth = 100f, fieldHeight = 100f)

    private fun emptyGrid(cols: Int = 10, rows: Int = 10): Array<BooleanArray> =
        Array(cols) { BooleanArray(rows) }

    private fun borderedGrid(cols: Int = 10, rows: Int = 10): Array<BooleanArray> {
        val g = emptyGrid(cols, rows)
        for (c in 0 until cols) { g[c][0] = true; g[c][rows - 1] = true }
        for (r in 0 until rows) { g[0][r] = true; g[cols - 1][r] = true }
        return g
    }

    @Test
    fun `movePlayer moves right on free grid`() {
        val grid = borderedGrid()
        val pos = floatArrayOf(50f, 50f)
        val moved = ms.movePlayer(pos, dirX = 1f, dirY = 0f, speed = 100f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10)
        assertTrue("should return true", moved)
        assertTrue("x should increase", pos[0] > 50f)
        assertEquals("y unchanged", 50f, pos[1], 0.01f)
    }

    @Test
    fun `movePlayer moves up on free grid`() {
        val grid = borderedGrid()
        val pos = floatArrayOf(50f, 50f)
        val moved = ms.movePlayer(pos, dirX = 0f, dirY = 1f, speed = 100f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10)
        assertTrue(moved)
        assertTrue(pos[1] > 50f)
        assertEquals(50f, pos[0], 0.01f)
    }

    @Test
    fun `movePlayer returns false when hitting right boundary`() {
        val grid = borderedGrid()
        val pos = floatArrayOf(98f, 50f)
        val moved = ms.movePlayer(pos, dirX = 1f, dirY = 0f, speed = 200f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10)
        assertFalse("should be blocked at boundary", moved)
    }

    @Test
    fun `movePlayer returns false when hitting left boundary`() {
        val grid = borderedGrid()
        val pos = floatArrayOf(2f, 50f)
        val moved = ms.movePlayer(pos, dirX = -1f, dirY = 0f, speed = 200f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10)
        assertFalse(moved)
    }

    @Test
    fun `movePlayer returns false when next cell is interior conquered`() {
        // Fully conquered 10×10 grid
        val grid = Array(10) { BooleanArray(10) { true } }
        // Player at pixel (15,50) → cell (1,5). Moving right → next cell (2,5).
        // Cell (2,5): all 4 neighbors are conquered (in-bounds) → interior → blocked.
        val pos = floatArrayOf(15f, 50f)
        val moved = ms.movePlayer(pos, dirX = 1f, dirY = 0f, speed = 100f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10)
        assertFalse("interior conquered cell should block movement", moved)
    }

    @Test
    fun `movePlayer allows entering free cell from perimeter`() {
        val grid = borderedGrid()
        // Player at (5, 5) → cell (0,0) border. Move right into free cell (1,0)... 
        // Actually start at border cell (0, 55) → cell (0,5). Move right into free cell (1,5).
        val pos = floatArrayOf(5f, 55f)
        val moved = ms.movePlayer(pos, dirX = 1f, dirY = 0f, speed = 100f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10)
        assertTrue("should be able to enter free zone", moved)
    }

    @Test
    fun `toGridPoint converts pixel to grid cell`() {
        val pt = ms.toGridPoint(35f, 55f)
        assertEquals(3, pt.col)
        assertEquals(5, pt.row)
    }

    @Test
    fun `toGridPoint clamps to valid range`() {
        val pt = ms.toGridPoint(-10f, 999f)
        assertEquals(0, pt.col)
        assertEquals(9, pt.row)
    }
}
