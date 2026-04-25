package game.ecs.systems

data class GridPoint(val col: Int, val row: Int)

sealed class CloseResult {
    object Empty : CloseResult()
    data class Success(val snailsTrapped: Int = 0) : CloseResult()
}

class TerritorySystem(
    val cols: Int = game.GameConstants.GRID_COLS,
    val rows: Int = game.GameConstants.GRID_ROWS
) {
    // true = conquered/border, false = free
    val grid: Array<BooleanArray> = Array(cols) { BooleanArray(rows) }

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

    fun isOnPerimeter(pt: GridPoint): Boolean {
        if (!isOnSafeZone(pt)) return false
        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        return dirs.any { (dc, dr) ->
            val nc = pt.col + dc; val nr = pt.row + dr
            nc !in 0 until cols || nr !in 0 until rows || !grid[nc][nr]
        }
    }

    fun startLine(startPt: GridPoint) {
        _currentLine.clear()
        _currentLine.add(startPt)
        isDrawing = true
    }

    fun extendLine(pt: GridPoint) {
        if (pt !in _currentLine) _currentLine.add(pt)
    }

    /**
     * Close the current drawing line. Captures the territory NOT reachable
     * from any dangerous enemy (i.e., the side separated from enemies by the line).
     *
     * dangerousEnemies: grid positions of Spider, Cockroach, Wasp.
     * snails: positions of Snail enemies (bonus if trapped).
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

        // Mark line cells as conquered (permanent — they become border)
        lineCells.forEach { grid[it.col][it.row] = true }

        // Flood-fill from every dangerous enemy to find "open" territory
        // (the side of the line that enemies occupy — must remain free)
        val openCells = mutableSetOf<GridPoint>()
        for (enemy in dangerousEnemies) {
            if (enemy.col in 0 until cols && enemy.row in 0 until rows
                && !grid[enemy.col][enemy.row]) {
                floodFillFrom(enemy, openCells)
            }
        }

        // Enclosed = free cells not reachable from any dangerous enemy
        val enclosed = mutableSetOf<GridPoint>()
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val pt = GridPoint(c, r)
                if (!grid[c][r] && pt !in openCells) enclosed.add(pt)
            }
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
     * Flood fill from [start] through free (not conquered) cells only.
     * Used to find territory reachable from enemy positions.
     */
    private fun floodFillFrom(start: GridPoint, visited: MutableSet<GridPoint>) {
        if (start in visited) return
        if (start.col !in 0 until cols || start.row !in 0 until rows) return
        if (grid[start.col][start.row]) return  // conquered — can't enter

        val queue = ArrayDeque<GridPoint>()
        queue.add(start)
        visited.add(start)

        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            for ((dc, dr) in dirs) {
                val nc = curr.col + dc
                val nr = curr.row + dr
                if (nc !in 0 until cols || nr !in 0 until rows) continue
                val next = GridPoint(nc, nr)
                if (next in visited || grid[nc][nr]) continue
                visited.add(next)
                queue.add(next)
            }
        }
    }
}
