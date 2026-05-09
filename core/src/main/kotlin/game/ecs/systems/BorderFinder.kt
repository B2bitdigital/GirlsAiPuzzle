package game.ecs.systems

object BorderFinder {

    private val DIRS = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)

    fun currentBorderCells(
        cells: Array<Array<CellType>>,
        cols: Int,
        rows: Int
    ): List<GridPoint> {
        val result = mutableListOf<GridPoint>()
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                if (cells[c][r] != CellType.CONQUERED) continue
                val touchesFree = DIRS.any { (dc, dr) ->
                    val nc = c + dc; val nr = r + dr
                    nc in 0 until cols && nr in 0 until rows && cells[nc][nr] == CellType.FREE
                }
                if (touchesFree) result.add(GridPoint(c, r))
            }
        }
        return result
    }

    fun randomBorderCell(
        cells: Array<Array<CellType>>,
        cols: Int,
        rows: Int
    ): GridPoint? = currentBorderCells(cells, cols, rows).randomOrNull()
}
