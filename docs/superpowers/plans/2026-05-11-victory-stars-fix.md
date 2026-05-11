# Victory Stars Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix victory screen stars that render as yellow disks — make them visually distinct 5-pointed star shapes by moving glow circles outside the star boundary.

**Architecture:** Two constant changes in `GameScreen.kt` (glow radius and star radius in two render methods) plus deletion of dead-code file `LevelCompleteScreen.kt`. No logic changes. No new functions.

**Tech Stack:** Kotlin, libGDX ShapeRenderer, Android

---

## Files

- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt` (lines 571, 576, 678, 683)
- Delete: `core/src/main/kotlin/game/screens/LevelCompleteScreen.kt`

---

### Task 1: Fix stars in stats overlay panel

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt:571,576`

These lines are inside `drawLevelCompleteOverlay()`, the panel shown after the 5-second celebration.

Current state (lines 569–576):
```kotlin
if (i <= overlayStars) {
    shapes.setColor(1f, 0.9f, 0f, 0.18f)
    shapes.circle(sx, starCY, 26f, 20)   // ← glow r=26 > outerR=16: fills notches
    shapes.setColor(1f, 0.9f, 0f, 1f)
} else {
    shapes.setColor(0.3f, 0.3f, 0.3f, 1f)
}
drawStar(sx, starCY, 16f, 6.5f)          // ← star too small relative to glow
```

- [ ] **Step 1: Edit `GameScreen.kt` — change glow circle and star sizes in overlay**

In `drawLevelCompleteOverlay()`, replace the two star-related constants:

```kotlin
if (i <= overlayStars) {
    shapes.setColor(1f, 0.9f, 0f, 0.18f)
    shapes.circle(sx, starCY, 32f, 22)   // glow r=32 = outerR(22)+10 — halo outside star
    shapes.setColor(1f, 0.9f, 0f, 1f)
} else {
    shapes.setColor(0.3f, 0.3f, 0.3f, 1f)
}
drawStar(sx, starCY, 22f, 8.5f)          // outerR=22, innerR=8.5, ratio≈0.386
```

- [ ] **Step 2: Commit**

```bash
git add core/src/main/kotlin/game/screens/GameScreen.kt
git commit -m "fix: stars overlay — glow halo outside star, larger star shape"
```

---

### Task 2: Fix stars in celebration phase

**Files:**
- Modify: `core/src/main/kotlin/game/screens/GameScreen.kt:678,683`

These lines are inside `drawCelebrationPhase()`, the 5-second zoom-and-sparks animation.

Current state (lines 676–683):
```kotlin
if (i < overlayStars) {
    shapes.setColor(0.918f, 0.9f, 0f, 0.18f * starPulse * starAlpha)
    shapes.circle(sx, starCY, 32f, 22)   // ← glow r=32 > outerR=17: fills notches
    shapes.setColor(0.918f, 0.85f + starPulse * 0.05f, 0f, starAlpha)
} else {
    shapes.setColor(0.22f, 0.22f, 0.22f, starAlpha * 0.6f)
}
drawStar(sx, starCY, 17f, 7f)            // ← star too small relative to glow
```

- [ ] **Step 1: Edit `GameScreen.kt` — change glow circle and star sizes in celebration**

In `drawCelebrationPhase()`, replace the two star-related constants:

```kotlin
if (i < overlayStars) {
    shapes.setColor(0.918f, 0.9f, 0f, 0.18f * starPulse * starAlpha)
    shapes.circle(sx, starCY, 36f, 22)   // glow r=36 = outerR(26)+10 — halo outside star
    shapes.setColor(0.918f, 0.85f + starPulse * 0.05f, 0f, starAlpha)
} else {
    shapes.setColor(0.22f, 0.22f, 0.22f, starAlpha * 0.6f)
}
drawStar(sx, starCY, 26f, 10f)           // outerR=26, innerR=10, ratio≈0.385
```

- [ ] **Step 2: Commit**

```bash
git add core/src/main/kotlin/game/screens/GameScreen.kt
git commit -m "fix: stars celebration — glow halo outside star, larger star shape"
```

---

### Task 3: Delete dead code — LevelCompleteScreen.kt

**Files:**
- Delete: `core/src/main/kotlin/game/screens/LevelCompleteScreen.kt`

This file is never instantiated anywhere in the codebase (confirmed by grep). It duplicates the victory display logic now handled by `GameScreen` and has the same glow-over-star bug.

- [ ] **Step 1: Verify the file is unreferenced**

```bash
grep -r "LevelCompleteScreen" core/src/main/kotlin/
```

Expected output: only the class definition line inside `LevelCompleteScreen.kt` itself. No other references.

- [ ] **Step 2: Delete the file**

```bash
rm core/src/main/kotlin/game/screens/LevelCompleteScreen.kt
```

- [ ] **Step 3: Build to confirm no compilation errors**

```bash
./gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: delete unused LevelCompleteScreen — dead code"
```

---

## Visual verification (manual)

Run the game, complete a level, and confirm:
- Celebration phase: 3 (or fewer) gold stars with clear 5-pointed shape and yellow halo outside the star boundary. Dark notches visible between points.
- Stats overlay: same star appearance on the LEVEL CLEAR panel.
- Unearned stars: gray star shapes (no glow), same size.
