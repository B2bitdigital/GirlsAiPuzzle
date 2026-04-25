package game.ecs.systems

import game.GameConstants
import org.junit.Assert.*
import org.junit.Test

class MovementDebugTest {
    
    @Test
    fun `player on top border can move down into free field`() {
        val ms = MovementSystem()
        
        // Grid 48×76, cell size 9.539474
        val grid = Array(48) { BooleanArray(76) { false } }
        val isPerimeter = Array(48) { BooleanArray(76) { false } }
        
        // Set borders as perimeter
        for (c in 0 until 48) {
            isPerimeter[c][0] = true // bottom
            isPerimeter[c][75] = true // top
        }
        for (r in 0 until 76) {
            isPerimeter[0][r] = true // left
            isPerimeter[47][r] = true // right
        }
        
        // Player at center top border: x = 230f (PLAY_WIDTH/2), y = 715.46f (PLAY_HEIGHT - CELL_SIZE)
        val pos = floatArrayOf(230f, 715.46f)
        println("Initial position: x=${pos[0]}, y=${pos[1]}")
        
        // Convert to grid
        val gridPt = ms.toGridPoint(pos[0], pos[1])
        println("Grid position: col=${gridPt.col}, row=${gridPt.row}")
        println("Is perimeter: ${isPerimeter[gridPt.col][gridPt.row]}")
        
        // Try to move DOWN (dirY = -1) into free field
        val moved = ms.movePlayer(
            pos, dirX = 0f, dirY = -1f,
            speed = 150f, delta = 0.016f,
            grid = grid,
            cols = 48, rows = 76
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
