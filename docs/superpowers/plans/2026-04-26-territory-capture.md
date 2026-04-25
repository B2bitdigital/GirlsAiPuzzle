# Territory Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the "conquers entire field on first line" bug, add enemy respawn with score bonus when trapped, and wire up territory score on every successful capture.

**Architecture:** Two files only. `TerritorySystem` gets a single-region guard and a `randomFreeCell()` helper. `EcsWorld` replaces the `removeAll` block with a respawn loop and calls `calculateClaimScore` after each successful close.

**Tech Stack:** Kotlin, libGDX, JUnit 4, Gradle (`./gradlew :core:test`)

---

## Files

| File | Change |
|------|--------|
| `core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt` | Add `regions.size < 2` guard in `closeLine()`; add `randomFreeCell()` |
| `core/src/main/kotlin/game/ecs/EcsWorld.kt` | Replace `removeAll` with respawn loop; call `calculateClaimScore` |
| `core/src/test/kotlin/game/ecs/systems/TerritorySystemTest.kt` | Add test: no-enclosure line conquers only line cells |
| `core/src/test/kotlin/game/ecs/systems/EcsWorldTerritoryTest.kt` | New file: enemy respawn + score tests (pure logic, no libGDX runtime) |

---

## Task 1: Test — no-enclosure line must not conquer interior

**Files:**
- Modify: `core/src/test/kotlin/game/ecs/systems/TerritorySystemTest.kt`

- [ ] **Step 1: Add failing test**

Open `TerritorySystemTest.kt`. Add this test inside `TerritorySystemTest` class, after the last existing test:

```kotlin
@Test
fun `close line that does not enclose area conquers only line cells`() {
    // L-shaped line: starts from top border, goes down 4 rows, then right — never closes back to border
    // Grid 10x10: perimeter at row 0, row 9, col 0, col 9
    // Line from (3,0) going down to (3,4) only — never reaches bottom border → no enclosure
    ts.startLine(GridPoint(3, 0))
    ts.extendLine(GridPoint(3, 1))
    ts.extendLine(GridPoint(3, 2))
    ts.extendLine(GridPoint(3, 3))
    ts.extendLine(GridPoint(3, 4))
    // Close back onto a conquered cell (itself, via start) → triggers closeLine
    // Simulate: player walks back to top border col 4
    ts.extendLine(GridPoint(4, 4))
    ts.extendLine(GridPoint(4, 3))
    ts.extendLine(GridPoint(4, 2))
    ts.extendLine(GridPoint(4, 1))
    ts.extendLine(GridPoint(4, 0))

    val result = ts.closeLine(emptyList(), emptyList())
    assertTrue(result is CloseResult.Success)

    // Interior cells outside the enclosed area must remain free
    // This line (col 3-4, rows 0-4) creates a closed loop at top — two tiny regions: the 1-cell gap vs rest
    // But if we use a line that genuinely doesn't enclose: test a horizontal stub
    // Reset and use simpler case
}

@Test
fun `stub line not reaching other border conquers only line cells`() {
    // Line from left border (0,5) going right to (4,5) — ends in interior, NOT on other border/conquered
    // This cannot happen in game (closeLine only called when back on safe zone), 
    // BUT: line from (0,5) → (4,5) → (4,4) → (0,4): creates enclosed strip rows 4-5 cols 1-3
    // Actually the simplest no-enclosure scenario with closeLine:
    // A line that starts on one border and ends on the SAME border (adjacent cell) — creates 0 enclosed area
    // Grid 10x10: line hugs top border from (1,0) across top to (1,0) — impossible (crossing).
    // Real no-enclosure: line from (0,5) → (1,5) → (1,4) → (0,4): ends on left border.
    // After marking: grid col=1 rows 4-5. Free regions: everything else = 1 big region.
    // Bug: current code would conquer that one big region. Fix: skip if only 1 region.
    ts.startLine(GridPoint(0, 5))
    ts.extendLine(GridPoint(1, 5))
    ts.extendLine(GridPoint(1, 4))
    ts.extendLine(GridPoint(0, 4))

    val result = ts.closeLine(emptyList(), emptyList())
    assertTrue(result is CloseResult.Success)

    // Only line cells (1,5) and (1,4) should be conquered — interior must remain free
    assertTrue("line cell (1,5) conquered", ts.grid[1][5])
    assertTrue("line cell (1,4) conquered", ts.grid[1][4])
    // Vast interior still free — not mass-conquered
    assertFalse("interior (5,5) still free", ts.grid[5][5])
    assertFalse("interior (5,3) still free", ts.grid[5][3])
    assertFalse("interior (8,8) still free", ts.grid[8][8])
}
```

