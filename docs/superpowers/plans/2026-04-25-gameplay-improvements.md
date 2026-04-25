# Gameplay Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 9 gameplay improvements: fix play area/image sizing, enemy confinement to free area, always-smallest-region capture, 90% level threshold, dynamic difficulty scaling, frequent powerup spawning, crossing-path death, level-end overlay, full image reveal as prize.

**Architecture:** All changes are within the ECS core (`ecs/`) and `GameScreen.kt`. A new `DifficultySystem` handles dynamic enemy spawning. `TerritorySystem.closeLine()` is reworked to capture the smallest region. `GameScreen` gains an in-screen level-complete overlay replacing the `LevelCompleteScreen` transition.

**Tech Stack:** Kotlin, libGDX, ECS architecture, OrthographicCamera/FitViewport

---

## File Map

| File | Action | What Changes |
|------|--------|-------------|
| `core/.../GameConstants.kt` | Modify | Add `HUD_HEIGHT`, `PLAY_HEIGHT`; change `GRID_ROWS` 80→74 |
| `core/.../ecs/systems/TerritorySystem.kt` | Modify | `closeLine()` always takes smallest region; `extendLine()` returns crossing signal; full-reveal method |
| `core/.../ecs/systems/EnemyAISystem.kt` | Modify | All enemy movement bounces off conquered territory, not just field edges |
| `core/.../ecs/systems/MovementSystem.kt` | Modify | Use `PLAY_HEIGHT` instead of `FIELD_HEIGHT` for player bounds |
| `core/.../ecs/systems/PowerupSystem.kt` | Modify | Spawn interval decreases with elapsed time; spawn in free cells only |
| `core/.../ecs/systems/DifficultySystem.kt` | Create | Tracks elapsed time; triggers extra enemy spawns; computes speed multiplier |
| `core/.../ecs/EcsWorld.kt` | Modify | Wire DifficultySystem; handle crossing death; remove enemies trapped by capture; full reveal on complete |
| `core/.../screens/GameScreen.kt` | Modify | Fix background image fit to play area; level-complete overlay instead of screen transition |
| `android/assets/levels/level_*.json` (all 50) | Modify | `targetPercent` → 90 for all levels |

---

## Task 1: Fix Constants — Play Area vs HUD

**Files:**
- Modify: `core/src/main/kotlin/game/GameConstants.kt`

**Why:** GRID_ROWS=80 means the grid covers 800px, overlapping the 60px HUD at the top. Play area should be 740px (74 rows). FIELD_HEIGHT stays 800 for viewport/HUD positioning. All enemy/movement/powerup systems will use PLAY_HEIGHT.

- [ ] **Step 1: Update GameConstants.kt**

```kotlin
package game

object GameConstants {
    const val FIELD_WIDTH = 480f
    const val FIELD_HEIGHT = 800f       // total viewport height (HUD + play area)
    const val HUD_HEIGHT = 60f          // HUD strip at top
    const val PLAY_HEIGHT = 740f        // FIELD_HEIGHT - HUD_HEIGHT — actual play area
    const val GRID_COLS = 48
    const val GRID_ROWS = 74            // PLAY_HEIGHT / CELL_SIZE = 740 / 10
    const val CELL_SIZE = 10f
    const val PLAYER_SPEED = 150f
    const val LIVES_PER_LEVEL = 3
    const val POWERUP_SPAWN_INTERVAL = 15f
    const val POWERUP_LIFETIME = 8f
    const val STARS_TWO_THRESHOLD = 85
    const val STARS_THREE_THRESHOLD = 95
    const val LEVEL_COMPLETE_THRESHOLD = 90f  // % area revealed to win
}
```

- [ ] **Step 2: Update MovementSystem to use PLAY_HEIGHT for player boundary**

In `core/src/main/kotlin/game/ecs/systems/MovementSystem.kt`, change the constructor default and the `toGridPoint` bound:

```kotlin
class MovementSystem(
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.PLAY_HEIGHT   // was FIELD_HEIGHT
) {
    fun movePlayer(
        pos: FloatArray,
        dirX: Float, dirY: Float,
        speed: Float, delta: Float,
        grid: Array<BooleanArray>,
        cols: Int, rows: Int
    ): Boolean {
        val nextX = pos[0] + dirX * speed * delta
        val nextY = pos[1] + dirY * speed * delta

        if (nextX < 0f || nextX >= cols * this.cellSize || nextY < 0f || nextY >= rows * this.cellSize) {
            return false
        }

        val nextCol = (nextX / this.cellSize).toInt().coerceIn(0, cols - 1)
        val nextRow = (nextY / this.cellSize).toInt().coerceIn(0, rows - 1)

        if (isInteriorConquered(GridPoint(nextCol, nextRow), grid, cols, rows)) {
            return false
        }

        pos[0] = nextX
        pos[1] = nextY
        return true
    }

    private fun isInteriorConquered(
        pt: GridPoint,
        grid: Array<BooleanArray>,
        cols: Int, rows: Int
    ): Boolean {
        if (!grid[pt.col][pt.row]) return false
        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        return dirs.all { (dc, dr) ->
            val nc = pt.col + dc; val nr = pt.row + dr
            nc in 0 until cols && nr in 0 until rows && grid[nc][nr]
        }
    }

    fun toGridPoint(x: Float, y: Float) = GridPoint(
        col = (x / cellSize).toInt().coerceIn(0, (fieldWidth / cellSize).toInt() - 1),
        row = (y / cellSize).toInt().coerceIn(0, (fieldHeight / cellSize).toInt() - 1)
    )
}
```

