# UI Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the pixelated default BitmapFont with FreeType/Orbitron across all screens, fix the HUD progress bar spacing, and shift GameOver UI elements below the GAME OVER title.

**Architecture:** A new `Fonts` singleton generates crisp `BitmapFont` instances at fixed pixel sizes via `FreeTypeFontGenerator`. `GirlsPanicGame` owns the lifecycle (load/dispose). All five screens drop their `BitmapFont()` field and reference `Fonts.*` directly. HUD and GameOver layout constants are adjusted in-place.

**Tech Stack:** LibGDX 1.12.1, gdx-freetype 1.12.1, Kotlin, Orbitron-Regular.ttf (Google Fonts, Apache 2.0)

---

## File Map

| File | Action |
|------|--------|
| `core/build.gradle.kts` | add gdx-freetype dep + test native |
| `android/build.gradle.kts` | add gdx-freetype impl + 4 native configs |
| `android/assets/fonts/Orbitron-Regular.ttf` | new asset (manual download) |
| `core/src/main/kotlin/game/screens/Fonts.kt` | new — singleton, load/dispose |
| `core/src/main/kotlin/game/GirlsPanicGame.kt` | call Fonts.load() / Fonts.dispose() |
| `core/src/main/kotlin/game/screens/GameScreen.kt` | Fonts.* + HUD bar fix |
| `core/src/main/kotlin/game/screens/GameOverScreen.kt` | Fonts.* + cardY fix |
| `core/src/main/kotlin/game/screens/MenuScreen.kt` | Fonts.* |
| `core/src/main/kotlin/game/screens/LevelSelectScreen.kt` | Fonts.* |
| `core/src/main/kotlin/game/screens/LevelCompleteScreen.kt` | Fonts.* |

### Font size mapping (old scale → new Fonts.*)
| Old `font.data.setScale(X)` | Fonts.* | px |
|---|---|---|
| 0.75, 0.85 | `Fonts.xs` | 12 |
| 0.9, 1.0, 1.1 | `Fonts.sm` | 16 |
| 1.2, 1.3, 1.4, 1.5, 1.6, 1.8 | `Fonts.md` | 22 |
| 1.9, 2.0, 2.2 | `Fonts.lg` | 30 |
| 2.4, 2.8, 4.0 | `Fonts.xl` | 40 |
| 5.0 | `Fonts.xxl` | 72 |

---

## Task 1: Add FreeType dependencies

**Files:**
- Modify: `core/build.gradle.kts`
- Modify: `android/build.gradle.kts`

- [ ] **Step 1: Update core/build.gradle.kts**

Replace the entire `dependencies` block:

```kotlin
val gdxVersion = "1.12.1"

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
    testImplementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    testRuntimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}
```

- [ ] **Step 2: Update android/build.gradle.kts**

Replace the entire `dependencies` block:

```kotlin
dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
    implementation("com.google.android.gms:play-services-ads:23.0.0")
}
```

- [ ] **Step 3: Sync and verify build compiles**

```bash
./gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL (no source changes yet, just dep resolution)

- [ ] **Step 4: Commit**

```bash
git add core/build.gradle.kts android/build.gradle.kts
git commit -m "build: add gdx-freetype dependency for crisp font rendering"
```

---

## Task 2: Add Orbitron font asset

**Files:**
- Create: `android/assets/fonts/Orbitron-Regular.ttf`

- [ ] **Step 1: Download Orbitron-Regular.ttf**

Go to https://fonts.google.com/specimen/Orbitron, click "Download family", unzip, take `Orbitron-Regular.ttf`.

- [ ] **Step 2: Place font in assets**

```bash
mkdir -p android/assets/fonts
cp /path/to/Orbitron-Regular.ttf android/assets/fonts/Orbitron-Regular.ttf
```

- [ ] **Step 3: Commit**

```bash
git add android/assets/fonts/Orbitron-Regular.ttf
git commit -m "assets: add Orbitron-Regular.ttf for FreeType font rendering"
```

---

## Task 3: Create Fonts singleton + wire into GirlsPanicGame

**Files:**
- Create: `core/src/main/kotlin/game/screens/Fonts.kt`
- Modify: `core/src/main/kotlin/game/GirlsPanicGame.kt`

> No unit-testable logic here — font generation requires a running LibGDX context. Visual verification happens in Task 4.

- [ ] **Step 1: Create Fonts.kt**

```kotlin
package game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

object Fonts {
    lateinit var xs: BitmapFont    // 12px — footer labels, small UI text
    lateinit var sm: BitmapFont    // 16px — HUD labels, secondary text
    lateinit var md: BitmapFont    // 22px — HUD values, card labels
    lateinit var lg: BitmapFont    // 30px — HUD large values, button text
    lateinit var xl: BitmapFont    // 40px — overlay titles, star characters
    lateinit var xxl: BitmapFont   // 72px — GAME OVER title

