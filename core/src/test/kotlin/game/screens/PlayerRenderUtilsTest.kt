package game.screens

import game.GameConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRenderUtilsTest {

    private val CS = GameConstants.CELL_SIZE      // 10f
    private val FW = GameConstants.FIELD_WIDTH    // 480f
    private val PH = GameConstants.PLAY_HEIGHT    // 740f

    @Test
    fun `interior position unchanged`() {
        val (rx, ry) = playerRenderPos(50f, 50f)
        assertEquals(50f, rx, 0f)
        assertEquals(50f, ry, 0f)
    }

    @Test
    fun `left border column snaps x to 0`() {
        val (rx, _) = playerRenderPos(5f, 50f)    // col 0, center
        assertEquals(0f, rx, 0f)
    }

    @Test
    fun `right border column snaps x to FIELD_WIDTH`() {
        val (rx, _) = playerRenderPos(475f, 50f)  // col 47
        assertEquals(FW, rx, 0f)
    }

    @Test
    fun `top border row snaps y to 0`() {
        val (_, ry) = playerRenderPos(50f, 5f)    // row 0
        assertEquals(0f, ry, 0f)
    }

    @Test
    fun `bottom border row snaps y to PLAY_HEIGHT`() {
        val (_, ry) = playerRenderPos(50f, 735f)  // row 73
        assertEquals(PH, ry, 0f)
    }

    @Test
    fun `corner snaps both axes`() {
        val (rx, ry) = playerRenderPos(5f, 735f)  // col 0, row 73
        assertEquals(0f, rx, 0f)
        assertEquals(PH, ry, 0f)
    }

    @Test
    fun `x at exactly 0 maps to 0`() {
        val (rx, _) = playerRenderPos(0f, 50f)
        assertEquals(0f, rx, 0f)
    }

    @Test
    fun `x just below right border column stays on right snap`() {
        val (rx, _) = playerRenderPos(479.9f, 50f)
        assertEquals(FW, rx, 0f)
    }

    @Test
    fun `x at exactly 0 y at exactly 0 snaps y to 0`() {
        val (_, ry) = playerRenderPos(50f, 0f)
        assertEquals(0f, ry, 0f)
    }

    @Test
    fun `x at first pixel of col 1 does not snap`() {
        val (rx, _) = playerRenderPos(10f, 50f)   // col 1, first interior col
        assertEquals(10f, rx, 0f)
    }

    @Test
    fun `x at first pixel of col 47 snaps to FIELD_WIDTH`() {
        val (rx, _) = playerRenderPos(470f, 50f)   // col 47, right perimeter
        assertEquals(GameConstants.FIELD_WIDTH, rx, 0f)
    }
}
