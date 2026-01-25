# PR0: Design Tokens Proposal

> Minimal token set extracted from Pencil (`untitled.pen`)
> Ready for implementation in `styles/tokens.css`

---

## 1. Color Tokens

### Background

| Token | Role | Value | Source |
|-------|------|-------|--------|
| `--color-bg-page` | Page background | `#F5F2ED` | `$--bg-page` |
| `--color-bg-card` | Card/surface background | `#FFFFFF` | `$--bg-card` |
| `--color-bg-dark` | Dark surfaces (footer, etc) | `#1A1A1A` | `$--bg-dark` |
| `--color-bg-hover` | Hover state background | `#E8E5E0` | `$--bg-hover` |
| `--color-bg-hover-subtle` | Subtle hover | `#F9F7F4` | `$--bg-hover-subtle` |
| `--color-bg-pressed` | Pressed state | `#EBE8E3` | `$--bg-pressed` |

### Text

| Token | Role | Value | Source |
|-------|------|-------|--------|
| `--color-text-primary` | Primary text | `#1A1A1A` | `$--text-primary` |
| `--color-text-secondary` | Secondary/muted text | `#888888` | `$--text-secondary` |
| `--color-text-muted` | Placeholder, disabled | `#BDBDBD` | `$--text-muted` |
| `--color-text-inverse` | Text on dark bg | `#F5F2ED` | `$--text-inverse` |

### Brand (Primary Action)

| Token | Role | Value | Source |
|-------|------|-------|--------|
| `--color-brand` | Primary action color | `#C45A3B` | `$--terracotta` |
| `--color-brand-hover` | Primary hover | `#A84A30` | `$--terracotta-hover` |

### Border

| Token | Role | Value | Source |
|-------|------|-------|--------|
| `--color-border-primary` | Strong borders | `#1A1A1A` | `$--border-primary` |
| `--color-border-subtle` | Default borders | `#E0DDD8` | `$--border-subtle` |
| `--color-border-muted` | Very light borders | `#F0EDE8` | `$--border-muted` |

### Semantic

| Token | Role | Value | Source |
|-------|------|-------|--------|
| `--color-error` | Error text/icon | `#D84315` | `$--error` |
| `--color-error-soft` | Error background | `#FFEBE6` | `$--error-soft` |
| `--color-success` | Success text/icon | `#6B8E5E` | `$--success` |
| `--color-success-soft` | Success background | `#E8F0E5` | `$--success-soft` |
| `--color-warning` | Warning text/icon | `#E8A055` | `$--warning` |
| `--color-warning-soft` | Warning background | `#FDF4E8` | `$--warning-soft` |
| `--color-info` | Info text/icon | `#5B8FC4` | `$--info` |
| `--color-info-soft` | Info background | `#E8F1F8` | `$--info-soft` |

### Focus

| Token | Role | Value | Source |
|-------|------|-------|--------|
| `--color-focus-ring` | Focus outline | `#C45A3B40` | `$--focus-ring` |

---

## 2. Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `--spacing-2` | 2px | Micro adjustments |
| `--spacing-4` | 4px | Tight spacing |
| `--spacing-6` | 6px | Form field gaps |
| `--spacing-8` | 8px | Small gaps |
| `--spacing-12` | 12px | Medium-small gaps |
| `--spacing-16` | 16px | Default gap |
| `--spacing-20` | 20px | Form section gaps |
| `--spacing-24` | 24px | Card padding (mobile) |
| `--spacing-28` | 28px | Tablet card gap |
| `--spacing-32` | 32px | Desktop card gap, modal padding |
| `--spacing-40` | 40px | Tablet card padding |
| `--spacing-48` | 48px | Desktop card padding |
| `--spacing-56` | 56px | — |
| `--spacing-64` | 64px | Large section spacing |

---

## 3. Border Radius Scale