    fun load() {
        val gen = FreeTypeFontGenerator(Gdx.files.internal("fonts/Orbitron-Regular.ttf"))
        fun param(size: Int) = FreeTypeFontParameter().apply { this.size = size }
        xs  = gen.generateFont(param(12))
        sm  = gen.generateFont(param(16))
        md  = gen.generateFont(param(22))
        lg  = gen.generateFont(param(30))
        xl  = gen.generateFont(param(40))
        xxl = gen.generateFont(param(72))
        gen.dispose()
    }

    fun dispose() {
        if (::xs.isInitialized)  xs.dispose()
        if (::sm.isInitialized)  sm.dispose()
        if (::md.isInitialized)  md.dispose()
        if (::lg.isInitialized)  lg.dispose()
        if (::xl.isInitialized)  xl.dispose()
        if (::xxl.isInitialized) xxl.dispose()
    }
}
```

- [ ] **Step 2: Update GirlsPanicGame.kt**

```kotlin
package game

import com.badlogic.gdx.Game
import com.badlogic.gdx.assets.AssetManager
import game.persistence.GamePrefs
import game.screens.Fonts
import game.screens.MenuScreen

class GirlsPanicGame(val prefs: GamePrefs) : Game() {

    val assets = AssetManager()
    var currentLevelId: Int = 1

    override fun create() {
        Fonts.load()
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        Fonts.dispose()
        assets.dispose()
        super.dispose()
    }
}
```

- [ ] **Step 3: Verify build compiles**

```bash
./gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/game/screens/Fonts.kt \
        core/src/main/kotlin/game/GirlsPanicGame.kt
git commit -m "feat: add Fonts singleton with FreeType/Orbitron at 6 fixed sizes"
```

---

## Task 4: Migrate GameScreen + fix HUD bar spacing

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt`

Changes:
1. Remove `private val font = BitmapFont()` field
2. Remove `font.dispose()` from `dispose()`
3. Replace all `font.*` with `Fonts.*` per size mapping
4. In `drawHUD()`: score y→`H-34f`, barY→`H-hudH+2f`, barH→`10f`, add separator line

- [ ] **Step 1: Remove font field and dispose**

In `GameScreen.kt`:
- Delete line: `private val font = BitmapFont()`
- In `dispose()`, remove `font.dispose()`
- Add import: `import game.screens.Fonts`

- [ ] **Step 2: Replace drawHUD() entirely**

```kotlin
private fun drawHUD() {
    val pct = world.territory.conqueredPercent().toInt()
    val timeStr = "%02d:%02d".format(world.timeRemaining.toInt() / 60, world.timeRemaining.toInt() % 60)
    val W = GameConstants.FIELD_WIDTH
    val H = GameConstants.FIELD_HEIGHT
    val hudH = 60f

    Gdx.gl.glEnable(GL20.GL_BLEND)
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

    shapes.begin(ShapeRenderer.ShapeType.Filled)
    shapes.setColor(0f, 0f, 0f, 0.88f)
    shapes.rect(0f, H - hudH, W, hudH)
    shapes.end()

    // Separator between stats zone and bar zone
    shapes.begin(ShapeRenderer.ShapeType.Line)
    shapes.setColor(0.22f, 0.22f, 0.22f, 1f)
    shapes.line(0f, H - hudH, W, H - hudH)
    shapes.setColor(0.18f, 0.18f, 0.18f, 1f)
    shapes.line(0f, H - hudH + 14f, W, H - hudH + 14f)
    shapes.end()

    batch.begin()

    // TIME label + value (left)
    Fonts.xs.color = Color(0.792f, 0.784f, 0.667f, 1f)
    Fonts.xs.draw(batch, "TIME", 12f, H - 10f)

    val timeDanger = world.timeRemaining < 30f
    Fonts.lg.color = if (timeDanger) Color(0.792f, 0f, 0.176f, 1f)
                     else Color(0f, 0.859f, 0.914f, 1f)
    Fonts.lg.draw(batch, timeStr, 12f, H - 22f)

    // CLEAR label + value (right)
    Fonts.xs.color = Color(0.792f, 0.784f, 0.667f, 1f)
    val pctStr = "$pct%"
    layout.setText(Fonts.xs, "CLEAR")
    Fonts.xs.draw(batch, "CLEAR", W - layout.width - 12f, H - 10f)

    Fonts.lg.color = Color(0.918f, 0.918f, 0f, 1f)
    layout.setText(Fonts.lg, pctStr)
    Fonts.lg.draw(batch, pctStr, W - layout.width - 12f, H - 22f)

    // Lives (center)
    Fonts.md.color = Color(0.792f, 0f, 0.176f, 1f)
    val livesStr = "♥ ".repeat(world.lives.coerceIn(0, 5)).trimEnd()
    layout.setText(Fonts.md, livesStr)
    Fonts.md.draw(batch, livesStr, (W - layout.width) / 2f, H - 12f)

    // Score (center, below lives — 34f gives ~10px gap above bar)
    Fonts.md.color = Color(0.576f, 0.573f, 0.467f, 1f)
    val scoreStr = "${world.score}"
    layout.setText(Fonts.md, scoreStr)
    Fonts.md.draw(batch, scoreStr, (W - layout.width) / 2f, H - 34f)

    batch.end()

    // Segmented territory bar (16 blocks)
    shapes.begin(ShapeRenderer.ShapeType.Filled)
    val segments = 16
    val barMargin = 12f
    val barY = H - hudH + 2f
    val barH = 10f
    val totalBarW = W - barMargin * 2f
    val segW = (totalBarW - (segments - 1) * 2f) / segments
    val filledSegs = (pct * segments / 100f).toInt()
    for (s in 0 until segments) {
        val sx = barMargin + s * (segW + 2f)
        val lit = s < filledSegs
        shapes.setColor(
            if (lit) 0.918f else 0.165f,
            if (lit) 0.918f else 0.165f,
            0f, 1f
        )
        shapes.rect(sx, barY, segW, barH)
    }
    shapes.end()

    Gdx.gl.glDisable(GL20.GL_BLEND)
}
```

