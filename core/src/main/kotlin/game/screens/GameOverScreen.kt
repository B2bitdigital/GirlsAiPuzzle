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

class GameOverScreen(
    private val game: GirlsPanicGame,
    private val levelId: Int
) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()

    private val btnRetry = Rect(60f, 280f, 160f, 55f)
    private val btnMenu  = Rect(260f, 280f, 160f, 55f)

    // Spider animation
    private var spiderX = -20f
    private val spiderSpeed = 80f

    override fun render(delta: Float) {
        spiderX += spiderSpeed * delta
        if (spiderX > GameConstants.FIELD_WIDTH + 30f) spiderX = -20f

        Gdx.gl.glClearColor(0.1f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        shapes.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // Animated spider (red circle crawling across screen)
        shapes.color = Color.RED
        shapes.circle(spiderX, 500f, 12f)
        // Buttons
        shapes.color = Color(0.5f, 0f, 0f, 1f)
        shapes.rect(btnRetry.x, btnRetry.y, btnRetry.w, btnRetry.h)
        shapes.rect(btnMenu.x, btnMenu.y, btnMenu.w, btnMenu.h)
        shapes.end()

        batch.begin()
        font.color = Color.RED
        font.draw(batch, "GAME OVER", 150f, 680f)
        font.color = Color.WHITE
        font.draw(batch, "RETRY", btnRetry.x + 45f, btnRetry.y + 35f)
        font.draw(batch, "MENU",  btnMenu.x + 50f,  btnMenu.y + 35f)
        batch.end()

        if (Gdx.input.justTouched()) {
            val tc = viewport.unproject(com.badlogic.gdx.math.Vector3(
                Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            when {
                btnRetry.contains(tc.x, tc.y) -> game.setScreen(GameScreen(game, levelId))
                btnMenu.contains(tc.x, tc.y)  -> game.setScreen(MenuScreen(game))
            }
        }
    }

    override fun resize(w: Int, h: Int) = viewport.update(w, h, true)
    override fun dispose() { batch.dispose(); shapes.dispose(); font.dispose() }
}
