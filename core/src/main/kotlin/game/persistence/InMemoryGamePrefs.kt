package game.persistence

class InMemoryGamePrefs : GamePrefs {
    private val stars = mutableMapOf<Int, Int>()
    private var highScore = 0
    private var levelsCompleted = 0

    override fun getStars(levelId: Int) = stars[levelId] ?: 0

    override fun saveStars(levelId: Int, stars: Int) {
        val current = this.stars[levelId] ?: 0
        if (stars > current) this.stars[levelId] = stars
    }

    override fun isLevelUnlocked(levelId: Int): Boolean =
        levelId == 1 || getStars(levelId - 1) > 0

    override fun getHighScore() = highScore

    override fun saveHighScore(score: Int) {
        if (score > highScore) highScore = score
    }

    override fun getLevelsCompleted() = levelsCompleted

    override fun incrementLevelsCompleted() { levelsCompleted++ }
}
