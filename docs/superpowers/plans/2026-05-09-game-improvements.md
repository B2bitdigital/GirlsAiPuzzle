# Game Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 6 gameplay bugs and improvements: 5px cell grid, glowing dot cursor, current-border respawn, no CONQUERED-zone spawning, and flash+ring explosion effect when territory captures enemies/powerups.

**Architecture:** Incremental changes to existing files plus 2 new pure-Kotlin systems (`BorderFinder`, `ExplosionSystem`) with no LibGDX dependencies. `EcsWorld` wires new systems; `GameScreen` renders them. `ExplosionSystem` uses float r/g/b (not `Color`) so it stays testable without a GDX backend.

**Tech Stack:** Kotlin, LibGDX (ShapeRenderer, GL20 for rendering), JUnit4, Gradle.

---

### Task 1: Update GameConstants + fix tests

**Files:**
- Modify: `core/src/main/kotlin/game/GameConstants.kt`
- Modify: `core/src/test/kotlin/game/GameConstantsTest.kt`

- [ ] **Step 1: Update GameConstants**

Replace the entire file:

```kotlin
package game

object GameConstants {
    const val FIELD_WIDTH = 480f
    const val FIELD_HEIGHT = 800f
    const val HUD_HEIGHT = 40f
    const val FIELD_OFFSET_X = 20f
    const val FIELD_OFFSET_Y = 20f
    const val PLAY_WIDTH = 440f
    const val PLAY_HEIGHT = 660f
    const val GRID_COLS = 88
    const val GRID_ROWS = 132
    const val CELL_SIZE = 5f
    const val PLAYER_SPEED = 150f
    const val LIVES_PER_LEVEL = 3
    const val POWERUP_SPAWN_INTERVAL = 15f
    const val POWERUP_LIFETIME = 8f
    const val STARS_TWO_THRESHOLD = 85
    const val STARS_THREE_THRESHOLD = 95
    const val LEVEL_COMPLETE_THRESHOLD = 90f
}
```

- [ ] **Step 2: Replace GameConstantsTest**

Replace the entire test file (old test referenced removed constant `PLAYER_DIAMOND_HALF`):

```kotlin
package game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameConstantsTest {

    @Test
    fun `GRID_COLS times CELL_SIZE equals PLAY_WIDTH`() {
        assertEquals(GameConstants.PLAY_WIDTH, GameConstants.GRID_COLS * GameConstants.CELL_SIZE, 0f)
    }

    @Test
    fun `GRID_ROWS times CELL_SIZE equals PLAY_HEIGHT`() {
        assertEquals(GameConstants.PLAY_HEIGHT, GameConstants.GRID_ROWS * GameConstants.CELL_SIZE, 0f)
    }
}
```

- [ ] **Step 3: Run the new constants tests**

```
.\gradlew :core:test --tests "game.GameConstantsTest"
```

Expected: 2 tests pass.

- [ ] **Step 4: Run full suite to catch regressions**

```
.\gradlew :core:test
```

Expected: All pass. Other tests use explicit constructor params (e.g. `MovementSystem(cellSize=10f)`) so they are isolated from constants.

- [ ] **Step 5: Commit**

```
git add core/src/main/kotlin/game/GameConstants.kt core/src/test/kotlin/game/GameConstantsTest.kt
git commit -m "feat: migrate grid to 5px cells — CELL_SIZE=5, GRID 88x132, remove PLAYER_DIAMOND_HALF"
```

---

### Task 2: BorderFinder + unit tests

**Files:**
- Create: `core/src/main/kotlin/game/ecs/systems/BorderFinder.kt`
- Create: `core/src/test/kotlin/game/ecs/systems/BorderFinderTest.kt`

- [ ] **Step 1: Write failing tests**

Create `core/src/test/kotlin/game/ecs/systems/BorderFinderTest.kt`:

```kotlin
package game.ecs.systems

import org.junit.Assert.*
import org.junit.Test

class BorderFinderTest {

    private fun borderedGrid(cols: Int = 5, rows: Int = 5): Array<Array<CellType>> {
        val cells = Array(cols) { Array(rows) { CellType.FREE } }
        for (c in 0 until cols) {
            cells[c][0] = CellType.CONQUERED
            cells[c][rows - 1] = CellType.CONQUERED
        }
        for (r in 0 until rows) {
            cells[0][r] = CellType.CONQUERED
            cells[cols - 1][r] = CellType.CONQUERED
        }
        return cells
    }

    @Test
    fun `initial perimeter cells are all border cells`() {
        val cells = borderedGrid()
        val border = BorderFinder.currentBorderCells(cells, 5, 5)
        assertTrue(border.contains(GridPoint(0, 0)))
        assertTrue(border.contains(GridPoint(1, 0)))
        assertTrue(border.contains(GridPoint(4, 4)))
    }

    @Test
    fun `fully surrounded CONQUERED cell is not a border cell`() {
        val cells = borderedGrid(5, 5)
        // Fill entire interior — (2,2) is surrounded on all 4 sides by CONQUERED
        for (c in 1..3) for (r in 1..3) cells[c][r] = CellType.CONQUERED
        val border = BorderFinder.currentBorderCells(cells, 5, 5)
        assertFalse("fully surrounded cell should not be border", border.contains(GridPoint(2, 2)))
    }

    @Test
    fun `CONQUERED cell adjacent to FREE cell is a border cell`() {
        val cells = borderedGrid(5, 5)
        cells[1][1] = CellType.CONQUERED  // touches FREE cells on 2 sides
        val border = BorderFinder.currentBorderCells(cells, 5, 5)
        assertTrue(border.contains(GridPoint(1, 1)))
    }

    @Test
    fun `randomBorderCell returns null when no FREE cells exist`() {
        val cells = Array(3) { Array(3) { CellType.CONQUERED } }
        assertNull(BorderFinder.randomBorderCell(cells, 3, 3))
    }

    @Test
    fun `randomBorderCell returns a CONQUERED cell`() {
        val cells = borderedGrid(5, 5)
        val result = BorderFinder.randomBorderCell(cells, 5, 5)
        assertNotNull(result)
        assertEquals(CellType.CONQUERED, cells[result!!.col][result.row])
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```
.\gradlew :core:test --tests "game.ecs.systems.BorderFinderTest"
```

Expected: FAIL — `BorderFinder` not found.

- [ ] **Step 3: Implement BorderFinder**

Create `core/src/main/kotlin/game/ecs/systems/BorderFinder.kt`:

```kotlin
package game.ecs.systems

object BorderFinder {

    private val DIRS = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)

    fun currentBorderCells(
        cells: Array<Array<CellType>>,
        cols: Int,
        rows: Int
    ): List<GridPoint> {
        val result = mutableListOf<GridPoint>()
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                if (cells[c][r] != CellType.CONQUERED) continue
                val touchesFree = DIRS.any { (dc, dr) ->
                    val nc = c + dc; val nr = r + dr
                    nc in 0 until cols && nr in 0 until rows && cells[nc][nr] == CellType.FREE
                }
                if (touchesFree) result.add(GridPoint(c, r))
            }
        }
        return result
    }

    fun randomBorderCell(
        cells: Array<Array<CellType>>,
        cols: Int,
        rows: Int
    ): GridPoint? = currentBorderCells(cells, cols, rows).randomOrNull()
}
```

- [ ] **Step 4: Run tests**

```
.\gradlew :core:test --tests "game.ecs.systems.BorderFinderTest"
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```
git add core/src/main/kotlin/game/ecs/systems/BorderFinder.kt core/src/test/kotlin/game/ecs/systems/BorderFinderTest.kt
git commit -m "feat: add BorderFinder — locates current CONQUERED border cells for player respawn"
```

---

### Task 3: Fix player respawn to use current border

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/EcsWorld.kt`

- [ ] **Step 1: Replace the hardcoded respawn position in loseLife()**

Find `loseLife()` in `EcsWorld.kt`. The current player position reset looks like:

```kotlin
        // Reset player to bottom border center
        player.position.x = GameConstants.FIELD_OFFSET_X + GameConstants.PLAY_WIDTH / 2f
        player.position.y = GameConstants.FIELD_OFFSET_Y
        player.playerComp!!.moving = false
```

Replace those three lines with:

```kotlin
        val respawn = BorderFinder.randomBorderCell(territory.cells, territory.cols, territory.rows)
            ?: GridPoint(territory.cols / 2, 0)
        player.position.x = GameConstants.FIELD_OFFSET_X + respawn.col * GameConstants.CELL_SIZE
        player.position.y = GameConstants.FIELD_OFFSET_Y + respawn.row * GameConstants.CELL_SIZE
        player.playerComp!!.moving = false
