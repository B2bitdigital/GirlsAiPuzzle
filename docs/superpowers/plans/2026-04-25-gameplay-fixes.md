# Gameplay Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix player movement (4-direction continuous), background vertical flip, and perimeter-only constraint for safe-zone movement.

**Architecture:** Replace `targetX/targetY` point-based movement with `dirX/dirY/moving` direction-based continuous movement. Player moves until hitting field boundary or interior conquered cell. Touch sets direction; new touch changes it. Background fix is a one-line srcY calculation change. Perimeter constraint added as `isInteriorConquered` check in `movePlayer`.

**Tech Stack:** Kotlin, libGDX, JUnit 4, Gradle (`gradlew.bat :core:test`, `gradlew.bat assembleDebug`)

---

## File Map

| File | Change |
|------|--------|
| `core/src/test/kotlin/game/ecs/systems/TerritorySystemTest.kt` | Remove `EnemyTrapped` test (class deleted); add `isOnPerimeter` tests |
| `core/src/test/kotlin/game/ecs/systems/MovementSystemTest.kt` | Replace `movePlayerToward` tests with `movePlayer` tests |
| `core/src/main/kotlin/game/ecs/Component.kt` | `PlayerComponent`: remove `targetX/targetY`, add `dirX/dirY/moving` |
| `core/src/main/kotlin/game/ecs/systems/MovementSystem.kt` | Replace `movePlayerToward`/`snapToGrid` with `movePlayer` + private `isInteriorConquered` |
| `core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt` | Add `isOnPerimeter()` |
| `core/src/main/kotlin/game/ecs/EcsWorld.kt` | Update player movement block to use `pc.dirX/dirY/moving` |
| `core/src/main/kotlin/game/screens/GameScreen.kt` | Input processor: direction-based; `drawBackground`: srcY flip |

---

## Task 1: Fix TerritorySystemTest (remove EnemyTrapped, add isOnPerimeter)

**Files:**
- Modify: `core/src/test/kotlin/game/ecs/systems/TerritorySystemTest.kt`

- [ ] **Step 1: Replace the broken EnemyTrapped test and add isOnPerimeter tests**

Replace the entire file content:

```kotlin
package game.ecs.systems

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TerritorySystemTest {

    private lateinit var ts: TerritorySystem

    @Before
    fun setup() {
        ts = TerritorySystem(cols = 10, rows = 10)
    }

    @Test
    fun `border cells are conquered on init`() {
        for (c in 0 until 10) {
            assertTrue("top border c=$c", ts.grid[c][0])
            assertTrue("bottom border c=$c", ts.grid[c][9])
        }
        for (r in 0 until 10) {
            assertTrue("left border r=$r", ts.grid[0][r])
            assertTrue("right border r=$r", ts.grid[9][r])
        }
    }

    @Test
    fun `interior cells are free on init`() {
        assertFalse(ts.grid[5][5])
        assertFalse(ts.grid[3][3])
    }

    @Test
    fun `conquered percent accounts for border`() {
        // 10x10 = 100 cells. Border = 2*(10+8) = 36 cells.
        val pct = ts.conqueredPercent()
        assertEquals(36f, pct, 0.1f)
    }

    @Test
    fun `close line with no enemies conquers enclosed interior`() {
        // Line at col=3 from row=0 to row=9 splits field
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))

        val result = ts.closeLine(emptyList(), emptyList())
        assertTrue(result is CloseResult.Success)
        // Both sides (cols 1-2 and cols 4-8) should be conquered — no enemies
        assertTrue(ts.grid[1][5])
        assertTrue(ts.grid[5][5])
    }

    @Test
    fun `close line with dangerous enemy — enemy side stays free, other side captured`() {
        // Line at col=3: left region = cols 1-2, right region = cols 4-8
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))

        // Enemy at (1,4) — inside left region
        val result = ts.closeLine(
            dangerousEnemies = listOf(GridPoint(1, 4)),
            snails = emptyList()
        )
        assertTrue(result is CloseResult.Success)
        // Left region (enemy side) stays free
        assertFalse("enemy side should remain free", ts.grid[1][5])
        // Right region (no enemy) gets conquered
        assertTrue("far side should be conquered", ts.grid[5][5])
        // Line cells become permanent border
        assertTrue("line cells conquered", ts.grid[3][4])
    }

    @Test
    fun `close line with snail in enclosed region gives bonus`() {
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))

        val result = ts.closeLine(
            dangerousEnemies = emptyList(),
            snails = listOf(GridPoint(1, 4))
        )
        assertTrue(result is CloseResult.Success)
        assertEquals(1, (result as CloseResult.Success).snailsTrapped)
    }

    @Test
    fun `isOnSafeZone returns true for conquered cell`() {
        assertTrue(ts.isOnSafeZone(GridPoint(0, 0)))
        assertTrue(ts.isOnSafeZone(GridPoint(5, 0)))
    }

    @Test
    fun `isOnSafeZone returns false for free cell`() {
        assertFalse(ts.isOnSafeZone(GridPoint(5, 5)))
    }

    @Test
    fun `conqueredPercent increases after successful close`() {
        val before = ts.conqueredPercent()
        ts.startLine(GridPoint(3, 0))
        for (r in 1 until 9) ts.extendLine(GridPoint(3, r))
        ts.extendLine(GridPoint(3, 9))
        ts.closeLine(emptyList(), emptyList())
        assertTrue(ts.conqueredPercent() > before)
    }

    @Test
    fun `isOnPerimeter returns true for border field edge`() {
        // Field edge cells always have out-of-bounds neighbor → perimeter
        assertTrue(ts.isOnPerimeter(GridPoint(0, 0)))
        assertTrue(ts.isOnPerimeter(GridPoint(5, 0)))
    }

    @Test
    fun `isOnPerimeter returns true for conquered cell adjacent to free cell`() {
        // col=0 row=5: conquered, neighbor (1,5) is free → perimeter
        assertTrue(ts.isOnPerimeter(GridPoint(0, 5)))
    }

    @Test
    fun `isOnPerimeter returns false for free cell`() {
        assertFalse(ts.isOnPerimeter(GridPoint(5, 5)))
    }

    @Test
    fun `isOnPerimeter returns false for interior conquered cell`() {
        // Conquer entire grid manually (simulating total capture)
        for (c in 0 until 10) for (r in 0 until 10) ts.grid[c][r] = true
        // Cell (5,5): all 4 neighbors are conquered, not on field edge → interior
        assertFalse(ts.isOnPerimeter(GridPoint(5, 5)))
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure until TerritorySystem + MovementSystem updated**

```
gradlew.bat :core:test
```

Expected: compilation error on `CloseResult.EnemyTrapped` in this file is gone. May still fail on MovementSystemTest. Note exact errors.

---

## Task 2: Fix MovementSystemTest (replace movePlayerToward tests with movePlayer tests)

**Files:**
- Modify: `core/src/test/kotlin/game/ecs/systems/MovementSystemTest.kt`

- [ ] **Step 1: Replace entire file**

```kotlin
package game.ecs.systems

import org.junit.Assert.*
import org.junit.Test

class MovementSystemTest {

    // 10×10 field, cellSize=10 → 100×100 pixels
    private val ms = MovementSystem(cellSize = 10f, fieldWidth = 100f, fieldHeight = 100f)

    private fun emptyGrid(cols: Int = 10, rows: Int = 10): Array<BooleanArray> =
        Array(cols) { BooleanArray(rows) }

    private fun borderedGrid(cols: Int = 10, rows: Int = 10): Array<BooleanArray> {
        val g = emptyGrid(cols, rows)
        for (c in 0 until cols) { g[c][0] = true; g[c][rows - 1] = true }
        for (r in 0 until rows) { g[0][r] = true; g[cols - 1][r] = true }
        return g
    }

    @Test
    fun `movePlayer moves right on free grid`() {
        val grid = borderedGrid()
        val pos = floatArrayOf(50f, 50f)
        val moved = ms.movePlayer(pos, dirX = 1f, dirY = 0f, speed = 100f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10, cellSize = 10f)
        assertTrue("should return true", moved)
        assertTrue("x should increase", pos[0] > 50f)
        assertEquals("y unchanged", 50f, pos[1], 0.01f)
    }

