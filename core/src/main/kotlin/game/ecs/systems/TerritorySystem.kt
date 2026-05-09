package game.ecs.systems

// Single enum for all possible cell states.
// No separate isPerimeter array — the frame is CONQUERED from init,
// and the "border" at any point is always the contour of CONQUERED cells.
enum class CellType {
    FREE,       // not yet captured; covered by overlay
    CONQUERED,  // image visible, safe — enemies cannot enter, player cannot draw here
    LINE        // player's temporary drawing line; vulnerable; becomes CONQUERED on successful capture
}

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
    // Single source of truth for field state.
    // Replaces the old (grid: Array<BooleanArray>, isPerimeter: Array<BooleanArray>) pair.
    val cells: Array<Array<CellType>> = Array(cols) { Array(rows) { CellType.FREE } }

    private val _currentLine = mutableListOf<GridPoint>()
    val currentLine: List<GridPoint> get() = _currentLine

    var isDrawing: Boolean = false
        private set

    init { initInitialBorder() }

    // The image frame starts as CONQUERED — this is the initial safe zone the player stands on.
    // There is no separate "perimeter" concept: the border mutates as territory is captured.
    private fun initInitialBorder() {
        for (c in 0 until cols) {
            cells[c][0] = CellType.CONQUERED
            cells[c][rows - 1] = CellType.CONQUERED
        }
        for (r in 0 until rows) {
            cells[0][r] = CellType.CONQUERED
            cells[cols - 1][r] = CellType.CONQUERED
        }
    }

    // Only internal cells (cols 1..cols-2, rows 1..rows-2) count toward the percentage.
    // The initial frame is always CONQUERED and must not inflate the score from 0%.
    fun conqueredPercent(): Float {
        var count = 0
        var total = 0
        for (c in 1 until cols - 1) {
            for (r in 1 until rows - 1) {
                total++
                if (cells[c][r] == CellType.CONQUERED) count++
            }
        }
        return if (total == 0) 0f else 100f * count / total
    }

    // Safe = standing on any CONQUERED cell (initial frame or captured interior).
    fun isOnSafeZone(pt: GridPoint): Boolean =
        pt.col in 0 until cols && pt.row in 0 until rows && cells[pt.col][pt.row] == CellType.CONQUERED

    fun startLine(startPt: GridPoint) {
        _currentLine.clear()
        _currentLine.add(startPt)
        // startPt is a CONQUERED cell (the border) — its CellType stays CONQUERED.
        // LINE marking begins only when the player enters FREE cells.
        isDrawing = true
    }

    fun extendLine(pt: GridPoint): ExtendResult {
        if (_currentLine.isEmpty()) return ExtendResult.OK
        // Backtrack: player stepped onto the cell before the current head
        if (_currentLine.size >= 2 && pt == _currentLine[_currentLine.size - 2]) {
            val removed = _currentLine.removeLast()
            // Restore only LINE cells to FREE; the anchor CONQUERED cell is untouched
            if (cells[removed.col][removed.row] == CellType.LINE) {
                cells[removed.col][removed.row] = CellType.FREE
            }
            return ExtendResult.OK
        }
        // Crossing own line = immediate failure
        if (pt in _currentLine) return ExtendResult.CROSSED
        _currentLine.add(pt)
        // Mark FREE cells as LINE so enemies can detect and destroy the line
        if (cells[pt.col][pt.row] == CellType.FREE) {
            cells[pt.col][pt.row] = CellType.LINE
        }
        return ExtendResult.OK
    }

    fun closeLine(
        dangerousEnemies: List<GridPoint>,
        snails: List<GridPoint>
    ): CloseResult {
        if (_currentLine.size < 2) {
            cancelLine()
            return CloseResult.Empty
        }

        val lineCells = _currentLine.toSet()

        // Convert LINE → CONQUERED first so the line acts as a barrier during flood fill.
        // The anchor CONQUERED cell is already CONQUERED — this only affects LINE cells.
        lineCells.forEach { pt ->
            if (cells[pt.col][pt.row] == CellType.LINE) {
                cells[pt.col][pt.row] = CellType.CONQUERED
            }
        }

        val regions = findAllFreeRegions()

        if (regions.size < 2) {
            // Line didn't enclose any area — only the line itself becomes CONQUERED
            _currentLine.clear()
            isDrawing = false
            return CloseResult.Success(conqueredCells = lineCells)
        }

        // Prefer the region with no dangerous enemies; if all regions have enemies,
        // take the smallest — caller's respawn logic handles any trapped enemies.
        val enemySet = dangerousEnemies.toSet()
        val chosen = regions.firstOrNull { region -> enemySet.none { it in region } }
            ?: regions.minByOrNull { it.size }!!

        chosen.forEach { cells[it.col][it.row] = CellType.CONQUERED }

        val snailsTrapped = snails.count { it in chosen }

        _currentLine.clear()
        isDrawing = false
        return CloseResult.Success(
            snailsTrapped = snailsTrapped,
            conqueredCells = chosen + lineCells
        )
    }

    fun cancelLine() {
        // Restore all LINE cells to FREE before discarding the path
        for (pt in _currentLine) {
            if (cells[pt.col][pt.row] == CellType.LINE) {
                cells[pt.col][pt.row] = CellType.FREE
            }
        }
        _currentLine.clear()
        isDrawing = false
    }

    fun revealAll() {
        for (c in 0 until cols) for (r in 0 until rows) cells[c][r] = CellType.CONQUERED
        _currentLine.clear()
        isDrawing = false
    }

    private fun findAllFreeRegions(): List<Set<GridPoint>> {
        val visited = mutableSetOf<GridPoint>()
        val regions = mutableListOf<Set<GridPoint>>()
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val pt = GridPoint(c, r)
                if (cells[c][r] == CellType.FREE && pt !in visited) {
                    val region = mutableSetOf<GridPoint>()
                    floodFillFrom(pt, region)
                    visited.addAll(region)
                    regions.add(region)
                }
            }
        }
        return regions
    }

    // Flood fill through FREE cells only.
    // By the time closeLine calls this, all former LINE cells are already CONQUERED,
    // so the filled line acts as a complete barrier without special-casing LINE.
    private fun floodFillFrom(start: GridPoint, visited: MutableSet<GridPoint>) {
        if (start in visited) return
        if (start.col !in 0 until cols || start.row !in 0 until rows) return
        if (cells[start.col][start.row] != CellType.FREE) return

        val queue = ArrayDeque<GridPoint>()
        queue.add(start)
        visited.add(start)

        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            for ((dc, dr) in dirs) {
                val nc = curr.col + dc; val nr = curr.row + dr
                if (nc !in 0 until cols || nr !in 0 until rows) continue
                val next = GridPoint(nc, nr)
                if (next in visited || cells[nc][nr] != CellType.FREE) continue
                visited.add(next)
                queue.add(next)
            }
        }
    }

    fun randomFreeCell(): GridPoint? {
        val free = mutableListOf<GridPoint>()
        for (c in 0 until cols) for (r in 0 until rows) {
            if (cells[c][r] == CellType.FREE) free.add(GridPoint(c, r))
        }
        return free.randomOrNull()
    }
}