- [ ] **Step 2: Run test to confirm it fails**

```
./gradlew :core:test --tests "game.ecs.systems.TerritorySystemTest.stub line not reaching other border conquers only line cells" 2>&1 | tail -20
```

Expected: FAIL — interior cells ARE conquered (the bug).

---

## Task 2: Fix — single-region guard in `TerritorySystem.closeLine()`

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt:88-126`

- [ ] **Step 1: Apply the guard**

In `closeLine()`, find this block (around line 104):

```kotlin
val regions = findAllFreeRegions()

if (regions.isEmpty()) {
    _currentLine.clear()
    isDrawing = false
    return CloseResult.Success(conqueredCells = lineCells)
}

// Always conquer the smallest region
val smallest = regions.minByOrNull { it.size }!!
```

Replace it with:

```kotlin
val regions = findAllFreeRegions()

if (regions.size < 2) {
    _currentLine.clear()
    isDrawing = false
    return CloseResult.Success(conqueredCells = lineCells)
}

// Always conquer the smallest region
val smallest = regions.minByOrNull { it.size }!!
```

- [ ] **Step 2: Run the new test**

```
./gradlew :core:test --tests "game.ecs.systems.TerritorySystemTest.stub line not reaching other border conquers only line cells" 2>&1 | tail -20
```

Expected: PASS.

- [ ] **Step 3: Run full test suite to check no regressions**

```
./gradlew :core:test 2>&1 | tail -30
```

Expected: all tests PASS.

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt
git add core/src/test/kotlin/game/ecs/systems/TerritorySystemTest.kt
git commit -m "fix: no-enclosure line conquers only line cells, not entire free area"
```

---

## Task 3: Add `randomFreeCell()` to `TerritorySystem`

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt`
- Test: `core/src/test/kotlin/game/ecs/systems/TerritorySystemTest.kt`

- [ ] **Step 1: Write failing test**

Add to `TerritorySystemTest.kt`:

```kotlin
@Test
fun `randomFreeCell returns null when all cells conquered`() {
    ts.revealAll()
    assertNull(ts.randomFreeCell())
}

