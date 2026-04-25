package game.ecs.systems

data class GridPoint(val col: Int, val row: Int)

sealed class CloseResult {
    object Empty : CloseResult()
    data class Success(
        val snailsTrapped: Int = 0,
        val conqueredCells: Set<GridPoint> = emptySet()
    ) : CloseResult()
}

enum class ExtendResult { OK, CROSSED }

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

    fun extendLine(pt: GridPoint): ExtendResult {
        if (_currentLine.isEmpty()) return ExtendResult.OK
        // Backtrack: stepping back onto the previous cell is allowed
        if (_currentLine.size >= 2 && pt == _currentLine[_currentLine.size - 2]) {
            _currentLine.removeAt(_currentLine.size - 1)
            return ExtendResult.OK
        }
        // Crossing: cell already in line (not the immediately previous position)
        if (pt in _currentLine) return ExtendResult.CROSSED
        _currentLine.add(pt)
        return ExtendResult.OK
    }

    @Suppress("UNUSED_PARAMETER")
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

        // Mark line cells as conquered (they become the new border)
        lineCells.forEach { grid[it.col][it.row] = true }

        // Find all disjoint free regions created by the closed line
        val regions = findAllFreeRegions()

        if (regions.isEmpty()) {
            _currentLine.clear()
            isDrawing = false
            return CloseResult.Success(conqueredCells = lineCells)
        }

        // Always conquer the smallest region
        val smallest = regions.minByOrNull { it.size }!!

        // Conquer smallest region
        smallest.forEach { grid[it.col][it.row] = true }

        val snailsTrapped = snails.count { it in smallest }

        _currentLine.clear()
        isDrawing = false
        return CloseResult.Success(
            snailsTrapped = snailsTrapped,
            conqueredCells = smallest + lineCells
        )
    }

    fun cancelLine() {
        _currentLine.clear()
        isDrawing = false
    }

    fun revealAll() {
        for (c in 0 until cols) for (r in 0 until rows) grid[c][r] = true
        _currentLine.clear()
        isDrawing = false
    }

    private fun findAllFreeRegions(): List<Set<GridPoint>> {
        val visited = mutableSetOf<GridPoint>()
        val regions = mutableListOf<Set<GridPoint>>()

        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val pt = GridPoint(c, r)
                if (!grid[c][r] && pt !in visited) {
                    val region = mutableSetOf<GridPoint>()
                    floodFillFrom(pt, region)
                    visited.addAll(region)
                    regions.add(region)
                }
            }
        }
        return regions
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
