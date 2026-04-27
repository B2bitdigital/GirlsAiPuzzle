package game.screens

import game.GameConstants

/**
 * Returns the render position for the player diamond.
 * When the player is in a perimeter column or row, snaps the
 * perpendicular coordinate to the field edge so the border
 * bisects the diamond exactly in half.
 */
fun playerRenderPos(posX: Float, posY: Float): Pair<Float, Float> {
    val col = (posX / GameConstants.CELL_SIZE).toInt()
        .coerceIn(0, GameConstants.GRID_COLS - 1)
    val row = (posY / GameConstants.CELL_SIZE).toInt()
        .coerceIn(0, GameConstants.GRID_ROWS - 1)

    val rx = when (col) {
        0                           -> 0f
        GameConstants.GRID_COLS - 1 -> GameConstants.FIELD_WIDTH
        else                        -> posX
    }
    val ry = when (row) {
        0                           -> 0f
        GameConstants.GRID_ROWS - 1 -> GameConstants.PLAY_HEIGHT
        else                        -> posY
    }
    return Pair(rx, ry)
}
