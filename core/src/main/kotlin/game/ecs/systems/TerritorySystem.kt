package game.ecs.systems

data class GridPoint(val col: Int, val row: Int)

sealed class CloseResult {
    object Empty : CloseResult()
    object EnemyTrapped : CloseResult()
    data class Success(val snailsTrapped: Int = 0) : CloseResult()
}

class TerritorySystem(
    val cols: Int = game.GameConstants.GRID_COLS,
    val rows: Int = game.GameConstants.GRID_ROWS
) {
    // true = conquered/border, false = free
    val grid: Array<BooleanArray> = Array(cols) { BooleanArray(rows) }

    // cells in the line currently being drawn (ordered)
    private val _currentLine = mutableListOf<GridPoint>()
    val currentLine: List<GridPoint> get() = _currentLine

    var isDrawing: Boolean = false
        private set

    init { initBorders() }

    private fun initBorders() {
        for (c in 0 until cols) { grid[c][0] = true; grid[c][rows - 1] = true }
        for (r in 0 until rows) { grid[0][r] = true; grid[cols - 1][r] = true }
    }

    fun conqueredPercent(): Float {
        val total = cols * rows
        val conquered = grid.sumOf { col -> col.count { it } }
        return conquered.toFloat() / total * 100f
    }

    fun isOnSafeZone(pt: GridPoint): Boolean =
        pt.col in 0 until cols && pt.row in 0 until rows && grid[pt.col][pt.row]

    fun startLine(startPt: GridPoint) {
        _currentLine.clear()
        _currentLine.add(startPt)
        isDrawing = true
    }

    fun extendLine(pt: GridPoint) {
        if (pt !in _currentLine) _currentLine.add(pt)
    }

    /** Call when player returns to safe zone.
     *  dangerousEnemies: spider, cockroach, wasp grid positions.
     *  snails: snail grid positions (bonus if trapped).
     */
    fun closeLine(
        dangerousEnemies: List<GridPoint>,
        snails: List<GridPoint>
    ): CloseResult {
        if (_currentLine.size < 2) {
            _currentLine.clear()
            isDrawing = false
            return CloseResult.Empty
        }

        val lineCells = _currentLine.toSet()

        // Temporarily mark line cells as conquered
        lineCells.forEach { grid[it.col][it.row] = true }

        // Flood fill from virtual border to find outer-reachable FREE cells
        val outerFree = floodFillOuter()

        // Enclosed = FREE cells not reachable from outside
        val enclosed = mutableSetOf<GridPoint>()
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val pt = GridPoint(c, r)
                if (!grid[c][r] && pt !in outerFree) enclosed.add(pt)
            }
        }

        // Check if any dangerous enemy is enclosed
        val hasEnemy = dangerousEnemies.any { it in enclosed }

        if (hasEnemy) {
            // Restore line cells to FREE
            lineCells.forEach { grid[it.col][it.row] = false }
            _currentLine.clear()
            isDrawing = false
            return CloseResult.EnemyTrapped
        }

        // Conquer enclosed cells
        enclosed.forEach { grid[it.col][it.row] = true }
        val snailsTrapped = snails.count { it in enclosed }

        _currentLine.clear()
        isDrawing = false
        return CloseResult.Success(snailsTrapped)
    }

    fun cancelLine() {
        _currentLine.clear()
        isDrawing = false
    }

    /**
     * Flood fill using a virtual ring of FREE cells surrounding the grid.
     * Returns all in-bounds FREE cells reachable from outside.
     */
    private fun floodFillOuter(): Set<GridPoint> {
        val visited = mutableSetOf<GridPoint>()
        val queue = ArrayDeque<GridPoint>()
        val start = GridPoint(-1, -1)
        queue.add(start)
        visited.add(start)

        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            for ((dc, dr) in dirs) {
                val nc = curr.col + dc
                val nr = curr.row + dr
                // Expand to one cell beyond grid bounds (virtual border)
                if (nc < -1 || nc > cols || nr < -1 || nr > rows) continue
                val next = GridPoint(nc, nr)
                if (next in visited) continue
                val isVirtual = nc < 0 || nc >= cols || nr < 0 || nr >= rows
                val isFree = isVirtual || !grid[nc][nr]
                if (isFree) {
                    visited.add(next)
                    queue.add(next)
                }
            }
        }

        return visited.filter { it.col in 0 until cols && it.row in 0 until rows }.toSet()
    }
}
