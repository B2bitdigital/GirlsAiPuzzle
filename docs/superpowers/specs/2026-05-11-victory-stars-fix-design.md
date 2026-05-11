# Victory Screen Stars Fix

**Date:** 2026-05-11  
**Status:** Approved

## Problem

Stars on the victory screen render as yellow disks instead of recognizable 5-pointed star shapes.

**Root cause:** Glow circles are larger than the star's outer radius. The glow fills the concave notch areas between star points, so the entire star area appears as a solid yellow disk.

Affected locations in `core/src/main/kotlin/game/screens/GameScreen.kt`:
- `drawLevelCompleteOverlay()` — stats overlay panel shown after celebration
- `drawCelebrationPhase()` — 5-second celebration animation

`LevelCompleteScreen.kt` has the same bug but is dead code (never instantiated).

## Approach: Outer-halo-only glow

Move glow circles to radius greater than `outerR`. The glow now exists only outside the star boundary. Concave notch areas hit the dark background directly → star shape is visually distinct.

Rule: `glowR = outerR + 10f`

Also increase star size and use the classic innerR/outerR ≈ 0.38 ratio for well-proportioned points.

## Changes

### `drawLevelCompleteOverlay` (line ~566–578)

| Parameter | Before | After |
|-----------|--------|-------|
| `outerR` | 16f | 22f |
| `innerR` | 6.5f | 8.5f |
| glow `circle` radius | 26f | 32f |
| glow `circle` segments | 20 | 22 |

Star spacing stays at 60f (3 stars at x = W/2 ± 60). Gap between star edges: 60 − 44 = 16px. OK.

### `drawCelebrationPhase` (line ~673–686)

| Parameter | Before | After |
|-----------|--------|-------|
| `outerR` | 17f | 26f |
| `innerR` | 7f | 10f |
| glow `circle` radius | 32f | 36f |
| glow `circle` segments | 22 | 22 |

Star spacing stays at 72f. Gap between star edges: 72 − 52 = 20px. OK.

### `LevelCompleteScreen.kt`

Delete file. Never instantiated; contains the same bug and duplicates logic now in `GameScreen`.

## Non-changes

- `drawStar()` algorithm is correct — no changes needed.
- Glow alpha values unchanged.
- `LevelCompleteScreen` is not referenced anywhere except its own file.

## Success criteria

- Earned stars show 5 distinct points with dark notches between them.
- Glow visible as halo outside star boundary only.
- Unearned stars render as gray star shapes (no glow, same size change applies).
