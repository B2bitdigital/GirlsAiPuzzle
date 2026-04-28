package game.screens

import game.GameConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRenderUtilsTest {

    // Coordinate reference:
    // ox=20, oy=20, cs=10, GRID_COLS=44, GRID_ROWS=66
    // Left snap  = ox + cs               = 30f
    // Right snap = ox + (GRID_COLS-1)*cs = 450f
    // Bottom snap= oy + cs               = 30f
    // Top snap   = oy + (GRID_ROWS-1)*cs = 670f

    @Test
    fun `interior position unchanged`() {
        val (rx, ry) = playerRenderPos(100f, 200f)
        assertEquals(100f, rx, 0f)
        assertEquals(200f, ry, 0f)
    }

    @Test
    fun `left border column snaps x to ox+cs`() {
        // posX=25f → col=(25-20)/10=0 → snap
        val (rx, _) = playerRenderPos(25f, 100f)
        assertEquals(30f, rx, 0f)
    }

    @Test
    fun `right border column snaps x to ox+(GRID_COLS-1)*cs`() {
        // posX=455f → col=(455-20)/10=43 → snap
        val (rx, _) = playerRenderPos(455f, 100f)
        assertEquals(450f, rx, 0f)
    }

    @Test
    fun `bottom border row snaps y to oy+cs`() {
        // posY=25f → row=(25-20)/10=0 → snap
        val (_, ry) = playerRenderPos(100f, 25f)
        assertEquals(30f, ry, 0f)
    }

    @Test
    fun `top border row snaps y to oy+(GRID_ROWS-1)*cs`() {
        // posY=675f → row=(675-20)/10=65 → snap
        val (_, ry) = playerRenderPos(100f, 675f)
        assertEquals(670f, ry, 0f)
    }

    @Test
    fun `corner snaps both axes`() {
        val (rx, ry) = playerRenderPos(25f, 25f)
        assertEquals(30f, rx, 0f)
        assertEquals(30f, ry, 0f)
    }

    @Test
    fun `posX at field left edge snaps`() {
        // posX=20f → col=(20-20)/10=0 → snap
        val (rx, _) = playerRenderPos(20f, 100f)
        assertEquals(30f, rx, 0f)
    }

    @Test
    fun `posX beyond field right edge coerces to right snap`() {
        // posX=460f → col=(460-20)/10=44 coerced to 43 → snap
        val (rx, _) = playerRenderPos(460f, 100f)
        assertEquals(450f, rx, 0f)
    }

    @Test
    fun `first interior column does not snap`() {
        // posX=35f → col=(35-20)/10=1 → no snap
        val (rx, _) = playerRenderPos(35f, 100f)
        assertEquals(35f, rx, 0f)
    }

    @Test
    fun `last interior column does not snap`() {
        // posX=449f → col=(449-20)/10=42 → no snap
        val (rx, _) = playerRenderPos(449f, 100f)
        assertEquals(449f, rx, 0f)
    }

    @Test
    fun `posX exactly at right snap value is right border`() {
        // posX=451f → col=(451-20)/10=43.1→43 → snap to 450f (not 451f)
        val (rx, _) = playerRenderPos(451f, 100f)
        assertEquals(450f, rx, 0f)
    }
}
