# Player Perimeter Snap & Trail Rendering — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix player diamond bisection by the perimeter line and replace the thick orange trail with a thin cyan polyline matching the territory border style.

**Architecture:** Two rendering bugs fixed in `PlayerRenderUtils.kt` (wrong coordinate math) and `GameScreen.kt` (wrong draw style). A new `gridPointRenderPos(GridPoint)` helper is added to `PlayerRenderUtils.kt` for trail rendering — same snap logic as `playerRenderPos` but takes a grid cell instead of a float position. All rendering stays in screen space (FIELD_OFFSET baked into every position).

**Coordinate system:** Field occupies screen rect `[ox, ox+PLAY_WIDTH] × [oy, oy+PLAY_HEIGHT]` where `ox = oy = 20f`, `CELL_SIZE = 10f`, `GRID_COLS = 44`, `GRID_ROWS = 66`. Left border line = `ox + cs = 30f`, right = `ox + 43·cs = 450f`, bottom = `oy + cs = 30f`, top = `oy + 65·cs = 670f`.

**Tech Stack:** Kotlin, LibGDX, JUnit 4, Gradle

---

### Task 1: Fix `playerRenderPos` — correct offset and snap targets

**Files:**
- Modify: `core/src/main/kotlin/game/screens/PlayerRenderUtils.kt`
- Modify: `core/src/test/kotlin/game/screens/PlayerRenderUtilsTest.kt`

The current implementation computes `col = (posX / cs).toInt()` — missing offset subtraction — and snaps to `0f` / `FIELD_WIDTH` / `PLAY_HEIGHT` instead of the actual border line positions where `drawTerritoryBorder` draws.

- [ ] **Step 1: Replace all tests in `PlayerRenderUtilsTest.kt` with correct-coordinate versions**

```kotlin
package game.screens

import game.GameConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRenderUtilsTest {

    // Coordinate reference:
    // ox=20, oy=20, cs=10, GRID_COLS=44, GRID_ROWS=66
    // Left snap  = ox + cs               = 30f
    // Right snap = ox + (GRID_COLS-1)*cs = 450f
    // Bottom snap= oy + cs               = 30f
    // Top snap   = oy + (GRID_ROWS-1)*cs = 670f

    @Test
    fun `interior position unchanged`() {
        val (rx, ry) = playerRenderPos(100f, 200f)
        assertEquals(100f, rx, 0f)
        assertEquals(200f, ry, 0f)
    }

    @Test
    fun `left border column snaps x to ox+cs`() {
        // posX=25f → col=(25-20)/10=0 → snap
        val (rx, _) = playerRenderPos(25f, 100f)
        assertEquals(30f, rx, 0f)
    }

    @Test
    fun `right border column snaps x to ox+(GRID_COLS-1)*cs`() {
        // posX=455f → col=(455-20)/10=43 → snap
        val (rx, _) = playerRenderPos(455f, 100f)
        assertEquals(450f, rx, 0f)
    }

    @Test
    fun `bottom border row snaps y to oy+cs`() {
        // posY=25f → row=(25-20)/10=0 → snap
        val (_, ry) = playerRenderPos(100f, 25f)
        assertEquals(30f, ry, 0f)
    }

    @Test
    fun `top border row snaps y to oy+(GRID_ROWS-1)*cs`() {
        // posY=675f → row=(675-20)/10=65 → snap
        val (_, ry) = playerRenderPos(100f, 675f)
        assertEquals(670f, ry, 0f)
    }

    @Test
    fun `corner snaps both axes`() {
        val (rx, ry) = playerRenderPos(25f, 25f)
        assertEquals(30f, rx, 0f)
        assertEquals(30f, ry, 0f)
    }

    @Test
    fun `posX at field left edge snaps`() {
        // posX=20f → col=(20-20)/10=0 → snap
        val (rx, _) = playerRenderPos(20f, 100f)
        assertEquals(30f, rx, 0f)
    }

    @Test
    fun `posX beyond field right edge coerces to right snap`() {
        // posX=460f → col=(460-20)/10=44 coerced to 43 → snap
        val (rx, _) = playerRenderPos(460f, 100f)
        assertEquals(450f, rx, 0f)
    }

    @Test
    fun `first interior column does not snap`() {
        // posX=35f → col=(35-20)/10=1 → no snap
        val (rx, _) = playerRenderPos(35f, 100f)
        assertEquals(35f, rx, 0f)
    }

    @Test
    fun `last interior column does not snap`() {
        // posX=449f → col=(449-20)/10=42 → no snap
        val (rx, _) = playerRenderPos(449f, 100f)
        assertEquals(449f, rx, 0f)
    }

    @Test
    fun `posX exactly at right snap value is right border`() {
        // posX=450f → col=(450-20)/10=43 → snap
        val (rx, _) = playerRenderPos(450f, 100f)
        assertEquals(450f, rx, 0f)
    }
}
```

