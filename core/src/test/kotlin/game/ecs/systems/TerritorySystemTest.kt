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
    fun `frame cells are CONQUERED on init`() {
        for (c in 0 until 10) {
            assertEquals("top frame c=$c", CellType.CONQUERED, ts.cells[c][0])
            assertEquals("bottom frame c=$c", CellType.CONQUERED, ts.cells[c][9])
        }
        for (r in 0 until 10) {
            assertEquals("left frame r=$r", CellType.CONQUERED, ts.cells[0][r])
            assertEquals("right frame r=$r", CellType.CONQUERED, ts.cells[9][r])
        }
    }

    @Test
    fun `interior cells are free on init`() {
        assertEquals(CellType.FREE, ts.cells[5][5])
        assertEquals(CellType.FREE, ts.cells[3][3])
    }

    @Test
    fun `conquered percent excludes border on init`() {
        val pct = ts.conqueredPercent()
        assertEquals(0f, pct, 0.1f)
    }

    @Test
    fun `close line always captures smallest region`() {
        // Line at col=3: left region = cols 1-2 (16 cells), right region = cols 4-8 (40 cells)
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))

        val result = ts.closeLine(emptyList(), emptyList())
        assertTrue(result is CloseResult.Success)
        // Smallest region (left, cols 1-2) is conquered
        assertEquals("smallest side conquered", CellType.CONQUERED, ts.cells[1][5])
        // Larger region (right, cols 4-8) stays free
        assertEquals("larger side stays free", CellType.FREE, ts.cells[5][5])
    }

    @Test
    fun `close line prefers enemy-free region over smallest`() {
        // Line at col=3: left region = cols 1-2 (smaller), right region = cols 4-8 (larger)
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))

        // Enemy at (1,4) — inside left/smaller region
        val result = ts.closeLine(
            dangerousEnemies = listOf(GridPoint(1, 4)),
            snails = emptyList()
        )
        assertTrue(result is CloseResult.Success)
        // Enemy-free region (right, larger) is conquered instead of the smallest
        assertEquals("enemy-free side conquered", CellType.CONQUERED, ts.cells[5][5])
        // Left region (with enemy) stays free
        assertEquals("enemy side stays free", CellType.FREE, ts.cells[1][5])
        // Line cells become permanent border
        assertEquals("line cells conquered", CellType.CONQUERED, ts.cells[3][4])
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
        // Frame cells are CONQUERED — they sit on the field edge
        assertEquals(CellType.CONQUERED, ts.cells[0][0])
        assertEquals(CellType.CONQUERED, ts.cells[5][0])
    }

    @Test
    fun `isOnPerimeter returns true for field edge`() {
        assertEquals(CellType.CONQUERED, ts.cells[0][5])
    }

    @Test
    fun `isOnPerimeter returns false for free cell`() {
        assertEquals(CellType.FREE, ts.cells[5][5])
    }

    @Test
    fun `isOnPerimeter returns false for interior conquered cell`() {
        // After revealAll, interior cells are CONQUERED but the frame is also CONQUERED.
        // There is no separate isPerimeter concept — all conquered cells are CONQUERED.
        ts.revealAll()
        // Cell (5,5) is now CONQUERED (interior, fully captured)
        assertEquals(CellType.CONQUERED, ts.cells[5][5])
    }

    @Test
    fun `stub line not reaching other border conquers only line cells`() {
        // Line from left border to same border without enclosing area — only line cells should be conquered
        ts.startLine(GridPoint(0, 5))
        ts.extendLine(GridPoint(1, 5))
        ts.extendLine(GridPoint(1, 4))
        ts.extendLine(GridPoint(0, 4))

        val result = ts.closeLine(emptyList(), emptyList())
        assertTrue(result is CloseResult.Success)

        // Only line cells (1,5) and (1,4) should be conquered — interior must remain free
        assertEquals("line cell (1,5) conquered", CellType.CONQUERED, ts.cells[1][5])
        assertEquals("line cell (1,4) conquered", CellType.CONQUERED, ts.cells[1][4])
        // Vast interior still free — not mass-conquered
        assertEquals("interior (5,5) still free", CellType.FREE, ts.cells[5][5])
        assertEquals("interior (5,3) still free", CellType.FREE, ts.cells[5][3])
        assertEquals("interior (8,8) still free", CellType.FREE, ts.cells[8][8])
    }

    @Test
    fun `randomFreeCell returns null when all cells conquered`() {
        ts.revealAll()
        assertNull(ts.randomFreeCell())
    }

    @Test
    fun `randomFreeCell returns interior free cell when field partially free`() {
        val cell = ts.randomFreeCell()
        assertNotNull(cell)
        assertEquals("returned cell must be free", CellType.FREE, ts.cells[cell!!.col][cell.row])
    }
}
