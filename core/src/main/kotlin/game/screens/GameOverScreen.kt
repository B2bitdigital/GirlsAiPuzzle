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

class GameOverScreen(
    private val game: GirlsPanicGame,
    private val levelId: Int,
    private val score: Int = 0
) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private val W = GameConstants.FIELD_WIDTH
    private val H = GameConstants.FIELD_HEIGHT

    // Header
    private val headerH = 50f

    // "GAME OVER" title area  — bottom of title line
    private val titleY = H - headerH - 60f

    // Stat cards (side by side)
    private val cardPad = 16f
    private val cardW = (W - cardPad * 3) / 2f
    private val cardH = 100f
    private val cardY = titleY - 40f - cardH

    private val card1X = cardPad
    private val card2X = cardPad * 2 + cardW

    // Buttons
    private val btnH = 75f
    private val btnW = W - cardPad * 2
    private val btnRetryY = cardY - cardPad - btnH
    private val btnMenuY  = btnRetryY - cardPad - btnH

    private val btnRetry = Rect(cardPad, btnRetryY, btnW, btnH)
    private val btnMenu  = Rect(cardPad, btnMenuY,  btnW, btnH)

    // Glitch animation
    private var time = 0f
    // Scanline dots for pixel grid effect
    private val dotCols = 30
    private val dotRows = 50

    // Score formatted with commas
    private val scoreText = formatScore(score)
    private val stageText = "STAGE $levelId"

    private fun formatScore(n: Int): String {
        if (n == 0) return "0"
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

        Gdx.gl.glClearColor(0.075f, 0.075f, 0.075f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        shapes.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        drawBackground()
        drawHeader()
        drawTitleGameOver()
        drawStatCards()
        drawButtons()
        drawFooter()

        Gdx.gl.glDisable(GL20.GL_BLEND)

        handleInput()
    }

    private fun drawBackground() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // Pixel grid dots
        val stepX = W / dotCols
        val stepY = H / dotRows
        shapes.setColor(1f, 1f, 1f, 0.04f)
        for (col in 0..dotCols) {
            for (row in 0..dotRows) {
                shapes.circle(col * stepX, row * stepY, 1.2f, 4)
            }
        }
        // Red bottom gradient fade
        for (i in 0..20) {
            val alpha = (1f - i / 20f) * 0.25f
            shapes.setColor(0.5f, 0f, 0f, alpha)
            shapes.rect(0f, 0f, W, (i / 20f) * H * 0.35f)
        }
        shapes.end()

        // Scanlines overlay
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 0f, 0f, 0.12f)
        var sy = 0f
        while (sy < H) {
            shapes.line(0f, sy, W, sy)
            sy += 4f
        }
        shapes.end()
    }

    private fun drawHeader() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, 1f)
        shapes.rect(0f, H - headerH, W, headerH)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.22f, 0.22f, 0.22f, 1f)
        shapes.line(0f, H - headerH, W, H - headerH)
        shapes.end()

        batch.begin()
        // Menu icon (three lines)
        font.data.setScale(1.5f)
        font.color = Color(1f, 0.9f, 0f, 1f)
        font.draw(batch, "=", 14f, H - 15f)

        // Title text
        font.data.setScale(1.2f)
        font.color = Color(1f, 0.9f, 0f, 1f)
        font.draw(batch, "PANIC_SYSTEM_v1.0", 42f, H - 15f)

        // Coin counter (top right)
        font.data.setScale(1.1f)
        font.color = Color(1f, 0.9f, 0f, 1f)
        val coinText = "00,450"
        layout.setText(font, coinText)
        font.draw(batch, coinText, W - layout.width - 16f, H - 15f)
        font.data.setScale(1f)
        batch.end()

        // Coin badge border
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.35f, 0.35f, 0.35f, 1f)
        val coinX = W - 85f
        shapes.rect(coinX, H - headerH + 8f, 75f, 32f)
        shapes.end()
    }

    private fun drawTitleGameOver() {
        val glitchShift = if ((time * 8).toInt() % 10 < 2) 3f else 0f
        val pulse = 0.85f + 0.15f * sin(time * 4.0).toFloat()

        // Red underline bar below title — #ca002d
        val barY = titleY - 80f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.792f, 0f, 0.176f, 0.9f)
        shapes.rect(0f, barY, W, 4f)
        // Glow on bar
        shapes.setColor(0.792f, 0f, 0.176f, 0.3f * pulse)
        shapes.rect(0f, barY - 4f, W, 12f)
        shapes.end()

        batch.begin()
        // Shadow layer 1 (dark red offset)
        font.data.setScale(5f)
        font.color = Color(0.25f, 0f, 0f, 1f)
        layout.setText(font, "GAME")
        val gameX = (W - layout.width) / 2f
        font.draw(batch, "GAME", gameX + 4f, titleY - 4f)

        font.color = Color(0.25f, 0f, 0f, 1f)
        layout.setText(font, "OVER")
        val overX = (W - layout.width) / 2f
        font.draw(batch, "OVER", overX + 4f, titleY - 55f)

        // Cyan glitch layer
        font.color = Color(0f, 1f, 1f, 0.35f)
        font.draw(batch, "GAME", gameX - glitchShift, titleY)
        font.draw(batch, "OVER", overX - glitchShift, titleY - 55f)

        // Red glitch layer
        font.color = Color(1f, 0f, 0f, 0.4f)
        font.draw(batch, "GAME", gameX + glitchShift, titleY)
        font.draw(batch, "OVER", overX + glitchShift, titleY - 55f)

        // Main red text — #ca002d hazard red
        font.color = Color(0.792f, 0f, 0.176f, 1f)
        font.draw(batch, "GAME", gameX, titleY)
        font.draw(batch, "OVER", overX, titleY - 55f)

        font.data.setScale(1f)
        batch.end()
    }

    private fun drawCard(x: Float, y: Float, w: Float, h: Float) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.11f, 0.11f, 0.11f, 1f)
        shapes.rect(x, y, w, h)
        // Subtle white tint
        shapes.setColor(1f, 1f, 1f, 0.03f)
        shapes.rect(x, y, w, h)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.28f, 0.28f, 0.28f, 1f)
        shapes.rect(x, y, w, h)
        shapes.end()
    }

    private fun drawStatCards() {
        drawCard(card1X, cardY, cardW, cardH)
        drawCard(card2X, cardY, cardW, cardH)

        batch.begin()

        // Card 1 — Final Score
        font.data.setScale(1.0f)
        font.color = Color(0.55f, 0.55f, 0.55f, 1f)
        layout.setText(font, "FINAL SCORE")
        font.draw(batch, "FINAL SCORE",
            card1X + (cardW - layout.width) / 2f,
            cardY + cardH - 14f)

        font.data.setScale(2.2f)
        font.color = Color(1f, 0.9f, 0f, 1f)
        layout.setText(font, scoreText)
        font.draw(batch, scoreText,
            card1X + (cardW - layout.width) / 2f,
            cardY + cardH - 36f)

        // 2 yellow dots indicator
        font.data.setScale(1f)
        batch.end()

        // Yellow indicator dots card 1
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val dot1BaseX = card1X + cardW / 2f - 12f
        val dotBaseY = cardY + 14f
        shapes.setColor(1f, 0.9f, 0f, 1f)
        shapes.rect(dot1BaseX,       dotBaseY, 8f, 8f)
        shapes.rect(dot1BaseX + 12f, dotBaseY, 8f, 8f)
        shapes.setColor(1f, 0.9f, 0f, 0.2f)
        shapes.rect(dot1BaseX + 24f, dotBaseY, 8f, 8f)
        shapes.end()

        batch.begin()
        // Card 2 — Level Reached
        font.data.setScale(1.0f)
        font.color = Color(0.55f, 0.55f, 0.55f, 1f)
        layout.setText(font, "LEVEL REACHED")
        font.draw(batch, "LEVEL REACHED",
            card2X + (cardW - layout.width) / 2f,
            cardY + cardH - 14f)

        font.data.setScale(2.2f)
        font.color = Color(0f, 0.86f, 0.91f, 1f)
        layout.setText(font, stageText)
        font.draw(batch, stageText,
            card2X + (cardW - layout.width) / 2f,
            cardY + cardH - 36f)

        font.data.setScale(1f)
        batch.end()

        // Cyan indicator dots card 2
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val dot2BaseX = card2X + cardW / 2f - 18f
        shapes.setColor(0f, 0.86f, 0.91f, 1f)
        shapes.rect(dot2BaseX,       dotBaseY, 8f, 8f)
        shapes.rect(dot2BaseX + 12f, dotBaseY, 8f, 8f)
        shapes.rect(dot2BaseX + 24f, dotBaseY, 8f, 8f)
        shapes.end()
    }

    private fun drawButtons() {
        // RETRY button — yellow #eaea00
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.918f, 0.918f, 0f, 1f)
        shapes.rect(btnRetry.x, btnRetry.y, btnRetry.w, btnRetry.h)
        // EXIT TO MENU — dark gray
        shapes.setColor(0.14f, 0.14f, 0.14f, 1f)
        shapes.rect(btnMenu.x, btnMenu.y, btnMenu.w, btnMenu.h)
        shapes.end()

        // Bevel effects (top+left highlight, bottom+right shadow)
        shapes.begin(ShapeRenderer.ShapeType.Line)
        // RETRY bevel
        shapes.setColor(1f, 1f, 0.6f, 0.5f)
        shapes.line(btnRetry.x, btnRetry.y + btnRetry.h, btnRetry.x + btnRetry.w, btnRetry.y + btnRetry.h)
        shapes.line(btnRetry.x, btnRetry.y, btnRetry.x, btnRetry.y + btnRetry.h)
        shapes.setColor(0.5f, 0.43f, 0f, 0.5f)
        shapes.line(btnRetry.x, btnRetry.y, btnRetry.x + btnRetry.w, btnRetry.y)
        shapes.line(btnRetry.x + btnRetry.w, btnRetry.y, btnRetry.x + btnRetry.w, btnRetry.y + btnRetry.h)
        // EXIT bevel
        shapes.setColor(0.4f, 0.4f, 0.4f, 0.5f)
        shapes.line(btnMenu.x, btnMenu.y + btnMenu.h, btnMenu.x + btnMenu.w, btnMenu.y + btnMenu.h)
        shapes.line(btnMenu.x, btnMenu.y, btnMenu.x, btnMenu.y + btnMenu.h)
        shapes.setColor(0f, 0f, 0f, 0.5f)
        shapes.line(btnMenu.x, btnMenu.y, btnMenu.x + btnMenu.w, btnMenu.y)
        shapes.line(btnMenu.x + btnMenu.w, btnMenu.y, btnMenu.x + btnMenu.w, btnMenu.y + btnMenu.h)
        shapes.end()

        batch.begin()
        // RETRY text (black on yellow)
        font.data.setScale(2.4f)
        font.color = Color(0.1f, 0.1f, 0.1f, 1f)
        layout.setText(font, "RETRY")
        font.draw(batch, "RETRY",
            btnRetry.x + (btnRetry.w - layout.width) / 2f,
            btnRetry.y + (btnRetry.h + layout.height) / 2f)

        // EXIT TO MENU text (white on dark)
        font.color = Color(0.88f, 0.88f, 0.88f, 1f)
        layout.setText(font, "EXIT TO MENU")
        font.draw(batch, "EXIT TO MENU",
            btnMenu.x + (btnMenu.w - layout.width) / 2f,
            btnMenu.y + (btnMenu.h + layout.height) / 2f)

        font.data.setScale(1f)
        batch.end()
    }

    private fun drawFooter() {
        // Separator dots row
        val segW = 44f
        val segY = btnMenuY - 24f
        val totalSegs = 5
        val totalW = totalSegs * segW + (totalSegs - 1) * 4f
        var segX = (W - totalW) / 2f

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until totalSegs) {
            val active = i == 2
            shapes.setColor(if (active) 0.8f else 0.18f,
                            if (active) 0f    else 0.18f,
                            if (active) 0.1f  else 0.18f,
                            1f)
            shapes.rect(segX, segY, segW, 4f)
            segX += segW + 4f
        }
        shapes.end()

        // Footer label
        batch.begin()
        font.data.setScale(0.85f)
        font.color = Color(0.35f, 0.35f, 0.35f, 1f)
        val footerText = "SYSTEM.CRITICAL.PROCESS.TERMINATED"
        layout.setText(font, footerText)
        font.draw(batch, footerText, (W - layout.width) / 2f, segY - 8f)
        font.data.setScale(1f)
        batch.end()

        // Bottom-left hardware info box
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, 0.75f)
        shapes.rect(0f, 0f, 175f, 44f)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.22f, 0.22f, 0.22f, 1f)
        shapes.line(175f, 0f, 175f, 44f)
        shapes.line(0f, 44f, 175f, 44f)
        shapes.end()

        batch.begin()
        font.data.setScale(0.75f)
        font.color = Color(0.3f, 0.3f, 0.3f, 1f)
        font.draw(batch, "HARDWARE: CABIN_UNIT_04", 6f, 38f)
        font.draw(batch, "LOCATION: DISTRICT_9_ARCADE", 6f, 22f)
        font.data.setScale(1f)
        batch.end()

        // Bottom-right status LEDs
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val pulse = 0.6f + 0.4f * sin(time * 6.0).toFloat()
        shapes.setColor(0.9f, 0f, 0f, pulse)
        shapes.circle(W - 46f, 16f, 6f, 12)
        shapes.setColor(0f, 0.6f, 0f, 1f)
        shapes.circle(W - 28f, 16f, 6f, 12)
        shapes.setColor(0f, 0f, 0.8f, 1f)
        shapes.circle(W - 10f, 16f, 6f, 12)
        shapes.end()
    }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val tc = viewport.unproject(com.badlogic.gdx.math.Vector3(
            Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
        when {
            btnRetry.contains(tc.x, tc.y) -> game.setScreen(GameScreen(game, levelId))
            btnMenu.contains(tc.x, tc.y)  -> game.setScreen(MenuScreen(game))
        }
    }

    override fun resize(w: Int, h: Int) = viewport.update(w, h, true)
    override fun dispose() { batch.dispose(); shapes.dispose(); font.dispose() }
}