```

- [ ] **Step 2: Run full test suite**

```
.\gradlew :core:test
```

Expected: All pass. The initial border is a full CONQUERED frame, so `randomBorderCell` always returns a valid cell even at game start.

- [ ] **Step 3: Commit**

```
git add core/src/main/kotlin/game/ecs/EcsWorld.kt
git commit -m "fix: respawn player on current CONQUERED border via BorderFinder, not hardcoded bottom"
```

---

### Task 4: Spawn zone guards — nothing spawns in CONQUERED cells

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/EcsWorld.kt`
- Modify: `core/src/main/kotlin/game/ecs/systems/PowerupSystem.kt`
- Modify: `core/src/test/kotlin/game/ecs/systems/PowerupSystemTest.kt`

**Subgoal A — EcsWorld.init(): use randomFreeCell() for enemy positions**

- [ ] **Step 1: Replace the enemy spawn arithmetic in init()**

Find the block in `EcsWorld.init()` starting with `var initIdCounter = 1`. Replace the entire `for (enemyCfg in levelData.enemies)` block:

```kotlin
        // FIND AND REMOVE:
        var initIdCounter = 1
        for (enemyCfg in levelData.enemies) {
            val type = enemyCfg.toEnemyType()
            repeat(enemyCfg.count) {
                val startX = ox + (initIdCounter * 80f) % (GameConstants.PLAY_WIDTH - 20f) + 10f
                val startY = oy + (GameConstants.PLAY_HEIGHT * 0.5f + initIdCounter * 30f).coerceAtMost(GameConstants.PLAY_HEIGHT - 10f)
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

        // REPLACE WITH:
        var initIdCounter = 1
        for (enemyCfg in levelData.enemies) {
            val type = enemyCfg.toEnemyType()
            repeat(enemyCfg.count) {
                val freeCell = territory.randomFreeCell()
                    ?: GridPoint(territory.cols / 2, territory.rows / 2)
                val startX = ox + freeCell.col * GameConstants.CELL_SIZE
                val startY = oy + freeCell.row * GameConstants.CELL_SIZE
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
```

**Subgoal B — EcsWorld.update(): override DifficultySystem spawn position**

- [ ] **Step 2: Replace dynamic enemy spawn block in update()**

Find the block that processes `newEnemy` from `difficultySys.update(...)`:

```kotlin
        // FIND AND REMOVE:
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

        // REPLACE WITH:
        if (newEnemy != null) {
            val freeCell = territory.randomFreeCell()
            val spawnX = if (freeCell != null) GameConstants.FIELD_OFFSET_X + freeCell.col * GameConstants.CELL_SIZE else newEnemy.x
            val spawnY = if (freeCell != null) GameConstants.FIELD_OFFSET_Y + freeCell.row * GameConstants.CELL_SIZE else newEnemy.y
            enemies.add(EntityState(
                entity = Entity.Enemy(idCounter++, newEnemy.type, newEnemy.speed),
                position = PositionComponent(spawnX, spawnY),
                velocity = VelocityComponent(),
                enemyComp = EnemyComponent(
                    speed = newEnemy.speed,
                    dirX = newEnemy.dirX,
                    dirY = newEnemy.dirY
                )
            ))
        }
```

**Subgoal C — Fix PowerupSystem coordinate math**

The old `PowerupSystem` used `fieldWidth = FIELD_WIDTH = 480` (full viewport) for spawn range and `x / cellSize` (no offset) for the FREE-cell check. Both are wrong — powerups could spawn in the 20px margins and the cell lookup was off by the field offset.

- [ ] **Step 3: Replace the entire PowerupSystem.kt**