- [ ] **Step 3: Update EnemyAISystem to use PLAY_HEIGHT**

In `core/src/main/kotlin/game/ecs/systems/EnemyAISystem.kt`, change constructor:

```kotlin
class EnemyAISystem(
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.PLAY_HEIGHT   // was FIELD_HEIGHT
)
```

Also change the coercion at the bottom of `updateEnemy()`:
```kotlin
pos[0] = pos[0].coerceIn(0f, fieldWidth - 1f)
pos[1] = pos[1].coerceIn(0f, fieldHeight - 1f)
```
(Values remain the same, just now fieldHeight = 740.)

- [ ] **Step 4: Update EcsWorld — player spawn Y, enemy spawn Y**

In `EcsWorld.kt` line 36, player spawns at `y=0f` — already correct (bottom of play area). No change needed.

Enemy spawn at line 46: `startY = GameConstants.FIELD_HEIGHT * 0.5f` → Change to use `PLAY_HEIGHT`:

```kotlin
val startY = GameConstants.PLAY_HEIGHT * 0.5f + idCounter * 30f
```

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/game/GameConstants.kt \
        core/src/main/kotlin/game/ecs/systems/MovementSystem.kt \
        core/src/main/kotlin/game/ecs/systems/EnemyAISystem.kt \
        core/src/main/kotlin/game/ecs/EcsWorld.kt
git commit -m "fix: constrain play area to 740px below HUD (GRID_ROWS 80→74)"
```

---

## Task 2: Fix Background Image Rendering

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt`

**Why:** Background renders cell-by-cell but the source texture must map to exactly the play area (480×740). Currently the HUD overlaps the top of the image. With GRID_ROWS=74, the grid now covers exactly y=0..740, and the texture is mapped to that area.

- [ ] **Step 1: Fix `drawBackground()` to scale texture to play area**

Replace the existing `drawBackground()` in `GameScreen.kt` (lines 129-147) with:

```kotlin
private fun drawBackground() {
    val tex = bgTexture ?: return
    batch.begin()
    val grid = world.territory.grid
    val cs = GameConstants.CELL_SIZE
    val texW = tex.width
    val texH = tex.height
    // Map texture proportionally to the play area grid (GRID_COLS x GRID_ROWS = 480x740)
    val cellTexW = texW.toFloat() / GameConstants.GRID_COLS
    val cellTexH = texH.toFloat() / GameConstants.GRID_ROWS
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
```

- [ ] **Step 2: Fix HUD drawing — use PLAY_HEIGHT as the boundary**

In `drawHUD()`, `H` is already `GameConstants.FIELD_HEIGHT = 800f`. The HUD is drawn at `y = H - hudH = 740f` which is exactly `PLAY_HEIGHT`. This is correct — no change needed to HUD position.

- [ ] **Step 3: Fix viewport camera — viewport stays 480×800, play area is 480×740**

Viewport is `FitViewport(GameConstants.FIELD_WIDTH, GameConstants.FIELD_HEIGHT, camera)` = 480×800. This is correct for the full screen including HUD. No change needed here.

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/game/screens/GameScreen.kt
git commit -m "fix: scale background texture to exact play area grid (480x740)"
```

---

## Task 3: Always Capture Smallest Region

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt`

**Why:** Currently, `closeLine()` floods from enemy positions and conquers everything NOT reachable from enemies. This can capture the large region when enemies are in the small one. New rule: always conquer the smallest free region created by the closed line.

`CloseResult.Success` needs to expose `conqueredCells` so `EcsWorld` can remove enemies trapped inside.

- [ ] **Step 1: Update CloseResult sealed class**

At top of `TerritorySystem.kt`, replace:
```kotlin
sealed class CloseResult {
    object Empty : CloseResult()
    data class Success(val snailsTrapped: Int = 0) : CloseResult()
}
```
With:
```kotlin
sealed class CloseResult {
    object Empty : CloseResult()
    data class Success(
        val snailsTrapped: Int = 0,
        val conqueredCells: Set<GridPoint> = emptySet()
    ) : CloseResult()
}
```

- [ ] **Step 2: Rewrite `closeLine()` to always take the smallest region**

Replace the entire `closeLine()` function (lines 65-106):