- [ ] **Step 3: Replace drawLevelCompleteOverlay() font calls**

In `drawLevelCompleteOverlay()`, replace all `font.*` with `Fonts.*`:

| Old | New |
|-----|-----|
| `font.data.setScale(2.4f)` + LEVEL CLEAR! | `Fonts.xl` |
| `font.data.setScale(2.8f)` + stars ★ | `Fonts.xl` |
| `font.data.setScale(1.2f)` + SCORE/CLEARED labels | `Fonts.sm` |
| `font.data.setScale(1.1f)` + RETRY/NEXT/MENU buttons | `Fonts.sm` |

Full replacement of all font calls in `drawLevelCompleteOverlay()`:

```kotlin
batch.begin()

// Title
Fonts.xl.color = Color(0f, 1f, 1f, 1f)
layout.setText(Fonts.xl, "LEVEL CLEAR!")
Fonts.xl.draw(batch, "LEVEL CLEAR!", (W - layout.width) / 2f, panelY + panelH - 24f)

// Stars
for (i in 1..3) {
    val starColor = if (i <= overlayStars) Color(1f, 0.9f, 0f, 1f) else Color(0.3f, 0.3f, 0.3f, 1f)
    Fonts.xl.color = starColor
    layout.setText(Fonts.xl, "★")
    val starX = W / 2f + (i - 2) * 60f - layout.width / 2f
    Fonts.xl.draw(batch, "★", starX, panelY + panelH - 70f)
}

// Score
Fonts.sm.color = Color(0.8f, 0.8f, 0.6f, 1f)
val scoreLabel = "SCORE: $overlayScore"
layout.setText(Fonts.sm, scoreLabel)
Fonts.sm.draw(batch, scoreLabel, (W - layout.width) / 2f, panelY + panelH - 130f)

// Area cleared
Fonts.sm.color = Color(0.9f, 0.9f, 0f, 1f)
val pctLabel = "CLEARED: ${world.territory.conqueredPercent().toInt()}%"
layout.setText(Fonts.sm, pctLabel)
Fonts.sm.draw(batch, pctLabel, (W - layout.width) / 2f, panelY + panelH - 165f)

// Buttons
val btnW = panelW / 3f - 12f
val btnY = panelY + 52f
val btnH = 38f

Fonts.sm.color = Color(0f, 0.85f, 0.85f, 1f)
layout.setText(Fonts.sm, "RETRY")
Fonts.sm.draw(batch, "RETRY", panelX + 10f + (btnW - layout.width) / 2f, btnY + btnH / 2f + 8f)

Fonts.sm.color = Color(1f, 0.9f, 0f, 1f)
layout.setText(Fonts.sm, "NEXT")
Fonts.sm.draw(batch, "NEXT", panelX + btnW + 16f + (btnW - layout.width) / 2f, btnY + btnH / 2f + 8f)

Fonts.sm.color = Color(0.7f, 0.7f, 0.7f, 1f)
layout.setText(Fonts.sm, "MENU")
Fonts.sm.draw(batch, "MENU", panelX + 2 * (btnW + 6f) + 10f + (btnW - layout.width) / 2f, btnY + btnH / 2f + 8f)

batch.end()
```

