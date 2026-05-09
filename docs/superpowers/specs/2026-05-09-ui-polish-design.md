# UI Polish — Design Spec
**Date:** 2026-05-09  
**Scope:** GameScreen HUD bar spacing · GameOver layout overlap · Font system (FreeType)

---

## 1. Font System

### Problem
All screens use `BitmapFont()` (LibGDX default 15px Arial) scaled dynamically via `font.data.setScale(X)`. At scales ≥1.5 the texture resampling produces visible pixelation, reducing readability.

### Solution
Replace default bitmap font with FreeType-generated fonts from **Orbitron-Regular.ttf** (geometric sans-serif, Apache 2.0, fits game's cyberpunk aesthetic).

### New dependency
Add to both `core/build.gradle.kts` and `android/build.gradle.kts`:
```
gdx-freetype:1.12.1        (core)
gdx-freetype-platform:1.12.1:natives-desktop   (core testRuntimeOnly)
gdx-freetype-platform:1.12.1:natives-armeabi-v7a / arm64-v8a / x86 / x86_64  (android)
```

### Asset
`android/assets/fonts/Orbitron-Regular.ttf`

### Fonts.kt singleton
Location: `core/src/main/kotlin/game/screens/Fonts.kt`

```kotlin
object Fonts {
    lateinit var xs: BitmapFont   // 12px — footer labels, small UI text
    lateinit var sm: BitmapFont   // 16px — HUD labels, secondary text
    lateinit var md: BitmapFont   // 22px — HUD values, card labels
    lateinit var lg: BitmapFont   // 30px — HUD large values, button text
    lateinit var xl: BitmapFont   // 40px — overlay titles, star characters
    lateinit var xxl: BitmapFont  // 72px — GAME OVER title

    fun load()   // generates all fonts via FreeTypeFontGenerator
    fun dispose() // disposes all BitmapFont instances and the generator
}
```

`GirlsPanicGame.create()` calls `Fonts.load()`.  
`GirlsPanicGame.dispose()` calls `Fonts.dispose()`.

### Screen changes
All screens (`GameScreen`, `GameOverScreen`, `MenuScreen`, `LevelSelectScreen`, `LevelCompleteScreen`) drop their `private val font = BitmapFont()` field and all `font.data.setScale(X)` calls. Each draw call uses the appropriate `Fonts.*` size directly.

Scale-to-size mapping:
| Old scale | New font |
|-----------|----------|
| 0.75–0.85 | `Fonts.xs` |
| 0.9–1.1   | `Fonts.sm` |
| 1.2–1.5   | `Fonts.md` |
| 1.9–2.2   | `Fonts.lg` |
| 2.4–2.8   | `Fonts.xl` |
| 5.0       | `Fonts.xxl` |

Each screen retains its own `GlyphLayout` for measurement.

---

## 2. HUD Progress Bar Spacing

### Problem
`drawHUD()` in `GameScreen` draws the score at `y = H - 38f` (scale 1.3) and the segmented bar at `barY = H - hudH + 6f`. They overlap by ~10px with no visual separation.

### Solution
Three targeted changes in `drawHUD()`:

1. **Score**: scale → `Fonts.md` (22px), y → `H - 34f`
2. **Bar**: `barY = H - hudH + 2f`, `barH = 10f` (was 8f, slightly taller)
3. **Separator line**: draw a 1px horizontal line at `y = H - hudH + 14f`, full width, color `(0.18f, 0.18f, 0.18f, 1f)` — explicit visual divider between stats zone and bar zone

Result: ~10px clear gap between score bottom and bar top. No changes to `HUD_HEIGHT`, `GRID_ROWS`, or gameplay constants.

---

## 3. GameOver Screen Layout

### Problem
Stat cards, buttons, and footer are positioned relative to `cardY = titleY - 40f - cardH`. At this value (550f) the top of the cards (650f) overlaps with the bottom of the "OVER" text (~567f), covering part of the title.

### Solution
Single constant change: increase the gap between title and cards from 40px to 140px.

```
cardY = titleY - 140f - cardH   // was: titleY - 40f - cardH
```

All dependent positions cascade automatically (no other changes needed):
- `btnRetryY = cardY - cardPad - btnH`  → 359f (was 459f)
- `btnMenuY  = btnRetryY - cardPad - btnH` → 268f (was 368f)
- `segY      = btnMenuY - 24f`           → 244f (was 344f)

GAME OVER title, header, and bottom hardware info box are unchanged. Footer lands at y=244, 200px above the bottom info box (y=0–44).

---

## Files Changed

| File | Change |
|------|--------|
| `core/build.gradle.kts` | add gdx-freetype deps |
| `android/build.gradle.kts` | add gdx-freetype native deps |
| `android/assets/fonts/Orbitron-Regular.ttf` | new asset |
| `core/.../screens/Fonts.kt` | new singleton |
| `core/.../GirlsPanicGame.kt` | call Fonts.load() / Fonts.dispose() |
| `core/.../screens/GameScreen.kt` | use Fonts.*, fix HUD bar spacing |
| `core/.../screens/GameOverScreen.kt` | use Fonts.*, fix cardY |
| `core/.../screens/MenuScreen.kt` | use Fonts.* |
| `core/.../screens/LevelSelectScreen.kt` | use Fonts.* |
| `core/.../screens/LevelCompleteScreen.kt` | use Fonts.* |

---

## Out of Scope
- No gameplay changes
- No HUD_HEIGHT or GRID_ROWS changes
- No new screen layouts beyond the three fixes above
- LevelCompleteScreen overlay in GameScreen uses same Fonts.* substitution, no layout change