- [ ] **Step 2: Run tests — verify they fail**

```bash
./gradlew.bat :core:test --tests "game.screens.PlayerRenderUtilsTest"
```

Expected: multiple FAIL — snapped values return `0f` / `480f` / `660f` instead of `30f` / `450f` / `670f`.

- [ ] **Step 3: Replace `PlayerRenderUtils.kt` with fixed implementation**

```kotlin
package game.screens

import game.GameConstants

data class RenderPos(val x: Float, val y: Float)

/**
 * Returns the render position for the player diamond.
 * Positions are in screen space (FIELD_OFFSET baked in).
 * Perimeter columns/rows snap the perpendicular coordinate
 * to the inner boundary line — the exact pixel where
 * drawTerritoryBorder draws the border edge — so that
 * line bisects the diamond exactly in half.
 */
fun playerRenderPos(posX: Float, posY: Float): RenderPos {
    val ox = GameConstants.FIELD_OFFSET_X
    val oy = GameConstants.FIELD_OFFSET_Y
    val cs = GameConstants.CELL_SIZE
    val col = ((posX - ox) / cs).toInt().coerceIn(0, GameConstants.GRID_COLS - 1)
    val row = ((posY - oy) / cs).toInt().coerceIn(0, GameConstants.GRID_ROWS - 1)
    val rx = when (col) {
        0                           -> ox + cs
        GameConstants.GRID_COLS - 1 -> ox + (GameConstants.GRID_COLS - 1) * cs
        else                        -> posX
    }
    val ry = when (row) {
        0                           -> oy + cs
        GameConstants.GRID_ROWS - 1 -> oy + (GameConstants.GRID_ROWS - 1) * cs
        else                        -> posY
    }
    return RenderPos(rx, ry)
}
```

- [ ] **Step 4: Run tests — verify they pass**

```bash
./gradlew.bat :core:test --tests "game.screens.PlayerRenderUtilsTest"
```

Expected: all PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/game/screens/PlayerRenderUtils.kt
git add core/src/test/kotlin/game/screens/PlayerRenderUtilsTest.kt
git commit -m "fix: playerRenderPos subtract field offset, snap to border line position"
```

---

### Task 2: Add `gridPointRenderPos` — trail point helper

**Files:**
- Modify: `core/src/main/kotlin/game/screens/PlayerRenderUtils.kt`
- Modify: `core/src/test/kotlin/game/screens/PlayerRenderUtilsTest.kt`

Same snap logic as `playerRenderPos` but input is a `GridPoint` (column/row integers) instead of a float screen position. Interior cells return the cell centre. Used by `drawCurrentLine` in Task 3.

- [ ] **Step 1: Add tests for `gridPointRenderPos` to `PlayerRenderUtilsTest.kt`**

Append these tests to the existing test class body:

```kotlin
    // gridPointRenderPos tests
    // Interior cell centre = ox + col*cs + cs/2f, oy + row*cs + cs/2f

    @Test
    fun `gridPoint interior returns cell centre`() {
        val (x, y) = gridPointRenderPos(GridPoint(5, 5))
        assertEquals(75f, x, 0f)   // 20 + 5*10 + 5 = 75
        assertEquals(75f, y, 0f)
    }

    @Test
    fun `gridPoint left perimeter snaps x to ox+cs`() {
        val (x, _) = gridPointRenderPos(GridPoint(0, 5))
        assertEquals(30f, x, 0f)
    }

    @Test
    fun `gridPoint right perimeter snaps x to ox+(GRID_COLS-1)*cs`() {
        val (x, _) = gridPointRenderPos(GridPoint(43, 5))
        assertEquals(450f, x, 0f)
    }

    @Test
    fun `gridPoint bottom perimeter snaps y to oy+cs`() {
        val (_, y) = gridPointRenderPos(GridPoint(5, 0))
        assertEquals(30f, y, 0f)
    }

    @Test
    fun `gridPoint top perimeter snaps y to oy+(GRID_ROWS-1)*cs`() {
        val (_, y) = gridPointRenderPos(GridPoint(5, 65))
        assertEquals(670f, y, 0f)
    }

    @Test
    fun `gridPoint corner snaps both axes`() {
        val (x, y) = gridPointRenderPos(GridPoint(0, 0))
        assertEquals(30f, x, 0f)
        assertEquals(30f, y, 0f)
    }

    @Test
    fun `gridPoint first interior cell returns centre`() {
        val (x, y) = gridPointRenderPos(GridPoint(1, 1))
        assertEquals(35f, x, 0f)   // 20 + 1*10 + 5 = 35
        assertEquals(35f, y, 0f)
    }