| Token | Value | Usage |
|-------|-------|-------|
| `--radius-none` | 0 | Sharp corners |
| `--radius-sm` | 4px | Small elements |
| `--radius-md` | 8px | Inputs, buttons |
| `--radius-lg` | 12px | Cards |
| `--radius-full` | 999px | Pills, avatars |

---

## 4. Border Width

| Token | Value | Usage |
|-------|-------|-------|
| `--border-thin` | 1px | Card borders |
| `--border-standard` | 2px | Inputs, buttons, modals |
| `--border-emphasis` | 4px | — |

---

## 5. Typography

### Font Families

| Token | Value | Usage |
|-------|-------|-------|
| `--font-heading` | `"Space Grotesk"` | Brand name, titles (auth) |
| `--font-body` | `"Inter"` | Body text, labels |

> Note: Design System frame uses DM Sans but Auth screens use Space Grotesk + Inter

### Type Scale (from Design System)

| Style | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| H1 | 32px | 700 | 1.2 | Page titles |
| H2 | 24px | 700 | 1.3 | Section titles |
| H3 | 20px | 700 | 1.4 | Card titles |
| Body | 15-16px | 400-500 | 1.5 | Default text |
| Body Small | 13px | 500 | 1.5 | Secondary info |
| Caption | 12px | 400 | 1.4 | Metadata |
| Label | 13-14px | 600 | 1.3 | Form labels |

### Recommended CSS Tokens

```css
--font-size-xs: 0.75rem;   /* 12px - caption */
--font-size-sm: 0.8125rem; /* 13px - label, small */
--font-size-base: 0.875rem; /* 14px - default */
--font-size-md: 1rem;       /* 16px - body */
--font-size-lg: 1.125rem;   /* 18px */
--font-size-xl: 1.25rem;    /* 20px - H3 */
--font-size-2xl: 1.5rem;    /* 24px - H2 */
--font-size-3xl: 2rem;      /* 32px - H1 */
```

---

## 6. Focus Ring

| Token | Value | Notes |
|-------|-------|-------|
| `--focus-ring-width` | 3px | From `$--focus-ring-width` |
| `--focus-ring-color` | `rgba(196, 90, 59, 0.25)` | Semi-transparent brand |
| `--focus-ring-offset` | 2px | Proposed |

---

## 7. Component Sizes (Derived)

### Buttons

| Token | Value | Source |
|-------|-------|--------|
| `--btn-height` | 40px | Button/Primary height |
| `--btn-height-lg` | 48px | Auth form buttons |
| `--btn-padding-x` | 24px | Primary/Secondary |
| `--btn-padding-x-sm` | 16px | Ghost button |

### Inputs

| Token | Value | Source |
|-------|-------|--------|
| `--input-height` | 40-48px | Input component |
| `--input-padding-x` | 12-16px | Input padding |

### Cards (Auth)

| Token | Desktop | Tablet | Mobile |
|-------|---------|--------|--------|
| `--card-width` | 420px | 400px | 100% |
| `--card-padding` | 48px | 40px | 24px |
| `--card-gap` | 32px | 28px | 24px |
| `--card-radius` | 12px | 12px | 0 (no card) |

---

## 8. Shadows (Proposed)

| Token | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `0 1px 2px rgba(18,18,18,0.05)` | Subtle |
| `--shadow-md` | `0 4px 12px rgba(18,18,18,0.08)` | Dropdowns |
| `--shadow-lg` | `0 10px 24px rgba(18,18,18,0.08)` | Cards |
| `--shadow-xl` | `0 20px 40px rgba(18,18,18,0.12)` | Modals |

---

## Implementation Notes

1. **Google Fonts import:**
   ```css
   @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&display=swap');
   ```

2. **Token file location:** `clients/web/src/styles/tokens.css`

3. **Import in index.css:**
   ```css
   @import './tokens.css';
   ```

4. **Usage in components:**
   ```css
   .btn-primary {
     background-color: var(--color-brand);
     color: var(--color-text-inverse);
   }
   ```
