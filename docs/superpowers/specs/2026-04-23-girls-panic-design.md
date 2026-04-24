# Girls AI Panic — Design Spec
**Date:** 2026-04-23  
**Platform:** Android 10+ (API 29)  
**Engine:** LibGDX (Kotlin)  
**Monetization:** Free + AdMob (banner + interstitial)

---

## 1. Concept

Gals Panic clone con sfondo rivelato progressivamente. Giocatore traccia linee sul campo per conquistare territorio e rivelare immagini di modelle generate con IA. 50 livelli di difficoltà crescente. Nemici tematici (ragni, scarafaggi, vespe, lumache). Power-up speciali. Sistema 3 stelle per livello.

---

## 2. Architettura

```
android/                        ← Android launcher (AdMob, manifest)
core/
  src/
    game/
      GirlsPanicGame.kt         ← LibGDX Application entry point
      screens/
        MenuScreen.kt
        LevelSelectScreen.kt
        GameScreen.kt           ← loop principale, aggiorna sistemi ECS
        LevelCompleteScreen.kt
        GameOverScreen.kt
      ecs/
        Entity.kt
        Component.kt            ← PlayerComponent, EnemyComponent, PowerupComponent
        systems/
          MovementSystem.kt     ← muove entità verso destinazione
          CollisionSystem.kt    ← hit detection giocatore/nemici/linea
          TerritorySystem.kt    ← logica Qix: linee, chiusura area, % calcolo
          EnemyAISystem.kt      ← comportamento per tipo nemico
          PowerupSystem.kt      ← spawning e effetti power-up
      level/
        LevelData.kt            ← data class: id, background, timeSeconds, targetPercent, enemies, powerups
        LevelLoader.kt          ← deserializza JSON da assets/levels/
      ads/
        AdManager.kt            ← wrapper AdMob interstitial + banner
assets/
  levels/
    level_01.json … level_50.json
  backgrounds/
    bg_01.png … bg_50.png      ← placeholder grigi 480×800, sostituire con immagini AI reali
  enemies/
    spider.png                  ← sprite sheet 4 frame 64×64
    cockroach.png               ← sprite sheet 4 frame 64×64
    wasp.png                    ← sprite sheet 6 frame 64×64
    snail.png                   ← sprite sheet 4 frame 64×64
  ui/
    orbitron.ttf
    hud_heart.png
    star_full.png
    star_empty.png
    button_*.png
  audio/
    menu_music.ogg
    sfx_claim.ogg
    sfx_die.ogg
    sfx_powerup.ogg
```

---

## 3. Meccanica di Gioco

### Campo
- Griglia bitmap 2D (480×800 unità logiche)
- Ogni cella: `CONQUERED` o `FREE`
- Overlay scuro su celle `FREE`, sfondo AI visibile su celle `CONQUERED`

### Movimento Giocatore
- Input: tap su punto del campo → giocatore si muove verso target
- Su bordo/area conquistata = sicuro
- In zona libera = traccia linea "in costruzione" (vulnerabile)

### Chiusura Area
- Giocatore torna su bordo/area conquistata → chiude poligono
- Area interna senza nemici → diventa `CONQUERED`
- Area interna con nemico → linea cancellata + vita persa

### Nemici

| Tipo | Zona | Comportamento | Pericolo |
|------|------|---------------|----------|
| Ragno | Bordi + conquistata | Segue bordo perimetro | Tocca giocatore = vita persa |
| Scarafaggio | Zona libera | Rimbalzo casuale | Tocca linea in costruzione = vita persa |
| Vespa | Entrambe | Insegue giocatore diretto | Tocca giocatore = vita persa |
| Lumaca | Zona libera | Lenta, casuale | Se chiusa dentro area = +500 bonus punti |

### Power-up

| Icona | Effetto | Durata |
|-------|---------|--------|
| ⏱ | +10 secondi al timer | Istantaneo |
| ❄️ | Freeze tutti nemici | 3 secondi |
| ⚡ | Velocità giocatore ×2 | 5 secondi |
| 🛡 | Immunità linea in costruzione | 4 secondi |

Power-up spawna random in zona `FREE`, scompare se non raccolto in 8 secondi.

