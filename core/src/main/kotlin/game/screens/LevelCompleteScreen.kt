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

class LevelCompleteScreen(
    private val game: GirlsPanicGame,
    private val levelId: Int,
    private val stars: Int,
    private val score: Int
) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()

    private val btnNext = Rect(80f, 240f, 140f, 55f)
    private val btnMenu = Rect(260f, 240f, 140f, 55f)

    override fun show() {
        // Trigger AdMob interstitial if levels_completed is multiple of 3
        // AdManager.instance?.showInterstitialIfReady() is called from AndroidLauncher
        // signal via game.onLevelComplete callback (wired in Task 16)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0.1f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        batch.projectionMatrix = camera.combined
        shapes.projectionMatrix = camera.combined

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0.5f, 0.2f, 1f)
        shapes.rect(btnNext.x, btnNext.y, btnNext.w, btnNext.h)
        shapes.rect(btnMenu.x, btnMenu.y, btnMenu.w, btnMenu.h)
        shapes.end()

        batch.begin()
        font.color = Color.YELLOW
        font.draw(batch, "LEVEL $levelId COMPLETE!", 100f, 680f)
        font.color = Color.WHITE
        font.draw(batch, "SCORE: $score", 170f, 580f)
        font.color = Color.YELLOW
        font.draw(batch, "★".repeat(stars) + "☆".repeat(3 - stars), 170f, 500f)
        font.color = Color.WHITE
        font.draw(batch, "NEXT", btnNext.x + 40f, btnNext.y + 35f)
        font.draw(batch, "MENU", btnMenu.x + 40f, btnMenu.y + 35f)
        batch.end()

        if (Gdx.input.justTouched()) {
            val tc = viewport.unproject(com.badlogic.gdx.math.Vector3(
                Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            when {
                btnNext.contains(tc.x, tc.y) -> {
                    val next = (levelId + 1).coerceAtMost(50)
                    game.setScreen(GameScreen(game, next))
                }
                btnMenu.contains(tc.x, tc.y) -> game.setScreen(MenuScreen(game))
            }
        }
    }

    override fun resize(w: Int, h: Int) = viewport.update(w, h, true)
    override fun dispose() { batch.dispose(); shapes.dispose(); font.dispose() }
}