```kotlin
fun closeLine(
    dangerousEnemies: List<GridPoint>,
    snails: List<GridPoint>
): CloseResult {
    if (_currentLine.size < 2) {
        _currentLine.clear()
        isDrawing = false
        return CloseResult.Empty
    }

    val lineCells = _currentLine.toSet()

    // Mark line cells as conquered (they become the new border)
    lineCells.forEach { grid[it.col][it.row] = true }

    // Find all disjoint free regions created by the closed line
    val regions = findAllFreeRegions()

    if (regions.isEmpty()) {
        _currentLine.clear()
        isDrawing = false
        return CloseResult.Success(conqueredCells = lineCells)
    }

    // Always conquer the smallest region
    val smallest = regions.minByOrNull { it.size }!!

    // Conquer smallest region
    smallest.forEach { grid[it.col][it.row] = true }

    val snailsTrapped = snails.count { it in smallest }

    _currentLine.clear()
    isDrawing = false
    return CloseResult.Success(
        snailsTrapped = snailsTrapped,
        conqueredCells = smallest + lineCells
    )
}

/**
 * Find all disjoint free (not conquered) regions via flood fill.
 * Returns list of sets, each set being one contiguous free region.
 */
private fun findAllFreeRegions(): List<Set<GridPoint>> {
    val visited = mutableSetOf<GridPoint>()
    val regions = mutableListOf<Set<GridPoint>>()

    for (c in 0 until cols) {
        for (r in 0 until rows) {
            val pt = GridPoint(c, r)
            if (!grid[c][r] && pt !in visited) {
                val region = mutableSetOf<GridPoint>()
                floodFillFrom(pt, region)
                // floodFillFrom only visits free cells, so union with visited is safe
                visited.addAll(region)
                regions.add(region)
            }
        }
    }
    return regions
}
```

- [ ] **Step 3: Update `EcsWorld.kt` to remove enemies trapped in conquered region**

In `EcsWorld.kt`, find the `closeLine` call block (lines 110-116) and replace:

```kotlin
when (val result = territory.closeLine(dangerousPositions, snailPositions)) {
    is CloseResult.Success -> {
        // Remove enemies whose position is now inside conquered territory
        enemies.removeAll { eState ->
            val gp = movement.toGridPoint(eState.position.x, eState.position.y)
            gp in result.conqueredCells
        }
        score += calculateClaimScore(territory.conqueredPercent(), result.snailsTrapped)
        checkLevelComplete()
    }
    else -> {}
}
```

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt \
        core/src/main/kotlin/game/ecs/EcsWorld.kt
git commit -m "feat: always capture smallest region when closing territory line"
```

---

## Task 4: Crossing Path Detection — Lose Life

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt`
- Modify: `core/src/main/kotlin/game/ecs/EcsWorld.kt`

**Why:** When the player's trail crosses over a cell that's already in the current line (not backtracking), they lose a life. Currently `extendLine()` silently skips duplicates.

- [ ] **Step 1: Add `ExtendResult` enum and update `extendLine()` in TerritorySystem.kt**

Add after the `CloseResult` sealed class:

```kotlin
enum class ExtendResult { OK, CROSSED }
```

Replace `extendLine()` (lines 54-56):

```kotlin
fun extendLine(pt: GridPoint): ExtendResult {
    if (_currentLine.isEmpty()) return ExtendResult.OK
    // Backtrack: stepping back onto the previous cell is allowed
    if (_currentLine.size >= 2 && pt == _currentLine[_currentLine.size - 2]) {
        _currentLine.removeAt(_currentLine.size - 1)
        return ExtendResult.OK
    }
    // Crossing: cell already in line (not the immediately previous one)
    if (pt in _currentLine) return ExtendResult.CROSSED
    _currentLine.add(pt)
    return ExtendResult.OK
}
```

- [ ] **Step 2: Handle crossing result in EcsWorld.kt**

In `EcsWorld.kt`, find the `extendLine` call (line 118):

```kotlin
} else if (!onSafe && territory.isDrawing) {
    territory.extendLine(playerGrid)
}
```

Replace with:

```kotlin
} else if (!onSafe && territory.isDrawing) {
    if (territory.extendLine(playerGrid) == ExtendResult.CROSSED) {
        return loseLife()
    }
}
```

Add import at top of EcsWorld.kt:
```kotlin
import game.ecs.systems.ExtendResult
```

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt \
        core/src/main/kotlin/game/ecs/EcsWorld.kt
git commit -m "feat: lose life when player trail crosses itself"
```

---

## Task 5: Enemy Confinement to Free Area

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/systems/EnemyAISystem.kt`

**Why:** Enemies currently bounce only off field edges. They should also bounce when they would enter a conquered (revealed) cell. As more territory is conquered, the free area shrinks and enemies are confined to it.

- [ ] **Step 1: Add helper to check if a pixel position is in a conquered cell**

Add private method to `EnemyAISystem`:

```kotlin
private fun isConquered(x: Float, y: Float, grid: Array<BooleanArray>): Boolean {
    val col = (x / cellSize).toInt().coerceIn(0, grid.size - 1)
    val row = (y / cellSize).toInt().coerceIn(0, grid[0].size - 1)
    return grid[col][row]
}
```

