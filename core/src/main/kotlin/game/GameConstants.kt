package game

object GameConstants {
    const val FIELD_WIDTH = 480f
    const val FIELD_HEIGHT = 800f       // total viewport height (HUD + play area)
    const val HUD_HEIGHT = 60f          // HUD strip at top
    const val PLAY_HEIGHT = 740f        // FIELD_HEIGHT - HUD_HEIGHT — actual play area
    const val GRID_COLS = 48
    const val GRID_ROWS = 74            // PLAY_HEIGHT / CELL_SIZE = 740 / 10
    const val CELL_SIZE = 10f
    const val PLAYER_SPEED = 150f
    const val LIVES_PER_LEVEL = 3
    const val POWERUP_SPAWN_INTERVAL = 15f
    const val POWERUP_LIFETIME = 8f
    const val STARS_TWO_THRESHOLD = 85
    const val STARS_THREE_THRESHOLD = 95
    const val LEVEL_COMPLETE_THRESHOLD = 90f  // minimum % area revealed to win
}
