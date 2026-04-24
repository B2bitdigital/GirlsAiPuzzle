package game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import game.GameConstants
import game.GirlsPanicGame
import game.ecs.EcsWorld
import game.ecs.EnemyType
import game.ecs.Entity
import game.ecs.WorldEvent
import game.ecs.systems.GridPoint
import game.level.LevelLoader

class GameScreen(
    private val game: GirlsPanicGame,
    private val levelId: Int
) : ScreenAdapter() {

    private lateinit var world: EcsWorld
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private var bgTexture: Texture? = null

    // Enemy colors
    private val colorSpider    = Color(1f, 0.15f, 0.15f, 1f)
    private val colorCockroach = Color(0.7f, 0.45f, 0.1f, 1f)
    private val colorWasp      = Color(1f, 0.9f, 0f, 1f)
    private val colorSnail     = Color(0.2f, 1f, 0.4f, 1f)
    private val colorPlayer    = Color(0f, 1f, 1f, 1f)
    private val colorLine      = Color(1f, 0.55f, 0f, 1f)

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

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val worldCoords = viewport.unproject(com.badlogic.gdx.math.Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
                world.player.playerComp!!.targetX = worldCoords.x.coerceIn(0f, GameConstants.FIELD_WIDTH)
                world.player.playerComp!!.targetY = worldCoords.y.coerceIn(0f, GameConstants.FIELD_HEIGHT)
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
            WorldEvent.GameOver -> game.setScreen(GameOverScreen(game, levelId))
            WorldEvent.LevelComplete -> {
                val stars = world.computeStars()
                game.prefs.saveStars(levelId, stars)
                game.prefs.saveHighScore(world.score)
                game.prefs.incrementLevelsCompleted()
                game.setScreen(LevelCompleteScreen(game, levelId, stars, world.score))
            }
            else -> {}
        }
    }

    private fun drawBackground() {
        val tex = bgTexture ?: return
        batch.begin()
        val grid = world.territory.grid
        val cs = GameConstants.CELL_SIZE
        for (c in 0 until GameConstants.GRID_COLS) {
            for (r in 0 until GameConstants.GRID_ROWS) {
                if (grid[c][r]) {
                    batch.draw(tex,
                        c * cs, r * cs, cs, cs,
                        (c * cs).toInt(), (r * cs).toInt(),
                        cs.toInt(), cs.toInt(),
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
            // Glow
            shapes.setColor(col.r, col.g, col.b, 0.1f)
            shapes.circle(pos.x, pos.y, 22f, 16)
            shapes.setColor(col.r, col.g, col.b, 0.25f)
            shapes.circle(pos.x, pos.y, 14f, 16)
            // Core
            shapes.setColor(col)
            val radius = if (e.type == EnemyType.SNAIL) 6f else 9f
            shapes.circle(pos.x, pos.y, radius, 16)
        }
        shapes.end()

        // Spider legs (Line mode)
        shapes.begin(ShapeRenderer.ShapeType.Line)
        for (eState in world.enemies) {
            val e = eState.entity as Entity.Enemy
            if (e.type != EnemyType.SPIDER) continue
            val pos = eState.position
            shapes.setColor(colorSpider)
            for (i in 0 until 4) {
                val angle = (i * 45f + 22.5f) * Math.PI.toFloat() / 180f
                shapes.line(pos.x, pos.y,
                    pos.x + Math.cos(angle.toDouble()).toFloat() * 14f,
                    pos.y + Math.sin(angle.toDouble()).toFloat() * 14f)
            }
        }
        shapes.end()
    }

    private fun drawPlayer() {
        val pos = world.player.position
        val pulse = (System.currentTimeMillis() % 800) / 800f

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 0.08f)
        shapes.circle(pos.x, pos.y, 28f, 20)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 0.22f)
        shapes.circle(pos.x, pos.y, 16f, 20)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 0.55f)
        shapes.circle(pos.x, pos.y, 10f, 20)
        shapes.setColor(colorPlayer.r, colorPlayer.g, colorPlayer.b, 1f)
        shapes.circle(pos.x, pos.y, 6f + pulse * 1.5f, 20)
        shapes.end()

        // Direction dot
        val pc = world.player.playerComp
        if (pc != null) {
            val tx = pc.targetX; val ty = pc.targetY
            val dx = tx - pos.x; val dy = ty - pos.y
            val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (len > 5f) {
                shapes.begin(ShapeRenderer.ShapeType.Line)
                shapes.setColor(0f, 1f, 1f, 0.35f)
                shapes.line(pos.x, pos.y,
                    pos.x + dx / len * 18f, pos.y + dy / len * 18f)
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

        // HUD bar background
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, 0.6f)
        shapes.rect(0f, H - 40f, W, 40f)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        batch.begin()
        font.data.setScale(1.8f)

        // Lives (hearts)
        font.color = Color(1f, 0.3f, 0.4f, 1f)
        font.draw(batch, "♥ ".repeat(world.lives), 8f, H - 10f)

        // Score
        font.color = Color(1f, 1f, 0.3f, 1f)
        layout.setText(font, "SCORE: ${world.score}")
        font.draw(batch, "SCORE: ${world.score}", (W - layout.width) / 2f, H - 10f)

        // Timer
        val timeColor = if (world.timeRemaining < 30f) Color(1f, 0.3f, 0.3f, 1f) else Color.WHITE
        font.color = timeColor
        layout.setText(font, timeStr)
        font.draw(batch, timeStr, W - layout.width - 8f, H - 10f)

        // Progress bar
        font.data.setScale(1.5f)
        font.color = Color.CYAN
        font.draw(batch, "$pct%", 8f, H - 24f)

        // Progress bar fill
        font.data.setScale(1f)
        batch.end()

        // Progress bar graphic
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val barX = 50f; val barY = H - 38f; val barW = W - 60f; val barH = 6f
        shapes.setColor(0.2f, 0.2f, 0.2f, 0.8f)
        shapes.rect(barX, barY, barW, barH)
        shapes.setColor(0f, 0.9f, 0.9f, 1f)
        shapes.rect(barX, barY, barW * pct / 100f, barH)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun createConqueredMask() = Unit

    override fun resize(width: Int, height: Int) = viewport.update(width, height, true)
    override fun hide() { Gdx.input.inputProcessor = null }
    override fun dispose() {
        batch.dispose()
        shapes.dispose()
        font.dispose()
        bgTexture?.dispose()
    }
}
