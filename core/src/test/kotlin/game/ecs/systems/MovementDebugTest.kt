package game.ecs.systems

import game.GameConstants
import org.junit.Assert.*
import org.junit.Test

class MovementDebugTest {

    @Test
    fun `player on top border can move down into free field`() {
        val ms = MovementSystem()

        // Grid 48×76, cell size 9.539474
        val cols = 48
        val rows = 76
        val cells = Array(cols) { Array(rows) { CellType.FREE } }

        // Set frame as CONQUERED
        for (c in 0 until cols) {
            cells[c][0] = CellType.CONQUERED   // bottom
            cells[c][rows - 1] = CellType.CONQUERED // top
        }
        for (r in 0 until rows) {
            cells[0][r] = CellType.CONQUERED   // left
            cells[cols - 1][r] = CellType.CONQUERED // right
        }

        // Player at center top border: x = 230f (PLAY_WIDTH/2), y = 715.46f (PLAY_HEIGHT - CELL_SIZE)
        val pos = floatArrayOf(230f, 715.46f)
        println("Initial position: x=${pos[0]}, y=${pos[1]}")

        // Convert to grid
        val gridPt = ms.toGridPoint(pos[0], pos[1])
        println("Grid position: col=${gridPt.col}, row=${gridPt.row}")
        println("Is frame (CONQUERED): ${cells[gridPt.col][gridPt.row] == CellType.CONQUERED}")

        // Try to move DOWN (dirY = -1) into free field
        val moved = ms.movePlayer(
            pos, dirX = 0f, dirY = -1f,
            speed = 150f, delta = 0.016f,
            cells = cells,
            cols = cols, rows = rows
        )

        println("After move attempt: x=${pos[0]}, y=${pos[1]}")
        println("Movement returned: $moved")

        assertTrue("player should be able to move down from top border", moved)
        assertTrue("y position should decrease", pos[1] < 715.46f)
    }

    @Test
    fun `verify boundary calculations`() {
        val cols = GameConstants.GRID_COLS
        val rows = GameConstants.GRID_ROWS
        val cellSize = GameConstants.CELL_SIZE

        println("GRID_COLS: $cols")
        println("GRID_ROWS: $rows")
        println("CELL_SIZE: $cellSize")
        println("cols * cellSize = ${cols * cellSize}")
        println("rows * cellSize = ${rows * cellSize}")
        println("FIELD_WIDTH: ${GameConstants.FIELD_WIDTH}")
        println("PLAY_HEIGHT: ${GameConstants.PLAY_HEIGHT}")

        val maxX = cols * cellSize
        val maxY = rows * cellSize

        println("\nBoundaries check:")
        println("x < 0 or x >= $maxX")
        println("y < 0 or y >= $maxY")
    }
}
