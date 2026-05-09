package game

import com.badlogic.gdx.Game
import com.badlogic.gdx.assets.AssetManager
import game.persistence.GamePrefs
import game.screens.Fonts
import game.screens.MenuScreen

class GirlsPanicGame(val prefs: GamePrefs) : Game() {

    val assets = AssetManager()
    var currentLevelId: Int = 1

    override fun create() {
        Fonts.load()
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        Fonts.dispose()
        assets.dispose()
        super.dispose()
    }
}
