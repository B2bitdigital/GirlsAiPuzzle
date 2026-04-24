package game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import game.GameConstants
import game.GirlsPanicGame
import kotlin.random.Random

class MenuScreen(private val game: GirlsPanicGame) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private val btnPlay   = Rect(140f, 380f, 200f, 65f)
    private val btnLevels = Rect(140f, 295f, 200f, 65f)

    private val stars = Array(80) {
        floatArrayOf(
            Random.nextFloat() * GameConstants.FIELD_WIDTH,
            Random.nextFloat() * GameConstants.FIELD_HEIGHT,
            Random.nextFloat() * 2f + 1f
        )
    }
    private var time = 0f

    override fun render(delta: Float) {
        time += delta
        Gdx.gl.glClearColor(0f, 0f, 0.06f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        shapes.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Stars
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (s in stars) {
            val twinkle = 0.5f + 0.5f * Math.sin((time * 1.5f + s[0] * 0.05f).toDouble()).toFloat()
            shapes.setColor(1f, 1f, 1f, twinkle * 0.8f)
            shapes.circle(s[0], s[1], s[2], 6)
        }
        shapes.end()

        // Outer neon border glow
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 1f, 1f, 0.15f)
        shapes.rect(2f, 2f, GameConstants.FIELD_WIDTH - 4f, GameConstants.FIELD_HEIGHT - 4f)
        shapes.setColor(0f, 1f, 1f, 0.3f)
        shapes.rect(4f, 4f, GameConstants.FIELD_WIDTH - 8f, GameConstants.FIELD_HEIGHT - 8f)
        shapes.setColor(0f, 1f, 1f, 1f)
        shapes.rect(6f, 6f, GameConstants.FIELD_WIDTH - 12f, GameConstants.FIELD_HEIGHT - 12f)
        shapes.end()

        // Button glow halos
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (btn in listOf(btnPlay, btnLevels)) {
            shapes.setColor(0f, 0.8f, 0.8f, 0.08f)
            shapes.rect(btn.x - 8f, btn.y - 8f, btn.w + 16f, btn.h + 16f)
            shapes.setColor(0f, 0.2f, 0.25f, 1f)
            shapes.rect(btn.x, btn.y, btn.w, btn.h)
        }
        shapes.end()

        // Button borders
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 1f, 1f, 1f)
        shapes.rect(btnPlay.x, btnPlay.y, btnPlay.w, btnPlay.h)
        shapes.rect(btnLevels.x, btnLevels.y, btnLevels.w, btnLevels.h)
        shapes.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)

        batch.begin()
        // Title shadow
        font.data.setScale(3.6f)
        font.color = Color(0f, 0.4f, 0.4f, 1f)
        layout.setText(font, "GIRLS AI PANIC")
        font.draw(batch, "GIRLS AI PANIC",
            (GameConstants.FIELD_WIDTH - layout.width) / 2f + 3f, 650f - 3f)
        // Title
        font.color = Color.CYAN
        font.draw(batch, "GIRLS AI PANIC",
            (GameConstants.FIELD_WIDTH - layout.width) / 2f, 650f)

        // Subtitle
        font.data.setScale(1.4f)
        font.color = Color(0.5f, 0.5f, 1f, 1f)
        layout.setText(font, "AI-POWERED PUZZLE")
        font.draw(batch, "AI-POWERED PUZZLE",
            (GameConstants.FIELD_WIDTH - layout.width) / 2f, 710f)

        // Buttons
        font.data.setScale(2.2f)
        font.color = Color.WHITE
        layout.setText(font, "PLAY")
        font.draw(batch, "PLAY",
            btnPlay.x + (btnPlay.w - layout.width) / 2f, btnPlay.y + (btnPlay.h + layout.height) / 2f)
        layout.setText(font, "LEVELS")
        font.draw(batch, "LEVELS",
            btnLevels.x + (btnLevels.w - layout.width) / 2f, btnLevels.y + (btnLevels.h + layout.height) / 2f)

        // High score
        font.data.setScale(1.5f)
        font.color = Color(1f, 1f, 0.5f, 1f)
        val hs = game.prefs.getHighScore()
        if (hs > 0) {
            layout.setText(font, "BEST: $hs")
            font.draw(batch, "BEST: $hs",
                (GameConstants.FIELD_WIDTH - layout.width) / 2f, 230f)
        }

        font.data.setScale(1f)
        batch.end()

        if (Gdx.input.justTouched()) {
            val tc = viewport.unproject(com.badlogic.gdx.math.Vector3(
                Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            when {
                btnPlay.contains(tc.x, tc.y) -> {
                    game.currentLevelId = 1
                    game.setScreen(GameScreen(game, game.currentLevelId))
                }
                btnLevels.contains(tc.x, tc.y) -> game.setScreen(LevelSelectScreen(game))
            }
        }
    }

    override fun resize(w: Int, h: Int) = viewport.update(w, h, true)
    override fun dispose() { batch.dispose(); shapes.dispose(); font.dispose() }
}

data class Rect(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun contains(px: Float, py: Float) = px in x..(x + w) && py in y..(y + h)
}