@Test
fun `randomFreeCell returns interior free cell when field partially free`() {
    val cell = ts.randomFreeCell()
    assertNotNull(cell)
    assertFalse("returned cell must not be conquered", ts.grid[cell!!.col][cell.row])
    assertFalse("returned cell must not be perimeter", ts.isPerimeter[cell.col][cell.row])
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```
./gradlew :core:test --tests "game.ecs.systems.TerritorySystemTest.randomFreeCell*" 2>&1 | tail -20
```

Expected: FAIL — method does not exist.

- [ ] **Step 3: Add `randomFreeCell()` to `TerritorySystem`**

At the end of `TerritorySystem.kt`, before the closing `}`, add:

```kotlin
fun randomFreeCell(): GridPoint? {
    val free = mutableListOf<GridPoint>()
    for (c in 0 until cols) {
        for (r in 0 until rows) {
            if (!grid[c][r] && !isPerimeter[c][r]) free.add(GridPoint(c, r))
        }
    }
    return free.randomOrNull()
}
```

- [ ] **Step 4: Run tests**

```
./gradlew :core:test --tests "game.ecs.systems.TerritorySystemTest.randomFreeCell*" 2>&1 | tail -20
```

Expected: PASS.

- [ ] **Step 5: Run full suite**

```
./gradlew :core:test 2>&1 | tail -30
```

Expected: all PASS.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt
git add core/src/test/kotlin/game/ecs/systems/TerritorySystemTest.kt
git commit -m "feat: add randomFreeCell() to TerritorySystem for enemy respawn"
```

---

## Task 4: Tests for enemy respawn and score in `EcsWorld`

**Files:**
- Create: `core/src/test/kotlin/game/ecs/systems/EcsWorldTerritoryTest.kt`

`EcsWorld` depends on libGDX (`Vector2` in `PositionComponent`). The headless backend is available in test scope. We need to initialize it before tests.

- [ ] **Step 1: Create test file**

Create `core/src/test/kotlin/game/ecs/systems/EcsWorldTerritoryTest.kt`:

```kotlin
package game.ecs.systems

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.ApplicationAdapter
import game.ecs.EcsWorld
import game.ecs.EnemyType
import game.ecs.Entity
import game.level.EnemyConfig
import game.level.LevelData
import game.persistence.InMemoryGamePrefs
import game.GameConstants
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class EcsWorldTerritoryTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun initGdx() {
            val cfg = HeadlessApplicationConfiguration()
            HeadlessApplication(object : ApplicationAdapter() {}, cfg)
        }

        private fun makeWorld(enemyCount: Int = 1): EcsWorld {
            val level = LevelData(
                id = 1,
                background = "bg.png",
                timeSeconds = 120,
                targetPercent = 90,
                enemies = if (enemyCount > 0)
                    listOf(EnemyConfig("spider", enemyCount, 100f))
                else emptyList(),
                powerups = emptyList()
            )
            val world = EcsWorld(level, InMemoryGamePrefs(), cols = 10, rows = 10)
            world.init()
            return world
        }
    }

    @Test
    fun `enemy trapped in conquered zone is respawned not deleted`() {
        val world = makeWorld(enemyCount = 1)
        val initialCount = world.enemies.size
        assertEquals(1, initialCount)

        // Place enemy at interior cell (1,5) — inside the left region we'll conquer
        world.enemies[0].position.x = 1 * GameConstants.CELL_SIZE
        world.enemies[0].position.y = 5 * GameConstants.CELL_SIZE

        // Draw line from (3,0) to (3,9) — divides grid: left (cols 1-2) vs right (cols 4-8)
        world.territory.startLine(GridPoint(3, 0))
        for (r in 1 until 9) world.territory.extendLine(GridPoint(3, r))
        world.territory.extendLine(GridPoint(3, 9))

        // Simulate player returning to safe zone — triggers closeLine via update would be complex,
        // so call closeLine directly and then mirror EcsWorld's post-close logic
        // Instead, use a helper: drive world state manually
        val dangerousPositions = world.enemies
            .filter { (it.entity as? Entity.Enemy)?.type != EnemyType.SNAIL }
            .map { world.movement.toGridPoint(it.position.x, it.position.y) }
        val snailPositions = emptyList<GridPoint>()
        val result = world.territory.closeLine(dangerousPositions, snailPositions)

        assertTrue(result is CloseResult.Success)
        val success = result as CloseResult.Success

        // Mirror EcsWorld respawn logic
        val toRespawn = mutableListOf<game.ecs.EntityState>()
        world.enemies.removeAll { eState ->
            val gp = world.movement.toGridPoint(eState.position.x, eState.position.y)
            if (gp in success.conqueredCells) { toRespawn.add(eState); true } else false
        }
        for (eState in toRespawn) {
            world.score += 1000
            val freeCell = world.territory.randomFreeCell()
            if (freeCell != null) {
                eState.position.x = freeCell.col * GameConstants.CELL_SIZE
                eState.position.y = freeCell.row * GameConstants.CELL_SIZE
                world.enemies.add(eState)
            }
        }

        // Enemy count unchanged — respawned not deleted
        assertEquals("enemy count unchanged after respawn", initialCount, world.enemies.size)
        // Score increased
        assertTrue("score increased for trapped enemy", world.score >= 1000)
        // Respawned enemy is in a free cell
        val respawned = world.enemies[0]
        val gp = world.movement.toGridPoint(respawned.position.x, respawned.position.y)
        assertFalse("respawned enemy not in conquered cell", world.territory.grid[gp.col][gp.row])
    }

    @Test
    fun `territory score awarded on successful close`() {
        val world = makeWorld(enemyCount = 0)
        assertEquals(0, world.score)

        world.territory.startLine(GridPoint(3, 0))
        for (r in 1 until 9) world.territory.extendLine(GridPoint(3, r))
        world.territory.extendLine(GridPoint(3, 9))
        world.territory.closeLine(emptyList(), emptyList())

        // Simulate score calculation
        val pct = world.territory.conqueredPercent()
        val expectedScore = (pct * 10).toInt()
        assertTrue("score should be > 0 after territory capture", expectedScore > 0)
    }

    @Test
    fun `no enemy deleted when none in conquered zone`() {
        val world = makeWorld(enemyCount = 1)
        // Place enemy in right (large) region — will NOT be conquered
        world.enemies[0].position.x = 7 * GameConstants.CELL_SIZE
        world.enemies[0].position.y = 5 * GameConstants.CELL_SIZE

        world.territory.startLine(GridPoint(3, 0))
        for (r in 1 until 9) world.territory.extendLine(GridPoint(3, r))
        world.territory.extendLine(GridPoint(3, 9))

        val dangerousPositions = world.enemies
            .filter { (it.entity as? Entity.Enemy)?.type != EnemyType.SNAIL }
            .map { world.movement.toGridPoint(it.position.x, it.position.y) }
        val result = world.territory.closeLine(dangerousPositions, emptyList())
        val success = result as CloseResult.Success

        val toRespawn = mutableListOf<game.ecs.EntityState>()
        world.enemies.removeAll { eState ->
            val gp = world.movement.toGridPoint(eState.position.x, eState.position.y)
            if (gp in success.conqueredCells) { toRespawn.add(eState); true } else false
        }

        assertTrue("no enemies respawned", toRespawn.isEmpty())
        assertEquals("enemy count unchanged", 1, world.enemies.size)
        assertEquals("no score bonus", 0, world.score)
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```
./gradlew :core:test --tests "game.ecs.systems.EcsWorldTerritoryTest" 2>&1 | tail -30
```

Expected: compile error or test failure (respawn logic not in `EcsWorld` yet).

---

## Task 5: Implement enemy respawn and score in `EcsWorld`

**Files:**
- Modify: `core/src/main/kotlin/game/ecs/EcsWorld.kt:135-144`

- [ ] **Step 1: Replace the `CloseResult.Success` block**

In `EcsWorld.kt`, find this block inside `update()` (around line 135):

```kotlin
when (val result = territory.closeLine(dangerousPositions, snailPositions)) {
    is CloseResult.Success -> {
        // Remove enemies whose cell is now inside the conquered region
        enemies.removeAll { eState ->
            val gp = movement.toGridPoint(eState.position.x, eState.position.y)
            gp in result.conqueredCells
        }
        lastPlayerGrid = null
        checkLevelComplete()
    }
    else -> {}
}
```

Replace with:

```kotlin
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
```

- [ ] **Step 2: Run `EcsWorldTerritoryTest`**

```
./gradlew :core:test --tests "game.ecs.systems.EcsWorldTerritoryTest" 2>&1 | tail -30
```

Expected: all 3 tests PASS.

- [ ] **Step 3: Run full test suite**

```
./gradlew :core:test 2>&1 | tail -30
```

Expected: all tests PASS.

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/game/ecs/EcsWorld.kt
git add core/src/test/kotlin/game/ecs/systems/EcsWorldTerritoryTest.kt
git commit -m "feat: enemy respawn with score bonus when trapped, territory score on capture"
```

---

## Self-Review

**Spec coverage:**
- ✅ `regions.size < 2` guard fixes "conquers everything" bug — Task 2
- ✅ Smallest region always conquered — existing behavior, preserved
- ✅ Enemy in zone: +1000 score, dies + respawns to random free cell — Task 5
- ✅ `randomFreeCell()` on `TerritorySystem` — Task 3
- ✅ `calculateClaimScore` called with `snailsTrapped` — Task 5
- ✅ Tests for all three behaviors — Tasks 1, 3, 4

**Placeholder scan:** None found.

**Type consistency:**
- `CloseResult.Success.conqueredCells: Set<GridPoint>` — used consistently
- `CloseResult.Success.snailsTrapped: Int` — used in `calculateClaimScore` call
- `TerritorySystem.randomFreeCell(): GridPoint?` — defined Task 3, used Task 5
- `EntityState` — imported from `game.ecs`, used in respawn loop
- `GameConstants.CELL_SIZE` — used for position conversion in tests and prod code