- [ ] **Step 2: Rewrite `updateEnemy()` to bounce off conquered territory**

Replace the full `updateEnemy()` function:

```kotlin
fun updateEnemy(
    type: EnemyType,
    pos: FloatArray,
    dirX: FloatArray,
    dirY: FloatArray,
    speed: Float,
    freezeTimer: Float,
    delta: Float,
    grid: Array<BooleanArray>,
    playerX: Float,
    playerY: Float
) {
    if (freezeTimer > 0f) return

    when (type) {
        EnemyType.SPIDER    -> moveSpider(pos, dirX, dirY, speed, delta, grid)
        EnemyType.COCKROACH -> moveBouncer(pos, dirX, dirY, speed, delta, grid, randomTurn = true)
        EnemyType.WASP      -> moveWasp(pos, dirX, dirY, speed, delta, playerX, playerY, grid)
        EnemyType.SNAIL     -> moveBouncer(pos, dirX, dirY, speed * 0.3f, delta, grid, randomTurn = false)
    }

    pos[0] = pos[0].coerceIn(0f, fieldWidth - 1f)
    pos[1] = pos[1].coerceIn(0f, fieldHeight - 1f)
}
```

- [ ] **Step 3: Rewrite each movement method to check conquered cells**

Replace all private movement methods with:

```kotlin
private fun moveSpider(
    pos: FloatArray, dirX: FloatArray, dirY: FloatArray,
    speed: Float, delta: Float, grid: Array<BooleanArray>
) {
    val nx = pos[0] + dirX[0] * speed * delta
    val ny = pos[1] + dirY[0] * speed * delta

    val hitWallX = nx <= 0f || nx >= fieldWidth - 1f || isConquered(nx, pos[1], grid)
    val hitWallY = ny <= 0f || ny >= fieldHeight - 1f || isConquered(pos[0], ny, grid)

    if (hitWallX) {
        dirX[0] = -dirX[0]
        if (dirY[0] == 0f && Random.nextFloat() > 0.5f) dirY[0] = if (Random.nextBoolean()) 1f else -1f
    } else {
        pos[0] = nx
    }
    if (hitWallY) {
        dirY[0] = -dirY[0]
    } else {
        pos[1] = ny
    }
}

private fun moveBouncer(
    pos: FloatArray, dirX: FloatArray, dirY: FloatArray,
    speed: Float, delta: Float, grid: Array<BooleanArray>,
    randomTurn: Boolean
) {
    val nx = pos[0] + dirX[0] * speed * delta
    val ny = pos[1] + dirY[0] * speed * delta

    val hitWallX = nx <= cellSize || nx >= fieldWidth - cellSize || isConquered(nx, pos[1], grid)
    val hitWallY = ny <= cellSize || ny >= fieldHeight - cellSize || isConquered(pos[0], ny, grid)

    if (hitWallX) dirX[0] = -dirX[0] else pos[0] = nx
    if (hitWallY) dirY[0] = -dirY[0] else pos[1] = ny

    if (randomTurn && Random.nextFloat() < 0.01f) {
        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
        dirX[0] = kotlin.math.cos(angle)
        dirY[0] = kotlin.math.sin(angle)
    }
}

private fun moveWasp(
    pos: FloatArray, dirX: FloatArray, dirY: FloatArray,
    speed: Float, delta: Float,
    playerX: Float, playerY: Float,
    grid: Array<BooleanArray>
) {
    val dx = playerX - pos[0]
    val dy = playerY - pos[1]
    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val nx = pos[0] + (dx / len) * speed * delta
    val ny = pos[1] + (dy / len) * speed * delta

    // If heading into conquered territory, reverse last direction
    if (!isConquered(nx, pos[1], grid) && nx > 0f && nx < fieldWidth - 1f) {
        pos[0] = nx
        dirX[0] = dx / len
    } else {
        dirX[0] = -dirX[0]
    }
    if (!isConquered(pos[0], ny, grid) && ny > 0f && ny < fieldHeight - 1f) {
        pos[1] = ny
        dirY[0] = dy / len
    } else {
        dirY[0] = -dirY[0]
    }
}

private fun isConquered(x: Float, y: Float, grid: Array<BooleanArray>): Boolean {
    val col = (x / cellSize).toInt().coerceIn(0, grid.size - 1)
    val row = (y / cellSize).toInt().coerceIn(0, grid[0].size - 1)
    return grid[col][row]
}
```

Remove old private methods: `moveSpider` (old), `moveCockroach`, `moveWasp` (old), `moveSnail`.

- [ ] **Step 4: Fix powerup spawn position — spawn in free cells only**

In `PowerupSystem.kt`, replace `update()`:

