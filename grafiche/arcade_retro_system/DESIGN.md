---
name: Arcade Retro System
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#20201f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353535'
  on-surface: '#e5e2e1'
  on-surface-variant: '#cac8aa'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#939277'
  outline-variant: '#484831'
  surface-tint: '#cdcd00'
  primary: '#ffffff'
  on-primary: '#323200'
  primary-container: '#eaea00'
  on-primary-container: '#686800'
  inverse-primary: '#626200'
  secondary: '#d3fbff'
  on-secondary: '#00363a'
  secondary-container: '#00eefc'
  on-secondary-container: '#00686f'
  tertiary: '#ffffff'
  on-tertiary: '#680012'
  tertiary-container: '#ffdad8'
  on-tertiary-container: '#ca002d'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#eaea00'
  primary-fixed-dim: '#cdcd00'
  on-primary-fixed: '#1d1d00'
  on-primary-fixed-variant: '#494900'
  secondary-fixed: '#7df4ff'
  secondary-fixed-dim: '#00dbe9'
  on-secondary-fixed: '#002022'
  on-secondary-fixed-variant: '#004f54'
  tertiary-fixed: '#ffdad8'
  tertiary-fixed-dim: '#ffb3b2'
  on-tertiary-fixed: '#410008'
  on-tertiary-fixed-variant: '#92001e'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353535'
typography:
  headline-xl:
    fontFamily: Space Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.1'
    letterSpacing: -0.05em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: 0.02em
  body-md:
    fontFamily: Be Vietnam Pro
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
    letterSpacing: '0'
  label-sm:
    fontFamily: Space Grotesk
    fontSize: 12px
    fontWeight: '700'
    lineHeight: '1'
    letterSpacing: 0.1em
spacing:
  unit: 4px
  gutter: 16px
  margin: 24px
  container-max: 1200px
---

## Brand & Style

This design system is a high-octane tribute to the 90s arcade era, specifically drawing from the frenetic energy of puzzle-action classics. The brand personality is electric, competitive, and unashamedly nostalgic, aiming to evoke the "Insert Coin" excitement of a smoke-filled game center. 

The aesthetic is a hybrid of **Retro-Skeuomorphism** and **High-Contrast Brutalism**. It utilizes the physical metaphors of CRT cabinets—beveled plastic, glowing phosphors, and metallic framing—while maintaining a flat, grid-based logic for modern usability. The target audience includes retro enthusiasts and gamers who value high-energy feedback and distinctive, expressive interfaces over muted corporate minimalism.

## Colors

The palette is built on a foundation of "True Black" and deep charcoals to simulate the glass of an unpowered CRT monitor. Against this dark void, we use high-saturation neon primaries that mimic light-emitting phosphors.

- **Primary (Electric Yellow):** Used for critical path actions, high scores, and active player indicators.
- **Secondary (Cyber Blue):** Reserved for technical readouts, secondary buttons, and UI chrome accents.
- **Tertiary (Hazard Red):** Dedicated to warnings, countdowns, and "Game Over" states.
- **Success (Nuclear Green):** Used for progress completion, health bars, and "Player Ready" prompts.

Gradients should be used sparingly and only to simulate metallic bezels or the inner glow of a cathode-ray tube.

## Typography

This design system uses a high-contrast pairing to balance retro character with modern legibility. 

**Headlines & UI Labels:** Use **Space Grotesk**. Its geometric, technical construction mimics the look of high-end pixel fonts when set in all-caps or heavy weights. For a true "pixel-art" effect in headlines, use CSS `text-shadow` to create a stepped, non-blurred outline or a "drop-shadow" effect using the primary color.

**Body Text:** Use **Be Vietnam Pro**. This ensures that long-form instructions or technical descriptions remain readable at smaller sizes. 

All headings should favor uppercase styling to reinforce the aggressive arcade aesthetic.

## Layout & Spacing

The layout philosophy follows a **Fixed Grid** model reminiscent of a game screen. Elements are often "boxed in" by thick, decorative borders that act as a chassis.

- **The 4px Baseline:** All spacing and sizing must be a multiple of 4px to ensure alignment with the "pixel" logic.
- **Dense Information:** Spacing should be tight and high-density, mimicking the crowded HUDs (Heads-Up Displays) of 90s action games.
- **Scanline Overlays:** Critical layout containers should feature a subtle scanline overlay (1px horizontal lines with 50% opacity) to ground the elements in the CRT aesthetic.

## Elevation & Depth

Hierarchy is not conveyed through soft shadows or blurring, but through **Tonal Layering and Hard Bevels**.

- **Beveling:** Every interactive element uses a 2-pixel "inner highlight" (top/left) and "inner shadow" (bottom/right) to create a tactile, extruded plastic look.
- **Glows:** Active or "High-Tier" elements utilize a sharp outer glow (box-shadow) in their respective brand color, simulating light bleed on a monitor.
- **Layering:** The background is the lowest tier (Level 0). "Chassis" or "Cabinets" sit at Level 1 with metallic textures. Interactive UI sits at Level 2, appearing as if it is floating or "burned in" to the screen.

## Shapes

The shape language is strictly **Sharp (0px)**. Roundness is avoided to maintain the pixel-perfect integrity of the 90s aesthetic. 

The only exception to the sharp-corner rule is the outer "screen" container, which may have a subtle 4px radius to simulate the curved glass of an old monitor. However, all internal components—buttons, inputs, and cards—must maintain 90-degree angles to reinforce the digital, block-based nature of the grid.

## Components

### Buttons
Buttons are the primary interactive element. They feature a heavy 3D bevel. 
- **Default:** Bright yellow background, black text, 2px white top/left highlight.
- **Active/Pressed:** The bevel reverses (dark top/left, light bottom/right) and the element shifts 1px down and right to simulate a physical "click."
- **Hover:** A vibrant outer glow in the primary color is applied.

### Segmented Progress Bars
Health, energy, or loading bars must be segmented into discrete blocks rather than a smooth fill. Each segment is a rectangle with a 1px gap between them. As the bar fills, segments should "light up" with a neon glow.

### CRT Monitor Frames
All main content areas must be enclosed in a "Frame" component. This is a multi-layered border consisting of:
1. An outer black border.
2. A middle metallic "chrome" or "hazard stripe" layer.
3. An inner glow that bleeds slightly into the content area.

### Input Fields
Inputs are recessed into the UI (inner shadow) with a dark background. The cursor should be a solid, blinking block in the primary color, mimicking a command-line interface.

### Chips & Tags
Chips are styled as small "LED Indicators." They use the secondary blue or tertiary red and feature a small circular "bulb" icon next to the text that appears to be "on" or "off."