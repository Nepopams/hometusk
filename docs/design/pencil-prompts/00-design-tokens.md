# Pencil Prompt Pack: Design Tokens

> Run this prompt first to set up the design system variables.

---

## Prompt

Create a design system for HomeTusk app with the following design tokens (variables):

### Color Palette

**Background Colors:**
- `bg-page`: #F5F2ED (warm cream - page background)
- `bg-card`: #FFFFFF (white - card surfaces)
- `bg-dark`: #1A1A1A (dark surfaces)
- `bg-hover`: #E8E5E0 (hover state)
- `bg-hover-subtle`: #F9F7F4 (subtle hover)
- `bg-pressed`: #EBE8E3 (pressed state)

**Text Colors:**
- `text-primary`: #1A1A1A (main text)
- `text-secondary`: #888888 (muted text)
- `text-muted`: #BDBDBD (placeholder, disabled)
- `text-inverse`: #F5F2ED (text on dark bg)

**Brand Colors (Terracotta):**
- `terracotta`: #C45A3B (primary action)
- `terracotta-hover`: #A84A30 (hover state)
- `terracotta-pressed`: #8E3F28 (pressed state)

**Border Colors:**
- `border-primary`: #1A1A1A (strong borders)
- `border-subtle`: #E0DDD8 (default borders)
- `border-muted`: #F0EDE8 (very light borders)

**Semantic Colors:**
- `error`: #D84315
- `error-soft`: #FFEBE6
- `success`: #6B8E5E
- `success-soft`: #E8F0E5
- `warning`: #E8A055
- `warning-soft`: #FDF4E8
- `info`: #5B8FC4
- `info-soft`: #E8F1F8

**Focus:**
- `focus-ring`: rgba(196, 90, 59, 0.25)

### Spacing Scale
- 2px, 4px, 6px, 8px, 12px, 16px, 20px, 24px, 28px, 32px, 40px, 48px, 56px, 64px

### Border Radius
- none: 0
- sm: 4px
- md: 8px (inputs, buttons)
- lg: 12px (cards)
- full: 999px (pills, avatars)

### Typography

**Font Families:**
- Heading: "Space Grotesk" (brand name, titles)
- Body: "Inter" (body text, labels)

**Type Scale:**
| Style | Size | Weight | Line Height |
|-------|------|--------|-------------|
| H1 | 32px | 700 | 1.2 |
| H2 | 24px | 700 | 1.3 |
| H3 | 20px | 700 | 1.4 |
| Body | 16px | 400 | 1.5 |
| Body Small | 13px | 500 | 1.5 |
| Caption | 12px | 400 | 1.4 |
| Label | 14px | 600 | 1.3 |

### Shadows
- sm: `0 1px 2px rgba(18,18,18,0.05)`
- md: `0 4px 12px rgba(18,18,18,0.08)`
- lg: `0 10px 24px rgba(18,18,18,0.08)` (cards)
- xl: `0 20px 40px rgba(18,18,18,0.12)` (modals)

### Component Sizes

**Buttons:**
- Height SM: 36px
- Height MD: 44px
- Height LG: 48px
- Padding X: 24px
- Padding X SM: 16px
- Border Radius: 8px

**Inputs:**
- Height: 48px
- Padding X: 16px
- Padding Y: 12px
- Border Radius: 8px

**Cards (Auth):**
- Desktop: width=420px, padding=48px
- Tablet: width=400px, padding=40px
- Mobile: width=100%, padding=24px
- Border Radius: 12px

---

## Expected Output

A "Design System" frame on the canvas with:
1. Color palette swatches
2. Typography samples
3. Spacing scale visualization
4. Border radius examples

All variables should be defined in the document variables panel.
