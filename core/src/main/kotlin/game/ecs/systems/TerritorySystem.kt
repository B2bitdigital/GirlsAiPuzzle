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
    // grid[c][r] = true se cella conquistata (NON include più il bordo iniziale)
    val grid: Array<BooleanArray> = Array(cols) { BooleanArray(rows) }
    // isPerimeter[c][r] = true se cella è bordo/perimetro (safe zone, non conta come conquistata)
    val isPerimeter: Array<BooleanArray> = Array(cols) { BooleanArray(rows) }

    private val _currentLine = mutableListOf<GridPoint>()
    val currentLine: List<GridPoint> get() = _currentLine

    var isDrawing: Boolean = false
        private set

    init { initBorders() }

    private fun initBorders() {
        // Imposta il bordo come perimetro (safe zone), NON come conquistato
        for (c in 0 until cols) {
            isPerimeter[c][0] = true
            isPerimeter[c][rows - 1] = true
        }
        for (r in 0 until rows) {
            isPerimeter[0][r] = true
            isPerimeter[cols - 1][r] = true
        }
        // grid resta tutto false (nessuna cella conquistata all'inizio)
    }

    fun conqueredPercent(): Float {
        var count = 0
        var total = 0
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                if (!isPerimeter[c][r]) {
                    total++
                    if (grid[c][r]) count++
                }
            }
        }
        return if (total == 0) 0f else 100f * count / total
    }

    fun isOnSafeZone(pt: GridPoint): Boolean {
        // Safe zone = bordo (perimetro) OPPURE cella conquistata
        return pt.col in 0 until cols && pt.row in 0 until rows && (isPerimeter[pt.col][pt.row] || grid[pt.col][pt.row])
    }

    fun isOnPerimeter(pt: GridPoint): Boolean {
        return pt.col in 0 until cols && pt.row in 0 until rows && isPerimeter[pt.col][pt.row]
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

        if (regions.size < 2) {
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
                // Skip perimeter and conquered cells
                if (!grid[c][r] && !isPerimeter[c][r] && pt !in visited) {
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
        if (isPerimeter[start.col][start.row]) return  // perimeter — never conquerable

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
                if (next in visited || grid[nc][nr] || isPerimeter[nc][nr]) continue
                visited.add(next)
                queue.add(next)
            }
        }
    }
}