- [ ] **Step 4: Verify build**

```bash
./gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/game/screens/GameScreen.kt
git commit -m "feat: migrate GameScreen to Fonts.*, fix HUD bar spacing and separator"
```

---

## Task 5: Migrate GameOverScreen + fix cardY

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameOverScreen.kt`

- [ ] **Step 1: Remove font field and dispose**

- Delete: `private val font = BitmapFont()`
- In `dispose()`, remove `font.dispose()`
- Add import: `import game.screens.Fonts`

- [ ] **Step 2: Fix cardY constant**

Change:
```kotlin
private val cardY = titleY - 40f - cardH
```
To:
```kotlin
private val cardY = titleY - 140f - cardH
```

- [ ] **Step 3: Replace drawHeader() font calls**

```kotlin
private fun drawHeader() {
    shapes.begin(ShapeRenderer.ShapeType.Filled)
    shapes.setColor(0f, 0f, 0f, 1f)
    shapes.rect(0f, H - headerH, W, headerH)
    shapes.end()

    shapes.begin(ShapeRenderer.ShapeType.Line)
    shapes.setColor(0.22f, 0.22f, 0.22f, 1f)
    shapes.line(0f, H - headerH, W, H - headerH)
    shapes.end()

    batch.begin()
    Fonts.sm.color = Color(1f, 0.9f, 0f, 1f)
    Fonts.sm.draw(batch, "=", 14f, H - 15f)

    Fonts.sm.color = Color(1f, 0.9f, 0f, 1f)
    Fonts.sm.draw(batch, "PANIC_SYSTEM_v1.0", 42f, H - 15f)

    Fonts.sm.color = Color(1f, 0.9f, 0f, 1f)
    val coinText = "00,450"
    layout.setText(Fonts.sm, coinText)
    Fonts.sm.draw(batch, coinText, W - layout.width - 16f, H - 15f)
    batch.end()

    shapes.begin(ShapeRenderer.ShapeType.Line)
    shapes.setColor(0.35f, 0.35f, 0.35f, 1f)
    val coinX = W - 85f
    shapes.rect(coinX, H - headerH + 8f, 75f, 32f)
    shapes.end()
}
```

- [ ] **Step 4: Replace drawTitleGameOver() font calls**

```kotlin
private fun drawTitleGameOver() {
    val glitchShift = if ((time * 8).toInt() % 10 < 2) 3f else 0f
    val pulse = 0.85f + 0.15f * sin(time * 4.0).toFloat()

    val barY = titleY - 80f
    shapes.begin(ShapeRenderer.ShapeType.Filled)
    shapes.setColor(0.792f, 0f, 0.176f, 0.9f)
    shapes.rect(0f, barY, W, 4f)
    shapes.setColor(0.792f, 0f, 0.176f, 0.3f * pulse)
    shapes.rect(0f, barY - 4f, W, 12f)
    shapes.end()

    batch.begin()
    // Shadow
    Fonts.xxl.color = Color(0.25f, 0f, 0f, 1f)
    layout.setText(Fonts.xxl, "GAME")
    val gameX = (W - layout.width) / 2f
    Fonts.xxl.draw(batch, "GAME", gameX + 4f, titleY - 4f)

    layout.setText(Fonts.xxl, "OVER")
    val overX = (W - layout.width) / 2f
    Fonts.xxl.draw(batch, "OVER", overX + 4f, titleY - 85f)

    // Cyan glitch
    Fonts.xxl.color = Color(0f, 1f, 1f, 0.35f)
    Fonts.xxl.draw(batch, "GAME", gameX - glitchShift, titleY)
    Fonts.xxl.draw(batch, "OVER", overX - glitchShift, titleY - 85f)

    // Red glitch
    Fonts.xxl.color = Color(1f, 0f, 0f, 0.4f)
    Fonts.xxl.draw(batch, "GAME", gameX + glitchShift, titleY)
    Fonts.xxl.draw(batch, "OVER", overX + glitchShift, titleY - 85f)

    // Main
    Fonts.xxl.color = Color(0.792f, 0f, 0.176f, 1f)
    Fonts.xxl.draw(batch, "GAME", gameX, titleY)
    Fonts.xxl.draw(batch, "OVER", overX, titleY - 85f)

    batch.end()
}
```

> Note: "OVER" y offset changes from `-55f` to `-85f` because `Fonts.xxl` at 72px has a larger line height than the old scale-5 default font (~60px cap height vs ~68px). Adjust if visual gap between GAME and OVER looks wrong.

- [ ] **Step 5: Replace drawStatCards() font calls**

```kotlin
private fun drawStatCards() {
    drawCard(card1X, cardY, cardW, cardH)
    drawCard(card2X, cardY, cardW, cardH)

    batch.begin()

    // Card 1 — Final Score
    Fonts.sm.color = Color(0.55f, 0.55f, 0.55f, 1f)
    layout.setText(Fonts.sm, "FINAL SCORE")
    Fonts.sm.draw(batch, "FINAL SCORE",
        card1X + (cardW - layout.width) / 2f,
        cardY + cardH - 14f)

    Fonts.lg.color = Color(1f, 0.9f, 0f, 1f)
    layout.setText(Fonts.lg, scoreText)
    Fonts.lg.draw(batch, scoreText,
        card1X + (cardW - layout.width) / 2f,
        cardY + cardH - 36f)

    batch.end()

    shapes.begin(ShapeRenderer.ShapeType.Filled)
    val dot1BaseX = card1X + cardW / 2f - 12f
    val dotBaseY = cardY + 14f
    shapes.setColor(1f, 0.9f, 0f, 1f)
    shapes.rect(dot1BaseX,       dotBaseY, 8f, 8f)
    shapes.rect(dot1BaseX + 12f, dotBaseY, 8f, 8f)
    shapes.setColor(1f, 0.9f, 0f, 0.2f)
    shapes.rect(dot1BaseX + 24f, dotBaseY, 8f, 8f)
    shapes.end()

    batch.begin()
    // Card 2 — Level Reached
    Fonts.sm.color = Color(0.55f, 0.55f, 0.55f, 1f)
    layout.setText(Fonts.sm, "LEVEL REACHED")
    Fonts.sm.draw(batch, "LEVEL REACHED",
        card2X + (cardW - layout.width) / 2f,
        cardY + cardH - 14f)

    Fonts.lg.color = Color(0f, 0.86f, 0.91f, 1f)
    layout.setText(Fonts.lg, stageText)
    Fonts.lg.draw(batch, stageText,
        card2X + (cardW - layout.width) / 2f,
        cardY + cardH - 36f)

    batch.end()

    shapes.begin(ShapeRenderer.ShapeType.Filled)
    val dot2BaseX = card2X + cardW / 2f - 18f
    shapes.setColor(0f, 0.86f, 0.91f, 1f)
    shapes.rect(dot2BaseX,       dotBaseY, 8f, 8f)
    shapes.rect(dot2BaseX + 12f, dotBaseY, 8f, 8f)
    shapes.rect(dot2BaseX + 24f, dotBaseY, 8f, 8f)
    shapes.end()
}
```

- [ ] **Step 6: Replace drawButtons() font calls**

```kotlin
batch.begin()
Fonts.xl.color = Color(0.1f, 0.1f, 0.1f, 1f)
layout.setText(Fonts.xl, "RETRY")
Fonts.xl.draw(batch, "RETRY",
    btnRetry.x + (btnRetry.w - layout.width) / 2f,
    btnRetry.y + (btnRetry.h + layout.height) / 2f)

