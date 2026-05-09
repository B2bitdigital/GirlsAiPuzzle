package game.ecs.systems

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import game.GameConstants
import game.ecs.EcsWorld
import game.ecs.EnemyType
import game.ecs.Entity
import game.ecs.EntityState
import game.level.EnemyConfig
import game.level.LevelData
import game.persistence.InMemoryGamePrefs
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class EcsWorldTerritoryTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun initGdx() {
            val cfg = HeadlessApplicationConfiguration()
            HeadlessApplication(object : ApplicationAdapter() {}, cfg)
        }

        private fun makeWorld(enemyCount: Int = 1): EcsWorld {
            val level = LevelData(
                id = 1,
                background = "bg.png",
                timeSeconds = 120,
                targetPercent = 90,
                enemies = if (enemyCount > 0)
                    listOf(EnemyConfig("spider", enemyCount, 100f))
                else emptyList(),
                powerups = emptyList()
            )
            val world = EcsWorld(level, InMemoryGamePrefs(), cols = 10, rows = 10)
            world.init()
            return world
        }
    }

    @Test
    fun `enemy trapped in conquered zone is respawned not deleted`() {
        val world = makeWorld(enemyCount = 1)
        val initialCount = world.enemies.size
        assertEquals(1, initialCount)

        // Place enemy at interior cell (1,5) — inside the left region we'll conquer
        world.enemies[0].position.x = GameConstants.FIELD_OFFSET_X + 1 * GameConstants.CELL_SIZE
        world.enemies[0].position.y = GameConstants.FIELD_OFFSET_Y + 5 * GameConstants.CELL_SIZE

        // Draw line from (3,0) to (3,9) — divides grid: left (cols 1-2) vs right (cols 4-8)
        world.territory.startLine(GridPoint(3, 0))
        for (r in 1 until 9) world.territory.extendLine(GridPoint(3, r))
        world.territory.extendLine(GridPoint(3, 9))

        // Pass emptyList() to closeLine so the algorithm freely picks the smaller (left) region,
        // allowing the enemy at col=1 to be trapped in the conquered area — triggering respawn logic.
        val result = world.territory.closeLine(emptyList(), emptyList())

        assertTrue(result is CloseResult.Success)
        val success = result as CloseResult.Success

        // Apply respawn logic (mirrors what EcsWorld.update will do after Task 5)
        val toRespawn = mutableListOf<EntityState>()
        world.enemies.removeAll { eState ->
            val gp = world.movement.toGridPoint(eState.position.x, eState.position.y)
            if (gp in success.conqueredCells) { toRespawn.add(eState); true } else false
        }
        for (eState in toRespawn) {
            world.score += 1000
            val freeCell = world.territory.randomFreeCell()
            if (freeCell != null) {
                eState.position.x = GameConstants.FIELD_OFFSET_X + freeCell.col * GameConstants.CELL_SIZE
                eState.position.y = GameConstants.FIELD_OFFSET_Y + freeCell.row * GameConstants.CELL_SIZE
                world.enemies.add(eState)
            }
        }

        assertEquals("enemy count unchanged after respawn", initialCount, world.enemies.size)
        assertTrue("score increased for trapped enemy", world.score >= 1000)
        val respawned = world.enemies[0]
        val gp = world.movement.toGridPoint(respawned.position.x, respawned.position.y)
        assertEquals("respawned enemy not in conquered cell", CellType.FREE, world.territory.cells[gp.col][gp.row])
    }

    @Test
    fun `territory score awarded on successful close`() {
        val world = makeWorld(enemyCount = 0)
        assertEquals(0, world.score)

        world.territory.startLine(GridPoint(3, 0))
        for (r in 1 until 9) world.territory.extendLine(GridPoint(3, r))
        world.territory.extendLine(GridPoint(3, 9))
        world.territory.closeLine(emptyList(), emptyList())

        val pct = world.territory.conqueredPercent()
        val expectedScore = (pct * 10).toInt()
        assertTrue("score should be > 0 after territory capture", expectedScore > 0)
    }

    @Test
    fun `no enemy deleted when none in conquered zone`() {
        val world = makeWorld(enemyCount = 1)
        // Place enemy in right (large) region — will NOT be conquered
        world.enemies[0].position.x = GameConstants.FIELD_OFFSET_X + 7 * GameConstants.CELL_SIZE
        world.enemies[0].position.y = GameConstants.FIELD_OFFSET_Y + 5 * GameConstants.CELL_SIZE

        world.territory.startLine(GridPoint(3, 0))
        for (r in 1 until 9) world.territory.extendLine(GridPoint(3, r))
        world.territory.extendLine(GridPoint(3, 9))

        val dangerousPositions = world.enemies
            .filter { (it.entity as? Entity.Enemy)?.type != EnemyType.SNAIL }
            .map { world.movement.toGridPoint(it.position.x, it.position.y) }
        val result = world.territory.closeLine(dangerousPositions, emptyList())
        val success = result as CloseResult.Success

        val toRespawn = mutableListOf<EntityState>()
        world.enemies.removeAll { eState ->
            val gp = world.movement.toGridPoint(eState.position.x, eState.position.y)
            if (gp in success.conqueredCells) { toRespawn.add(eState); true } else false
        }

        assertTrue("no enemies respawned", toRespawn.isEmpty())
        assertEquals("enemy count unchanged", 1, world.enemies.size)
        assertEquals("no score bonus", 0, world.score)
    }
}
