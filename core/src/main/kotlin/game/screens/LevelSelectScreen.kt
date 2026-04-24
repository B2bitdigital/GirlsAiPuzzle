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

class LevelSelectScreen(private val game: GirlsPanicGame) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()

    // 5 columns × 10 rows grid
    private val cols = 5
    private val rows = 10
    private val cellW = 80f
    private val cellH = 70f
    private val startX = 40f
    private val startY = 730f

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        shapes.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        batch.begin()
        font.color = Color.CYAN
        font.draw(batch, "SELECT LEVEL", 150f, 790f)
        batch.end()

        for (i in 1..50) {
            val col = (i - 1) % cols
            val row = (i - 1) / cols
            val x = startX + col * cellW
            val y = startY - row * cellH
            val unlocked = game.prefs.isLevelUnlocked(i)
            val stars = game.prefs.getStars(i)

            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.color = if (unlocked) Color(0f, 0.4f, 0.4f, 1f) else Color(0.2f, 0.2f, 0.2f, 1f)
            shapes.rect(x, y - cellH + 10f, cellW - 10f, cellH - 10f)
            shapes.end()

            batch.begin()
            font.color = if (unlocked) Color.WHITE else Color.DARK_GRAY
            font.draw(batch, "$i", x + 28f, y - 14f)
            if (stars > 0) {
                font.color = Color.YELLOW
                font.draw(batch, "★".repeat(stars), x + 5f, y - 32f)
            }
            batch.end()
        }

        if (Gdx.input.justTouched()) {
            val tc = viewport.unproject(com.badlogic.gdx.math.Vector3(
                Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            for (i in 1..50) {
                val col = (i - 1) % cols
                val row = (i - 1) / cols
                val x = startX + col * cellW
                val y = startY - row * cellH
                val rect = Rect(x, y - cellH + 10f, cellW - 10f, cellH - 10f)
                if (rect.contains(tc.x, tc.y) && game.prefs.isLevelUnlocked(i)) {
                    game.currentLevelId = i
                    game.setScreen(GameScreen(game, i))
                    return
                }
            }
            // Back button area (top-left)
            if (tc.x < 60f && tc.y > 760f) game.setScreen(MenuScreen(game))
        }
    }

    override fun resize(w: Int, h: Int) = viewport.update(w, h, true)
    override fun dispose() { batch.dispose(); shapes.dispose(); font.dispose() }
}
