package game.ecs

import game.GameConstants
import game.ecs.systems.*
import game.ecs.systems.DifficultySystem
import game.ecs.systems.ExtendResult
import game.ecs.systems.SpawnedEnemy
import game.level.LevelData
import game.persistence.GamePrefs

class EcsWorld(
    val levelData: LevelData,
    val prefs: GamePrefs,
    cols: Int = GameConstants.GRID_COLS,
    rows: Int = GameConstants.GRID_ROWS
) {
    val territory = TerritorySystem(cols, rows)
    val collision = CollisionSystem()
    val movement = MovementSystem()
    val enemyAI = EnemyAISystem()
    val powerupSys = PowerupSystem()
    val difficultySys = DifficultySystem(levelData.id)

    // Entity states
    lateinit var player: EntityState
    val enemies = mutableListOf<EntityState>()
    val activePowerups = mutableListOf<EntityState>()

    var timeRemaining: Float = levelData.timeSeconds.toFloat()
    var lives: Int = GameConstants.LIVES_PER_LEVEL
    var score: Int = 0
    var gameOver: Boolean = false
    var levelComplete: Boolean = false
    
    // Track last safe grid position before entering free area
    private var lastSafeGrid: GridPoint? = null
    // Track last processed grid position to avoid duplicate extendLine calls
    private var lastPlayerGrid: GridPoint? = null
    private var idCounter: Int = 1

    fun init() {
        // Spawn player at top border (last row)
        val pc = PlayerComponent(lives = lives)
        player = EntityState(
            entity = Entity.Player(0),
            position = PositionComponent(GameConstants.PLAY_WIDTH / 2f, GameConstants.PLAY_HEIGHT - GameConstants.CELL_SIZE),
            playerComp = pc
        )

        // Spawn enemies from level config
        var initIdCounter = 1
        for (enemyCfg in levelData.enemies) {
            val type = enemyCfg.toEnemyType()
            repeat(enemyCfg.count) {
                val startX = (initIdCounter * 80f) % (GameConstants.PLAY_WIDTH - 20f) + 10f
                val startY = (GameConstants.PLAY_HEIGHT * 0.5f + initIdCounter * 30f).coerceAtMost(GameConstants.PLAY_HEIGHT - 10f)
                enemies.add(EntityState(
                    entity = Entity.Enemy(initIdCounter, type, enemyCfg.speed),
                    position = PositionComponent(startX, startY),
                    velocity = VelocityComponent(),
                    enemyComp = EnemyComponent(speed = enemyCfg.speed,
                        dirX = if (initIdCounter % 2 == 0) 1f else -1f,
                        dirY = if (type == EnemyType.COCKROACH) 0.5f else 0f)
                ))
                initIdCounter++
            }
        }
    }

    /** Call every frame from GameScreen. Returns true if something changed (life lost / level done). */
    fun update(delta: Float): WorldEvent {
        if (gameOver || levelComplete) {
            return when {
                levelComplete -> WorldEvent.LevelComplete
                gameOver -> WorldEvent.GameOver
                else -> WorldEvent.None
            }
        }
        val delta = delta.coerceAtMost(1f / 20f)

        // Tick player speed boost and shield timers
        player.playerComp?.let { pc ->
            if (pc.speedBoostTimer > 0f) {
                pc.speedBoostTimer -= delta
                if (pc.speedBoostTimer <= 0f) pc.speedMultiplier = 1f
            }
            if (pc.shieldTimer > 0f) pc.shieldTimer -= delta
        }

        // Tick freeze timers
        enemies.forEach { it.enemyComp?.let { ec -> if (ec.freezeTimer > 0f) ec.freezeTimer -= delta } }

        // Move player
        val pc = player.playerComp!!
        val pos = player.position
        val effectiveSpeed = GameConstants.PLAYER_SPEED * pc.speedMultiplier
        val posArr = floatArrayOf(pos.x, pos.y)
        if (pc.moving) {
            val stillMoving = movement.movePlayer(
                posArr, pc.dirX, pc.dirY, effectiveSpeed, delta,
                territory.grid, territory.isPerimeter, GameConstants.GRID_COLS, GameConstants.GRID_ROWS
            )
            pos.x = posArr[0]; pos.y = posArr[1]
            if (!stillMoving) pc.moving = false
        }

        // Detect territory transition
        val playerGrid = movement.toGridPoint(pos.x, pos.y)
        val onSafe = territory.isOnSafeZone(playerGrid)
        val onPerimeter = territory.isOnPerimeter(playerGrid)
        
        // Track last safe position for line start
        if (onSafe) {
            lastSafeGrid = playerGrid
        }
        
        if (!onSafe && !territory.isDrawing) {
            // Start line from last safe position, not current interior position
            val startPt = lastSafeGrid ?: playerGrid
            territory.startLine(startPt)
            lastPlayerGrid = startPt
            // Extend immediately to current position if player moved beyond start point
            if (playerGrid != startPt) {
                territory.extendLine(playerGrid)
                lastPlayerGrid = playerGrid
            }
        } else if (onSafe && territory.isDrawing) {
            val dangerousPositions = enemies
                .filter { (it.entity as? Entity.Enemy)?.type != EnemyType.SNAIL }
                .map { movement.toGridPoint(it.position.x, it.position.y) }
            val snailPositions = enemies
                .filter { (it.entity as? Entity.Enemy)?.type == EnemyType.SNAIL }
                .map { movement.toGridPoint(it.position.x, it.position.y) }

            when (val result = territory.closeLine(dangerousPositions, snailPositions)) {
                is CloseResult.Success -> {
                    val toRespawn = mutableListOf<EntityState>()
                    enemies.removeAll { eState ->
                        val gp = movement.toGridPoint(eState.position.x, eState.position.y)
                        if (gp in result.conqueredCells) { toRespawn.add(eState); true }
                        else false
                    }
                    for (eState in toRespawn) {
                        score += 1000
                        val freeCell = territory.randomFreeCell()
                        if (freeCell != null) {
                            eState.position.x = freeCell.col * GameConstants.CELL_SIZE
                            eState.position.y = freeCell.row * GameConstants.CELL_SIZE
                            enemies.add(eState)
                        }
                    }
                    score += calculateClaimScore(territory.conqueredPercent(), result.snailsTrapped)
                    lastPlayerGrid = null
                    checkLevelComplete()
                }
                else -> {}
            }
        } else if (!onSafe && territory.isDrawing) {
            // Only extend line when player moves to a NEW cell
            if (playerGrid != lastPlayerGrid) {
                if (territory.extendLine(playerGrid) == ExtendResult.CROSSED) {
                    return loseLife()
                }
                lastPlayerGrid = playerGrid
            }
        }

        // Dynamic difficulty: apply speed multiplier and spawn extra enemies
        val speedMult = difficultySys.speedMultiplier()
        val newEnemy = difficultySys.update(delta, enemies.size)
        if (newEnemy != null) {
            enemies.add(EntityState(
                entity = Entity.Enemy(idCounter++, newEnemy.type, newEnemy.speed),
                position = PositionComponent(newEnemy.x, newEnemy.y),
                velocity = VelocityComponent(),
                enemyComp = EnemyComponent(
                    speed = newEnemy.speed,
                    dirX = newEnemy.dirX,
                    dirY = newEnemy.dirY
                )
            ))
        }

        // Move enemies + collision
        for (eState in enemies) {
            val e = eState.entity as Entity.Enemy
            val ePos = eState.position
            val ec = eState.enemyComp!!
            val ePosArr = floatArrayOf(ePos.x, ePos.y)
            val eDirX = floatArrayOf(ec.dirX)
            val eDirY = floatArrayOf(ec.dirY)

            enemyAI.updateEnemy(
                type = e.type, pos = ePosArr, dirX = eDirX, dirY = eDirY,
                speed = ec.speed * speedMult,
                freezeTimer = ec.freezeTimer, delta = delta,
                grid = territory.grid, playerX = pos.x, playerY = pos.y
            )
            ePos.x = ePosArr[0]; ePos.y = ePosArr[1]
            ec.dirX = eDirX[0]; ec.dirY = eDirY[0]

            val shielded = (pc.shieldTimer > 0f)

            // Spider/Wasp: hit player directly SOLO se NON su perimetro
            if (e.type == EnemyType.SPIDER || e.type == EnemyType.WASP) {
                if (!shielded && !territory.isOnPerimeter(playerGrid) && collision.playerHitByEnemy(pos.x, pos.y, ePos.x, ePos.y)) {
                    return loseLife()
                }
            }
            // All dangerous enemies (Spider, Cockroach, Wasp): hit open line → lose life
            if (e.type != EnemyType.SNAIL && territory.isDrawing && !shielded) {
                if (collision.enemyHitsLine(ePos.x, ePos.y, territory.currentLine)) {
                    return loseLife()
                }
            }
        }

        // Powerup tick
        timeRemaining -= delta
        if (timeRemaining <= 0f) return loseLife()

        val timerRef = floatArrayOf(timeRemaining)
        val spawnResult = powerupSys.update(delta, levelData.powerupTypes(), territory.grid)
        if (spawnResult != null) {
            activePowerups.add(EntityState(
                entity = Entity.Powerup(idCounter++, spawnResult.type),
                position = PositionComponent(spawnResult.x, spawnResult.y),
                powerupComp = PowerupComponent(lifetime = GameConstants.POWERUP_LIFETIME)
            ))
        }

        // Powerup collect + lifetime
        val toRemove = mutableListOf<EntityState>()
        for (pwState in activePowerups) {
            val pwPos = pwState.position
            val pwComp = pwState.powerupComp!!
            pwComp.lifetime -= delta
            if (pwComp.lifetime <= 0f) { toRemove.add(pwState); continue }
            if (collision.playerCollectsPowerup(pos.x, pos.y, pwPos.x, pwPos.y)) {
                val type = (pwState.entity as Entity.Powerup).type
                powerupSys.applyEffect(type, timerRef, pc,
                    enemies.mapNotNull { it.enemyComp })
                timeRemaining = timerRef[0]
                toRemove.add(pwState)
                score += 200
            }
        }
        activePowerups.removeAll(toRemove)

        return WorldEvent.None
    }

    private fun loseLife(): WorldEvent {
        lives--
        territory.cancelLine()
        lastSafeGrid = null  // Reset on life lost
        if (lives <= 0) {
            gameOver = true
            return WorldEvent.GameOver
        }
        // Reset player position to top border
        player.position.x = GameConstants.PLAY_WIDTH / 2f
        player.position.y = GameConstants.PLAY_HEIGHT - GameConstants.CELL_SIZE
        player.playerComp!!.moving = false
        return WorldEvent.LifeLost
    }

    private fun calculateClaimScore(pct: Float, snailBonus: Int): Int =
        (pct * 10).toInt() + snailBonus * 500

    private fun checkLevelComplete() {
        val threshold = maxOf(levelData.targetPercent.toFloat(), GameConstants.LEVEL_COMPLETE_THRESHOLD)
        if (territory.conqueredPercent() >= threshold) {
            levelComplete = true
        }
    }

    fun computeStars(): Int = when {
        territory.conqueredPercent() >= GameConstants.STARS_THREE_THRESHOLD && timeRemaining > 0 -> 3
        territory.conqueredPercent() >= GameConstants.STARS_TWO_THRESHOLD -> 2
        else -> 1
    }
}

enum class WorldEvent { None, LifeLost, GameOver, LevelComplete }
