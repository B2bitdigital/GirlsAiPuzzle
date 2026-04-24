package game.persistence

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GamePrefsTest {

    private lateinit var prefs: GamePrefs

    @Before
    fun setup() { prefs = InMemoryGamePrefs() }

    @Test
    fun `default stars for unplayed level is 0`() {
        assertEquals(0, prefs.getStars(1))
    }

    @Test
    fun `save and retrieve stars`() {
        prefs.saveStars(levelId = 3, stars = 2)
        assertEquals(2, prefs.getStars(3))
    }

    @Test
    fun `does not overwrite higher star count`() {
        prefs.saveStars(1, 3)
        prefs.saveStars(1, 1)
        assertEquals(3, prefs.getStars(1))
    }

    @Test
    fun `level unlocked if previous level has stars`() {
        prefs.saveStars(1, 1)
        assertTrue(prefs.isLevelUnlocked(2))
    }

    @Test
    fun `level 2 locked when level 1 has 0 stars`() {
        assertFalse(prefs.isLevelUnlocked(2))
    }

    @Test
    fun `level 1 always unlocked`() {
        assertTrue(prefs.isLevelUnlocked(1))
    }

    @Test
    fun `high score saves and retrieves`() {
        prefs.saveHighScore(99999)
        assertEquals(99999, prefs.getHighScore())
    }

    @Test
    fun `high score does not decrease`() {
        prefs.saveHighScore(1000)
        prefs.saveHighScore(500)
        assertEquals(1000, prefs.getHighScore())
    }

    @Test
    fun `levels completed increments`() {
        assertEquals(0, prefs.getLevelsCompleted())
        prefs.incrementLevelsCompleted()
        prefs.incrementLevelsCompleted()
        assertEquals(2, prefs.getLevelsCompleted())
    }
}
