package game.ecs.systems

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TerritorySystemTest {

    private lateinit var ts: TerritorySystem

    @Before
    fun setup() {
        ts = TerritorySystem(cols = 10, rows = 10)
    }

    @Test
    fun `border cells are conquered on init`() {
        for (c in 0 until 10) {
            assertTrue("top border c=$c", ts.grid[c][0])
            assertTrue("bottom border c=$c", ts.grid[c][9])
        }
        for (r in 0 until 10) {
            assertTrue("left border r=$r", ts.grid[0][r])
            assertTrue("right border r=$r", ts.grid[9][r])
        }
    }

    @Test
    fun `interior cells are free on init`() {
        assertFalse(ts.grid[5][5])
        assertFalse(ts.grid[3][3])
    }

    @Test
    fun `conquered percent accounts for border`() {
        // 10x10 = 100 cells. Border = 2*(10+8) = 36 cells.
        val pct = ts.conqueredPercent()
        assertEquals(36f, pct, 0.1f)
    }

    @Test
    fun `close line with no enemies conquers enclosed interior`() {
        // Line at col=3 from row=0 to row=9 splits field
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))

        val result = ts.closeLine(emptyList(), emptyList())
        assertTrue(result is CloseResult.Success)
        // Both sides (cols 1-2 and cols 4-8) should be conquered — no enemies
        assertTrue(ts.grid[1][5])
        assertTrue(ts.grid[5][5])
    }

    @Test
    fun `close line with dangerous enemy — enemy side stays free, other side captured`() {
        // Line at col=3: left region = cols 1-2, right region = cols 4-8
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))

        // Enemy at (1,4) — inside left region
        val result = ts.closeLine(
            dangerousEnemies = listOf(GridPoint(1, 4)),
            snails = emptyList()
        )
        assertTrue(result is CloseResult.Success)
        // Left region (enemy side) stays free
        assertFalse("enemy side should remain free", ts.grid[1][5])
        // Right region (no enemy) gets conquered
        assertTrue("far side should be conquered", ts.grid[5][5])
        // Line cells become permanent border
        assertTrue("line cells conquered", ts.grid[3][4])
    }

    @Test
    fun `close line with snail in enclosed region gives bonus`() {
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))

        val result = ts.closeLine(
            dangerousEnemies = emptyList(),
            snails = listOf(GridPoint(1, 4))
        )
        assertTrue(result is CloseResult.Success)
        assertEquals(1, (result as CloseResult.Success).snailsTrapped)
    }

    @Test
    fun `isOnSafeZone returns true for conquered cell`() {
        assertTrue(ts.isOnSafeZone(GridPoint(0, 0)))
        assertTrue(ts.isOnSafeZone(GridPoint(5, 0)))
    }

    @Test
    fun `isOnSafeZone returns false for free cell`() {
        assertFalse(ts.isOnSafeZone(GridPoint(5, 5)))
    }

    @Test
    fun `conqueredPercent increases after successful close`() {
        val before = ts.conqueredPercent()
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))
        ts.closeLine(emptyList(), emptyList())
        assertTrue(ts.conqueredPercent() > before)
    }

    @Test
    fun `isOnPerimeter returns true for border field edge`() {
        // Field edge cells always have out-of-bounds neighbor → perimeter
        assertTrue(ts.isOnPerimeter(GridPoint(0, 0)))
        assertTrue(ts.isOnPerimeter(GridPoint(5, 0)))
    }

    @Test
    fun `isOnPerimeter returns true for conquered cell adjacent to free cell`() {
        // col=0 row=5: conquered, neighbor (1,5) is free → perimeter
        assertTrue(ts.isOnPerimeter(GridPoint(0, 5)))
    }

    @Test
    fun `isOnPerimeter returns false for free cell`() {
        assertFalse(ts.isOnPerimeter(GridPoint(5, 5)))
    }

    @Test
    fun `isOnPerimeter returns false for interior conquered cell`() {
        // Conquer entire grid manually (simulating total capture)
        for (c in 0 until 10) for (r in 0 until 10) ts.grid[c][r] = true
        // Cell (5,5): all 4 neighbors are conquered, not on field edge → interior
        assertFalse(ts.isOnPerimeter(GridPoint(5, 5)))
    }
}