```kotlin
fun update(delta: Float, availableTypes: List<PowerupType>, grid: Array<BooleanArray>? = null): SpawnedPowerup? {
    spawnTimer += delta
    if (spawnTimer < currentInterval() || availableTypes.isEmpty()) return null
    spawnTimer = 0f

    val type = availableTypes.random()
    val margin = cellSize * 2
    // Try up to 20 times to find a free cell to spawn in
    repeat(20) {
        val x = Random.nextFloat() * (fieldWidth - margin * 2) + margin
        val y = Random.nextFloat() * (fieldHeight - margin * 2) + margin
        if (grid == null || !isConquered(x, y, grid)) {
            return SpawnedPowerup(type, x, y)
        }
    }
    return null  // No free spot found this tick
}

private fun isConquered(x: Float, y: Float, grid: Array<BooleanArray>): Boolean {
    val col = (x / cellSize).toInt().coerceIn(0, grid.size - 1)
    val row = (y / cellSize).toInt().coerceIn(0, grid[0].size - 1)
    return grid[col][row]
}
```

Note: `currentInterval()` is added in Task 7. For now it returns `spawnInterval`.

- [ ] **Step 5: Update EcsWorld.kt to pass grid to powerupSys.update()**

In `EcsWorld.kt`, find line:
```kotlin
val spawnResult = powerupSys.update(delta, levelData.powerupTypes())
```
Replace with:
```kotlin
val spawnResult = powerupSys.update(delta, levelData.powerupTypes(), territory.grid)
```

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/game/ecs/systems/EnemyAISystem.kt \
        core/src/main/kotlin/game/ecs/systems/PowerupSystem.kt \
        core/src/main/kotlin/game/ecs/EcsWorld.kt
git commit -m "feat: confine enemies and powerups to free (unconquered) area"
```

---

## Task 6: Dynamic Difficulty System

**Files:**
- Create: `core/src/main/kotlin/game/ecs/systems/DifficultySystem.kt`
- Modify: `core/src/main/kotlin/game/ecs/EcsWorld.kt`

**Why:** Enemy count and speed should scale with elapsed time and level number. Every 30 seconds, spawn an extra enemy. Base speed scales with both time and level. Max enemies capped at 12.

- [ ] **Step 1: Create DifficultySystem.kt**

```kotlin
package game.ecs.systems

import game.ecs.EnemyType
import kotlin.math.min
import kotlin.random.Random

data class SpawnedEnemy(
    val type: EnemyType,
    val x: Float,
    val y: Float,
    val speed: Float,
    val dirX: Float,
    val dirY: Float
)

class DifficultySystem(
    private val levelId: Int,
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.PLAY_HEIGHT
) {
    private var elapsedTime = 0f
    private var lastSpawnTime = 0f
    private val spawnInterval = 30f    // extra enemy every 30 seconds
    private val maxExtraEnemies = 8    // cap on dynamically spawned enemies
    private var extraSpawned = 0

    /** Returns speed multiplier for existing enemies. Increases every 20s, capped at 2.5x. */
    fun speedMultiplier(): Float {
        val timeBonus = (elapsedTime / 20f) * 0.1f
        val levelBonus = (levelId - 1) * 0.05f
        return (1f + timeBonus + levelBonus).coerceAtMost(2.5f)
    }

    /**
     * Call each frame. Returns a new enemy to spawn, or null.
     * [currentEnemyCount] = total enemies alive now.
     */
    fun update(delta: Float, currentEnemyCount: Int): SpawnedEnemy? {
        elapsedTime += delta

        if (extraSpawned >= maxExtraEnemies) return null
        if (currentEnemyCount >= 12) return null
        if (elapsedTime - lastSpawnTime < spawnInterval) return null

        lastSpawnTime = elapsedTime
        extraSpawned++

        val type = when {
            levelId <= 10 -> if (Random.nextBoolean()) EnemyType.COCKROACH else EnemyType.SPIDER
            levelId <= 25 -> listOf(EnemyType.COCKROACH, EnemyType.SPIDER, EnemyType.WASP).random()
            else          -> listOf(EnemyType.COCKROACH, EnemyType.WASP, EnemyType.SPIDER, EnemyType.SNAIL).random()
        }
        val baseSpeed = 60f + levelId * 3f + (elapsedTime / 60f) * 10f
        val x = Random.nextFloat() * (fieldWidth * 0.6f) + fieldWidth * 0.2f
        val y = Random.nextFloat() * (fieldHeight * 0.6f) + fieldHeight * 0.2f
        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
        return SpawnedEnemy(
            type = type, x = x, y = y,
            speed = baseSpeed.coerceAtMost(220f),
            dirX = kotlin.math.cos(angle),
            dirY = kotlin.math.sin(angle)
        )
    }

    fun elapsedSeconds() = elapsedTime
}
```

- [ ] **Step 2: Add DifficultySystem to EcsWorld and wire update loop**

In `EcsWorld.kt`, add field after `val powerupSys`:
```kotlin
val difficultySys = DifficultySystem(levelData.id)
```

Add import:
```kotlin
import game.ecs.systems.DifficultySystem
import game.ecs.systems.SpawnedEnemy
```

In the `update()` function, just before the enemies loop (around line 121), add:

```kotlin
// Apply dynamic speed multiplier to all enemies
val speedMult = difficultySys.speedMultiplier()
// (speed multiplier applied inline when calling updateEnemy — see below)