Fonts.lg.color = Color(0.88f, 0.88f, 0.88f, 1f)
layout.setText(Fonts.lg, "EXIT TO MENU")
Fonts.lg.draw(batch, "EXIT TO MENU",
    btnMenu.x + (btnMenu.w - layout.width) / 2f,
    btnMenu.y + (btnMenu.h + layout.height) / 2f)
batch.end()
```

- [ ] **Step 7: Replace drawFooter() font calls**

```kotlin
batch.begin()
Fonts.xs.color = Color(0.35f, 0.35f, 0.35f, 1f)
val footerText = "SYSTEM.CRITICAL.PROCESS.TERMINATED"
layout.setText(Fonts.xs, footerText)
Fonts.xs.draw(batch, footerText, (W - layout.width) / 2f, segY - 8f)
batch.end()

// ... (shapes for bottom box unchanged) ...

batch.begin()
Fonts.xs.color = Color(0.3f, 0.3f, 0.3f, 1f)
Fonts.xs.draw(batch, "HARDWARE: CABIN_UNIT_04", 6f, 38f)
Fonts.xs.draw(batch, "LOCATION: DISTRICT_9_ARCADE", 6f, 22f)
batch.end()
```

- [ ] **Step 8: Verify build**

```bash
./gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add core/src/main/kotlin/game/screens/GameOverScreen.kt
git commit -m "feat: migrate GameOverScreen to Fonts.*, fix cardY overlap with title"
```

---

## Task 6: Migrate MenuScreen

**Files:**
- Modify: `core/src/main/kotlin/game/screens/MenuScreen.kt`

- [ ] **Step 1: Remove font field and dispose**

- Delete: `private val font = BitmapFont()`
- In `dispose()`, remove `font.dispose()`
- Add import: `import game.screens.Fonts`

- [ ] **Step 2: Replace drawHeader() font calls**

```kotlin
// scale 1.3 → Fonts.md, scale 1.0 → Fonts.sm
Fonts.md.color = Color(0.918f, 0.918f, 0f, 1f)
Fonts.md.draw(batch, "= PANIC_SYSTEM_V1.0", 12f, H - 14f)

