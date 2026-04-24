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

class GameOverScreen(
    private val game: GirlsPanicGame,
    private val levelId: Int
) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private val btnRetry = Rect(60f, 260f, 160f, 62f)
    private val btnMenu  = Rect(260f, 260f, 160f, 62f)

    private var spiderX = -30f
    private val spiderSpeed = 90f
    private var time = 0f

    override fun render(delta: Float) {
        time += delta
        spiderX += spiderSpeed * delta
        if (spiderX > GameConstants.FIELD_WIDTH + 40f) spiderX = -30f

        Gdx.gl.glClearColor(0.05f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        shapes.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapes.begin(ShapeRenderer.ShapeType.Filled)

        // Spider glow
        shapes.setColor(1f, 0f, 0f, 0.1f)
        shapes.circle(spiderX, 480f, 30f, 16)
        shapes.setColor(1f, 0.1f, 0.1f, 0.3f)
        shapes.circle(spiderX, 480f, 18f, 16)
        shapes.setColor(0.9f, 0.1f, 0.1f, 1f)
        shapes.circle(spiderX, 480f, 10f, 16)

        // Buttons
        shapes.setColor(0.3f, 0f, 0f, 0.8f)
        shapes.rect(btnRetry.x, btnRetry.y, btnRetry.w, btnRetry.h)
        shapes.rect(btnMenu.x, btnMenu.y, btnMenu.w, btnMenu.h)
        shapes.end()

        // Spider legs
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.9f, 0.2f, 0.2f, 0.8f)
        for (i in 0 until 4) {
            val angle = (i * 45f + 22.5f) * Math.PI.toFloat() / 180f
            shapes.line(spiderX, 480f,
                spiderX + Math.cos(angle.toDouble()).toFloat() * 20f,
                480f + Math.sin(angle.toDouble()).toFloat() * 14f)
        }

        // Button borders
        shapes.setColor(1f, 0.3f, 0.3f, 1f)
        shapes.rect(btnRetry.x, btnRetry.y, btnRetry.w, btnRetry.h)
        shapes.rect(btnMenu.x, btnMenu.y, btnMenu.w, btnMenu.h)
        shapes.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)

        batch.begin()
        // "GAME OVER" shadow
        font.data.setScale(4f)
        font.color = Color(0.4f, 0f, 0f, 1f)
        layout.setText(font, "GAME OVER")
        val goX = (GameConstants.FIELD_WIDTH - layout.width) / 2f
        font.draw(batch, "GAME OVER", goX + 3f, 660f - 3f)
        font.color = Color(1f, 0.15f, 0.15f, 1f)
        font.draw(batch, "GAME OVER", goX, 660f)

        // Level info
        font.data.setScale(1.8f)
        font.color = Color(0.8f, 0.5f, 0.5f, 1f)
        layout.setText(font, "LEVEL $levelId")
        font.draw(batch, "LEVEL $levelId",
            (GameConstants.FIELD_WIDTH - layout.width) / 2f, 560f)

        // Buttons
        font.data.setScale(2.2f)
        font.color = Color.WHITE
        layout.setText(font, "RETRY")
        font.draw(batch, "RETRY",
            btnRetry.x + (btnRetry.w - layout.width) / 2f,
            btnRetry.y + (btnRetry.h + layout.height) / 2f)
        layout.setText(font, "MENU")
        font.draw(batch, "MENU",
            btnMenu.x + (btnMenu.w - layout.width) / 2f,
            btnMenu.y + (btnMenu.h + layout.height) / 2f)

        font.data.setScale(1f)
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
