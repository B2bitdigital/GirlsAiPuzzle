package game.screens

import game.GameConstants
import game.ecs.systems.GridPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRenderUtilsTest {

    // Coordinate reference:
    // ox=20, oy=20, cs=5, GRID_COLS=88, GRID_ROWS=132
    // Left snap  = ox + cs               = 25f
    // Right snap = ox + (GRID_COLS-1)*cs = 455f
    // Bottom snap= oy + cs               = 25f
    // Top snap   = oy + (GRID_ROWS-1)*cs = 675f

    @Test
    fun `interior position unchanged`() {
        val (rx, ry) = playerRenderPos(100f, 200f)
        assertEquals(100f, rx, 0f)
        assertEquals(200f, ry, 0f)
    }

    @Test
    fun `left border column snaps x to ox+cs`() {
        // posX=22f → col=(22-20)/5=0 → snap
        val (rx, _) = playerRenderPos(22f, 100f)
        assertEquals(25f, rx, 0f)
    }

    @Test
    fun `right border column snaps x to ox+(GRID_COLS-1)*cs`() {
        // posX=457f → col=(457-20)/5=87 → snap
        val (rx, _) = playerRenderPos(457f, 100f)
        assertEquals(455f, rx, 0f)
    }

    @Test
    fun `bottom border row snaps y to oy+cs`() {
        // posY=22f → row=(22-20)/5=0 → snap
        val (_, ry) = playerRenderPos(100f, 22f)
        assertEquals(25f, ry, 0f)
    }

    @Test
    fun `top border row snaps y to oy+(GRID_ROWS-1)*cs`() {
        // posY=677f → row=(677-20)/5=131 → snap
        val (_, ry) = playerRenderPos(100f, 677f)
        assertEquals(675f, ry, 0f)
    }

    @Test
    fun `corner snaps both axes`() {
        val (rx, ry) = playerRenderPos(22f, 22f)
        assertEquals(25f, rx, 0f)
        assertEquals(25f, ry, 0f)
    }

    @Test
    fun `posX at field left edge snaps`() {
        // posX=20f → col=(20-20)/5=0 → snap
        val (rx, _) = playerRenderPos(20f, 100f)
        assertEquals(25f, rx, 0f)
    }

    @Test
    fun `posX beyond field right edge coerces to right snap`() {
        // posX=461f → col=(461-20)/5=88 coerced to 87 → snap
        val (rx, _) = playerRenderPos(461f, 100f)
        assertEquals(455f, rx, 0f)
    }

    @Test
    fun `first interior column does not snap`() {
        // posX=27f → col=(27-20)/5=1 → no snap
        val (rx, _) = playerRenderPos(27f, 100f)
        assertEquals(27f, rx, 0f)
    }

    @Test
    fun `last interior column does not snap`() {
        // posX=454f → col=(454-20)/5=86 → no snap
        val (rx, _) = playerRenderPos(454f, 100f)
        assertEquals(454f, rx, 0f)
    }

    @Test
    fun `posX exactly at right snap value is right border`() {
        // posX=456f → col=(456-20)/5=87.2→87 → snap to 455f (not 456f)
        val (rx, _) = playerRenderPos(456f, 100f)
        assertEquals(455f, rx, 0f)
    }

    // gridPointRenderPos tests
    // Interior cell centre = ox + col*cs + cs/2f, oy + row*cs + cs/2f

    @Test
    fun `gridPoint interior returns cell centre`() {
        val (x, y) = gridPointRenderPos(GridPoint(5, 5))
        assertEquals(47.5f, x, 0f)   // 20 + 5*5 + 2.5 = 47.5
        assertEquals(47.5f, y, 0f)
    }

    @Test
    fun `gridPoint left perimeter snaps x to ox+cs`() {
        val (x, y) = gridPointRenderPos(GridPoint(0, 5))
        assertEquals(25f, x, 0f)
        assertEquals(47.5f, y, 0f)   // interior row 5: 20 + 5*5 + 2.5 = 47.5f
    }

    @Test
    fun `gridPoint right perimeter snaps x to ox+(GRID_COLS-1)*cs`() {
        val (x, y) = gridPointRenderPos(GridPoint(87, 5))
        assertEquals(455f, x, 0f)
        assertEquals(47.5f, y, 0f)   // interior row 5: 20 + 5*5 + 2.5 = 47.5f
    }

    @Test
    fun `gridPoint bottom perimeter snaps y to oy+cs`() {
        val (x, y) = gridPointRenderPos(GridPoint(5, 0))
        assertEquals(47.5f, x, 0f)   // interior col 5: 20 + 5*5 + 2.5 = 47.5f
        assertEquals(25f, y, 0f)
    }

    @Test
    fun `gridPoint top perimeter snaps y to oy+(GRID_ROWS-1)*cs`() {
        val (x, y) = gridPointRenderPos(GridPoint(5, 131))
        assertEquals(47.5f, x, 0f)   // interior col 5: 20 + 5*5 + 2.5 = 47.5f
        assertEquals(675f, y, 0f)
    }

    @Test
    fun `gridPoint corner snaps both axes`() {
        val (x, y) = gridPointRenderPos(GridPoint(0, 0))
        assertEquals(25f, x, 0f)
        assertEquals(25f, y, 0f)
    }

    @Test
    fun `gridPoint first interior cell returns centre`() {
        val (x, y) = gridPointRenderPos(GridPoint(1, 1))
        assertEquals(27.5f, x, 0f)   // 20 + 1*5 + 2.5 = 27.5
        assertEquals(27.5f, y, 0f)
    }
}