val badgeTxt = "CREDITS  99"
layout.setText(Fonts.sm, badgeTxt)
val badgeX = W - layout.width - 32f
batch.end()
// ... shapes for badge background (unchanged) ...
batch.begin()
Fonts.sm.color = Color(0.918f, 0.918f, 0f, 1f)
Fonts.sm.draw(batch, badgeTxt, badgeX, H - 18f)
```

- [ ] **Step 3: Replace drawLogo() font calls**

```kotlin
// scale 3.6 → Fonts.xl, scale 1.3 → Fonts.md
Fonts.xl.color = Color(0.28f, 0.28f, 0f, 1f)
layout.setText(Fonts.xl, "GIRLS AI PANIC")
val tx = (W - layout.width) / 2f
Fonts.xl.draw(batch, "GIRLS AI PANIC", tx + 3f, 720f - 3f)

Fonts.xl.color = Color(0.918f * glow, 0.918f * glow, 0f, 1f)
Fonts.xl.draw(batch, "GIRLS AI PANIC", tx, 720f)

Fonts.md.color = Color(0.792f, 0.784f, 0.667f, 1f)
layout.setText(Fonts.md, "AI-POWERED PUZZLE")
Fonts.md.draw(batch, "AI-POWERED PUZZLE", (W - layout.width) / 2f, 648f)
```

- [ ] **Step 4: Replace drawButtons() font calls**

```kotlin
// scale 2.8 → Fonts.xl, scale 1.8 → Fonts.md
Fonts.xl.color = Color(0.196f, 0.196f, 0f, 1f)
layout.setText(Fonts.xl, "PLAY")
Fonts.xl.draw(batch, "PLAY",
    btnPlay.x + (btnPlay.w - layout.width) / 2f,
    btnPlay.y + (btnPlay.h + layout.height) / 2f)

Fonts.md.color = Color(0.918f, 0.918f, 0f, 1f)
layout.setText(Fonts.md, "LEVELS")
Fonts.md.draw(batch, "LEVELS",
    btnLevels.x + (btnLevels.w - layout.width) / 2f,
    btnLevels.y + (btnLevels.h + layout.height) / 2f)

Fonts.md.color = Color(0.576f, 0.573f, 0.467f, 1f)
layout.setText(Fonts.md, "SETTINGS")
Fonts.md.draw(batch, "SETTINGS",
    btnSettingsArea.x + (btnSettingsArea.w - layout.width) / 2f,
    btnSettingsArea.y + (btnSettingsArea.h + layout.height) / 2f)
```

- [ ] **Step 5: Replace drawScoreArea() font calls**

```kotlin
// scale 1.0 → Fonts.sm, scale 2.2 → Fonts.lg
Fonts.sm.color = Color(0.576f, 0.573f, 0.467f, 1f)
layout.setText(Fonts.sm, "BEST SCORE")
Fonts.sm.draw(batch, "BEST SCORE", (W - layout.width) / 2f, 230f)

Fonts.lg.color = Color(0.918f, 0.918f, 0f, 1f)
val hsText = formatScore(hs)
layout.setText(Fonts.lg, hsText)
Fonts.lg.draw(batch, hsText, (W - layout.width) / 2f, 205f)
```

- [ ] **Step 6: Replace drawBottomNav() font calls**

```kotlin
// scale 0.85 → Fonts.xs
for ((i, tab) in tabs.withIndex()) {
    Fonts.xs.color = if (tab.second) Color(0.918f, 0.918f, 0f, 1f)
                     else Color(0.576f, 0.573f, 0.467f, 1f)
    layout.setText(Fonts.xs, tab.first)
    Fonts.xs.draw(batch, tab.first, i * tabW + (tabW - layout.width) / 2f, 33f)
}
```

- [ ] **Step 7: Verify and commit**

```bash
./gradlew :core:compileKotlin
git add core/src/main/kotlin/game/screens/MenuScreen.kt
git commit -m "feat: migrate MenuScreen to Fonts.*"
```

---

## Task 7: Migrate LevelSelectScreen

**Files:**
- Modify: `core/src/main/kotlin/game/screens/LevelSelectScreen.kt`

- [ ] **Step 1: Remove font field and dispose**

- Delete: `private val font = BitmapFont()`
- In `dispose()`, remove `font.dispose()`
- Add import: `import game.screens.Fonts`

- [ ] **Step 2: Replace drawHeader() font calls**

```kotlin
// scale 1.3 → Fonts.md, scale 1.0 → Fonts.sm
Fonts.md.color = Color(0.918f, 0.918f, 0f, 1f)
Fonts.md.draw(batch, "= PANIC_SYSTEM_V1.0", 12f, H - 14f)

