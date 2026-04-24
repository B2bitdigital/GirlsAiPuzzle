package game

import com.badlogic.gdx.Game
import com.badlogic.gdx.assets.AssetManager
import game.persistence.GamePrefs
import game.screens.MenuScreen

class GirlsPanicGame(val prefs: GamePrefs) : Game() {

    val assets = AssetManager()
    var currentLevelId: Int = 1

    override fun create() {
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        assets.dispose()
        super.dispose()
    }
}
