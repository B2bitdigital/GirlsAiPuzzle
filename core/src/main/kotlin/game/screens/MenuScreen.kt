package game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import game.GameConstants
import game.GirlsPanicGame

class MenuScreen(private val game: GirlsPanicGame) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()

    private val btnPlay   = Rect(140f, 420f, 200f, 60f)
    private val btnLevels = Rect(140f, 340f, 200f, 60f)

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        shapes.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        // Neon border
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color.CYAN
        shapes.rect(5f, 5f, GameConstants.FIELD_WIDTH - 10f, GameConstants.FIELD_HEIGHT - 10f)
        shapes.end()

        // Buttons
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0.5f, 0.5f, 1f)
        shapes.rect(btnPlay.x, btnPlay.y, btnPlay.w, btnPlay.h)
        shapes.rect(btnLevels.x, btnLevels.y, btnLevels.w, btnLevels.h)
        shapes.end()

        batch.begin()
        font.color = Color.CYAN
        font.draw(batch, "GIRLS AI PANIC", 100f, 650f)
        font.color = Color.WHITE
        font.draw(batch, "PLAY", btnPlay.x + 70f, btnPlay.y + 38f)
        font.draw(batch, "LEVELS", btnLevels.x + 60f, btnLevels.y + 38f)
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