// Spawn extra enemies if difficulty warrants
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
```

In the enemies loop where `updateEnemy` is called, scale the speed:
```kotlin
enemyAI.updateEnemy(
    type = e.type, pos = ePosArr, dirX = eDirX, dirY = eDirY,
    speed = ec.speed * speedMult,   // apply difficulty multiplier
    freezeTimer = ec.freezeTimer, delta = delta,
    grid = territory.grid, playerX = pos.x, playerY = pos.y
)
```

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/game/ecs/systems/DifficultySystem.kt \
        core/src/main/kotlin/game/ecs/EcsWorld.kt
git commit -m "feat: dynamic difficulty — enemy count and speed scale with time and level"
```

---

## Task 7: Dynamic Powerup Spawn Interval

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/systems/PowerupSystem.kt`
- Modify: `core/src/main/kotlin/game/ecs/EcsWorld.kt`

**Why:** Powerups should spawn more frequently as time passes. Starting at 15s, decreasing by 1s every 30 seconds of gameplay, floored at 5s.

- [ ] **Step 1: Add dynamic interval to PowerupSystem**

In `PowerupSystem.kt`, add field and method:

```kotlin
private var elapsedTime = 0f

private fun currentInterval(): Float {
    val reduction = (elapsedTime / 30f).toInt() * 1f
    return (spawnInterval - reduction).coerceAtLeast(5f)
}
```

Update `update()` to tick elapsed time:

```kotlin
fun update(delta: Float, availableTypes: List<PowerupType>, grid: Array<BooleanArray>? = null): SpawnedPowerup? {
    elapsedTime += delta
    spawnTimer += delta
    if (spawnTimer < currentInterval() || availableTypes.isEmpty()) return null
    spawnTimer = 0f

    val type = availableTypes.random()
    val margin = cellSize * 2
    repeat(20) {
        val x = Random.nextFloat() * (fieldWidth - margin * 2) + margin
        val y = Random.nextFloat() * (fieldHeight - margin * 2) + margin
        if (grid == null || !isConquered(x, y, grid)) {
            return SpawnedPowerup(type, x, y)
        }
    }
    return null
}
```

- [ ] **Step 2: Commit**

```bash
git add core/src/main/kotlin/game/ecs/systems/PowerupSystem.kt
git commit -m "feat: powerup spawn interval decreases with time (15s → min 5s)"
```

---

## Task 8: Update Level JSON Files — 90% Threshold

**Files:**
- Modify: all 50 `android/assets/levels/level_*.json` files

**Why:** All levels currently use 75% or 80% target. New requirement is 90%.

- [ ] **Step 1: Bulk update all level JSONs**

Run this command to update all 50 files at once:

```bash
for f in android/assets/levels/level_*.json; do
    sed -i 's/"targetPercent": [0-9]*/"targetPercent": 90/' "$f"