Fonts.sm.color = Color(0.918f, 0.918f, 0f, 1f)
Fonts.sm.draw(batch, "V1.0-BETA", W - 88f, H - 18f)
```

- [ ] **Step 3: Replace drawTitle() font calls**

```kotlin
// scale 1.6 → Fonts.md
Fonts.md.color = Color(0.918f, 0.918f, 0f, 1f)
layout.setText(Fonts.md, "SELECT LEVEL")
val tx = (W - layout.width) / 2f
Fonts.md.draw(batch, "SELECT LEVEL", tx, H - 58f)
```

- [ ] **Step 4: Replace drawGrid() font calls**

```kotlin
// Level number: scale 1.4 → Fonts.md
Fonts.md.color = when {
    selected  -> Color(0.918f, 0.918f, 0f, 1f)
    completed -> Color(0.918f, 0.918f, 0f, 1f)
    unlocked  -> Color(0.898f, 0.886f, 0.882f, 1f)
    else      -> Color(0.208f, 0.208f, 0.208f, 1f)
}
val numStr = "%02d".format(i)
layout.setText(Fonts.md, numStr)
Fonts.md.draw(batch, numStr,
    r.x + (r.w - layout.width) / 2f,
    r.y + r.h - 6f)

// Lock/stars: scale 0.75 → Fonts.xs
if (!unlocked) {
    Fonts.xs.color = Color(0.282f, 0.282f, 0.192f, 1f)
    layout.setText(Fonts.xs, "LOCK")
    Fonts.xs.draw(batch, "LOCK",
        r.x + (r.w - layout.width) / 2f,
        r.y + 14f)
}
if (stars > 0) {
    Fonts.xs.color = Color(0.918f, 0.918f, 0f, 1f)
    val starStr = "★".repeat(stars)
    layout.setText(Fonts.xs, starStr)
    Fonts.xs.draw(batch, starStr,
        r.x + (r.w - layout.width) / 2f,
        r.y + 14f)
}
```

- [ ] **Step 5: Replace drawButtons() font calls**

```kotlin
// scale 2.0 → Fonts.lg, scale 1.5 → Fonts.md
Fonts.lg.color = Color(0.055f, 0.055f, 0.055f, 1f)
val startLabel = if (selectedLevel > 0) "START  LEVEL $selectedLevel" else "START SELECTED"
layout.setText(Fonts.lg, startLabel)
Fonts.lg.draw(batch, startLabel,
    btnStart.x + (btnStart.w - layout.width) / 2f,
    btnStart.y + (btnStart.h + layout.height) / 2f)

Fonts.md.color = Color(0.918f, 0.918f, 0f, 1f)
layout.setText(Fonts.md, "HOME")
Fonts.md.draw(batch, "HOME",
    btnHome.x + (btnHome.w - layout.width) / 2f,
    btnHome.y + (btnHome.h + layout.height) / 2f)
layout.setText(Fonts.md, "RESET")
Fonts.md.draw(batch, "RESET",
    btnReset.x + (btnReset.w - layout.width) / 2f,
    btnReset.y + (btnReset.h + layout.height) / 2f)
```

- [ ] **Step 6: Verify and commit**

```bash
./gradlew :core:compileKotlin
git add core/src/main/kotlin/game/screens/LevelSelectScreen.kt
git commit -m "feat: migrate LevelSelectScreen to Fonts.*"
```

---

## Task 8: Migrate LevelCompleteScreen

**Files:**
- Modify: `core/src/main/kotlin/game/screens/LevelCompleteScreen.kt`

- [ ] **Step 1: Remove font field and dispose**

- Delete: `private val font = BitmapFont()`
- In `dispose()`, remove `font.dispose()`
- Add import: `import game.screens.Fonts`

- [ ] **Step 2: Replace drawHeader() font calls**

```kotlin
// scale 1.3 → Fonts.md, scale 1.0 → Fonts.sm
Fonts.md.color = Color(0.918f, 0.918f, 0f, 1f)
Fonts.md.draw(batch, "= PANIC_SYSTEM_V1.0", 12f, H - 14f)

