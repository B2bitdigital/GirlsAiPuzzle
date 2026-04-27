package game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameConstantsTest {
    @Test
    fun `PLAYER_DIAMOND_HALF equals CELL_SIZE`() {
        assertEquals(GameConstants.CELL_SIZE, GameConstants.PLAYER_DIAMOND_HALF, 0f)
    }
}
