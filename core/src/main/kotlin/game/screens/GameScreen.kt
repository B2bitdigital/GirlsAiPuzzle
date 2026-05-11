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
import game.ecs.systems.CellType
import game.ecs.systems.GridPoint
import game.level.LevelLoader
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin

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

    private var levelCompleteOverlay = false
    private var overlayStars = 0
    private var overlayScore = 0
    private var overlayLevelId = 0
    private var retryPressed = false
    private var nextPressed = false
    private var menuPressed = false

    private val OVERLAY_PANEL_H = 340f

    // Celebration reveal phase
    private var celebrationTime = 0f
    private val REVEAL_DURATION = 5f
    private var sparkAccum = 0f
    private data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float,
                              val r: Float, val g: Float, val b: Float, var life: Float)
    private val sparks = mutableListOf<Spark>()
    private val sparkColors = listOf(
        Triple(1f, 0.9f, 0f), Triple(0f, 1f, 1f), Triple(1f, 0.25f, 0.55f),
        Triple(0.3f, 1f, 0.3f), Triple(1f, 1f, 1f), Triple(1f, 0.5f, 0.1f)
    )

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
        camera.update()
        batch.projectionMatrix = camera.combined
        shapes.projectionMatrix = camera.combined

        // Celebration reveal: 5s full-image zoom before stats overlay appears
        if (levelCompleteOverlay && celebrationTime < REVEAL_DURATION) {
            celebrationTime += delta
            spawnSparks(delta)
            updateSparks(delta)
            drawCelebrationPhase()
            if (Gdx.input.justTouched()) celebrationTime = REVEAL_DURATION
            return
        }

        val event = world.update(delta)

        Gdx.gl.glClearColor(0f, 0f, 0.06f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        drawBackground()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        drawFreeOverlay()
        drawTerritoryBorder()
        drawCurrentLine()
        drawPowerups()
        drawEnemies()
        drawExplosions()
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
                    initCelebration()
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
        val cells = world.territory.cells
        val cs = GameConstants.CELL_SIZE
        val ox = GameConstants.FIELD_OFFSET_X
        val oy = GameConstants.FIELD_OFFSET_Y
        val texW = tex.width.toFloat()
        val texH = tex.height.toFloat()
        val cellTexW = texW / GameConstants.GRID_COLS
        val cellTexH = texH / GameConstants.GRID_ROWS
        for (c in 0 until GameConstants.GRID_COLS) {
            for (r in 0 until GameConstants.GRID_ROWS) {
                if (cells[c][r] == CellType.CONQUERED) {
                    val srcX = (c * cellTexW).toInt()
                    val srcY = (texH - (r + 1) * cellTexH).toInt()
                    val srcW = cellTexW.toInt().coerceAtLeast(1)
                    val srcH = cellTexH.toInt().coerceAtLeast(1)
                    batch.draw(tex,
                        ox + c * cs, oy + r * cs, cs, cs,
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
        val cells = world.territory.cells
        val cs = GameConstants.CELL_SIZE
        val ox = GameConstants.FIELD_OFFSET_X
        val oy = GameConstants.FIELD_OFFSET_Y
        for (c in 0 until GameConstants.GRID_COLS) {
            for (r in 0 until GameConstants.GRID_ROWS) {
                // LINE cells are left uncovered so the cyan trail polyline is visible over them
                if (cells[c][r] == CellType.FREE) shapes.rect(ox + c * cs, oy + r * cs, cs, cs)
            }
        }
        shapes.end()
    }

    private fun drawTerritoryBorder() {
        val cells = world.territory.cells
        val cs = GameConstants.CELL_SIZE
        val ox = GameConstants.FIELD_OFFSET_X
        val oy = GameConstants.FIELD_OFFSET_Y
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 0.8f, 0.8f, 0.5f)
        for (c in 0 until GameConstants.GRID_COLS) {
            for (r in 0 until GameConstants.GRID_ROWS) {
                if (cells[c][r] != CellType.CONQUERED) continue
                if (c + 1 < GameConstants.GRID_COLS && cells[c + 1][r] != CellType.CONQUERED)
                    shapes.line(ox + (c + 1) * cs, oy + r * cs, ox + (c + 1) * cs, oy + (r + 1) * cs)
                if (c - 1 >= 0 && cells[c - 1][r] != CellType.CONQUERED)
                    shapes.line(ox + c * cs, oy + r * cs, ox + c * cs, oy + (r + 1) * cs)
                if (r + 1 < GameConstants.GRID_ROWS && cells[c][r + 1] != CellType.CONQUERED)
                    shapes.line(ox + c * cs, oy + (r + 1) * cs, ox + (c + 1) * cs, oy + (r + 1) * cs)
                if (r - 1 >= 0 && cells[c][r - 1] != CellType.CONQUERED)
                    shapes.line(ox + c * cs, oy + r * cs, ox + (c + 1) * cs, oy + r * cs)
            }
        }
        shapes.end()
    }

    private fun drawCurrentLine() {
        val line = world.territory.currentLine
        if (line.size < 2) return
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0f, 0.8f, 0.8f, 0.5f)
        for (i in 0 until line.size - 1) {
            val (x1, y1) = gridPointRenderPos(line[i])
            val (x2, y2) = gridPointRenderPos(line[i + 1])
            shapes.line(x1, y1, x2, y2)
        }
        shapes.end()
    }

    private fun drawEnemies() {
        val tex = enemyTexture
        val offsetX = 0f  // positions already in screen space
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

    private fun drawPlayer() {
        val pos = world.player.position
        val pulse = (System.currentTimeMillis() % 800) / 800f
        val renderPos = playerRenderPos(pos.x, pos.y)
        val cx = renderPos.x
        val cy = renderPos.y

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 0.12f)
        shapes.circle(cx, cy, 8f, 16)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 0.30f)
        shapes.circle(cx, cy, 5f, 16)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 1f)
        shapes.circle(cx, cy, 2.5f + pulse * 1.0f, 12)
        shapes.end()

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

    private fun drawExplosions() {
        val explosions = world.explosionSys.active
        if (explosions.isEmpty()) return

        // Flash phase: filled white circle, progress 0..0.25
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (exp in explosions) {
            if (exp.progress < 0.25f) {
                val t = exp.progress / 0.25f        // 0→1 over flash phase
                val radius = 4f + t * 16f           // 4→20
                val alpha = 0.9f * (1f - t)         // 0.9→0
                shapes.setColor(1f, 1f, 1f, alpha)
                shapes.circle(exp.x, exp.y, radius, 20)
            }
        }
        shapes.end()

        // Ring phase: stroke circle expands, progress 0.15..1.0
        shapes.begin(ShapeRenderer.ShapeType.Line)
        for (exp in explosions) {
            if (exp.progress >= 0.15f) {
                val t = (exp.progress - 0.15f) / 0.85f  // 0→1 over ring phase
                val radius = 8f + t * 32f               // 8→40
                val alpha = 0.8f * (1f - t)             // 0.8→0
                shapes.setColor(exp.r, exp.g, exp.b, alpha)
                shapes.circle(exp.x, exp.y, radius, 24)
            }
        }
        shapes.end()
    }

    private fun drawHUD() {
        val pct = world.territory.conqueredPercent().toInt()
        val timeStr = "%02d:%02d".format(world.timeRemaining.toInt() / 60, world.timeRemaining.toInt() % 60)
        val W = GameConstants.FIELD_WIDTH
        val H = GameConstants.FIELD_HEIGHT
        val hudH = GameConstants.HUD_HEIGHT   // 60f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Background panel
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, 0.88f)
        shapes.rect(0f, H - hudH, W, hudH)
        shapes.end()

        // Separator lines: outer edge + stats/bar divider
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.setColor(0.22f, 0.22f, 0.22f, 1f)
        shapes.line(0f, H - hudH, W, H - hudH)
        shapes.setColor(0.18f, 0.18f, 0.18f, 1f)
        shapes.line(0f, H - hudH + 14f, W, H - hudH + 14f)
        shapes.end()

        // Lives hearts (center, row 1) — drawn with shapes, Orbitron lacks glyph
        val lives = world.lives.coerceIn(0, 5)
        val heartSize = 11f
        val heartGap = 5f
        val totalHeartsW = lives * heartSize + (lives - 1).coerceAtLeast(0) * heartGap
        val heartStartCX = W / 2f - totalHeartsW / 2f + heartSize / 2f
        val heartCY = H - 15f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.85f, 0.08f, 0.18f, 1f)
        for (i in 0 until lives) {
            drawHeart(heartStartCX + i * (heartSize + heartGap), heartCY, heartSize)
        }
        shapes.end()

        // Territory bar (16 segments, bottom of HUD)
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
            shapes.setColor(if (lit) 0.918f else 0.165f, if (lit) 0.918f else 0.165f, 0f, 1f)
            shapes.rect(sx, barY, segW, barH)
        }
        shapes.end()

        batch.begin()

        val labelColor = Color(0.792f, 0.784f, 0.667f, 1f)

        // TIME label (left, row 1)
        Fonts.xs.color = labelColor
        Fonts.xs.draw(batch, "TIME", 12f, H - 4f)

        // TIME value (left, row 2)
        val timeDanger = world.timeRemaining < 30f
        Fonts.sm.color = if (timeDanger) Color(0.85f, 0.08f, 0.18f, 1f)
                         else Color(0f, 0.859f, 0.914f, 1f)
        Fonts.sm.draw(batch, timeStr, 12f, H - 20f)

        // CLEAR label (right, row 1)
        Fonts.xs.color = labelColor
        layout.setText(Fonts.xs, "CLEAR")
        Fonts.xs.draw(batch, "CLEAR", W - layout.width - 12f, H - 4f)

        // CLEAR value (right, row 2)
        val pctStr = "$pct%"
        Fonts.sm.color = Color(0.918f, 0.918f, 0f, 1f)
        layout.setText(Fonts.sm, pctStr)
        Fonts.sm.draw(batch, pctStr, W - layout.width - 12f, H - 20f)

        // Score (center, row 2 — below hearts)
        Fonts.xs.color = Color(0.576f, 0.573f, 0.467f, 1f)
        val scoreStr = "${world.score}"
        layout.setText(Fonts.xs, scoreStr)
        Fonts.xs.draw(batch, scoreStr, (W - layout.width) / 2f, H - 30f)

        batch.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawHeart(cx: Float, cy: Float, size: Float) {
        val r = size / 4f
        // Two bumps at top
        shapes.circle(cx - r, cy + r * 0.5f, r, 12)
        shapes.circle(cx + r, cy + r * 0.5f, r, 12)
        // V-point at bottom
        shapes.triangle(
            cx - r * 2f, cy + r * 0.5f,
            cx + r * 2f, cy + r * 0.5f,
            cx, cy - r * 1.8f
        )
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

        // Stars (★ glyph absent in Orbitron — draw as polygon)
        val starCY = panelY + panelH - 62f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 1..3) {
            val sx = W / 2f + (i - 2) * 60f
            if (i <= overlayStars) {
                shapes.setColor(1f, 0.9f, 0f, 0.18f)
                shapes.circle(sx, starCY, 32f, 22)
                shapes.setColor(1f, 0.9f, 0f, 1f)
            } else {
                shapes.setColor(0.3f, 0.3f, 0.3f, 1f)
            }
            drawStar(sx, starCY, 22f, 8.5f)
        }
        shapes.end()

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

    private fun initCelebration() {
        celebrationTime = 0f
        sparkAccum = 0f
        sparks.clear()
    }

    private fun spawnSparks(delta: Float) {
        sparkAccum += delta
        val interval = 0.025f
        while (sparkAccum >= interval) {
            sparkAccum -= interval
            val c = sparkColors[(Math.random() * sparkColors.size).toInt()]
            sparks.add(Spark(
                x = (GameConstants.FIELD_WIDTH * (0.1f + Math.random() * 0.8f)).toFloat(),
                y = (Math.random() * 80f).toFloat(),
                vx = ((Math.random() - 0.5) * 220f).toFloat(),
                vy = (200f + Math.random() * 380f).toFloat(),
                r = c.first, g = c.second, b = c.third,
                life = (1.5f + Math.random() * 2.2f).toFloat()
            ))
        }
    }

    private fun updateSparks(delta: Float) {
        val gravity = 260f
        sparks.forEach { s -> s.x += s.vx * delta; s.y += s.vy * delta; s.vy -= gravity * delta; s.life -= delta }
        sparks.removeAll { it.life <= 0f || it.y < -20f }
    }

    private fun drawCelebrationPhase() {
        val W = GameConstants.FIELD_WIDTH
        val H = GameConstants.FIELD_HEIGHT
        val t = (celebrationTime / REVEAL_DURATION).coerceIn(0f, 1f)
        val ease = 1f - (1f - t) * (1f - t)

        Gdx.gl.glClearColor(0.04f, 0.04f, 0.04f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Background: starts slightly zoomed, eases to normal scale
        val scale = 1.14f - ease * 0.14f
        val tex = bgTexture
        batch.begin()
        if (tex != null) {
            val sw = W * scale; val sh = H * scale
            batch.setColor(1f, 1f, 1f, 1f)
            batch.draw(tex, (W - sw) / 2f, (H - sh) / 2f, sw, sh)
        }
        batch.end()

        // Sparks
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (s in sparks) {
            val a = (s.life / 2.2f).coerceIn(0f, 1f)
            shapes.setColor(s.r, s.g, s.b, a)
            shapes.rect(s.x - 3f, s.y - 3f, 6f, 6f)
        }
        shapes.end()

        // Dark bands for text legibility
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, 0.6f)
        shapes.rect(0f, H * 0.70f, W, H * 0.30f)
        shapes.setColor(0f, 0f, 0f, 0.45f)
        shapes.rect(0f, 0f, W, H * 0.18f)
        shapes.end()

        // Stars
        if (t > 0.25f) {
            val starAlpha = ((t - 0.25f) / 0.25f).coerceIn(0f, 1f)
            val starPulse = 0.85f + 0.15f * sin(celebrationTime * 3f)
            val starCY = H * 0.83f
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            for (i in 0 until 3) {
                val sx = W / 2f + (i - 1) * 72f
                if (i < overlayStars) {
                    shapes.setColor(0.918f, 0.9f, 0f, 0.18f * starPulse * starAlpha)
                    shapes.circle(sx, starCY, 32f, 22)
                    shapes.setColor(0.918f, 0.85f + starPulse * 0.05f, 0f, starAlpha)
                } else {
                    shapes.setColor(0.22f, 0.22f, 0.22f, starAlpha * 0.6f)
                }
                drawStar(sx, starCY, 17f, 7f)
            }
            shapes.end()
        }

        // "LEVEL CLEAR!" title
        batch.begin()
        Fonts.xl.color = Color(0f, 1f, 1f, ease)
        layout.setText(Fonts.xl, "LEVEL CLEAR!")
        Fonts.xl.draw(batch, "LEVEL CLEAR!", (W - layout.width) / 2f, H * 0.97f)

        // "TAP TO CONTINUE"
        if (t > 0.45f) {
            val skipAlpha = ((t - 0.45f) / 0.3f).coerceIn(0f, 0.75f)
            val blink = 0.6f + 0.4f * sin(celebrationTime * 5f)
            Fonts.xs.color = Color(1f, 1f, 1f, skipAlpha * blink)
            layout.setText(Fonts.xs, "TAP TO CONTINUE")
            Fonts.xs.draw(batch, "TAP TO CONTINUE", (W - layout.width) / 2f, H * 0.07f)
        }
        batch.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawStar(cx: Float, cy: Float, outerR: Float, innerR: Float) {
        for (i in 0 until 5) {
            val a0 = Math.PI * 2 * i / 5 - Math.PI / 2
            val a1 = a0 + Math.PI / 5
            val a2 = a0 + Math.PI * 2 / 5
            val ox0 = cx + (outerR * Math.cos(a0)).toFloat(); val oy0 = cy + (outerR * Math.sin(a0)).toFloat()
            val ix  = cx + (innerR * Math.cos(a1)).toFloat(); val iy  = cy + (innerR * Math.sin(a1)).toFloat()
            val ox2 = cx + (outerR * Math.cos(a2)).toFloat(); val oy2 = cy + (outerR * Math.sin(a2)).toFloat()
            shapes.triangle(cx, cy, ox0, oy0, ix, iy)
            shapes.triangle(cx, cy, ix, iy, ox2, oy2)
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
