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

class LevelSelectScreen(private val game: GirlsPanicGame) : ScreenAdapter() {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private val W = GameConstants.FIELD_WIDTH
    private val H = GameConstants.FIELD_HEIGHT

    private val cols = 5
    private val rows = 10
    private val cellW = 84f
    private val cellH = 48f
    private val cellGap = 4f
    private val gridMargin = 18f

    // Grid top-left: y of bottom of first row
    // Header at H-48..H, title at H-84..H-48
    // Grid occupies downward from y=724 (below title)
    private val gridTop = 718f  // bottom-left y of row-0 rect = gridTop - cellH

    // Buttons at bottom
    private val btnStart = Rect(16f, 68f, 448f, 64f)
    private val btnHome  = Rect(16f, 4f,  216f, 56f)
    private val btnReset = Rect(248f, 4f, 216f, 56f)

    private var selectedLevel: Int = -1
    private var time = 0f

    private fun cellRect(i: Int): Rect {
        val col = (i - 1) % cols
        val row = (i - 1) / cols
        val x = gridMargin + col * (cellW + cellGap)
        val y = gridTop - row * (cellH + cellGap) - cellH
        return Rect(x, y, cellW, cellH)
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
        drawTitle()
        drawGrid()
        drawButtons()
        drawStatusBar()

        Gdx.gl.glDisable(GL20.GL_BLEND)
        handleInput()
    }

    private fun drawBackground() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(1f, 1f, 1f, 0.035f)
        var dx = 0f
        while (dx <= W) {
            var dy = 0f
            while (dy <= H) {
                shapes.circle(dx, dy, 1.5f, 4)
                dy += 16f
            }
            dx += 16f
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
        font.color = Color(0.918f, 0.918f, 0f, 1f)
        font.draw(batch, "V1.0-BETA", W - 88f, H - 18f)
        font.data.setScale(1f)
        batch.end()
    }

