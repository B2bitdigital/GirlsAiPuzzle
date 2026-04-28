package game.screens

import game.GameConstants

data class RenderPos(val x: Float, val y: Float)

/**
 * Returns the render position for the player diamond.
 * Positions are in screen space (FIELD_OFFSET baked in).
 * Perimeter columns/rows snap the perpendicular coordinate
 * to the inner boundary line — the exact pixel where
 * drawTerritoryBorder draws the border edge — so that
 * line bisects the diamond exactly in half.
 */
fun playerRenderPos(posX: Float, posY: Float): RenderPos {
    val ox = GameConstants.FIELD_OFFSET_X
    val oy = GameConstants.FIELD_OFFSET_Y
    val cs = GameConstants.CELL_SIZE
    val col = ((posX - ox) / cs).toInt().coerceIn(0, GameConstants.GRID_COLS - 1)
    val row = ((posY - oy) / cs).toInt().coerceIn(0, GameConstants.GRID_ROWS - 1)
    val rx = when (col) {
        0                           -> ox + cs
        GameConstants.GRID_COLS - 1 -> ox + (GameConstants.GRID_COLS - 1) * cs
        else                        -> posX
    }
    val ry = when (row) {
        0                           -> oy + cs
        GameConstants.GRID_ROWS - 1 -> oy + (GameConstants.GRID_ROWS - 1) * cs
        else                        -> posY
    }
    return RenderPos(rx, ry)
}
