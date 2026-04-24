package game.persistence

interface GamePrefs {
    fun getStars(levelId: Int): Int
    fun saveStars(levelId: Int, stars: Int)
    fun isLevelUnlocked(levelId: Int): Boolean
    fun getHighScore(): Int
    fun saveHighScore(score: Int)
    fun getLevelsCompleted(): Int
    fun incrementLevelsCompleted()
}
