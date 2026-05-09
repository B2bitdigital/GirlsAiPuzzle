package game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import game.GameConstants
import game.GirlsPanicGame
import game.ecs.EcsWorld
import game.ecs.EnemyType
import game.ecs.Entity
import game.ecs.WorldEvent
import game.ecs.systems.GridPoint
import game.level.LevelLoader
import kotlin.math.abs
import kotlin.math.sign

class GameScreen(
    private val game: GirlsPanicGame,
    private val levelId: Int
) : ScreenAdapter() {

    private lateinit var world: EcsWorld
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val layout = GlyphLayout()

    private var bgTexture: Texture? = null
    private var enemyTexture: Texture? = null

    // Enemy tint colors
    private val colorSpider    = Color(1f, 0.15f, 0.15f, 1f)
    private val colorCockroach = Color(0.7f, 0.45f, 0.1f, 1f)
    private val colorWasp      = Color(1f, 0.9f, 0f, 1f)
    private val colorSnail     = Color(0.2f, 1f, 0.4f, 1f)
    private val colorPlayer    = Color(0f, 1f, 1f, 1f)
    private val colorLine      = Color(1f, 0.55f, 0f, 1f)

    private var levelCompleteOverlay = false
    private var overlayStars = 0
    private var overlayScore = 0
    private var overlayLevelId = 0
    private var retryPressed = false
    private var nextPressed = false
    private var menuPressed = false

    private val OVERLAY_PANEL_H = 340f

    override fun show() {
        val json = Gdx.files.internal("levels/level_%02d.json".format(levelId)).readString()
        val levelData = LevelLoader.fromJson(json)
        world = EcsWorld(levelData, game.prefs)
        world.init()

        try {
            bgTexture = Texture(Gdx.files.internal(levelData.background))
        } catch (e: Exception) {
            Gdx.app.log("GameScreen", "Background not found: ${levelData.background}")
        }
        try {
            enemyTexture = Texture(Gdx.files.internal("nemico.png"))
        } catch (e: Exception) {
            Gdx.app.log("GameScreen", "Enemy texture not found")
        }

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (levelCompleteOverlay) {
                    val worldCoords = viewport.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
                    val W = GameConstants.FIELD_WIDTH
                    val panelW = W * 0.82f
                    val panelX = (W - panelW) / 2f
                    val btnW = panelW / 3f - 12f
                    val btnY = (GameConstants.FIELD_HEIGHT - GameConstants.HUD_HEIGHT - OVERLAY_PANEL_H) / 2f + 52f
                    val btnH = 38f
                    val wx = worldCoords.x; val wy = worldCoords.y
                    retryPressed = wx >= panelX + 10f && wx <= panelX + 10f + btnW && wy >= btnY && wy <= btnY + btnH
                    nextPressed  = wx >= panelX + btnW + 16f && wx <= panelX + 2f * btnW + 16f && wy >= btnY && wy <= btnY + btnH
                    menuPressed  = wx >= panelX + 2f * (btnW + 6f) + 10f && wx <= panelX + 3f * btnW + 22f && wy >= btnY && wy <= btnY + btnH
                    return true
                }
                applyDirection(screenX, screenY)
                return true
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if (levelCompleteOverlay) return true
                applyDirection(screenX, screenY)
                return true
            }
        }
    }

    override fun render(delta: Float) {
        val event = world.update(delta)

        Gdx.gl.glClearColor(0f, 0f, 0.06f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        batch.projectionMatrix = camera.combined
        shapes.projectionMatrix = camera.combined

        drawBackground()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        drawFreeOverlay()
        drawTerritoryBorder()
        drawCurrentLine()
        drawPowerups()
        drawEnemies()
        drawPlayer()

        Gdx.gl.glDisable(GL20.GL_BLEND)

        drawHUD()

        when (event) {
            WorldEvent.GameOver -> game.setScreen(GameOverScreen(game, levelId, world.score))
            WorldEvent.LevelComplete -> {
                if (!levelCompleteOverlay) {
                    overlayStars = world.computeStars()
                    overlayScore = world.score
                    overlayLevelId = levelId
                    game.prefs.saveStars(levelId, overlayStars)
                    game.prefs.saveHighScore(overlayScore)
                    game.prefs.incrementLevelsCompleted()
                    levelCompleteOverlay = true
                    world.territory.revealAll()
                }
            }
            else -> {}
        }

        if (levelCompleteOverlay) {
            drawLevelCompleteOverlay()
            handleOverlayNavigation()
        }
    }

    private fun applyDirection(screenX: Int, screenY: Int) {
        val worldCoords = viewport.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
        val pc = world.player.playerComp ?: return
        val pos = world.player.position
        val dx = worldCoords.x - pos.x
        val dy = worldCoords.y - pos.y
        if (abs(dx) > 5f || abs(dy) > 5f) {
            if (abs(dx) > abs(dy)) {
                pc.dirX = sign(dx); pc.dirY = 0f
            } else {
                pc.dirX = 0f; pc.dirY = sign(dy)
            }
            pc.moving = true
        }
    }

    private fun drawBackground() {
        val tex = bgTexture ?: return
        batch.begin()
        val grid = world.territory.grid
        val cs = GameConstants.CELL_SIZE
        val texW = tex.width.toFloat()
        val texH = tex.height.toFloat()
        val cellTexW = texW / GameConstants.GRID_COLS
        val cellTexH = texH / GameConstants.GRID_ROWS
        for (c in 0 until GameConstants.GRID_COLS) {
            for (r in 0 until GameConstants.GRID_ROWS) {
                if (grid[c][r]) {
                    val srcX = (c * cellTexW).toInt()
                    val srcY = (texH - (r + 1) * cellTexH).toInt()
                    val srcW = cellTexW.toInt().coerceAtLeast(1)
                    val srcH = cellTexH.toInt().coerceAtLeast(1)
                    batch.draw(tex,
                        c * cs, r * cs, cs, cs,
                        srcX, srcY, srcW, srcH,
                        false, false)
                }
            }
        }
        batch.end()
    }

    private fun drawFreeOverlay() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0.06f, 0.92f)
        val grid = world.territory.grid
        val cs = GameConstants.CELL_SIZE
        for (c in 0 until GameConstants.GRID_COLS) {
            for (r in 0 until GameConstants.GRID_ROWS) {
                if (!grid[c][r]) shapes.rect(c * cs, r * cs, cs, cs)
            }
        }
        shapes.end()
    }

    private fun drawTerritoryBorder() {
        val grid = world.territory.grid
        val cs = GameConstants.CELL_SIZE
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 0.8f, 0.8f, 0.5f)
        for (c in 0 until GameConstants.GRID_COLS) {
            for (r in 0 until GameConstants.GRID_ROWS) {
                if (!grid[c][r]) continue
                if (c + 1 < GameConstants.GRID_COLS && !grid[c + 1][r])
                    shapes.line((c + 1) * cs, r * cs, (c + 1) * cs, (r + 1) * cs)
                if (c - 1 >= 0 && !grid[c - 1][r])
                    shapes.line(c * cs, r * cs, c * cs, (r + 1) * cs)
                if (r + 1 < GameConstants.GRID_ROWS && !grid[c][r + 1])
                    shapes.line(c * cs, (r + 1) * cs, (c + 1) * cs, (r + 1) * cs)
                if (r - 1 >= 0 && !grid[c][r - 1])
                    shapes.line(c * cs, r * cs, (c + 1) * cs, r * cs)
            }
        }
        shapes.end()
    }

    private fun drawCurrentLine() {
        if (world.territory.currentLine.isEmpty()) return
        val pulse = (System.currentTimeMillis() % 600) / 600f
        val cs = GameConstants.CELL_SIZE

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // Glow layer
        shapes.setColor(colorLine.r, colorLine.g, colorLine.b, 0.18f)
        for (pt in world.territory.currentLine) {
            shapes.rect(pt.col * cs - cs * 0.5f, pt.row * cs - cs * 0.5f, cs * 2f, cs * 2f)
        }
        // Core
        val bright = 0.7f + pulse * 0.3f
        shapes.setColor(colorLine.r, colorLine.g * bright, 0f, 1f)
        for (pt in world.territory.currentLine) {
            shapes.rect(pt.col * cs, pt.row * cs, cs, cs)
        }
        shapes.end()
    }

    private fun drawEnemies() {
        val tex = enemyTexture
        val offsetX = 0f
        val offsetY = 0f
        if (tex != null) {
            batch.begin()
            for (eState in world.enemies) {
                val e = eState.entity as Entity.Enemy
                val pos = eState.position
                val col = when (e.type) {
                    EnemyType.SPIDER    -> colorSpider
                    EnemyType.COCKROACH -> colorCockroach
                    EnemyType.WASP      -> colorWasp
                    EnemyType.SNAIL     -> colorSnail
                }
                val size = if (e.type == EnemyType.SNAIL) 24f else 32f
                batch.setColor(col.r, col.g, col.b, 1f)
                batch.draw(tex,
                    offsetX + pos.x - size / 2f,
                    offsetY + pos.y - size / 2f,
                    size, size)
            }
            batch.setColor(1f, 1f, 1f, 1f)
            batch.end()
            // Restore blend state for subsequent ShapeRenderer calls
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        } else {
            // Fallback: glowing circles
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            for (eState in world.enemies) {
                val e = eState.entity as Entity.Enemy
                val pos = eState.position
                val col = when (e.type) {
                    EnemyType.SPIDER    -> colorSpider
                    EnemyType.COCKROACH -> colorCockroach
                    EnemyType.WASP      -> colorWasp
                    EnemyType.SNAIL     -> colorSnail
                }
                shapes.setColor(col.r, col.g, col.b, 0.25f)
                shapes.circle(offsetX + pos.x, offsetY + pos.y, 14f, 16)
                shapes.setColor(col)
                val radius = if (e.type == EnemyType.SNAIL) 6f else 9f
                shapes.circle(offsetX + pos.x, offsetY + pos.y, radius, 16)
            }
            shapes.end()
        }
    }

    /** Draws a filled diamond (rhombus) centered at (cx, cy) with half-span s. */
    private fun drawDiamond(cx: Float, cy: Float, s: Float) {
        shapes.triangle(cx, cy + s, cx + s, cy, cx - s, cy)
        shapes.triangle(cx - s, cy, cx + s, cy, cx, cy - s)
    }

    private fun drawPlayer() {
        val pos = world.player.position
        val pulse = (System.currentTimeMillis() % 800) / 800f
        val renderPos = playerRenderPos(pos.x, pos.y)
        val cx = renderPos.x
        val cy = renderPos.y
        val S = GameConstants.PLAYER_DIAMOND_HALF

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 0.08f)
        drawDiamond(cx, cy, S * 2.8f)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 0.22f)
        drawDiamond(cx, cy, S * 1.6f)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 0.55f)
        drawDiamond(cx, cy, S)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 1f)
        drawDiamond(cx, cy, S * 0.6f + pulse * S * 0.15f)
        shapes.end()

        // Direction indicator
        val pc = world.player.playerComp
        if (pc != null && pc.moving) {
            val dx = pc.dirX * 18f
            val dy = pc.dirY * 18f
            if (dx != 0f || dy != 0f) {
                shapes.begin(ShapeRenderer.ShapeType.Line)
                shapes.setColor(0f, 1f, 1f, 0.35f)
                shapes.line(cx, cy, cx + dx, cy + dy)
                shapes.end()
            }
        }
    }

    private fun drawPowerups() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val pulse = (System.currentTimeMillis() % 800) / 800f
        for (pw in world.activePowerups) {
            val px = pw.position.x; val py = pw.position.y
            val size = 9f + pulse * 3f
            shapes.setColor(1f, 1f, 0f, 0.15f)
            shapes.circle(px, py, size * 2.5f, 12)
            shapes.setColor(1f, 0.8f, 0f, 1f)
            // Diamond
            shapes.triangle(px, py + size, px + size, py, px - size, py)
            shapes.triangle(px, py - size, px + size, py, px - size, py)
        }
        shapes.end()
    }

    private fun drawHUD() {
        val pct = world.territory.conqueredPercent().toInt()
        val timeStr = "%02d:%02d".format(world.timeRemaining.toInt() / 60, world.timeRemaining.toInt() % 60)
        val W = GameConstants.FIELD_WIDTH
        val H = GameConstants.FIELD_HEIGHT
        val hudH = 60f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, 0.88f)
        shapes.rect(0f, H - hudH, W, hudH)
        shapes.end()

        // Separator between stats zone and bar zone
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.22f, 0.22f, 0.22f, 1f)
        shapes.line(0f, H - hudH, W, H - hudH)
        shapes.setColor(0.18f, 0.18f, 0.18f, 1f)
        shapes.line(0f, H - hudH + 14f, W, H - hudH + 14f)
        shapes.end()

        batch.begin()

        // TIME label + value (left)
        Fonts.xs.color = Color(0.792f, 0.784f, 0.667f, 1f)
        Fonts.xs.draw(batch, "TIME", 12f, H - 16f)

        val timeDanger = world.timeRemaining < 30f
        Fonts.lg.color = if (timeDanger) Color(0.792f, 0f, 0.176f, 1f)
                     else Color(0f, 0.859f, 0.914f, 1f)
        Fonts.lg.draw(batch, timeStr, 12f, H - 32f)

        // CLEAR label + value (right)
        Fonts.xs.color = Color(0.792f, 0.784f, 0.667f, 1f)
        val pctStr = "$pct%"
        layout.setText(Fonts.xs, "CLEAR")
        Fonts.xs.draw(batch, "CLEAR", W - layout.width - 12f, H - 16f)

        Fonts.lg.color = Color(0.918f, 0.918f, 0f, 1f)
        layout.setText(Fonts.lg, pctStr)
        Fonts.lg.draw(batch, pctStr, W - layout.width - 12f, H - 32f)

        // Lives (center)
        Fonts.md.color = Color(0.792f, 0f, 0.176f, 1f)
        val livesStr = "♥ ".repeat(world.lives.coerceIn(0, 5)).trimEnd()
        layout.setText(Fonts.md, livesStr)
        Fonts.md.draw(batch, livesStr, (W - layout.width) / 2f, H - 24f)

        // Score (center, below lives — 34f gives ~10px gap above bar)
        Fonts.md.color = Color(0.576f, 0.573f, 0.467f, 1f)
        val scoreStr = "${world.score}"
        layout.setText(Fonts.md, scoreStr)
        Fonts.md.draw(batch, scoreStr, (W - layout.width) / 2f, H - 34f)

        batch.end()

        // Segmented territory bar (16 blocks)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val segments = 16
        val barMargin = 12f
        val barY = H - hudH + 2f
        val barH = 10f
        val totalBarW = W - barMargin * 2f
        val segW = (totalBarW - (segments - 1) * 2f) / segments
        val filledSegs = (pct * segments / 100f).toInt()
        for (s in 0 until segments) {
            val sx = barMargin + s * (segW + 2f)
            val lit = s < filledSegs
            shapes.setColor(
                if (lit) 0.918f else 0.165f,
                if (lit) 0.918f else 0.165f,
                0f, 1f
            )
            shapes.rect(sx, barY, segW, barH)
        }
        shapes.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawLevelCompleteOverlay() {
        val W = GameConstants.FIELD_WIDTH
        val H = GameConstants.FIELD_HEIGHT
        val hudH = GameConstants.HUD_HEIGHT

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val panelW = W * 0.82f
        val panelH = OVERLAY_PANEL_H
        val panelX = (W - panelW) / 2f
        val panelY = (H - hudH - panelH) / 2f

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0.04f, 0.88f)
        shapes.rect(panelX, panelY, panelW, panelH)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 0.85f, 0.85f, 1f)
        shapes.rect(panelX, panelY, panelW, panelH)
        shapes.end()

        batch.begin()

        // Title
        Fonts.xl.color = Color(0f, 1f, 1f, 1f)
        layout.setText(Fonts.xl, "LEVEL CLEAR!")
        Fonts.xl.draw(batch, "LEVEL CLEAR!", (W - layout.width) / 2f, panelY + panelH - 24f)

        // Stars
        for (i in 1..3) {
            val starColor = if (i <= overlayStars) Color(1f, 0.9f, 0f, 1f) else Color(0.3f, 0.3f, 0.3f, 1f)
            Fonts.xl.color = starColor
            layout.setText(Fonts.xl, "★")
            val starX = W / 2f + (i - 2) * 60f - layout.width / 2f
            Fonts.xl.draw(batch, "★", starX, panelY + panelH - 70f)
        }

        // Score
        Fonts.sm.color = Color(0.8f, 0.8f, 0.6f, 1f)
        val scoreLabel = "SCORE: $overlayScore"
        layout.setText(Fonts.sm, scoreLabel)
        Fonts.sm.draw(batch, scoreLabel, (W - layout.width) / 2f, panelY + panelH - 130f)

        // Area cleared
        Fonts.sm.color = Color(0.9f, 0.9f, 0f, 1f)
        val pctLabel = "CLEARED: ${world.territory.conqueredPercent().toInt()}%"
        layout.setText(Fonts.sm, pctLabel)
        Fonts.sm.draw(batch, pctLabel, (W - layout.width) / 2f, panelY + panelH - 165f)

        // Buttons
        val btnW = panelW / 3f - 12f
        val btnY = panelY + 52f
        val btnH = 38f

        Fonts.sm.color = Color(0f, 0.85f, 0.85f, 1f)
        layout.setText(Fonts.sm, "RETRY")
        Fonts.sm.draw(batch, "RETRY", panelX + 10f + (btnW - layout.width) / 2f, btnY + btnH / 2f + 8f)

        Fonts.sm.color = Color(1f, 0.9f, 0f, 1f)
        layout.setText(Fonts.sm, "NEXT")
        Fonts.sm.draw(batch, "NEXT", panelX + btnW + 16f + (btnW - layout.width) / 2f, btnY + btnH / 2f + 8f)

        Fonts.sm.color = Color(0.7f, 0.7f, 0.7f, 1f)
        layout.setText(Fonts.sm, "MENU")
        Fonts.sm.draw(batch, "MENU", panelX + 2 * (btnW + 6f) + 10f + (btnW - layout.width) / 2f, btnY + btnH / 2f + 8f)

        batch.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 0.85f, 0.85f, 1f)
        shapes.rect(panelX + 10f, btnY, btnW, btnH)
        shapes.setColor(1f, 0.9f, 0f, 1f)
        shapes.rect(panelX + btnW + 16f, btnY, btnW, btnH)
        shapes.setColor(0.5f, 0.5f, 0.5f, 1f)
        shapes.rect(panelX + 2 * (btnW + 6f) + 10f, btnY, btnW, btnH)
        shapes.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun handleOverlayNavigation() {
        when {
            retryPressed -> { retryPressed = false; game.setScreen(GameScreen(game, overlayLevelId)) }
            nextPressed  -> { nextPressed = false; game.setScreen(GameScreen(game, (overlayLevelId + 1).coerceAtMost(50))) }
            menuPressed  -> { menuPressed = false; game.setScreen(MenuScreen(game)) }
        }
    }

    override fun resize(width: Int, height: Int) = viewport.update(width, height, true)
    override fun hide() { Gdx.input.inputProcessor = null }
    override fun dispose() {
        batch.dispose()
        shapes.dispose()
        bgTexture?.dispose()
        enemyTexture?.dispose()
    }
}
