package game.ecs.systems

import kotlin.math.abs

class CollisionSystem(private val cellSize: Float = game.GameConstants.CELL_SIZE) {

    private val pickupRadius = cellSize * 1.5f

    /** Spider, Wasp: touching the player's cell = life lost */
    fun playerHitByEnemy(px: Float, py: Float, ex: Float, ey: Float): Boolean {
        val pgc = toGridCol(px); val pgr = toGridRow(py)
        val egc = toGridCol(ex); val egr = toGridRow(ey)
        return pgc == egc && pgr == egr
    }

    /** Cockroach: touching any cell in the current drawing line = life lost */
    fun enemyHitsLine(ex: Float, ey: Float, line: List<GridPoint>): Boolean {
        val egc = toGridCol(ex); val egr = toGridRow(ey)
        return line.any { it.col == egc && it.row == egr }
    }

    /** Player walks into a powerup's cell */
    fun playerCollectsPowerup(px: Float, py: Float, pwx: Float, pwy: Float): Boolean =
        abs(px - pwx) < pickupRadius && abs(py - pwy) < pickupRadius

    private fun toGridCol(x: Float) = (x / cellSize).toInt()
    private fun toGridRow(y: Float) = (y / cellSize).toInt()
}
