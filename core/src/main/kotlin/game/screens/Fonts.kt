package game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

object Fonts {
    lateinit var xs: BitmapFont    // 12px — footer labels, small UI text
    lateinit var sm: BitmapFont    // 16px — HUD labels, secondary text
    lateinit var md: BitmapFont    // 22px — HUD values, card labels
    lateinit var lg: BitmapFont    // 30px — HUD large values, button text
    lateinit var xl: BitmapFont    // 40px — overlay titles, star characters
    lateinit var xxl: BitmapFont   // 72px — GAME OVER title

    fun load() {
        val gen = FreeTypeFontGenerator(Gdx.files.internal("fonts/Orbitron-Regular.ttf"))
        fun param(size: Int) = FreeTypeFontParameter().apply { this.size = size }
        xs  = gen.generateFont(param(12))
        sm  = gen.generateFont(param(16))
        md  = gen.generateFont(param(22))
        lg  = gen.generateFont(param(30))
        xl  = gen.generateFont(param(40))
        xxl = gen.generateFont(param(72))
        gen.dispose()
    }

    fun dispose() {
        if (::xs.isInitialized)  xs.dispose()
        if (::sm.isInitialized)  sm.dispose()
        if (::md.isInitialized)  md.dispose()
        if (::lg.isInitialized)  lg.dispose()
        if (::xl.isInitialized)  xl.dispose()
        if (::xxl.isInitialized) xxl.dispose()
    }
}
