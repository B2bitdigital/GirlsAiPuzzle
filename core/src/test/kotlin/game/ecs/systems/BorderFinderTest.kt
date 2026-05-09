package game.ecs.systems

import org.junit.Assert.*
import org.junit.Test

class BorderFinderTest {

    private fun borderedGrid(cols: Int = 5, rows: Int = 5): Array<Array<CellType>> {
        val cells = Array(cols) { Array(rows) { CellType.FREE } }
        for (c in 0 until cols) {
            cells[c][0] = CellType.CONQUERED
            cells[c][rows - 1] = CellType.CONQUERED
        }
        for (r in 0 until rows) {
            cells[0][r] = CellType.CONQUERED
            cells[cols - 1][r] = CellType.CONQUERED
        }
        return cells
    }

    @Test
    fun `initial perimeter cells are all border cells`() {
        val cells = borderedGrid()
        val border = BorderFinder.currentBorderCells(cells, 5, 5)
        assertTrue(border.contains(GridPoint(1, 0)))
        assertTrue(border.contains(GridPoint(0, 1)))
        assertTrue(border.contains(GridPoint(4, 3)))
    }

    @Test
    fun `fully surrounded CONQUERED cell is not a border cell`() {
        val cells = borderedGrid(5, 5)
        // Fill entire interior — (2,2) is surrounded on all 4 sides by CONQUERED
        for (c in 1..3) for (r in 1..3) cells[c][r] = CellType.CONQUERED
        val border = BorderFinder.currentBorderCells(cells, 5, 5)
        assertFalse("fully surrounded cell should not be border", border.contains(GridPoint(2, 2)))
    }

    @Test
    fun `CONQUERED cell adjacent to FREE cell is a border cell`() {
        val cells = borderedGrid(5, 5)
        cells[1][1] = CellType.CONQUERED  // touches FREE cells on 2 sides
        val border = BorderFinder.currentBorderCells(cells, 5, 5)
        assertTrue(border.contains(GridPoint(1, 1)))
    }

    @Test
    fun `randomBorderCell returns null when no FREE cells exist`() {
        val cells = Array(3) { Array(3) { CellType.CONQUERED } }
        assertNull(BorderFinder.randomBorderCell(cells, 3, 3))
    }

    @Test
    fun `randomBorderCell returns a CONQUERED cell`() {
        val cells = borderedGrid(5, 5)
        val result = BorderFinder.randomBorderCell(cells, 5, 5)
        assertNotNull(result)
        assertEquals(CellType.CONQUERED, cells[result!!.col][result.row])
    }
}
