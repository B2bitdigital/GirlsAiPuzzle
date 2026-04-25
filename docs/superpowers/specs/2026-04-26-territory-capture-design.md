# Territory Capture — Design Spec
**Date:** 2026-04-26

## Problem

Three bugs/missing features in the territory capture system:

1. **Level ends immediately with 100%** when player closes any line, even one that doesn't enclose any area.
2. **Enemy trapping awards no score** and removes enemies permanently instead of respawning them.
3. **Territory capture awards no score** — `calculateClaimScore()` exists but is never called.

---

## Root Cause: Bug #1

In `TerritorySystem.closeLine()`, after marking line cells as conquered, `findAllFreeRegions()` is called. When the drawn line doesn't divide the free area into 2+ separate regions (i.e., no enclosure was formed), the function returns exactly 1 region — the entire remaining free area. The code then conquers it as "smallest," triggering level completion immediately.

---

## Fix 1 — `TerritorySystem.closeLine()` (single-region guard)

**File:** `core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt`

After calling `findAllFreeRegions()`, guard on `regions.size < 2`:

```kotlin
val regions = findAllFreeRegions()
if (regions.size < 2) {
    _currentLine.clear()
    isDrawing = false
    return CloseResult.Success(conqueredCells = lineCells)
}
val smallest = regions.minByOrNull { it.size }!!
smallest.forEach { grid[it.col][it.row] = true }
```

When there is only 1 region, the line formed no enclosure — only the line cells themselves are marked as conquered. No interior area is captured.

---

## Fix 2 — Enemy trapping with respawn (`EcsWorld.update`)

**File:** `core/src/main/kotlin/game/ecs/EcsWorld.kt`

Replace the current `enemies.removeAll` block in the `CloseResult.Success` branch:

```kotlin
is CloseResult.Success -> {
    val toRespawn = mutableListOf<EntityState>()
    enemies.removeAll { eState ->
        val gp = movement.toGridPoint(eState.position.x, eState.position.y)
        if (gp in result.conqueredCells) { toRespawn.add(eState); true }
        else false
    }
    for (eState in toRespawn) {
        score += 1000  // bonus per trapped enemy
        val freeCell = territory.randomFreeCell()
        if (freeCell != null) {
            eState.position.x = freeCell.col * GameConstants.CELL_SIZE
            eState.position.y = freeCell.row * GameConstants.CELL_SIZE
            enemies.add(eState)
        }
        // if no free cell exists, enemy stays dead (field nearly full)
    }
    score += calculateClaimScore(territory.conqueredPercent(), result.snailsTrapped)
    checkLevelComplete()
}
```

**New method on `TerritorySystem`:**

```kotlin
fun randomFreeCell(): GridPoint? {
    val free = (0 until cols).flatMap { c ->
        (0 until rows).mapNotNull { r ->
            if (!grid[c][r] && !isPerimeter[c][r]) GridPoint(c, r) else null
        }
    }
    return free.randomOrNull()
}
```

---

## Fix 3 — Territory score on capture

`calculateClaimScore(pct, snailBonus)` already implemented as:
```kotlin
(pct * 10).toInt() + snailBonus * 500
```

Called after each successful `closeLine`. Snail bonus passed via `result.snailsTrapped`. Enemy bonus (1000 per enemy) added separately in the respawn loop.

---

## Scoring Summary

| Event | Score |
|-------|-------|
| Territory captured | `conqueredPercent() * 10` points |
| Snail trapped | +500 per snail |
| Enemy trapped + respawned | +1000 per enemy |
| Powerup collected | +200 (existing, unchanged) |

---

## Files Changed

| File | Change |
|------|--------|
| `core/src/main/kotlin/game/ecs/systems/TerritorySystem.kt` | Add `regions.size < 2` guard; add `randomFreeCell()` |
| `core/src/main/kotlin/game/ecs/EcsWorld.kt` | Replace `removeAll` with respawn loop; call `calculateClaimScore` |

---

## Out of Scope

- Visual feedback for enemy respawn (flash, animation)
- UI showing per-capture score popup
- Changing enemy type or speed on respawn
