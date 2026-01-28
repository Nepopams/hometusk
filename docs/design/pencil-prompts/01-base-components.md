# Pencil Prompt Pack: Base Components

> Run after 00-design-tokens.md to create reusable UI components.

---

## Prompt

Create the following reusable components using the established design tokens.

### 1. Button Component

Create button variants with these specifications:

**Button/Primary:**
- Background: #C45A3B (terracotta)
- Text: #F5F2ED (inverse)
- Border: 2px solid #C45A3B
- Height: 48px (LG), 44px (MD), 36px (SM)
- Padding horizontal: 24px
- Border radius: 8px
- Font: Inter, 14px, weight 600
- Hover state: background #A84A30
- States: Default, Hover, Focus (3px ring rgba(196,90,59,0.25)), Disabled (opacity 0.5), Loading (spinner)

**Button/Secondary:**
- Background: #FFFFFF
- Text: #1A1A1A
- Border: 2px solid #1A1A1A
- Same sizes as Primary
- Hover state: background #F9F7F4

**Button/Ghost:**
- Background: transparent
- Text: #1A1A1A
- Border: 2px solid #E0DDD8 (subtle)
- Hover: background #F9F7F4, border #1A1A1A

**Button/Text:**
- Background: transparent
- Text: #C45A3B (terracotta)
- No border
- Padding: 8px 12px
- Hover: background #F9F7F4

### 2. Input/TextField Component

- Height: 48px
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px
- Padding: 12px 16px
- Font: Inter, 16px, color #1A1A1A
- Placeholder color: #BDBDBD
- Focus state: border #C45A3B, box-shadow 0 0 0 3px rgba(196,90,59,0.25)
- Error state: border #D84315

Include:
- Label above (14px, weight 600)
- Error message below (13px, #D84315, with icon)
- Hint text option (13px, #888888)

### 3. Badge Components

Create badge variants (pill-shaped):

**Badge/Success:**
- Background: #E8F0E5
- Text: #166534
- Padding: 4px 12px
- Border radius: 999px
- Font: 12px, weight 500

**Badge/Warning:**
- Background: #FDF4E8
- Text: #92400E

**Badge/Error:**
- Background: #FFEBE6
- Text: #B91C1C

**Badge/Info:**
- Background: #E8F1F8
- Text: #0369A1

**Badge/Neutral:**
- Background: #F5F2ED
- Text: #1A1A1A

### 4. Card Component

- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 12px
- Shadow: 0 10px 24px rgba(18,18,18,0.08)
- Padding variants: none, sm (24px), md (40px), lg (48px)

### 5. ErrorBanner Component

- Background: #FFEBE6
- Border: 1px solid #D84315
- Border radius: 8px
- Padding: 16px
- Icon: error circle (left)
- Title: 14px, weight 600, #B91C1C
- Description: 14px, #B91C1C

### 6. Divider Component

- Height: 1px
- Background: #E0DDD8
- Optional text in center ("or") with padding

### 7. TextLink Component

- Text color: #C45A3B
- Font: Inter, 14px
- Underline on hover
- Focus ring: 3px rgba(196,90,59,0.25)

### 8. Spinner Component

- Circle stroke animation
- Sizes: SM (16px), MD (20px), LG (24px)
- Color: inherit from context (inverse for primary button)

---

## Expected Output

A "Components" frame containing all reusable components, each marked as reusable in Pencil:
- Button/Primary (with all states)
- Button/Secondary
- Button/Ghost
- Button/Text
- Input (with label, states)
- Badge/Success, Warning, Error, Info, Neutral
- Card
- ErrorBanner
- Divider
- TextLink
- Spinner
