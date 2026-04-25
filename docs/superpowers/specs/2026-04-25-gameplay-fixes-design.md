# Girls AI Panic — Gameplay Fixes Design Spec
**Date:** 2026-04-25  
**Scope:** 3 gameplay fixes — movement, background rendering, perimeter constraint

---

## 1. Fix: Movimento Giocatore (4 direzioni discrete, continuo)

### Problema
`PlayerComponent` usa `targetX/targetY` (punto di arrivo). Player si ferma al target, devia, non raggiunge autonomamente i bordi.

### Soluzione

**Input model:** touch → angolo verso punto toccato → snap a 4 direzioni.
- `|dx| >= |dy|` → orizzontale (`dirX = ±1, dirY = 0`)
- `|dx| < |dy|` → verticale (`dirX = 0, dirY = ±1`)
- Nuovo touch in qualsiasi momento aggiorna direzione
- `moving = false` solo su ostacolo o fuori campo

**PlayerComponent** — campi sostituiti:
```
targetX: Float  → dirX: Float   (valori: -1, 0, +1)
targetY: Float  → dirY: Float   (valori: -1, 0, +1)
              aggiunto: moving: Boolean = false
```

**MovementSystem.movePlayer()** — nuova firma:
```kotlin
fun movePlayer(pos: FloatArray, dirX: Float, dirY: Float,
               speed: Float, delta: Float,
               grid: Array<BooleanArray>, cols: Int, rows: Int,
               cellSize: Float): Boolean  // returns false se bloccato
```

Logica per frame:
1. Calcola `nextX = pos[0] + dirX * speed * delta`, `nextY = pos[1] + dirY * speed * delta`
2. Clamp a bordo campo → se fuori bounds: `moving = false`, return false
3. Converti `nextX/nextY` in `GridPoint(nextCol, nextRow)`
4. Se cella = interno conquistato (tutti 4 vicini conquistati): `moving = false`, return false
5. Altrimenti: aggiorna pos, return true

**EcsWorld.update()** — sostituzione blocco movimento player:
```kotlin
if (pc.moving) {
    val blocked = !movement.movePlayer(posArr, pc.dirX, pc.dirY,
        effectiveSpeed, delta, territory.grid,
        GameConstants.GRID_COLS, GameConstants.GRID_ROWS, GameConstants.CELL_SIZE)
    if (blocked) pc.moving = false
}
```

**GameScreen — input processor:**
```kotlin
override fun touchDown(screenX, screenY, pointer, button): Boolean {
    val wc = viewport.unproject(Vector3(screenX.f, screenY.f, 0f))
    val dx = wc.x - pos.x; val dy = wc.y - pos.y
    if (abs(dx) >= abs(dy)) { pc.dirX = sign(dx); pc.dirY = 0f }
    else                    { pc.dirX = 0f;        pc.dirY = sign(dy) }
    pc.moving = true
    return true
}
// touchDragged: stesso blocco (aggiorna direzione in corsa)
```

---

## 2. Fix: Background — Flip Verticale + Dimensione Corretta

### Problema
`batch.draw` usa `srcY = r * cs` (da top texture). Asse Y schermo va verso l'alto, asse Y texture verso il basso → immagine capovolta. Dimensione: cell draw esatto ma texture generata a proporzioni leggermente errate.

### Soluzione

In `GameScreen.drawBackground()`, per ogni cella conquistata:

```kotlin
batch.draw(
    tex,
    c * cs, r * cs,          // screen position (bottom-left of cell)
    cs, cs,                  // screen size
    (c * cs).toInt(),        // srcX
    tex.height - (r + 1) * cs.toInt(),  // srcY: flip Y axis
    cs.toInt(), cs.toInt(),  // src size
    false, false
)
```

`srcY = tex.height - (r+1)*cs`:
- `r=0` (bottom screen) → `srcY = 800 - cs` = fondo texture ✓
- `r=GRID_ROWS-1` (top screen) → `srcY = 0` = top texture ✓

**Nessun cambiamento** alle texture stesse. Se `tex.height != 800` (texture leggermente diversa), il calcolo rimane corretto perché usa `tex.height` reale.

---

## 3. Fix: Perimetro Dinamico

### Problema
Il player deve muoversi solo sul perimetro esterno dell'area conquistata (celle conquistate adiacenti ad almeno una cella libera). Non deve attraversare l'interno conquistato.

### Soluzione

`TerritorySystem` — aggiunta:
```kotlin
fun isOnPerimeter(pt: GridPoint): Boolean {
    if (!isOnSafeZone(pt)) return false
    val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
    return dirs.any { (dc, dr) ->
        val nc = pt.col + dc; val nr = pt.row + dr
        (nc !in 0 until cols || nr !in 0 until rows) ||  // bordo campo = perimetro
        !grid[nc][nr]                                      // vicino libero = perimetro
    }
}
```

`MovementSystem.movePlayer()` usa `isInteriorConquered(nextCell, grid)`:
```kotlin
fun isInteriorConquered(pt: GridPoint, grid: Array<BooleanArray>, cols: Int, rows: Int): Boolean {
    if (!grid[pt.col][pt.row]) return false  // cella libera, non interno
    val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
    return dirs.all { (dc, dr) ->
        val nc = pt.col + dc; val nr = pt.row + dr
        nc in 0 until cols && nr in 0 until rows && grid[nc][nr]
    }
}
```

Se `isInteriorConquered(nextCell)` → blocca movimento.

**Bordi campo:** le celle del bordo hanno vicini fuori campo → sempre `isOnPerimeter = true`. Player può sempre muoversi lungo i 4 lati del campo.

**Dopo closeLine():** il perimetro si aggiorna automaticamente — `grid[][]` viene aggiornata da `TerritorySystem`, il check successivo riflette le nuove celle conquistate.

---

## 4. File Modificati

| File | Modifica |
|------|----------|
| `Component.kt` | `PlayerComponent`: `targetX/targetY` → `dirX/dirY/moving` |
| `MovementSystem.kt` | Nuova `movePlayer()`, helper `isInteriorConquered()` |
| `TerritorySystem.kt` | Aggiunta `isOnPerimeter()` |
| `EcsWorld.kt` | Blocco movimento player aggiornato, input direction handling |
| `GameScreen.kt` | Input processor aggiornato, `drawBackground()` srcY fix |

---

## 5. Invarianti Preservati

- `TerritorySystem.closeLine()` invariata (flood fill da nemici)
- Collisione nemici invariata
- Power-up system invariato
- HUD invariato
- Tutte le schermate non-GameScreen invariate
