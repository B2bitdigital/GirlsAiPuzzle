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
    private val layout = GlyphLayout()

    private val btnNext = Rect(60f, 210f, 150f, 62f)
    private val btnMenu = Rect(270f, 210f, 150f, 62f)

    private var time = 0f

    override fun render(delta: Float) {
        time += delta
        val pulse = (Math.sin(time.toDouble() * 2.5) * 0.5 + 0.5).toFloat()

        Gdx.gl.glClearColor(0f, 0.04f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        batch.projectionMatrix = camera.combined
        shapes.projectionMatrix = camera.combined

        val W = GameConstants.FIELD_WIDTH

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // Buttons
        shapes.setColor(0f, 0.25f, 0.1f, 0.9f)
        shapes.rect(btnNext.x, btnNext.y, btnNext.w, btnNext.h)
        shapes.rect(btnMenu.x, btnMenu.y, btnMenu.w, btnMenu.h)

        // Stars glow halos
        val starXPositions = listOf(W / 2f - 80f, W / 2f, W / 2f + 80f)
        for ((i, sx) in starXPositions.withIndex()) {
            val earned = i < stars
            if (earned) {
                shapes.setColor(1f, 0.85f, 0f, 0.15f + pulse * 0.1f)
                shapes.circle(sx, 430f, 35f, 20)
            }
        }
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 1f, 0.5f, 1f)
        shapes.rect(btnNext.x, btnNext.y, btnNext.w, btnNext.h)
        shapes.rect(btnMenu.x, btnMenu.y, btnMenu.w, btnMenu.h)
        shapes.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)

        batch.begin()

        // Title
        font.data.setScale(3.2f)
        font.color = Color(0f, 0.35f, 0.15f, 1f)
        layout.setText(font, "LEVEL CLEAR!")
        val tx = (W - layout.width) / 2f
        font.draw(batch, "LEVEL CLEAR!", tx + 3f, 650f - 3f)
        font.color = Color(0.3f, 1f, 0.5f, 1f)
        font.draw(batch, "LEVEL CLEAR!", tx, 650f)

        // Level number
        font.data.setScale(2f)
        font.color = Color(0.5f, 0.9f, 0.6f, 1f)
        layout.setText(font, "LEVEL $levelId")
        font.draw(batch, "LEVEL $levelId", (W - layout.width) / 2f, 700f)

        // Score
        font.data.setScale(2.2f)
        font.color = Color.WHITE
        layout.setText(font, "SCORE: $score")
        font.draw(batch, "SCORE: $score", (W - layout.width) / 2f, 550f)

        // Stars
        font.data.setScale(3.5f)
        val starPositions = listOf(W / 2f - 80f, W / 2f, W / 2f + 80f)
        for ((i, sx) in starPositions.withIndex()) {
            font.color = if (i < stars) Color(1f, 0.85f + pulse * 0.15f, 0f, 1f)
                         else Color(0.3f, 0.3f, 0.3f, 1f)
            layout.setText(font, "★")
            font.draw(batch, "★", sx - layout.width / 2f, 460f)
        }

        // Buttons
        font.data.setScale(2.2f)
        font.color = Color.WHITE
        layout.setText(font, "NEXT")
        font.draw(batch, "NEXT",
            btnNext.x + (btnNext.w - layout.width) / 2f,
            btnNext.y + (btnNext.h + layout.height) / 2f)
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