```kotlin
package game.ecs.systems

import game.ecs.EnemyComponent
import game.ecs.PlayerComponent
import game.ecs.PowerupType
import kotlin.random.Random

data class SpawnedPowerup(val type: PowerupType, val x: Float, val y: Float)

class PowerupSystem(
    private val offsetX: Float = game.GameConstants.FIELD_OFFSET_X,
    private val offsetY: Float = game.GameConstants.FIELD_OFFSET_Y,
    private val playWidth: Float = game.GameConstants.PLAY_WIDTH,
    private val playHeight: Float = game.GameConstants.PLAY_HEIGHT,
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val spawnInterval: Float = game.GameConstants.POWERUP_SPAWN_INTERVAL,
    private val lifetime: Float = game.GameConstants.POWERUP_LIFETIME
) {
    private var spawnTimer = 0f
    private var elapsedTime = 0f

    fun update(delta: Float, availableTypes: List<PowerupType>, cells: Array<Array<CellType>>? = null): SpawnedPowerup? {
        elapsedTime += delta
        spawnTimer += delta
        if (spawnTimer < currentInterval() || availableTypes.isEmpty()) return null
        spawnTimer = 0f

        val type = availableTypes.random()
        val margin = cellSize * 2
        repeat(20) {
            val x = offsetX + Random.nextFloat() * (playWidth - margin * 2) + margin
            val y = offsetY + Random.nextFloat() * (playHeight - margin * 2) + margin
            if (cells == null || !isConquered(x, y, cells)) {
                return SpawnedPowerup(type, x, y)
            }
        }
        return null
    }

    fun applyEffect(
        type: PowerupType,
        timerRef: FloatArray? = null,
        playerComp: PlayerComponent? = null,
        enemyComps: List<EnemyComponent> = emptyList()
    ) {
        when (type) {
            PowerupType.TIME   -> timerRef?.let { it[0] += 10f }
            PowerupType.FREEZE -> enemyComps.forEach { it.freezeTimer = 3f }
            PowerupType.SPEED  -> playerComp?.let {
                it.speedMultiplier = 2f
                it.speedBoostTimer = 5f
            }
            PowerupType.SHIELD -> playerComp?.let { it.shieldTimer = 4f }
        }
    }

    private fun currentInterval(): Float {
        val reduction = (elapsedTime / 30f).toInt() * 1f
        return (spawnInterval - reduction).coerceAtLeast(5f)
    }

    // FREE-cell check uses (x - offsetX) to align with grid coordinates.
    private fun isConquered(x: Float, y: Float, cells: Array<Array<CellType>>): Boolean {
        val col = ((x - offsetX) / cellSize).toInt().coerceIn(0, cells.size - 1)
        val row = ((y - offsetY) / cellSize).toInt().coerceIn(0, cells[0].size - 1)
        return cells[col][row] != CellType.FREE
    }
}
```

- [ ] **Step 4: Update PowerupSystemTest to use new constructor param names**

Replace the entire `PowerupSystemTest.kt` (old test used `fieldWidth`/`fieldHeight` which no longer exist):

```kotlin
package game.ecs.systems

import game.ecs.PowerupType
import org.junit.Assert.*
import org.junit.Test

class PowerupSystemTest {

    // offsetX=0, offsetY=0 keeps spawn coords in a simple [margin, 100-margin] range for assertions
    private val ps = PowerupSystem(
        offsetX = 0f, offsetY = 0f,
        playWidth = 100f, playHeight = 100f,
        cellSize = 10f,
        spawnInterval = 5f, lifetime = 8f
    )

    @Test
    fun `no powerup on first small delta`() {
        val spawned = ps.update(delta = 0.1f, availableTypes = listOf(PowerupType.TIME))
        assertNull(spawned)
    }

    @Test
    fun `powerup spawns after interval`() {
        val spawned = ps.update(delta = 6f, availableTypes = listOf(PowerupType.FREEZE))
        assertNotNull(spawned)
        assertEquals(PowerupType.FREEZE, spawned!!.type)
    }

    @Test
    fun `powerup position is within play field`() {
        val spawned = ps.update(delta = 6f, availableTypes = listOf(PowerupType.SPEED))!!
        assertTrue("x in play area", spawned.x in 10f..90f)
        assertTrue("y in play area", spawned.y in 10f..90f)
    }

    @Test
    fun `applying TIME adds seconds to timer`() {
        val timer = floatArrayOf(30f)
        ps.applyEffect(PowerupType.TIME, timerRef = timer, playerComp = null)
        assertEquals(40f, timer[0], 0.01f)
    }

    @Test
    fun `timer resets after spawn`() {
        ps.update(delta = 6f, availableTypes = listOf(PowerupType.TIME))
        val spawned2 = ps.update(delta = 1f, availableTypes = listOf(PowerupType.TIME))
        assertNull(spawned2)
    }

    @Test
    fun `powerup does not spawn when all cells are CONQUERED`() {
        val cells = Array(10) { Array(10) { CellType.CONQUERED } }
        val spawned = ps.update(delta = 6f, availableTypes = listOf(PowerupType.TIME), cells = cells)
        assertNull("should not spawn in fully-conquered field", spawned)
    }
}
```

- [ ] **Step 5: Run PowerupSystem tests**

```
.\gradlew :core:test --tests "game.ecs.systems.PowerupSystemTest"
```

Expected: 6 tests pass.

