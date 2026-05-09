# Game Improvements Design — 2026-05-09

## Scope

Six targeted changes to fix core gameplay bugs and improve game feel, without cross-cutting refactor.

## Decisions

| Topic | Choice |
|-------|--------|
| Cell size | 5×5px (half-cell), grid 88×132 |
| Play area | 440×660 (unchanged), 20px margins stay |
| Player cursor | Glowing dot (3-layer circles), cyan |
| Explosion effect | White flash → color ring expand |
| Respawn zone | Current CONQUERED border (not initial) |
| Spawn guard | Nothing spawns in CONQUERED cells |

---

## Section 1: Cell-Size Migration

`GameConstants` changes:
- `CELL_SIZE = 5f` (was 10f)
- `GRID_COLS = 88` (was 44)
- `GRID_ROWS = 132` (was 66)
- Remove `PLAYER_DIAMOND_HALF`

Grid: 88×132 = 11,616 cells. Flood-fill BFS is iterative — no stack overflow risk.  
All coordinate math in `MovementSystem.toGridPoint()` and `drawBackground()` auto-scales.  
Background texture maps proportionally via `texW / GRID_COLS` — no render logic change.

---

## Section 2: Player Cursor (Glowing Dot)

Replace 4-layer diamond with 3-layer glowing dot in `GameScreen.drawPlayer()`:

| Layer | Shape | Radius | Alpha |
|-------|-------|--------|-------|
| Outer glow | filled circle | 8f | 0.12f |
| Mid glow | filled circle | 5f | 0.30f |
| Core dot | filled circle | 2.5f + pulse×1.0f | 1.0f |

Pulse: `(System.currentTimeMillis() % 800) / 800f`  
Color: `colorPlayer = Color(0f, 1f, 1f, 1f)` (cyan, unchanged)  
Direction indicator: short line from dot center toward `dirX/dirY` when `pc.moving` (unchanged)

Remove: `drawDiamond()` helper, `PLAYER_DIAMOND_HALF` constant.  
Unchanged: `PlayerRenderUtils.playerRenderPos()` — still positions in screen space, snaps perimeter rows/cols.

---

## Section 3: BorderFinder + Respawn Fix

New file: `core/src/main/kotlin/game/ecs/systems/BorderFinder.kt`

```kotlin
object BorderFinder {
    fun currentBorderCells(cells: Array<Array<CellType>>, cols: Int, rows: Int): List<GridPoint>
    fun randomBorderCell(cells: Array<Array<CellType>>, cols: Int, rows: Int): GridPoint?
}
```

`currentBorderCells`: returns all CONQUERED cells adjacent to ≥1 FREE cell (4-directional scan).  
`randomBorderCell`: random pick from above; returns null only if field 100% conquered.

`EcsWorld.loseLife()` fix:
```kotlin
// BEFORE
player.position.x = FIELD_OFFSET_X + PLAY_WIDTH / 2f
player.position.y = FIELD_OFFSET_Y

// AFTER
val respawn = BorderFinder.randomBorderCell(territory.cells, territory.cols, territory.rows)
    ?: GridPoint(territory.cols / 2, 0)
player.position.x = FIELD_OFFSET_X + respawn.col * CELL_SIZE
player.position.y = FIELD_OFFSET_Y + respawn.row * CELL_SIZE
```

`lastSafeGrid` reset and `pc.moving = false` unchanged.

**Bug this fixes:** player spawning into FREE zone → can't move → game freeze/crash.

**Tests:** `BorderFinder` is a pure function on a cell array — fully unit-testable.

---

## Section 4: Spawn Zone Guards

Rule: nothing spawns in CONQUERED (revealed) cells.

| Spawn site | Current state | Fix |
|------------|---------------|-----|
| Enemy initial spawn (`init()`) | Arithmetic — may hit CONQUERED frame | Replace with `territory.randomFreeCell()` |
| Enemy trapped-respawn | `randomFreeCell()` ✓ | None |
| Dynamic enemy spawn (`DifficultySystem`) | Arithmetic | Override x/y with `territory.randomFreeCell()` in `EcsWorld.update()` after receiving `SpawnedEnemy` |
| Powerup spawn (`PowerupSystem`) | Receives `territory.cells` — verify FREE guard | Add FREE guard if missing |
| Player respawn | Hardcoded bottom | `BorderFinder.randomBorderCell()` (Section 3) |

---

## Section 5: ExplosionSystem

New file: `core/src/main/kotlin/game/ecs/systems/ExplosionSystem.kt`

```kotlin
data class Explosion(val x: Float, val y: Float, val color: Color, var elapsed: Float = 0f) {
    val duration = 0.6f
    val progress get() = elapsed / duration  // 0..1
}

class ExplosionSystem {
    private val _active = mutableListOf<Explosion>()
    val active: List<Explosion> get() = _active

    fun spawn(x: Float, y: Float, color: Color) { _active.add(Explosion(x, y, color)) }
    fun update(delta: Float) {
        _active.forEach { it.elapsed += delta }
        _active.removeAll { it.progress >= 1f }
    }
}
```

### Render phases (`GameScreen.drawExplosions()`)

| Progress | Effect |
|----------|--------|
| 0–0.25 | Filled white circle, radius 4f→20f, alpha 0.9f→0f (flash) |
| 0.15–1.0 | Stroke circle (ring), radius 8f→40f, alpha 0.8f→0f, color = entity color |

Phases overlap at 0.15–0.25 for smooth flash→ring transition.

### Integration

`EcsWorld`:
- Add `val explosionSys = ExplosionSystem()`
- After `closeLine` returns `CloseResult.Success`: for each enemy/powerup whose grid point is in `conqueredCells`, call `explosionSys.spawn(pos.x, pos.y, entityColor)` + apply score bonus
  - Entity colors: SPIDER=red(1,0.15,0.15), COCKROACH=brown(0.7,0.45,0.1), WASP=yellow(1,0.9,0), SNAIL=green(0.2,1,0.4), powerup=gold(1,0.8,0) — same values as `GameScreen` tint colors
- `explosionSys.update(delta)` called in `EcsWorld.update()`

`GameScreen.render()`:
- Add `drawExplosions()` after `drawEnemies()`, before `drawPlayer()`

### Score bonus

- Trapped enemy in conquered area: +1000 pts (unchanged)
- Powerup inside conquered area at capture: +200 pts (matches collect bonus)

---

## Files Changed

| File | Change |
|------|--------|
| `GameConstants.kt` | CELL_SIZE=5, GRID_COLS=88, GRID_ROWS=132, remove PLAYER_DIAMOND_HALF |
| `BorderFinder.kt` | **new** — current border cell finder |
| `ExplosionSystem.kt` | **new** — flash+ring explosion lifecycle |
| `EcsWorld.kt` | loseLife fix, explosion triggers, spawn guards |
| `GameScreen.kt` | drawPlayer (dot), drawExplosions, remove drawDiamond |
| `PowerupSystem.kt` | FREE-only spawn guard (verify + fix if needed) |
| `DifficultySystem.kt` | spawn position override via randomFreeCell |
