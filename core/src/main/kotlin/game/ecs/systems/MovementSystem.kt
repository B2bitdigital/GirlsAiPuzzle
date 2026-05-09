package game.ecs.systems

class MovementSystem(
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val offsetX: Float = game.GameConstants.FIELD_OFFSET_X,
    private val offsetY: Float = game.GameConstants.FIELD_OFFSET_Y,
    private val playWidth: Float = game.GameConstants.PLAY_WIDTH,
    private val playHeight: Float = game.GameConstants.PLAY_HEIGHT
) {
    fun movePlayer(
        pos: FloatArray,
        dirX: Float, dirY: Float,
        speed: Float, delta: Float,
        cells: Array<Array<CellType>>,
        cols: Int, rows: Int
    ): Boolean {
        val nextX = pos[0] + dirX * speed * delta
        val nextY = pos[1] + dirY * speed * delta

        // positions are in screen space; field occupies [offsetX, offsetX+playWidth] × [offsetY, offsetY+playHeight]
        if (nextX < offsetX || nextX >= offsetX + cols * cellSize ||
            nextY < offsetY || nextY >= offsetY + rows * cellSize) {
            return false
        }

        val nextCol = ((nextX - offsetX) / cellSize).toInt().coerceIn(0, cols - 1)
        val nextRow = ((nextY - offsetY) / cellSize).toInt().coerceIn(0, rows - 1)

        if (isInteriorConquered(GridPoint(nextCol, nextRow), cells, cols, rows)) {
            return false
        }

        pos[0] = nextX
        pos[1] = nextY
        return true
    }

    // A CONQUERED cell is "interior" (blocked) only if all 4 neighbours are also CONQUERED.
    // Border CONQUERED cells (adjacent to FREE or LINE) remain passable — the player walks the edge.
    private fun isInteriorConquered(
        pt: GridPoint,
        cells: Array<Array<CellType>>,
        cols: Int, rows: Int
    ): Boolean {
        if (cells[pt.col][pt.row] != CellType.CONQUERED) return false
        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        return dirs.all { (dc, dr) ->
            val nc = pt.col + dc; val nr = pt.row + dr
            nc in 0 until cols && nr in 0 until rows && cells[nc][nr] == CellType.CONQUERED
        }
    }

    // Convert screen-space position to grid cell, accounting for field offset.
    fun toGridPoint(x: Float, y: Float) = GridPoint(
        col = ((x - offsetX) / cellSize).toInt().coerceIn(0, (playWidth / cellSize).toInt() - 1),
        row = ((y - offsetY) / cellSize).toInt().coerceIn(0, (playHeight / cellSize).toInt() - 1)
    )
}