- [ ] **Step 6: Run full suite**

```
.\gradlew :core:test
```

Expected: All pass.

- [ ] **Step 7: Commit**

```
git add core/src/main/kotlin/game/ecs/EcsWorld.kt core/src/main/kotlin/game/ecs/systems/PowerupSystem.kt core/src/test/kotlin/game/ecs/systems/PowerupSystemTest.kt
git commit -m "fix: spawn guards — enemies and powerups only spawn in FREE cells"
```

---

### Task 5: ExplosionSystem + unit tests

**Files:**
- Create: `core/src/main/kotlin/game/ecs/systems/ExplosionSystem.kt`
- Create: `core/src/test/kotlin/game/ecs/systems/ExplosionSystemTest.kt`

`Explosion` uses float r/g/b (not LibGDX `Color`) so these tests run without a GDX backend.

- [ ] **Step 1: Write failing tests**

Create `core/src/test/kotlin/game/ecs/systems/ExplosionSystemTest.kt`:

```kotlin
package game.ecs.systems

import org.junit.Assert.*
import org.junit.Test

class ExplosionSystemTest {

    @Test
    fun `spawn adds an explosion`() {
        val sys = ExplosionSystem()
        sys.spawn(100f, 200f, 1f, 0f, 0f)
        assertEquals(1, sys.active.size)
        assertEquals(100f, sys.active[0].x, 0f)
        assertEquals(200f, sys.active[0].y, 0f)
    }

    @Test
    fun `progress starts at zero`() {
        val sys = ExplosionSystem()
        sys.spawn(0f, 0f, 1f, 1f, 1f)
        assertEquals(0f, sys.active[0].progress, 0.001f)
    }

    @Test
    fun `progress increases after update`() {
        val sys = ExplosionSystem()
        sys.spawn(0f, 0f, 1f, 1f, 1f)
        sys.update(0.3f)
        assertTrue("progress > 0.4", sys.active[0].progress > 0.4f)
    }

    @Test
    fun `expired explosion is removed`() {
        val sys = ExplosionSystem()
        sys.spawn(0f, 0f, 1f, 1f, 1f)
        sys.update(Explosion.DURATION + 0.1f)
        assertEquals(0, sys.active.size)
    }

    @Test
    fun `explosions tick independently`() {
        val sys = ExplosionSystem()
        sys.spawn(0f, 0f, 1f, 0f, 0f)
        sys.update(0.2f)
        sys.spawn(50f, 50f, 0f, 1f, 0f)
        // First elapsed=0.2, second elapsed=0 at this point.
        // Advance 0.5 more: first=0.7 > DURATION(0.6) → removed; second=0.5 → alive.
        sys.update(0.5f)
        assertEquals(1, sys.active.size)
        assertEquals(50f, sys.active[0].x, 0f)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```
.\gradlew :core:test --tests "game.ecs.systems.ExplosionSystemTest"
```

Expected: FAIL — `ExplosionSystem` not found.

- [ ] **Step 3: Implement ExplosionSystem**

Create `core/src/main/kotlin/game/ecs/systems/ExplosionSystem.kt`:

```kotlin
package game.ecs.systems

data class Explosion(
    val x: Float,
    val y: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    var elapsed: Float = 0f
) {
    companion object { const val DURATION = 0.6f }
    val progress: Float get() = elapsed / DURATION
}

class ExplosionSystem {
    private val _active = mutableListOf<Explosion>()
    val active: List<Explosion> get() = _active

    fun spawn(x: Float, y: Float, r: Float, g: Float, b: Float) {
        _active.add(Explosion(x, y, r, g, b))
    }

    fun update(delta: Float) {
        _active.forEach { it.elapsed += delta }
        _active.removeAll { it.progress >= 1f }
    }
}
```

- [ ] **Step 4: Run tests**

```
.\gradlew :core:test --tests "game.ecs.systems.ExplosionSystemTest"
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```
git add core/src/main/kotlin/game/ecs/systems/ExplosionSystem.kt core/src/test/kotlin/game/ecs/systems/ExplosionSystemTest.kt
git commit -m "feat: add ExplosionSystem — flash+ring lifecycle, GDX-free, unit-tested"
```

---

### Task 6: Wire ExplosionSystem into EcsWorld

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/EcsWorld.kt`

- [ ] **Step 1: Add explosionSys field**

In `EcsWorld.kt`, find the system declarations and add `explosionSys` after `difficultySys`:

```kotlin
    // FIND:
    val difficultySys = DifficultySystem(levelData.id)

    // ADD ON NEXT LINE:
    val explosionSys = ExplosionSystem()