    @Test
    fun `movePlayer moves up on free grid`() {
        val grid = borderedGrid()
        val pos = floatArrayOf(50f, 50f)
        val moved = ms.movePlayer(pos, dirX = 0f, dirY = 1f, speed = 100f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10, cellSize = 10f)
        assertTrue(moved)
        assertTrue(pos[1] > 50f)
        assertEquals(50f, pos[0], 0.01f)
    }

    @Test
    fun `movePlayer returns false when hitting right boundary`() {
        val grid = borderedGrid()
        val pos = floatArrayOf(98f, 50f)
        val moved = ms.movePlayer(pos, dirX = 1f, dirY = 0f, speed = 200f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10, cellSize = 10f)
        assertFalse("should be blocked at boundary", moved)
    }

    @Test
    fun `movePlayer returns false when hitting left boundary`() {
        val grid = borderedGrid()
        val pos = floatArrayOf(2f, 50f)
        val moved = ms.movePlayer(pos, dirX = -1f, dirY = 0f, speed = 200f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10, cellSize = 10f)
        assertFalse(moved)
    }

    @Test
    fun `movePlayer returns false when next cell is interior conquered`() {
        // Fully conquered 10×10 grid
        val grid = Array(10) { BooleanArray(10) { true } }
        // Player at pixel (15,50) → cell (1,5). Moving right → next cell (2,5).
        // Cell (2,5): all 4 neighbors are conquered (in-bounds) → interior → blocked.
        val pos = floatArrayOf(15f, 50f)
        val moved = ms.movePlayer(pos, dirX = 1f, dirY = 0f, speed = 100f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10, cellSize = 10f)
        assertFalse("interior conquered cell should block movement", moved)
    }

    @Test
    fun `movePlayer allows entering free cell from perimeter`() {
        val grid = borderedGrid()
        // Player at (5, 5) → cell (0,0) border. Move right into free cell (1,0)... 
        // Actually start at border cell (0, 55) → cell (0,5). Move right into free cell (1,5).
        val pos = floatArrayOf(5f, 55f)
        val moved = ms.movePlayer(pos, dirX = 1f, dirY = 0f, speed = 100f, delta = 0.1f,
            grid = grid, cols = 10, rows = 10, cellSize = 10f)
        assertTrue("should be able to enter free zone", moved)
    }

    @Test
    fun `toGridPoint converts pixel to grid cell`() {
        val pt = ms.toGridPoint(35f, 55f)
        assertEquals(3, pt.col)
        assertEquals(5, pt.row)
    }

    @Test
    fun `toGridPoint clamps to valid range`() {
        val pt = ms.toGridPoint(-10f, 999f)
        assertEquals(0, pt.col)
        assertEquals(9, pt.row)
    }
}
```

- [ ] **Step 2: Run tests — expect failures since movePlayer doesn't exist yet**

```
gradlew.bat :core:test --tests "game.ecs.systems.MovementSystemTest"
```

Expected: compilation error `unresolved reference: movePlayer`.

---

## Task 3: Update PlayerComponent

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/Component.kt`

- [ ] **Step 1: Replace `targetX/targetY` with `dirX/dirY/moving` in PlayerComponent**

In `Component.kt`, replace:

```kotlin
data class PlayerComponent(
    var lives: Int,
    var score: Int = 0,
    var targetX: Float = -1f,
    var targetY: Float = -1f,
    var speedMultiplier: Float = 1f,
    var speedBoostTimer: Float = 0f,
    var shieldTimer: Float = 0f
)
```

with:

```kotlin
data class PlayerComponent(
    var lives: Int,
    var score: Int = 0,
    var dirX: Float = 0f,
    var dirY: Float = 0f,
    var moving: Boolean = false,
    var speedMultiplier: Float = 1f,
    var speedBoostTimer: Float = 0f,
    var shieldTimer: Float = 0f
)
```

