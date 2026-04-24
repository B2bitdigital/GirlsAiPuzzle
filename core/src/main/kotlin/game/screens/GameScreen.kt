package game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import game.GameConstants
import game.GirlsPanicGame
import game.ecs.EcsWorld
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

    private var bgTexture: Texture? = null
    private val conqueredPixmap by lazy { createConqueredMask() }

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

        Gdx.gl.glClearColor(0f, 0f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined
        shapes.projectionMatrix = camera.combined

        // Draw background only on conquered cells
        drawBackground()
        // Draw overlay on free cells
        drawFreeOverlay()
        // Draw current line
        drawCurrentLine()
        // Draw enemies
        drawEnemies()
        // Draw powerups
        drawPowerups()
        // Draw HUD
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
        shapes.setColor(0f, 0f, 0f, 0.85f)
        val grid = world.territory.grid
        val cs = GameConstants.CELL_SIZE
        for (c in 0 until GameConstants.GRID_COLS) {
            for (r in 0 until GameConstants.GRID_ROWS) {
                if (!grid[c][r]) shapes.rect(c * cs, r * cs, cs, cs)
            }
        }
        shapes.end()
    }

    private fun drawCurrentLine() {
        if (world.territory.currentLine.isEmpty()) return
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val pulse = (System.currentTimeMillis() % 500) / 500f
        shapes.setColor(1f, 0.6f + pulse * 0.4f, 0f, 1f)
        val cs = GameConstants.CELL_SIZE
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
            shapes.color = when (e.type) {
                game.ecs.EnemyType.SPIDER    -> Color.RED
                game.ecs.EnemyType.COCKROACH -> Color.BROWN
                game.ecs.EnemyType.WASP      -> Color.YELLOW
                game.ecs.EnemyType.SNAIL     -> Color.GREEN
            }
            shapes.circle(pos.x, pos.y, 8f)
        }
        // Player
        shapes.setColor(0f, 1f, 1f, 1f)
        shapes.circle(world.player.position.x, world.player.position.y, 6f)
        shapes.end()
    }

    private fun drawPowerups() {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (pw in world.activePowerups) {
            shapes.setColor(1f, 1f, 0f, 1f)
            shapes.rect(pw.position.x - 8f, pw.position.y - 8f, 16f, 16f)
        }
        shapes.end()
    }

    private fun drawHUD() {
        batch.begin()
        val pct = world.territory.conqueredPercent().toInt()
        val timeStr = "%02d:%02d".format(world.timeRemaining.toInt() / 60, world.timeRemaining.toInt() % 60)
        font.draw(batch, "❤ ".repeat(world.lives), 10f, GameConstants.FIELD_HEIGHT - 10f)
        font.draw(batch, "SCORE: ${world.score}", GameConstants.FIELD_WIDTH / 2f - 60f, GameConstants.FIELD_HEIGHT - 10f)
        font.draw(batch, timeStr, GameConstants.FIELD_WIDTH - 70f, GameConstants.FIELD_HEIGHT - 10f)
        font.draw(batch, "$pct%", 10f, GameConstants.FIELD_HEIGHT - 30f)
        batch.end()
    }

    private fun createConqueredMask() = Unit // placeholder; rendering uses per-cell drawing above

    override fun resize(width: Int, height: Int) = viewport.update(width, height, true)

    override fun hide() { Gdx.input.inputProcessor = null }

    override fun dispose() {
        batch.dispose()
        shapes.dispose()
        font.dispose()
        bgTexture?.dispose()
    }
}
