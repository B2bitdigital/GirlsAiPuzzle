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
import kotlin.math.sin

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

    private val W = GameConstants.FIELD_WIDTH
    private val H = GameConstants.FIELD_HEIGHT

    private val cardPad = 16f
    private val cardW = W - cardPad * 2f
    private val cardH = 88f
    private val cardX = cardPad
    private val cardY = 360f

    private val btnW = 130f
    private val btnGap = 12f
    private val btnH = 76f
    private val btnRetryX = cardPad
    private val btnNextX = cardPad + btnW + btnGap
    private val btnMenuX = cardPad + 2 * (btnW + btnGap)
    private val btnY = 260f

    private val btnRetry = Rect(btnRetryX, btnY, btnW, btnH)
    private val btnNext = Rect(btnNextX, btnY, btnW, btnH)
    private val btnMenu = Rect(btnMenuX, btnY, btnW, btnH)

    private var time = 0f

    private fun formatScore(n: Int): String {
        val s = n.toString()
        val sb = StringBuilder()
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
            sb.append(c)
        }
        return sb.toString()
    }

    override fun render(delta: Float) {
        time += delta
        val pulse = 0.85f + 0.15f * sin(time * 3.0).toFloat()

        Gdx.gl.glClearColor(0.075f, 0.075f, 0.075f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        shapes.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        drawBackground()
        drawHeader()
        drawTitle(pulse)
        drawStars(pulse)
        drawScoreCard()
        drawButtons()
        drawFooter()

        Gdx.gl.glDisable(GL20.GL_BLEND)
        handleInput()
    }

    private fun drawBackground() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(1f, 1f, 1f, 0.04f)
        var dx = 0f
        while (dx <= W) {
            var dy = 0f
            while (dy <= H) {
                shapes.circle(dx, dy, 1.5f, 4)
                dy += 16f
            }
            dx += 16f
        }
        // Subtle yellow bottom glow
        for (i in 0..16) {
            val alpha = (1f - i / 16f) * 0.18f
            shapes.setColor(0.918f, 0.918f, 0f, alpha)
            shapes.rect(0f, 0f, W, (i / 16f) * H * 0.3f)
        }
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 0f, 0f, 0.1f)
        var sy = 0f
        while (sy <= H) {
            shapes.line(0f, sy, W, sy)
            sy += 4f
        }
        shapes.end()
    }

    private fun drawHeader() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, 1f)
        shapes.rect(0f, H - 48f, W, 48f)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.22f, 0.22f, 0.22f, 1f)
        shapes.line(0f, H - 48f, W, H - 48f)
        shapes.end()

        batch.begin()
        font.data.setScale(1.3f)
        font.color = Color(0.918f, 0.918f, 0f, 1f)
        font.draw(batch, "= PANIC_SYSTEM_V1.0", 12f, H - 14f)

        font.data.setScale(1.0f)
        val stageStr = "STAGE $levelId"
        layout.setText(font, stageStr)
        font.color = Color(0f, 0.859f, 0.914f, 1f)
        font.draw(batch, stageStr, W - layout.width - 16f, H - 18f)
        font.data.setScale(1f)
        batch.end()
    }

    private fun drawTitle(pulse: Float) {
        // Yellow underline bar
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.918f, 0.918f, 0f, 0.9f)
        shapes.rect(0f, H - 115f, W, 4f)
        shapes.setColor(0.918f, 0.918f, 0f, 0.25f * pulse)
        shapes.rect(0f, H - 123f, W, 12f)
        shapes.end()

        batch.begin()
        // Shadow
        font.data.setScale(4f)
        font.color = Color(0.28f, 0.28f, 0f, 1f)
        layout.setText(font, "MISSION")
        val t1x = (W - layout.width) / 2f
        font.draw(batch, "MISSION", t1x + 3f, H - 60f - 3f)

        font.color = Color(0.28f, 0.28f, 0f, 1f)
        layout.setText(font, "COMPLETE!")
        val t2x = (W - layout.width) / 2f
        font.draw(batch, "COMPLETE!", t2x + 3f, H - 118f - 3f)

        // Main yellow text
        font.color = Color(0.918f * pulse, 0.918f * pulse, 0f, 1f)
        font.draw(batch, "MISSION", t1x, H - 60f)

        font.color = Color(0.918f, 0.918f, 0f, 1f)
        font.draw(batch, "COMPLETE!", t2x, H - 118f)

        font.data.setScale(1f)
        batch.end()
    }

    private fun drawStars(pulse: Float) {
        val starY = 590f
        val spacing = 88f
        val starPositions = listOf(W / 2f - spacing, W / 2f, W / 2f + spacing)

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for ((i, sx) in starPositions.withIndex()) {
            val earned = i < stars
            if (earned) {
                // Glow halo
                shapes.setColor(0.918f, 0.918f, 0f, 0.12f * pulse)
                shapes.circle(sx, starY, 44f, 24)
                shapes.setColor(0.918f, 0.918f, 0f, 0.22f * pulse)
                shapes.circle(sx, starY, 28f, 20)
            }
        }
        shapes.end()

        batch.begin()
        font.data.setScale(4f)
        for ((i, sx) in starPositions.withIndex()) {
            val earned = i < stars
            font.color = if (earned) Color(0.918f, 0.85f + pulse * 0.05f, 0f, 1f)
                         else Color(0.208f, 0.208f, 0.208f, 1f)
            layout.setText(font, "★")
            font.draw(batch, "★", sx - layout.width / 2f, starY + layout.height / 2f)
        }
        font.data.setScale(1f)
        batch.end()
    }

    private fun drawScoreCard() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.11f, 0.106f, 0.106f, 1f)
        shapes.rect(cardX, cardY, cardW, cardH)
        shapes.setColor(1f, 1f, 1f, 0.03f)
        shapes.rect(cardX, cardY, cardW, cardH)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.282f, 0.282f, 0.192f, 1f)
        shapes.rect(cardX, cardY, cardW, cardH)
        shapes.end()

        batch.begin()
        // Label
        font.data.setScale(1.0f)
        font.color = Color(0.576f, 0.573f, 0.467f, 1f)
        layout.setText(font, "FINAL SCORE")
        font.draw(batch, "FINAL SCORE", cardX + (cardW - layout.width) / 2f, cardY + cardH - 12f)

        // Score value
        font.data.setScale(2.4f)
        font.color = Color(0.918f, 0.918f, 0f, 1f)
        val scoreText = formatScore(score)
        layout.setText(font, scoreText)
        font.draw(batch, scoreText, cardX + (cardW - layout.width) / 2f, cardY + cardH - 36f)

        font.data.setScale(1f)
        batch.end()

        // Stars indicator
        val nStars = stars.coerceIn(0, 3)
        val dotSz = 8f; val dotGap = 4f
        val totalDotW = 3 * dotSz + 2 * dotGap
        var dotX = cardX + (cardW - totalDotW) / 2f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (j in 0..2) {
            val lit = j < nStars
            shapes.setColor(if (lit) 0.918f else 0.208f, if (lit) 0.918f else 0.208f, 0f, 1f)
            shapes.rect(dotX, cardY + 14f, dotSz, dotSz)
            dotX += dotSz + dotGap
        }
        shapes.end()
    }

    private fun drawButtons() {
        // RETRY — cyan
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0.8f, 0.8f, 1f)
        shapes.rect(btnRetry.x, btnRetry.y, btnRetry.w, btnRetry.h)
        // NEXT — yellow primary
        shapes.setColor(0.918f, 0.918f, 0f, 1f)
        shapes.rect(btnNext.x, btnNext.y, btnNext.w, btnNext.h)
        // MENU — dark secondary
        shapes.setColor(0.165f, 0.165f, 0.165f, 1f)
        shapes.rect(btnMenu.x, btnMenu.y, btnMenu.w, btnMenu.h)
        shapes.end()

        // Bevels
        shapes.begin(ShapeRenderer.ShapeType.Line)
        // Retry bevel
        shapes.setColor(0.55f, 1f, 1f, 0.5f)
        shapes.line(btnRetry.x, btnRetry.y + btnRetry.h, btnRetry.x + btnRetry.w, btnRetry.y + btnRetry.h)
        shapes.line(btnRetry.x, btnRetry.y, btnRetry.x, btnRetry.y + btnRetry.h)
        shapes.setColor(0f, 0.4f, 0.4f, 0.5f)
        shapes.line(btnRetry.x, btnRetry.y, btnRetry.x + btnRetry.w, btnRetry.y)
        shapes.line(btnRetry.x + btnRetry.w, btnRetry.y, btnRetry.x + btnRetry.w, btnRetry.y + btnRetry.h)

        // Next bevel
        shapes.setColor(1f, 1f, 0.55f, 0.5f)
        shapes.line(btnNext.x, btnNext.y + btnNext.h, btnNext.x + btnNext.w, btnNext.y + btnNext.h)
        shapes.line(btnNext.x, btnNext.y, btnNext.x, btnNext.y + btnNext.h)
        shapes.setColor(0.38f, 0.38f, 0f, 0.5f)
        shapes.line(btnNext.x, btnNext.y, btnNext.x + btnNext.w, btnNext.y)
        shapes.line(btnNext.x + btnNext.w, btnNext.y, btnNext.x + btnNext.w, btnNext.y + btnNext.h)

        // Menu bevel
        shapes.setColor(0.35f, 0.35f, 0.35f, 0.5f)
        shapes.line(btnMenu.x, btnMenu.y + btnMenu.h, btnMenu.x + btnMenu.w, btnMenu.y + btnMenu.h)
        shapes.line(btnMenu.x, btnMenu.y, btnMenu.x, btnMenu.y + btnMenu.h)
        shapes.setColor(0f, 0f, 0f, 0.5f)
        shapes.line(btnMenu.x, btnMenu.y, btnMenu.x + btnMenu.w, btnMenu.y)
        shapes.line(btnMenu.x + btnMenu.w, btnMenu.y, btnMenu.x + btnMenu.w, btnMenu.y + btnMenu.h)
        shapes.end()

        batch.begin()
        font.data.setScale(2.2f)
        // RETRY text
        font.color = Color(0.055f, 0.055f, 0.055f, 1f)
        layout.setText(font, "RETRY")
        font.draw(batch, "RETRY",
            btnRetry.x + (btnRetry.w - layout.width) / 2f,
            btnRetry.y + (btnRetry.h + layout.height) / 2f)

        // NEXT text
        font.color = Color(0.055f, 0.055f, 0.055f, 1f)
        layout.setText(font, "NEXT")
        font.draw(batch, "NEXT",
            btnNext.x + (btnNext.w - layout.width) / 2f,
            btnNext.y + (btnNext.h + layout.height) / 2f)

        // MENU text
        font.color = Color(0.898f, 0.886f, 0.882f, 1f)
        layout.setText(font, "MENU")
        font.draw(batch, "MENU",
            btnMenu.x + (btnMenu.w - layout.width) / 2f,
            btnMenu.y + (btnMenu.h + layout.height) / 2f)
        font.data.setScale(1f)
        batch.end()
    }

    private fun drawFooter() {
        // 5-segment separator
        val segW = 44f; val segY = btnY - 24f
        val totalSegs = 5
        val totalSegW = totalSegs * segW + (totalSegs - 1) * 4f
        var segX = (W - totalSegW) / 2f

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until totalSegs) {
            val active = i == 2
            shapes.setColor(if (active) 0.918f else 0.18f, if (active) 0.918f else 0.18f, 0f, 1f)
            shapes.rect(segX, segY, segW, 4f)
            segX += segW + 4f
        }
        shapes.end()

        batch.begin()
        font.data.setScale(0.85f)
        font.color = Color(0.35f, 0.35f, 0.35f, 1f)
        val label = "SYSTEM.STAGE.COMPLETE"
        layout.setText(font, label)
        font.draw(batch, label, (W - layout.width) / 2f, segY - 8f)
        font.data.setScale(1f)
        batch.end()

        // Bottom-right LEDs
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0.8f, 0f, 1f)
        shapes.circle(W - 10f, 16f, 6f, 12)
        shapes.setColor(0.918f, 0.918f, 0f, 1f)
        shapes.circle(W - 28f, 16f, 6f, 12)
        shapes.setColor(0f, 0f, 0.8f, 1f)
        shapes.circle(W - 46f, 16f, 6f, 12)
        shapes.end()
    }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val tc = viewport.unproject(com.badlogic.gdx.math.Vector3(
            Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
        when {
            btnRetry.contains(tc.x, tc.y) -> game.setScreen(GameScreen(game, levelId))
            btnNext.contains(tc.x, tc.y) -> {
                val next = (levelId + 1).coerceAtMost(50)
                game.setScreen(GameScreen(game, next))
            }
            btnMenu.contains(tc.x, tc.y) -> game.setScreen(MenuScreen(game))
        }
    }

    override fun resize(w: Int, h: Int) = viewport.update(w, h, true)
    override fun dispose() { batch.dispose(); shapes.dispose(); font.dispose() }
}
