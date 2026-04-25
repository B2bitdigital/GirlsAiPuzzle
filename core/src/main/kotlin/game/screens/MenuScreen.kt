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

class MenuScreen(private val game: GirlsPanicGame) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private val W = GameConstants.FIELD_WIDTH
    private val H = GameConstants.FIELD_HEIGHT

    // Primary full-width PLAY button
    private val btnPlay   = Rect(16f, 540f, 448f, 80f)
    // Secondary side-by-side buttons
    private val btnLevels = Rect(16f, 452f, 214f, 76f)
    // (settings placeholder — non-functional, right half)
    private val btnSettingsArea = Rect(246f, 452f, 218f, 76f)

    private var time = 0f

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
        drawLogo()
        drawButtons()
        drawScoreArea()
        drawBottomNav()

        Gdx.gl.glDisable(GL20.GL_BLEND)
        handleInput()
    }

    private fun drawBackground() {
        // Pixel grid dots
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(1f, 1f, 1f, 0.04f)
        var dx = 0f
        while (dx <= W) {
            var dy = 52f
            while (dy <= H - 48f) {
                shapes.circle(dx, dy, 1.5f, 4)
                dy += 16f
            }
            dx += 16f
        }
        shapes.end()
        // Scanlines
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 0f, 0f, 0.1f)
        var sy = 52f
        while (sy <= H - 48f) {
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

        // Credits badge
        font.data.setScale(1.0f)
        val badgeTxt = "CREDITS  99"
        layout.setText(font, badgeTxt)
        val badgeX = W - layout.width - 32f
        batch.end()

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.11f, 0.106f, 0.106f, 1f)
        shapes.rect(badgeX - 8f, H - 42f, layout.width + 24f, 30f)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.35f, 0.35f, 0.35f, 1f)
        shapes.rect(badgeX - 8f, H - 42f, layout.width + 24f, 30f)
        shapes.end()

        batch.begin()
        font.color = Color(0.918f, 0.918f, 0f, 1f)
        font.draw(batch, badgeTxt, badgeX, H - 18f)
        font.data.setScale(1f)
        batch.end()
    }

    private fun drawLogo() {
        val glow = 0.88f + 0.12f * sin(time * 2.5).toFloat()

        batch.begin()
        // Shadow
        font.data.setScale(3.6f)
        font.color = Color(0.28f, 0.28f, 0f, 1f)
        layout.setText(font, "GIRLS AI PANIC")
        val tx = (W - layout.width) / 2f
        font.draw(batch, "GIRLS AI PANIC", tx + 3f, 720f - 3f)
        // Main text — electric yellow
        font.color = Color(0.918f * glow, 0.918f * glow, 0f, 1f)
        font.draw(batch, "GIRLS AI PANIC", tx, 720f)

        // Subtitle
        font.data.setScale(1.3f)
        font.color = Color(0.792f, 0.784f, 0.667f, 1f)
        layout.setText(font, "AI-POWERED PUZZLE")
        font.draw(batch, "AI-POWERED PUZZLE", (W - layout.width) / 2f, 650f)
        font.data.setScale(1f)
        batch.end()

        // 4 indicator squares below subtitle
        val dotSz = 8f; val dotGap = 6f
        val totalW = 4f * dotSz + 3f * dotGap
        val dotX = (W - totalW) / 2f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0..3) {
            val lit = i < 2
            shapes.setColor(if (lit) 0.918f else 0.208f, if (lit) 0.918f else 0.208f, 0f, 1f)
            shapes.rect(dotX + i * (dotSz + dotGap), 618f, dotSz, dotSz)
        }
        shapes.end()
    }

    private fun drawButtons() {
        // PLAY — filled yellow, sharp corners
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.918f, 0.918f, 0f, 1f)
        shapes.rect(btnPlay.x, btnPlay.y, btnPlay.w, btnPlay.h)
        shapes.end()
        // Bevel PLAY
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(1f, 1f, 0.55f, 0.6f)
        shapes.line(btnPlay.x, btnPlay.y + btnPlay.h, btnPlay.x + btnPlay.w, btnPlay.y + btnPlay.h)
        shapes.line(btnPlay.x, btnPlay.y, btnPlay.x, btnPlay.y + btnPlay.h)
        shapes.setColor(0.38f, 0.38f, 0f, 0.6f)
        shapes.line(btnPlay.x, btnPlay.y, btnPlay.x + btnPlay.w, btnPlay.y)
        shapes.line(btnPlay.x + btnPlay.w, btnPlay.y, btnPlay.x + btnPlay.w, btnPlay.y + btnPlay.h)
        shapes.end()

        // LEVELS — dark secondary, yellow outline
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.165f, 0.165f, 0.165f, 1f)
        shapes.rect(btnLevels.x, btnLevels.y, btnLevels.w, btnLevels.h)
        // Settings placeholder (dark, gray outline)
        shapes.setColor(0.125f, 0.125f, 0.122f, 1f)
        shapes.rect(btnSettingsArea.x, btnSettingsArea.y, btnSettingsArea.w, btnSettingsArea.h)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.804f, 0.804f, 0f, 1f)
        shapes.rect(btnLevels.x, btnLevels.y, btnLevels.w, btnLevels.h)
        shapes.setColor(0.282f, 0.282f, 0.192f, 1f)
        shapes.rect(btnSettingsArea.x, btnSettingsArea.y, btnSettingsArea.w, btnSettingsArea.h)
        shapes.end()

        batch.begin()
        // PLAY text — black on yellow
        font.data.setScale(2.8f)
        font.color = Color(0.196f, 0.196f, 0f, 1f)
        layout.setText(font, "PLAY")
        font.draw(batch, "PLAY",
            btnPlay.x + (btnPlay.w - layout.width) / 2f,
            btnPlay.y + (btnPlay.h + layout.height) / 2f)

        // LEVELS text — yellow on dark
        font.data.setScale(1.8f)
        font.color = Color(0.918f, 0.918f, 0f, 1f)
        layout.setText(font, "LEVELS")
        font.draw(batch, "LEVELS",
            btnLevels.x + (btnLevels.w - layout.width) / 2f,
            btnLevels.y + (btnLevels.h + layout.height) / 2f)

        // Settings text — muted
        font.color = Color(0.576f, 0.573f, 0.467f, 1f)
        layout.setText(font, "SETTINGS")
        font.draw(batch, "SETTINGS",
            btnSettingsArea.x + (btnSettingsArea.w - layout.width) / 2f,
            btnSettingsArea.y + (btnSettingsArea.h + layout.height) / 2f)

        font.data.setScale(1f)
        batch.end()
    }

    private fun drawScoreArea() {
        val hs = game.prefs.getHighScore()
        if (hs <= 0) return

        batch.begin()
        font.data.setScale(1.0f)
        font.color = Color(0.576f, 0.573f, 0.467f, 1f)
        layout.setText(font, "BEST SCORE")
        font.draw(batch, "BEST SCORE", (W - layout.width) / 2f, 420f)

        font.data.setScale(2.2f)
        font.color = Color(0.918f, 0.918f, 0f, 1f)
        val hsText = formatScore(hs)
        layout.setText(font, hsText)
        font.draw(batch, hsText, (W - layout.width) / 2f, 398f)
        font.data.setScale(1f)
        batch.end()

        // Two cyan indicator dots
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0.859f, 0.914f, 1f)
        val dotY = 358f
        shapes.rect(W / 2f - 10f, dotY, 8f, 8f)
        shapes.rect(W / 2f + 2f, dotY, 8f, 8f)
        shapes.end()
    }

    private fun formatScore(n: Int): String {
        val s = n.toString()
        val sb = StringBuilder()
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
            sb.append(c)
        }
        return sb.toString()
    }

    private fun drawBottomNav() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, 1f)
        shapes.rect(0f, 0f, W, 52f)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.22f, 0.22f, 0.22f, 1f)
        shapes.line(0f, 52f, W, 52f)
        shapes.end()

        val tabs = listOf("GAME" to true, "SCORES" to false, "LEVELS" to false, "CFG" to false)
        val tabW = W / tabs.size

        batch.begin()
        font.data.setScale(0.85f)
        for ((i, tab) in tabs.withIndex()) {
            font.color = if (tab.second) Color(0.918f, 0.918f, 0f, 1f)
                         else Color(0.576f, 0.573f, 0.467f, 1f)
            layout.setText(font, tab.first)
            font.draw(batch, tab.first, i * tabW + (tabW - layout.width) / 2f, 33f)
        }
        font.data.setScale(1f)
        batch.end()

        // Active tab indicator bar (GAME tab)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.918f, 0.918f, 0f, 1f)
        shapes.rect(0f, 48f, tabW, 4f)
        shapes.end()
    }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val tc = viewport.unproject(com.badlogic.gdx.math.Vector3(
            Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
        when {
            btnPlay.contains(tc.x, tc.y) -> {
                game.currentLevelId = 1
                game.setScreen(GameScreen(game, game.currentLevelId))
            }
            btnLevels.contains(tc.x, tc.y) -> game.setScreen(LevelSelectScreen(game))
            tc.y < 52f -> {
                val navIdx = (tc.x / (W / 4f)).toInt().coerceIn(0, 3)
                if (navIdx == 2) game.setScreen(LevelSelectScreen(game))
            }
        }
    }

    override fun resize(w: Int, h: Int) = viewport.update(w, h, true)
    override fun dispose() { batch.dispose(); shapes.dispose(); font.dispose() }
}

data class Rect(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun contains(px: Float, py: Float) = px in x..(x + w) && py in y..(y + h)
}
