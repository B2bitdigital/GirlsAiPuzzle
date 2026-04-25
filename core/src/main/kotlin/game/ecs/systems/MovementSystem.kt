package game.ecs.systems

import kotlin.math.abs

class MovementSystem(
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.FIELD_HEIGHT
) {
    /**
     * Move player in direction (dirX, dirY) at given speed for one frame.
     * Returns false if movement is blocked (boundary or interior conquered cell).
     * pos = [x, y], mutated in place.
     */
    fun movePlayer(
        pos: FloatArray,
        dirX: Float, dirY: Float,
        speed: Float, delta: Float,
        grid: Array<BooleanArray>,
        cols: Int, rows: Int
    ): Boolean {
        val nextX = pos[0] + dirX * speed * delta
        val nextY = pos[1] + dirY * speed * delta

        if (nextX < 0f || nextX >= cols * this.cellSize || nextY < 0f || nextY >= rows * this.cellSize) {
            return false
        }

        val nextCol = (nextX / this.cellSize).toInt().coerceIn(0, cols - 1)
        val nextRow = (nextY / this.cellSize).toInt().coerceIn(0, rows - 1)

        if (isInteriorConquered(GridPoint(nextCol, nextRow), grid, cols, rows)) {
            return false
        }

        pos[0] = nextX
        pos[1] = nextY
        return true
    }

    private fun isInteriorConquered(
        pt: GridPoint,
        grid: Array<BooleanArray>,
        cols: Int, rows: Int
    ): Boolean {
        if (!grid[pt.col][pt.row]) return false
        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        return dirs.all { (dc, dr) ->
            val nc = pt.col + dc; val nr = pt.row + dr
            nc in 0 until cols && nr in 0 until rows && grid[nc][nr]
        }
    }

    /** Convert pixel position to GridPoint */
    fun toGridPoint(x: Float, y: Float) = GridPoint(
        col = (x / cellSize).toInt().coerceIn(0, (fieldWidth / cellSize).toInt() - 1),
        row = (y / cellSize).toInt().coerceIn(0, (fieldHeight / cellSize).toInt() - 1)
    )
}