### Vite e Timer
- 3 vite per livello
- Timer countdown visibile in HUD
- Timer scade = vita persa, livello si resetta con vite rimaste
- 0 vite = Game Over

---

## 4. Sistema Livelli

### Progressione Difficoltà

| Fascia | Livelli | Timer | Nemici | % Target |
|--------|---------|-------|--------|----------|
| 1 | 1–10 | 90s | 1 ragno | 75% |
| 2 | 11–20 | 75s | 2 ragni + 1 scarafaggio | 75% |
| 3 | 21–30 | 60s | 2 ragni + 2 scarafaggi + 1 vespa | 80% |
| 4 | 31–40 | 50s | mix 4 nemici | 80% |
| 5 | 41–50 | 40s | mix 5+ nemici, lumache bonus | 85% |

### Formato JSON Livello

```json
{
  "id": 1,
  "background": "backgrounds/bg_01.png",
  "timeSeconds": 90,
  "targetPercent": 75,
  "enemies": [
    { "type": "spider", "count": 1, "speed": 80 }
  ],
  "powerups": ["time", "freeze"]
}
```

### Sistema Stelle

| Stelle | Condizione |
|--------|-----------|
| ⭐ | Completa livello (≥ targetPercent) |
| ⭐⭐ | ≥ 85% territorio conquistato |
| ⭐⭐⭐ | ≥ 95% territorio + tempo rimasto > 0 |

Progressione: livello N+1 si sblocca con almeno 1 stella su N. Stelle persistite in `SharedPreferences`.

---

## 5. UI/UX e Grafica

### Stile Visivo
- Sfondo campo: overlay scuro (rgba 0,0,0,0.85) su zona `FREE`
- Bordi campo: linee neon ciano
- Linea in costruzione: tratto giallo/arancio pulsante (animazione alpha)
- Area conquistata: sfondo AI modella con glow leggero ai bordi
- Font: Orbitron (Google Fonts) — stile arcade/sci-fi

### HUD In-Game
```
[❤ ❤ ❤]        [SCORE: 12450]        [⏱ 00:47]
[████████████░░░░░░░░] 62%      ★★☆
```

### Schermate

**Menu:** logo animato "GIRLS AI PANIC", pulsanti GIOCA / LIVELLI / CREDITI, musica loop, banner AdMob bottom.

**Level Select:** griglia 5×10. Livelli completati = stelline. Livelli bloccati = icona lucchetto.

**In-Game:** HUD top + campo di gioco + power-up animati.

**Level Complete:** sfondo AI rivelato al 100%, stelle animate (cascade), punteggio, pulsanti NEXT / MENU. AdMob interstitial ogni 3 livelli completati.

**Game Over:** animazione ragno che cammina sullo schermo, pulsanti RIPROVA / MENU.

---

## 6. Asset Sfondi

### Placeholder
- 50 PNG 480×800px, sfondo grigio (#555555)
- Testo centrato bianco: "LEVEL N — PLACEHOLDER"
- Generati programmaticamente all'avvio se assenti

### Sostituzione con Immagini AI Reali
1. Genera immagini modelle AI 480×800px (o superiore, ridimensionato)
2. Rinomina: `bg_01.png` … `bg_50.png`
3. Copia in `android/assets/backgrounds/`
4. Nessuna modifica al codice richiesta

---

## 7. AdMob Integration

- **Banner:** `MenuScreen` e `LevelSelectScreen` — bottom banner 320×50
- **Interstitial:** post `LevelCompleteScreen` ogni 3 livelli (contatore in `AdManager`)
- `AdManager.kt` wrappa SDK AdMob, espone `showInterstitialIfReady()` e `createBanner()`
- Test ad unit ID in debug, reali in release via `BuildConfig`

---

## 8. Persistenza Dati

`SharedPreferences` chiave `girls_panic_prefs`:
- `level_stars_N` (int 0–3) per ogni livello N
- `high_score` (int)
- `levels_completed` (int) — per trigger AdMob

---

## 9. Testing

- `TerritorySystem`: unit test chiusura area, calcolo %, edge cases (area vuota, area con nemico)
- `CollisionSystem`: unit test hit detection linea in costruzione
- `LevelLoader`: test parsing JSON tutti 50 livelli
- Integration test: completamento livello 1 end-to-end