```

- [ ] **Step 2: Spawn explosion for each trapped enemy**

Inside the `is CloseResult.Success ->` block, find the `for (eState in toRespawn)` loop and add the explosion spawn at the top of that loop:

```kotlin
        // FIND:
        for (eState in toRespawn) {
            score += 1000

        // REPLACE WITH:
        for (eState in toRespawn) {
            val (er, eg, eb) = when ((eState.entity as Entity.Enemy).type) {
                EnemyType.SPIDER    -> Triple(1f, 0.15f, 0.15f)
                EnemyType.COCKROACH -> Triple(0.7f, 0.45f, 0.1f)
                EnemyType.WASP      -> Triple(1f, 0.9f, 0f)
                EnemyType.SNAIL     -> Triple(0.2f, 1f, 0.4f)
            }
            explosionSys.spawn(eState.position.x, eState.position.y, er, eg, eb)
            score += 1000
```

- [ ] **Step 3: Explode powerups captured inside conquered area**

Still inside the `is CloseResult.Success ->` block, after `score += calculateClaimScore(territory.conqueredPercent(), result.snailsTrapped)` and before `checkLevelComplete()`, add:

```kotlin
            val capturedPowerups = activePowerups.filter { pwState ->
                movement.toGridPoint(pwState.position.x, pwState.position.y) in result.conqueredCells
            }
            capturedPowerups.forEach { pwState ->
                explosionSys.spawn(pwState.position.x, pwState.position.y, 1f, 0.8f, 0f)
                score += 200
            }
            activePowerups.removeAll(capturedPowerups)
```

- [ ] **Step 4: Tick ExplosionSystem each frame**

Find `activePowerups.removeAll(toRemove)` near the end of `update()`. Add `explosionSys.update(delta)` on the next line:

```kotlin
        // FIND:
        activePowerups.removeAll(toRemove)

        // ADD AFTER:
        explosionSys.update(delta)
```

- [ ] **Step 5: Run full test suite**

```
.\gradlew :core:test
```

Expected: All pass.

- [ ] **Step 6: Commit**

```
git add core/src/main/kotlin/game/ecs/EcsWorld.kt
git commit -m "feat: wire ExplosionSystem — spawn on enemy trap and powerup capture, +200 pts per powerup"
```

---

### Task 7: Replace player diamond with glowing dot

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt`

- [ ] **Step 1: Remove drawDiamond() and replace drawPlayer()**

In `GameScreen.kt`, remove the entire `drawDiamond()` helper function:

```kotlin
// REMOVE:
    /** Draws a filled diamond (rhombus) centered at (cx, cy) with half-span s. */
    private fun drawDiamond(cx: Float, cy: Float, s: Float) {
        shapes.triangle(cx, cy + s, cx + s, cy, cx - s, cy)
        shapes.triangle(cx - s, cy, cx + s, cy, cx, cy - s)
    }
```

Then replace the entire `drawPlayer()` function:

```kotlin
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
```

- [ ] **Step 2: Verify no remaining references to PLAYER_DIAMOND_HALF or drawDiamond**

```
.\gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL (no unresolved references).

- [ ] **Step 3: Run full suite**

```
.\gradlew :core:test
```

Expected: All pass.

- [ ] **Step 4: Commit**

```
git add core/src/main/kotlin/game/screens/GameScreen.kt
git commit -m "feat: replace player diamond with 3-layer glowing dot cursor"
```

---

### Task 8: Render explosions in GameScreen

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt`

- [ ] **Step 1: Add drawExplosions() method**

Add this method to `GameScreen`, just before `drawHUD()`:

```kotlin
    private fun drawExplosions() {
        val explosions = world.explosionSys.active
        if (explosions.isEmpty()) return

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

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
```

- [ ] **Step 2: Call drawExplosions() in render() between enemies and player**

Find the draw order in `render()`:

```kotlin
        // FIND:
        drawEnemies()
        drawPlayer()

        // REPLACE WITH:
        drawEnemies()
        drawExplosions()
        drawPlayer()
```

- [ ] **Step 3: Build**

```
.\gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run full suite**

```
.\gradlew :core:test
```

Expected: All pass.

- [ ] **Step 5: Commit**

```
git add core/src/main/kotlin/game/screens/GameScreen.kt
git commit -m "feat: render flash+ring explosions on territory capture"
```