done
```

- [ ] **Step 2: Verify sample files**

```bash
grep "targetPercent" android/assets/levels/level_01.json android/assets/levels/level_25.json android/assets/levels/level_50.json
```

Expected output:
```
android/assets/levels/level_01.json:  "targetPercent": 90,
android/assets/levels/level_25.json:  "targetPercent": 90,
android/assets/levels/level_50.json:  "targetPercent": 90,
```

- [ ] **Step 3: Also update `EcsWorld.checkLevelComplete()` to use the constant as a floor**

In `EcsWorld.kt`, replace `checkLevelComplete()`:

```kotlin
private fun checkLevelComplete() {
    val threshold = maxOf(levelData.targetPercent.toFloat(), GameConstants.LEVEL_COMPLETE_THRESHOLD)
    if (territory.conqueredPercent() >= threshold) {
        levelComplete = true
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add android/assets/levels/ core/src/main/kotlin/game/ecs/EcsWorld.kt
git commit -m "feat: raise level completion threshold to 90%"
```

---

## Task 9: Level Complete Overlay on GameScreen

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt`

**Why:** Instead of transitioning to `LevelCompleteScreen`, show the level-end data as an overlay directly on the frozen game screen. The player sees the revealed image behind the overlay as their "prize".

- [ ] **Step 1: Add overlay state fields to GameScreen**

After the `colorLine` field (around line 48), add:

```kotlin
private var levelCompleteOverlay = false
private var overlayStars = 0
private var overlayScore = 0
private var overlayLevelId = 0
// Touch state for overlay buttons
private var retryPressed = false
private var nextPressed = false
private var menuPressed = false
```

- [ ] **Step 2: Replace LevelComplete transition with overlay trigger**

In `render()` (lines 100-110), replace the `WorldEvent.LevelComplete` branch:

```kotlin
when (event) {
    WorldEvent.GameOver -> game.setScreen(GameOverScreen(game, levelId, world.score))
    WorldEvent.LevelComplete -> {
        if (!levelCompleteOverlay) {
            overlayStars = world.computeStars()
            overlayScore = world.score
            overlayLevelId = levelId
            game.prefs.saveStars(levelId, overlayStars)
            game.prefs.saveHighScore(world.score)
            game.prefs.incrementLevelsCompleted()
            levelCompleteOverlay = true
            // Reveal full background as prize
            world.territory.revealAll()
        }
    }
    else -> {}
}

if (levelCompleteOverlay) {
    drawLevelCompleteOverlay()
    handleOverlayNavigation()
}
```

- [ ] **Step 3: Add `revealAll()` to TerritorySystem**

In `TerritorySystem.kt`, add after `cancelLine()`:

```kotlin
fun revealAll() {
    for (c in 0 until cols) for (r in 0 until rows) grid[c][r] = true
    _currentLine.clear()
    isDrawing = false
}
```

- [ ] **Step 4: Implement `drawLevelCompleteOverlay()` in GameScreen**

Add method to `GameScreen`:

```kotlin
private fun drawLevelCompleteOverlay() {
    val W = GameConstants.FIELD_WIDTH
    val H = GameConstants.FIELD_HEIGHT
    val hudH = GameConstants.HUD_HEIGHT

    Gdx.gl.glEnable(GL20.GL_BLEND)
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

    // Semi-transparent dark panel in center of play area
    val panelW = W * 0.82f
    val panelH = 340f
    val panelX = (W - panelW) / 2f
    val panelY = (H - hudH - panelH) / 2f

    shapes.begin(ShapeRenderer.ShapeType.Filled)
    shapes.setColor(0f, 0f, 0.04f, 0.88f)
    shapes.rect(panelX, panelY, panelW, panelH)
    shapes.setColor(0f, 0.85f, 0.85f, 1f)
    // Border lines
    shapes.rect(panelX, panelY + panelH - 2f, panelW, 2f)
    shapes.rect(panelX, panelY, panelW, 2f)
    shapes.rect(panelX, panelY, 2f, panelH)
    shapes.rect(panelX + panelW - 2f, panelY, 2f, panelH)
    shapes.end()

    batch.begin()

    // Title
    font.data.setScale(2.4f)
    font.color = Color(0f, 1f, 1f, 1f)
    layout.setText(font, "LEVEL CLEAR!")
    font.draw(batch, "LEVEL CLEAR!", (W - layout.width) / 2f, panelY + panelH - 24f)

    // Stars
    font.data.setScale(2.8f)
    for (i in 1..3) {
        val starColor = if (i <= overlayStars) Color(1f, 0.9f, 0f, 1f) else Color(0.3f, 0.3f, 0.3f, 1f)
        font.color = starColor
        layout.setText(font, "★")
        val starX = W / 2f + (i - 2) * 60f - layout.width / 2f
        font.draw(batch, "★", starX, panelY + panelH - 70f)
    }

    // Score
    font.data.setScale(1.2f)
    font.color = Color(0.8f, 0.8f, 0.6f, 1f)
    val scoreLabel = "SCORE: $overlayScore"
    layout.setText(font, scoreLabel)
    font.draw(batch, scoreLabel, (W - layout.width) / 2f, panelY + panelH - 130f)

    // Area cleared
    font.data.setScale(1.2f)
    font.color = Color(0.9f, 0.9f, 0f, 1f)
    val pctLabel = "CLEARED: ${world.territory.conqueredPercent().toInt()}%"
    layout.setText(font, pctLabel)
    font.draw(batch, pctLabel, (W - layout.width) / 2f, panelY + panelH - 165f)

    // Button labels
    val btnY = panelY + 52f
    val btnH = 38f
    val btnW = panelW / 3f - 12f

    // RETRY button
    font.data.setScale(1.1f)
    font.color = Color(0f, 0.85f, 0.85f, 1f)
    layout.setText(font, "RETRY")
    font.draw(batch, "RETRY", panelX + 10f + (btnW - layout.width) / 2f, btnY + btnH / 2f + 8f)

    // NEXT button
    font.color = Color(1f, 0.9f, 0f, 1f)
    layout.setText(font, "NEXT")
    font.draw(batch, "NEXT", panelX + btnW + 16f + (btnW - layout.width) / 2f, btnY + btnH / 2f + 8f)

    // MENU button
    font.color = Color(0.7f, 0.7f, 0.7f, 1f)
    layout.setText(font, "MENU")
    font.draw(batch, "MENU", panelX + 2 * (btnW + 6f) + 10f + (btnW - layout.width) / 2f, btnY + btnH / 2f + 8f)

    font.data.setScale(1f)
    batch.end()

    // Button outlines
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
```

- [ ] **Step 5: Implement `handleOverlayNavigation()` in GameScreen**

Store button bounds and handle touch:

```kotlin
private fun handleOverlayNavigation() {
    if (!retryPressed && !nextPressed && !menuPressed) return
    when {
        retryPressed -> game.setScreen(GameScreen(game, overlayLevelId))
        nextPressed  -> game.setScreen(GameScreen(game, (overlayLevelId + 1).coerceAtMost(50)))
        menuPressed  -> game.setScreen(MenuScreen(game))
    }
}
```

- [ ] **Step 6: Update touch input in `show()` to detect overlay button taps**

After the `applyDirection` calls in the `InputAdapter`, detect overlay taps. Add to the `touchDown` handler:

```kotlin
override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (levelCompleteOverlay) {
        val worldCoords = viewport.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
        val W = GameConstants.FIELD_WIDTH
        val panelW = W * 0.82f
        val panelX = (W - panelW) / 2f
        val btnW = panelW / 3f - 12f
        val btnY = (GameConstants.FIELD_HEIGHT - GameConstants.HUD_HEIGHT - 340f) / 2f + 52f
        val btnH = 38f
        val wx = worldCoords.x; val wy = worldCoords.y
        retryPressed = wx in panelX + 10f..(panelX + 10f + btnW) && wy in btnY..(btnY + btnH)
        nextPressed  = wx in (panelX + btnW + 16f)..(panelX + 2 * btnW + 16f) && wy in btnY..(btnY + btnH)
        menuPressed  = wx in (panelX + 2 * (btnW + 6f) + 10f)..(panelX + 3 * btnW + 18f) && wy in btnY..(btnY + btnH)
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
```

- [ ] **Step 7: Commit**

```bash
git add core/src/main/kotlin/game/screens/GameScreen.kt \
        core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt
git commit -m "feat: show level-complete overlay on game screen with full image reveal"
```

---

## Self-Review

### Spec Coverage

| Requirement | Task | Status |
|-------------|------|--------|
| Perimeter shrinks for enemies | Task 5 | ✓ enemies bounce off conquered cells |
| Image fits exactly play area | Task 1 + 2 | ✓ GRID_ROWS=74, texture mapped to play area |
| Always capture smallest region | Task 3 | ✓ findAllFreeRegions + minByOrNull |
| Level threshold 90% | Task 8 | ✓ all JSONs + constant floor |
| More enemies, increasing speed | Task 6 | ✓ DifficultySystem |
| More frequent bonus spawning | Task 7 | ✓ decreasing interval |
| Elements move in free area only | Task 5 | ✓ enemy confinement + powerup free-cell spawn |
| No crossing paths → lose life | Task 4 | ✓ ExtendResult.CROSSED → loseLife() |
| Level end on game screen | Task 9 | ✓ overlay drawn on GameScreen |
| Background fully revealed as prize | Task 9 step 2+3 | ✓ revealAll() called on complete |

### Placeholder Scan

No TBD, TODO, or "implement later" found. All methods fully coded.

### Type Consistency

- `ExtendResult` defined in Task 4 and imported in EcsWorld — ✓
- `CloseResult.Success.conqueredCells: Set<GridPoint>` defined Task 3 step 1, used Task 3 step 3 — ✓
- `SpawnedEnemy` defined Task 6 step 1, used Task 6 step 2 — ✓
- `revealAll()` defined Task 9 step 3 (TerritorySystem), called Task 9 step 2 (GameScreen) — ✓
- `DifficultySystem` constructor takes `levelId: Int` — EcsWorld passes `levelData.id` which is `Int` — ✓
- `PowerupSystem.update()` gains `grid` param Task 5 step 4 — EcsWorld call updated Task 5 step 5 — ✓
- `currentInterval()` added Task 7 step 1 — referenced in `update()` which was introduced Task 5 step 4 — must ensure Task 5 step 4 code already calls `currentInterval()` or uses `spawnInterval` until Task 7 is done. Since tasks run in order, Task 5 adds `update()` with `currentInterval()` call, but `currentInterval()` is defined in Task 7. **Fix:** In Task 5 step 4, use `spawnInterval` directly. In Task 7 step 1, replace it with `currentInterval()`. Task 5 step 4 code already written with `currentInterval()` — change it to use `spawnInterval` as placeholder:

**Correction to Task 5 step 4** — in the `update()` body use `spawnInterval` instead of `currentInterval()`:
```kotlin
if (spawnTimer < spawnInterval || availableTypes.isEmpty()) return null
```
Then Task 7 step 1 adds `elapsedTime` and `currentInterval()` and replaces that check.

### Gap Check

- Enemies that START inside a to-be-conquered region on game init: Task 1 sets spawn Y to `PLAY_HEIGHT * 0.5f` inside free area. Initial conquered cells are only the border perimeter. Spawn positions are at center of field which is free. ✓
- `drawFreeOverlay()` in GameScreen: still loops over GRID_ROWS (now 74) — ✓ uses `GameConstants.GRID_ROWS`
- `drawTerritoryBorder()`: same — uses `GameConstants.GRID_COLS / GRID_ROWS` — ✓
- When overlay is shown, `world.update()` is still called each frame. We should freeze it once level is complete. In `EcsWorld.update()`, the early return already handles `levelComplete = true` returning `WorldEvent.LevelComplete` every frame without further updates — ✓ (line 62-68 in original).