```

You also need to add the import for `GridPoint` at the top of `PlayerRenderUtilsTest.kt`:

```kotlin
import game.ecs.systems.GridPoint
```

- [ ] **Step 2: Run tests — verify new ones fail**

```bash
./gradlew.bat :core:test --tests "game.screens.PlayerRenderUtilsTest"
```

Expected: 7 new tests FAIL with "unresolved reference: gridPointRenderPos".

- [ ] **Step 3: Add `gridPointRenderPos` to `PlayerRenderUtils.kt`**

Append after the closing brace of `playerRenderPos` (before end of file):

```kotlin
import game.ecs.systems.GridPoint
```

Add this import at the top of `PlayerRenderUtils.kt` alongside the existing imports, then append this function:

```kotlin
/**
 * Returns the render position for a trail GridPoint.
 * Perimeter cells snap to the inner boundary (same targets as playerRenderPos).
 * Interior cells return the cell centre in screen space.
 */
fun gridPointRenderPos(pt: GridPoint): RenderPos {
    val ox = GameConstants.FIELD_OFFSET_X
    val oy = GameConstants.FIELD_OFFSET_Y
    val cs = GameConstants.CELL_SIZE
    val x = when (pt.col) {
        0                           -> ox + cs
        GameConstants.GRID_COLS - 1 -> ox + (GameConstants.GRID_COLS - 1) * cs
        else                        -> ox + pt.col * cs + cs / 2f
    }
    val y = when (pt.row) {
        0                           -> oy + cs
        GameConstants.GRID_ROWS - 1 -> oy + (GameConstants.GRID_ROWS - 1) * cs
        else                        -> oy + pt.row * cs + cs / 2f
    }
    return RenderPos(x, y)
}
```

The full `PlayerRenderUtils.kt` after both tasks:

```kotlin
package game.screens

import game.GameConstants
import game.ecs.systems.GridPoint

data class RenderPos(val x: Float, val y: Float)

/**
 * Returns the render position for the player diamond.
 * Positions are in screen space (FIELD_OFFSET baked in).
 * Perimeter columns/rows snap the perpendicular coordinate
 * to the inner boundary line — the exact pixel where
 * drawTerritoryBorder draws the border edge — so that
 * line bisects the diamond exactly in half.
 */
fun playerRenderPos(posX: Float, posY: Float): RenderPos {
    val ox = GameConstants.FIELD_OFFSET_X
    val oy = GameConstants.FIELD_OFFSET_Y
    val cs = GameConstants.CELL_SIZE
    val col = ((posX - ox) / cs).toInt().coerceIn(0, GameConstants.GRID_COLS - 1)
    val row = ((posY - oy) / cs).toInt().coerceIn(0, GameConstants.GRID_ROWS - 1)
    val rx = when (col) {
        0                           -> ox + cs
        GameConstants.GRID_COLS - 1 -> ox + (GameConstants.GRID_COLS - 1) * cs
        else                        -> posX
    }
    val ry = when (row) {
        0                           -> oy + cs
        GameConstants.GRID_ROWS - 1 -> oy + (GameConstants.GRID_ROWS - 1) * cs
        else                        -> posY
    }
    return RenderPos(rx, ry)
}

