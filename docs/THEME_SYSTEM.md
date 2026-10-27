# Vitalith Multi-Theme Design Token System

This documentation describes the multi-theme architecture implemented for the Vitalith Health application, supporting 9 distinct clinical palettes across web stylesheets and native Jetpack Compose.

---

## 1. Supported Themes

| Key | Theme Name | Base | Primary Accent | Background | Use Case |
|---|---|---|---|---|---|
| `original` | Original Navy | Dark | `#2D9CDB` (Cyan) | `#060E1A` | Default clinical monitoring |
| `plum` | Plum & Sage | Dark | `#C87A9E` (Rose) | `#1A1219` | Evening & low-stimulation care |
| `forest` | Forest & Amber | Dark | `#5FC88A` (Emerald) | `#0B1410` | Vitality & biological telemetry |
| `mono` | Monochrome Slate | Dark | `#FFFFFF` (White) | `#0A0A0B` | Distraction-free clinical review |
| `indigo` | Royal Indigo | Dark | `#8A7AFF` (Violet) | `#08060F` | Diagnostic imaging & neurology |
| `sandstone` | Sandstone & Clay | Dark | `#E09050` (Clay) | `#1A130E` | Warm circadian balance |
| `arctic` | Arctic Ice | Light | `#4AA0D0` (Sky) | `#EAF2F7` | Daytime ambient clinical daylight |
| `rose` | Dusty Rose | Light | `#D87A9A` (Blush) | `#F7EEEE` | Soft pediatric & wellness care |
| `contrast` | High-Contrast AAA | Dark | `#FFFF00` (Yellow) | `#000000` | WCAG AAA accessibility |

---

## 2. Storage & Persistence
- **Storage Key**: `"app-theme"`
- **Behavior**:
  - Selection is saved synchronously in `SharedPreferences` (native) and `localStorage` (web).
  - On app launch with no user preference, matches system `prefers-color-scheme`:
    - System Dark $\rightarrow$ `original` (Original Navy)
    - System Light $\rightarrow$ `arctic` (Arctic Ice)
  - Switching themes applies immediately via Compose `StateFlow` and dynamic `ColorScheme` without reloading or recreating the activity.

---

## 3. How to Add a New Theme

To add a new theme (e.g., `emerald`):

### Step A: Add CSS Tokens to `src/styles/themes.css`
Add a new selector block under `[data-theme="emerald"]`:
```css
[data-theme="emerald"] {
  --bg-page: #04140E; --bg-surface: #082017; --bg-card: #0D2D20;
  --bg-elevated: #133D2C; --bg-input: #061A12; --bg-nav: rgba(8, 32, 23, 0.9);
  --accent: #10B981; --accent-bright: #34D399;
  --text-primary: #ECFDF5; --text-secondary: #A7F3D0;
  /* populate remaining standard tokens */
}
```

### Step B: Add Compose Tokens to `VitalithThemeTokens.kt`
Define `EmeraldTokens`:
```kotlin
val EmeraldTokens = VitalithThemeTokens(
  key = "emerald",
  label = "Emerald Glow",
  isDark = true,
  bgPage = Color(0xFF04140E),
  bgSurface = Color(0xFF082017),
  bgCard = Color(0xFF0D2D20),
  bgElevated = Color(0xFF133D2C),
  bgInput = Color(0xFF061A12),
  bgNav = Color(0xE6082017),
  // ...
  accent = Color(0xFF10B981),
  accentBright = Color(0xFF34D399)
)
```
Add to `ALL_THEMES`:
```kotlin
ThemeDescriptor("emerald", "Emerald Glow", Color(0xFF10B981), Color(0xFF04140E), true, EmeraldTokens)
```

### Step C: Add to Web `THEMES` Array in `src/theme/ThemeProvider.tsx`
```typescript
export const THEMES = [
  // ... existing themes
  { key: 'emerald', label: 'Emerald Glow' }
] as const;
```

---

## 4. Files Created & Modified

1. `src/styles/themes.css`: Canonical tokens for all 9 themes with base `:root` fallback.
2. `public/index.html`: Pre-hydration `<script>` in `<head>` preventing flash of unstyled theme.
3. `src/theme/ThemeProvider.tsx`: TypeScript React context + hook + `localStorage` persistence.
4. `app/src/main/java/com/example/ui/theme/VitalithThemeTokens.kt`: Kotlin data classes, `toColorScheme()` mapping, and token sets for all 9 themes.
5. `app/src/main/java/com/example/ui/theme/ThemeManager.kt`: Persistent Android state manager backing `"app-theme"` with zero-flash startup.
6. `app/src/main/java/com/example/ui/theme/Theme.kt`: Dynamically applies `MaterialTheme(colorScheme = currentTokens.toColorScheme())` and `LocalVitalithThemeTokens`.
7. `app/src/main/java/com/example/MainActivity.kt`: Synchronous `ThemeManager.init(this, isDark)` call in `onCreate()`.
8. `app/src/main/java/com/example/ui/screens/HomeScreen.kt`: Injected Section 3.5 Appearance radio group with color swatches into existing `UserMenuDropdown`.
