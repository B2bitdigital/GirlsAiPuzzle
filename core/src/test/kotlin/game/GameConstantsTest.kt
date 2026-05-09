package game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameConstantsTest {

    @Test
    fun `GRID_COLS times CELL_SIZE equals PLAY_WIDTH`() {
        assertEquals(GameConstants.PLAY_WIDTH, GameConstants.GRID_COLS * GameConstants.CELL_SIZE, 0f)
    }

    @Test
    fun `GRID_ROWS times CELL_SIZE equals PLAY_HEIGHT`() {
        assertEquals(GameConstants.PLAY_HEIGHT, GameConstants.GRID_ROWS * GameConstants.CELL_SIZE, 0f)
    }
}
