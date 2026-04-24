package game.android.persistence

import android.content.Context
import game.persistence.GamePrefs

class AndroidGamePrefs(context: Context) : GamePrefs {

    private val prefs = context.getSharedPreferences("girls_panic_prefs", Context.MODE_PRIVATE)

    override fun getStars(levelId: Int): Int = prefs.getInt("level_stars_$levelId", 0)

    override fun saveStars(levelId: Int, stars: Int) {
        if (stars > getStars(levelId)) {
            prefs.edit().putInt("level_stars_$levelId", stars).apply()
        }
    }

    override fun isLevelUnlocked(levelId: Int): Boolean =
        levelId == 1 || getStars(levelId - 1) > 0

    override fun getHighScore(): Int = prefs.getInt("high_score", 0)

    override fun saveHighScore(score: Int) {
        if (score > getHighScore()) prefs.edit().putInt("high_score", score).apply()
    }

    override fun getLevelsCompleted(): Int = prefs.getInt("levels_completed", 0)

    override fun incrementLevelsCompleted() {
        prefs.edit().putInt("levels_completed", getLevelsCompleted() + 1).apply()
    }
}