- [ ] **Step 2: Run tests — expect compile errors in EcsWorld and GameScreen referencing targetX/targetY**

```
gradlew.bat :core:test
```

Note the exact compile errors. They will be fixed in Tasks 6 and 7.

---

## Task 4: Rewrite MovementSystem

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/systems/MovementSystem.kt`

- [ ] **Step 1: Replace entire file**

```kotlin
package game.ecs.systems

import kotlin.math.abs

class MovementSystem(
    private val cellSize: Float = game.GameConstants.CELL_SIZE,
    private val fieldWidth: Float = game.GameConstants.FIELD_WIDTH,
    private val fieldHeight: Float = game.GameConstants.FIELD_HEIGHT
) {
    /**
     * Move player in direction (dirX, dirY) at given speed for one frame.
     * Returns false if movement is blocked (boundary or interior conquered cell).
     * pos = [x, y], mutated in place.
     */
    fun movePlayer(
        pos: FloatArray,
        dirX: Float, dirY: Float,
        speed: Float, delta: Float,
        grid: Array<BooleanArray>,
        cols: Int, rows: Int,
        cellSize: Float
    ): Boolean {
        val nextX = pos[0] + dirX * speed * delta
        val nextY = pos[1] + dirY * speed * delta

        if (nextX < 0f || nextX > cols * cellSize || nextY < 0f || nextY > rows * cellSize) {
            return false
        }

        val nextCol = (nextX / cellSize).toInt().coerceIn(0, cols - 1)
        val nextRow = (nextY / cellSize).toInt().coerceIn(0, rows - 1)

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

    /** Convert pixel position to GridPoint */
    fun toGridPoint(x: Float, y: Float) = GridPoint(
        col = (x / cellSize).toInt().coerceIn(0, (fieldWidth / cellSize).toInt() - 1),
        row = (y / cellSize).toInt().coerceIn(0, (fieldHeight / cellSize).toInt() - 1)
    )
}
```

- [ ] **Step 2: Run MovementSystemTest — all tests must pass**

```
gradlew.bat :core:test --tests "game.ecs.systems.MovementSystemTest"
```

Expected output: `BUILD SUCCESSFUL`, all 8 tests green.

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/game/ecs/systems/MovementSystem.kt
git add core/src/main/kotlin/game/ecs/Component.kt
git add core/src/test/kotlin/game/ecs/systems/MovementSystemTest.kt
git add core/src/test/kotlin/game/ecs/systems/TerritorySystemTest.kt
git commit -m "refactor: replace point-target movement with direction-based continuous movement"
```

---

## Task 5: Add isOnPerimeter to TerritorySystem

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt`

- [ ] **Step 1: Add `isOnPerimeter` after `isOnSafeZone`**

In `TerritorySystem.kt`, after the `isOnSafeZone` function, insert:

```kotlin
fun isOnPerimeter(pt: GridPoint): Boolean {
    if (!isOnSafeZone(pt)) return false
    val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
    return dirs.any { (dc, dr) ->
        val nc = pt.col + dc; val nr = pt.row + dr
        nc !in 0 until cols || nr !in 0 until rows || !grid[nc][nr]
    }
}
```

- [ ] **Step 2: Run TerritorySystemTest — all tests must pass**

```
gradlew.bat :core:test --tests "game.ecs.systems.TerritorySystemTest"
```

Expected: `BUILD SUCCESSFUL`, all 11 tests green.

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt
git commit -m "feat: add isOnPerimeter to TerritorySystem"
```

---

## Task 6: Update EcsWorld — new movement block

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/EcsWorld.kt`

- [ ] **Step 1: Replace player movement block**

Find and replace the player movement block (lines 77-84 approximately):

Old:
```kotlin
// Move player
val pc = player.playerComp!!
val pos = player.position
val effectiveSpeed = GameConstants.PLAYER_SPEED * pc.speedMultiplier
val posArr = floatArrayOf(pos.x, pos.y)
if (pc.targetX >= 0f) {
    movement.movePlayerToward(posArr, pc.targetX, pc.targetY, effectiveSpeed, delta)
    pos.x = posArr[0]; pos.y = posArr[1]
}
```

New:
```kotlin
// Move player
val pc = player.playerComp!!
val pos = player.position
val effectiveSpeed = GameConstants.PLAYER_SPEED * pc.speedMultiplier
val posArr = floatArrayOf(pos.x, pos.y)
if (pc.moving) {
    val stillMoving = movement.movePlayer(
        posArr, pc.dirX, pc.dirY, effectiveSpeed, delta,
        territory.grid, GameConstants.GRID_COLS, GameConstants.GRID_ROWS, GameConstants.CELL_SIZE
    )
    pos.x = posArr[0]; pos.y = posArr[1]
    if (!stillMoving) pc.moving = false
}
```

- [ ] **Step 2: Run full test suite**

```
gradlew.bat :core:test
```

Expected: all tests pass. If GameScreen still references `targetX`, fix is in Task 7.

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/game/ecs/EcsWorld.kt
git commit -m "feat: update EcsWorld to use direction-based player movement"
```

---

## Task 7: Update GameScreen — input processor + background flip

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt`

- [ ] **Step 1: Add missing imports at top of GameScreen.kt**

After the existing imports, ensure these are present (add if missing):

```kotlin
import kotlin.math.abs
import kotlin.math.sign
```

- [ ] **Step 2: Replace the input processor in `show()`**

Old (inside `show()`):
```kotlin
Gdx.input.inputProcessor = object : InputAdapter() {
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val worldCoords = viewport.unproject(com.badlogic.gdx.math.Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
        world.player.playerComp!!.targetX = worldCoords.x.coerceIn(0f, GameConstants.FIELD_WIDTH)
        world.player.playerComp!!.targetY = worldCoords.y.coerceIn(0f, GameConstants.FIELD_HEIGHT)
        return true
    }
}
```

New:
```kotlin
Gdx.input.inputProcessor = object : InputAdapter() {
    private fun applyDirection(screenX: Int, screenY: Int) {
        val wc = viewport.unproject(com.badlogic.gdx.math.Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
        val pc = world.player.playerComp!!
        val pos = world.player.position
        val dx = wc.x - pos.x
        val dy = wc.y - pos.y
        if (abs(dx) >= abs(dy)) { pc.dirX = sign(dx); pc.dirY = 0f }
        else                    { pc.dirX = 0f;       pc.dirY = sign(dy) }
        pc.moving = true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        applyDirection(screenX, screenY)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        applyDirection(screenX, screenY)
        return true
    }
}
```

- [ ] **Step 3: Fix background vertical flip in `drawBackground()`**

Old:
```kotlin
batch.draw(tex,
    c * cs, r * cs, cs, cs,
    (c * cs).toInt(), (r * cs).toInt(),
    cs.toInt(), cs.toInt(),
    false, false)
```

New:
```kotlin
batch.draw(tex,
    c * cs, r * cs, cs, cs,
    (c * cs).toInt(), tex.height - (r + 1) * cs.toInt(),
    cs.toInt(), cs.toInt(),
    false, false)
```

- [ ] **Step 4: Build APK to verify no compile errors**

```
gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/game/screens/GameScreen.kt
git commit -m "fix: direction-based touch input, background vertical flip"
```

---

## Task 8: Run full test suite + final build

**Files:** none (verification only)

- [ ] **Step 1: Run all tests**

```
gradlew.bat :core:test
```

Expected: `BUILD SUCCESSFUL`. All test classes green:
- `TerritorySystemTest` (11 tests)
- `MovementSystemTest` (8 tests)
- `CollisionSystemTest`
- `EnemyAISystemTest`
- `PowerupSystemTest`
- `LevelLoaderTest`
- `GamePrefsTest`

- [ ] **Step 2: Build release-quality debug APK**

```
gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL in ~1m`.

- [ ] **Step 3: Verify APK location**

```
ls android/build/outputs/apk/debug/android-debug.apk
```

Expected: file exists.

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete gameplay fixes — 4-dir movement, background flip, perimeter constraint"
```
