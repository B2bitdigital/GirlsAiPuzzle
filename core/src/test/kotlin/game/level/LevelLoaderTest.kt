package game.level

import org.junit.Assert.*
import org.junit.Test
import game.ecs.EnemyType
import game.ecs.PowerupType

class LevelLoaderTest {

    private val sampleJson = """
        {
          "id": 1,
          "background": "backgrounds/bg_01.png",
          "timeSeconds": 90,
          "targetPercent": 75,
          "enemies": [
            { "type": "spider", "count": 1, "speed": 80.0 }
          ],
          "powerups": ["time", "freeze"]
        }
    """.trimIndent()

    @Test
    fun `parses level id and background`() {
        val level = LevelLoader.fromJson(sampleJson)
        assertEquals(1, level.id)
        assertEquals("backgrounds/bg_01.png", level.background)
    }

    @Test
    fun `parses timer and target percent`() {
        val level = LevelLoader.fromJson(sampleJson)
        assertEquals(90, level.timeSeconds)
        assertEquals(75, level.targetPercent)
    }

    @Test
    fun `parses enemy list`() {
        val level = LevelLoader.fromJson(sampleJson)
        assertEquals(1, level.enemies.size)
        assertEquals(EnemyType.SPIDER, level.enemies[0].toEnemyType())
        assertEquals(1, level.enemies[0].count)
        assertEquals(80f, level.enemies[0].speed, 0.01f)
    }

    @Test
    fun `parses powerup list`() {
        val level = LevelLoader.fromJson(sampleJson)
        val types = level.powerupTypes()
        assertEquals(listOf(PowerupType.TIME, PowerupType.FREEZE), types)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws on unknown enemy type`() {
        val bad = sampleJson.replace("\"spider\"", "\"dragon\"")
        val level = LevelLoader.fromJson(bad)
        level.enemies[0].toEnemyType()
    }
}