    private fun drawTitle() {
        // Dashed separator + title
        batch.begin()
        font.data.setScale(1.6f)
        font.color = Color(0.918f, 0.918f, 0f, 1f)
        layout.setText(font, "SELECT LEVEL")
        val tx = (W - layout.width) / 2f
        font.draw(batch, "SELECT LEVEL", tx, H - 58f)
        font.data.setScale(1f)
        batch.end()

        // Dashed lines left and right of title
        val lineY = H - 70f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.804f, 0.804f, 0f, 0.7f)
        var lx = 12f
        while (lx < tx - 8f) {
            shapes.rect(lx, lineY, 8f, 2f)
            lx += 12f
        }
        var rx = tx + layout.width + 8f
        while (rx < W - 12f) {
            shapes.rect(rx, lineY, 8f, 2f)
            rx += 12f
        }
        shapes.end()
    }

    private fun drawGrid() {
        for (i in 1..50) {
            val r = cellRect(i)
            val unlocked = game.prefs.isLevelUnlocked(i)
            val selected = i == selectedLevel
            val stars = game.prefs.getStars(i)

            // Cell background
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            when {
                selected  -> shapes.setColor(0.18f, 0.18f, 0f, 1f)
                unlocked  -> shapes.setColor(0.11f, 0.106f, 0.106f, 1f)
                else      -> shapes.setColor(0.055f, 0.055f, 0.055f, 1f)
            }
            shapes.rect(r.x, r.y, r.w, r.h)
            shapes.end()

            // Cell border
            shapes.begin(ShapeRenderer.ShapeType.Line)
            when {
                selected -> shapes.setColor(0.918f, 0.918f, 0f, 1f)
                unlocked -> shapes.setColor(0.282f, 0.282f, 0.192f, 1f)
                else     -> shapes.setColor(0.125f, 0.125f, 0.122f, 1f)
            }
            shapes.rect(r.x, r.y, r.w, r.h)
            shapes.end()

            // Level number
            batch.begin()
            font.data.setScale(1.4f)
            when {
                selected -> font.color = Color(0.918f, 0.918f, 0f, 1f)
                unlocked -> font.color = Color(0.898f, 0.886f, 0.882f, 1f)
                else     -> font.color = Color(0.208f, 0.208f, 0.208f, 1f)
            }
            val numStr = "%02d".format(i)
            layout.setText(font, numStr)
            font.draw(batch, numStr,
                r.x + (r.w - layout.width) / 2f,
                r.y + r.h - 6f)

            // Stars (unlocked only, if any)
            if (unlocked && stars > 0) {
                font.data.setScale(0.75f)
                font.color = Color(0.918f, 0.918f, 0f, 1f)
                val starStr = "★".repeat(stars)
                layout.setText(font, starStr)
                font.draw(batch, starStr,
                    r.x + (r.w - layout.width) / 2f,
                    r.y + 14f)
            }
            font.data.setScale(1f)
            batch.end()
        }
    }

    private fun drawButtons() {
        // START SELECTED — yellow if a level selected, dimmed if not
        val canStart = selectedLevel > 0 && game.prefs.isLevelUnlocked(selectedLevel)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(if (canStart) 0.918f else 0.35f, if (canStart) 0.918f else 0.35f, 0f, 1f)
        shapes.rect(btnStart.x, btnStart.y, btnStart.w, btnStart.h)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(if (canStart) 1f else 0.5f, if (canStart) 1f else 0.5f, if (canStart) 0.5f else 0f, 0.5f)
        shapes.rect(btnStart.x, btnStart.y, btnStart.w, btnStart.h)
        shapes.end()

        // HOME + RESET — dark
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.165f, 0.165f, 0.165f, 1f)
        shapes.rect(btnHome.x, btnHome.y, btnHome.w, btnHome.h)
        shapes.rect(btnReset.x, btnReset.y, btnReset.w, btnReset.h)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.804f, 0.804f, 0f, 1f)
        shapes.rect(btnHome.x, btnHome.y, btnHome.w, btnHome.h)
        shapes.rect(btnReset.x, btnReset.y, btnReset.w, btnReset.h)
        shapes.end()

        batch.begin()
        // START text
        font.data.setScale(2.0f)
        font.color = Color(0.055f, 0.055f, 0.055f, 1f)
        val startLabel = if (selectedLevel > 0) "START  LEVEL $selectedLevel" else "START SELECTED"
        layout.setText(font, startLabel)
        font.draw(batch, startLabel,
            btnStart.x + (btnStart.w - layout.width) / 2f,
            btnStart.y + (btnStart.h + layout.height) / 2f)

        // HOME / RESET text
        font.data.setScale(1.5f)
        font.color = Color(0.918f, 0.918f, 0f, 1f)
        layout.setText(font, "HOME")
        font.draw(batch, "HOME",
            btnHome.x + (btnHome.w - layout.width) / 2f,
            btnHome.y + (btnHome.h + layout.height) / 2f)
        layout.setText(font, "RESET")
        font.draw(batch, "RESET",
            btnReset.x + (btnReset.w - layout.width) / 2f,
            btnReset.y + (btnReset.h + layout.height) / 2f)
        font.data.setScale(1f)
        batch.end()
    }

    private fun drawStatusBar() {
        // Thin status strip at very bottom (behind buttons)
        // Only draw the server status text below HOME/RESET
        batch.begin()
        font.data.setScale(0.8f)
        font.color = Color(0.282f, 0.282f, 0.192f, 1f)
        font.draw(batch, "SERVER: CONNECTED", 16f, 3f)
        font.data.setScale(1f)
        batch.end()
        // Green status LED
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0.8f, 0.3f, 1f)
        shapes.circle(130f, -3f, 4f, 8)
        shapes.end()
    }

    private fun handleInput() {
        if (!Gdx.input.justTouched()) return
        val tc = viewport.unproject(com.badlogic.gdx.math.Vector3(
            Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

        when {
            btnHome.contains(tc.x, tc.y) -> game.setScreen(MenuScreen(game))
            btnReset.contains(tc.x, tc.y) -> selectedLevel = -1
            btnStart.contains(tc.x, tc.y) -> {
                if (selectedLevel > 0 && game.prefs.isLevelUnlocked(selectedLevel)) {
                    game.currentLevelId = selectedLevel
                    game.setScreen(GameScreen(game, selectedLevel))
                }
            }
            else -> {
                for (i in 1..50) {
                    val r = cellRect(i)
                    if (r.contains(tc.x, tc.y)) {
                        if (game.prefs.isLevelUnlocked(i)) {
                            selectedLevel = if (selectedLevel == i) -1 else i
                        }
                        return
                    }
                }
                // Header back tap
                if (tc.y > H - 48f && tc.x < 60f) game.setScreen(MenuScreen(game))
            }
        }
    }

    override fun resize(w: Int, h: Int) = viewport.update(w, h, true)
    override fun dispose() { batch.dispose(); shapes.dispose(); font.dispose() }
}