val stageStr = "STAGE $levelId"
layout.setText(Fonts.sm, stageStr)
Fonts.sm.color = Color(0f, 0.859f, 0.914f, 1f)
Fonts.sm.draw(batch, stageStr, W - layout.width - 16f, H - 18f)
```

- [ ] **Step 3: Replace drawTitle() font calls**

```kotlin
// scale 4.0 → Fonts.xl
Fonts.xl.color = Color(0.28f, 0.28f, 0f, 1f)
layout.setText(Fonts.xl, "MISSION")
val t1x = (W - layout.width) / 2f
Fonts.xl.draw(batch, "MISSION", t1x + 3f, H - 60f - 3f)

Fonts.xl.color = Color(0.28f, 0.28f, 0f, 1f)
layout.setText(Fonts.xl, "COMPLETE!")
val t2x = (W - layout.width) / 2f
Fonts.xl.draw(batch, "COMPLETE!", t2x + 3f, H - 118f - 3f)

Fonts.xl.color = Color(0.918f * pulse, 0.918f * pulse, 0f, 1f)
Fonts.xl.draw(batch, "MISSION", t1x, H - 60f)

Fonts.xl.color = Color(0.918f, 0.918f, 0f, 1f)
Fonts.xl.draw(batch, "COMPLETE!", t2x, H - 118f)
```

- [ ] **Step 4: Replace drawStars() font calls**

```kotlin
// scale 4.0 → Fonts.xl
Fonts.xl.color = if (earned) Color(0.918f, 0.85f + pulse * 0.05f, 0f, 1f)
                 else Color(0.208f, 0.208f, 0.208f, 1f)
layout.setText(Fonts.xl, "★")
Fonts.xl.draw(batch, "★", sx - layout.width / 2f, starY + layout.height / 2f)
```

- [ ] **Step 5: Replace drawScoreCard() font calls**

```kotlin
// scale 1.0 → Fonts.sm, scale 2.4 → Fonts.xl
Fonts.sm.color = Color(0.576f, 0.573f, 0.467f, 1f)
layout.setText(Fonts.sm, "FINAL SCORE")
Fonts.sm.draw(batch, "FINAL SCORE", cardX + (cardW - layout.width) / 2f, cardY + cardH - 12f)

Fonts.xl.color = Color(0.918f, 0.918f, 0f, 1f)
val scoreText = formatScore(score)
layout.setText(Fonts.xl, scoreText)
Fonts.xl.draw(batch, scoreText, cardX + (cardW - layout.width) / 2f, cardY + cardH - 36f)
```

- [ ] **Step 6: Replace drawButtons() font calls**

```kotlin
// scale 2.2 → Fonts.lg
Fonts.lg.color = Color(0.055f, 0.055f, 0.055f, 1f)
layout.setText(Fonts.lg, "RETRY")
Fonts.lg.draw(batch, "RETRY",
    btnRetry.x + (btnRetry.w - layout.width) / 2f,
    btnRetry.y + (btnRetry.h + layout.height) / 2f)

Fonts.lg.color = Color(0.055f, 0.055f, 0.055f, 1f)
layout.setText(Fonts.lg, "NEXT")
Fonts.lg.draw(batch, "NEXT",
    btnNext.x + (btnNext.w - layout.width) / 2f,
    btnNext.y + (btnNext.h + layout.height) / 2f)

Fonts.lg.color = Color(0.898f, 0.886f, 0.882f, 1f)
layout.setText(Fonts.lg, "MENU")
Fonts.lg.draw(batch, "MENU",
    btnMenu.x + (btnMenu.w - layout.width) / 2f,
    btnMenu.y + (btnMenu.h + layout.height) / 2f)
```

- [ ] **Step 7: Replace drawFooter() font calls**

```kotlin
// scale 0.85 → Fonts.xs
Fonts.xs.color = Color(0.35f, 0.35f, 0.35f, 1f)
val label = "SYSTEM.STAGE.COMPLETE"
layout.setText(Fonts.xs, label)
Fonts.xs.draw(batch, label, (W - layout.width) / 2f, segY - 8f)
```

- [ ] **Step 8: Verify full build**

```bash
./gradlew :core:compileKotlin :android:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add core/src/main/kotlin/game/screens/LevelCompleteScreen.kt
git commit -m "feat: migrate LevelCompleteScreen to Fonts.*"
```

---

## Visual Verification Checklist

After all tasks complete, run the app and verify:

- [ ] Menu screen: "GIRLS AI PANIC" title crisp, no pixelation
- [ ] HUD: progress bar has visible gap from score, separator line visible
- [ ] HUD: time/pct values readable at all sizes
- [ ] GameOver: "GAME OVER" fully visible, cards/buttons below title
- [ ] GameOver: "OVER" word not overlapped by stat cards
- [ ] LevelSelect: level numbers readable in grid cells
- [ ] LevelComplete: "MISSION COMPLETE!" crisp
- [ ] All button labels readable
