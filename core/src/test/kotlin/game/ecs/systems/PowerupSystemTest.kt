package game.ecs.systems

import game.ecs.PowerupType
import org.junit.Assert.*
import org.junit.Test

class PowerupSystemTest {

    // offsetX=0, offsetY=0 keeps spawn coords in a simple [margin, 100-margin] range for assertions
    private val ps = PowerupSystem(
        offsetX = 0f, offsetY = 0f,
        playWidth = 100f, playHeight = 100f,
        cellSize = 10f,
        spawnInterval = 5f, lifetime = 8f
    )

    @Test
    fun `no powerup on first small delta`() {
        val spawned = ps.update(delta = 0.1f, availableTypes = listOf(PowerupType.TIME))
        assertNull(spawned)
    }

    @Test
    fun `powerup spawns after interval`() {
        val spawned = ps.update(delta = 6f, availableTypes = listOf(PowerupType.FREEZE))
        assertNotNull(spawned)
        assertEquals(PowerupType.FREEZE, spawned!!.type)
    }

    @Test
    fun `powerup position is within play field`() {
        val spawned = ps.update(delta = 6f, availableTypes = listOf(PowerupType.SPEED))!!
        assertTrue("x in play area", spawned.x in 10f..90f)
        assertTrue("y in play area", spawned.y in 10f..90f)
    }

    @Test
    fun `applying TIME adds seconds to timer`() {
        val timer = floatArrayOf(30f)
        ps.applyEffect(PowerupType.TIME, timerRef = timer, playerComp = null)
        assertEquals(40f, timer[0], 0.01f)
    }

    @Test
    fun `timer resets after spawn`() {
        ps.update(delta = 6f, availableTypes = listOf(PowerupType.TIME))
        val spawned2 = ps.update(delta = 1f, availableTypes = listOf(PowerupType.TIME))
        assertNull(spawned2)
    }

    @Test
    fun `powerup does not spawn when all cells are CONQUERED`() {
        val cells = Array(10) { Array(10) { CellType.CONQUERED } }
        val spawned = ps.update(delta = 6f, availableTypes = listOf(PowerupType.TIME), cells = cells)
        assertNull("should not spawn in fully-conquered field", spawned)
    }
}