/**
 * Returns the render position for a trail GridPoint.
 * Perimeter cells snap to the inner boundary (same targets as playerRenderPos).
 * Interior cells return the cell centre in screen space.
 */
fun gridPointRenderPos(pt: GridPoint): RenderPos {
    val ox = GameConstants.FIELD_OFFSET_X
    val oy = GameConstants.FIELD_OFFSET_Y
    val cs = GameConstants.CELL_SIZE
    val x = when (pt.col) {
        0                           -> ox + cs
        GameConstants.GRID_COLS - 1 -> ox + (GameConstants.GRID_COLS - 1) * cs
        else                        -> ox + pt.col * cs + cs / 2f
    }
    val y = when (pt.row) {
        0                           -> oy + cs
        GameConstants.GRID_ROWS - 1 -> oy + (GameConstants.GRID_ROWS - 1) * cs
        else                        -> oy + pt.row * cs + cs / 2f
    }
    return RenderPos(x, y)
}
```

- [ ] **Step 4: Run tests — verify all pass**

```bash
./gradlew.bat :core:test --tests "game.screens.PlayerRenderUtilsTest"
```

Expected: all 18 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/game/screens/PlayerRenderUtils.kt
git add core/src/test/kotlin/game/screens/PlayerRenderUtilsTest.kt
git commit -m "feat: add gridPointRenderPos for trail polyline rendering"
```

---

### Task 3: Fix `drawCurrentLine` — replace thick orange with thin cyan polyline

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt`

Replace the thick filled-rectangle orange trail with a thin `ShapeType.Line` polyline in `(0f, 0.8f, 0.8f, 0.5f)` — identical color/alpha to `drawTerritoryBorder`. Remove the unused `colorLine` field.

- [ ] **Step 1: Remove `colorLine` field and replace `drawCurrentLine` in `GameScreen.kt`**

Remove line 50:
```kotlin
    private val colorLine      = Color(1f, 0.55f, 0f, 1f)
```

Replace the entire `drawCurrentLine` method (lines 238–256):

```kotlin
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
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew.bat :core:compileKotlin
```

Expected: BUILD SUCCESSFUL. If `colorLine` is still referenced anywhere, the compiler will catch it.

- [ ] **Step 3: Run full test suite**

```bash
./gradlew.bat :core:test
```

Expected: BUILD SUCCESSFUL, all tests PASS.

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/game/screens/GameScreen.kt
git commit -m "fix: trail renders as thin cyan polyline matching territory border style"
```

---

## Self-Review

### Spec coverage

| Requirement | Task |
|---|---|
| Player diamond bisected by perimeter line | Task 1 (playerRenderPos snaps to border-line coordinate) |
| Trail color = territory border color | Task 3 (hardcoded `0f, 0.8f, 0.8f, 0.5f` same as drawTerritoryBorder) |
| Trail thickness = territory border thickness | Task 3 (ShapeType.Line — same as drawTerritoryBorder) |
| Trail follows player path (option A polyline) | Task 3 (segments between consecutive GridPoints) |
| Trail start snaps to perimeter boundary | Task 2+3 (gridPointRenderPos snaps col 0/43 / row 0/65) |
| Trail interior points at cell centres | Task 2+3 (gridPointRenderPos interior branch) |

### Placeholder scan

No TBDs, TODOs, or "similar to task N" references. All code in full.

### Type consistency

- `RenderPos` defined in `PlayerRenderUtils.kt`, destructured as `(x, y)` in both `drawPlayer` and `drawCurrentLine` — consistent.
- `gridPointRenderPos` returns `RenderPos`, called in `drawCurrentLine` via destructuring — consistent.
- `GridPoint` imported in `PlayerRenderUtils.kt` and already imported in `GameScreen.kt` (line 24) — consistent.
- `colorLine` removed from field and from all usages in `drawCurrentLine` — no dangling references.
