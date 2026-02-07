# Pencil Prompt Pack: Shopping Run Page

> Run after 08-shopping-screens.md to create the Shopping Run (shopping trip checklist) page.

---

## Prompt

Create the Shopping Run page for HomeTusk — a checklist interface for marking items as purchased during a shopping trip.

---

## 1. Page Layout

### Background
- Color: #F5F2ED (warm cream)

### Content Container
- Max-width: 720px
- Centered horizontally
- Padding: 32px 24px

---

## 2. Header Section

### Back Link
- "← Back to list" - 13px, #888888
- Hover: #C45A3B
- Margin-bottom: 16px

### Title Row
- Flex layout, justify space-between, align center
- Gap: 16px

**Left side:**
- List name: 24px, Space Grotesk, weight 700, #1A1A1A
- Status badge (if not ACTIVE): inline, margin-left 12px

**Right side (ACTIVE only):**
- "Cancel" button: Ghost, red variant (#D84315)
- "Complete" button: Primary (#C45A3B)

### Status Badges

**ACTIVE:**
- Not shown (implicit)

**COMPLETED:**
- Background: #E8F0E5
- Color: #6B8E5E
- Text: "COMPLETED"
- 11px, weight 600, uppercase
- Padding: 4px 8px
- Border-radius: 4px

**CANCELLED:**
- Background: #F5F2ED
- Color: #888888
- Text: "CANCELLED"
- Same styling as COMPLETED

---

## 3. Progress Section

### Progress Card
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border-radius: 8px
- Padding: 16px 20px
- Margin-bottom: 16px

### Progress Bar Container
- Height: 8px
- Background: #E0DDD8
- Border-radius: 4px
- Overflow: hidden

### Progress Bar Fill
- Height: 100%
- Background: #6B8E5E (success green)
- Border-radius: 4px
- Transition: width 0.3s ease
- Width: calculated percentage

### Progress Text
- Below bar, margin-top 8px
- Flex layout, justify space-between

**Left:**
- "X of Y purchased" - 13px, #1A1A1A

**Right:**
- "XX%" - 13px, weight 600, #6B8E5E

---

## 4. Items Checklist Card

### Card Container
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border-radius: 8px

### Checklist Item Row
- Padding: 14px 16px
- Flex layout, align center, gap 12px
- Border-bottom: 1px solid #E0DDD8 (except last)
- Cursor: pointer (when ACTIVE)
- Hover (ACTIVE): background #F9F7F4

### Checkbox

**Unchecked:**
- Size: 24x24px
- Border: 2px solid #E0DDD8
- Border-radius: 6px
- Background: transparent

**Unchecked Hover:**
- Border-color: #C45A3B

**Checked (Purchased):**
- Background: #6B8E5E
- Border-color: #6B8E5E
- Checkmark icon: white, 14px

**Loading:**
- Border-color: #C45A3B
- Contains spinner (14px, #C45A3B)
- Cursor: wait

**Disabled (closed run):**
- Opacity: 0.6
- Cursor: default

### Item Content

**Item name:**
- 14px, Space Grotesk, weight 500, #1A1A1A
- Purchased: color #888888, text-decoration line-through

**Item meta (optional):**
- 12px, #BDBDBD
- Quantity/unit if present: "2 kg"

### Purchased Section Divider
- If mixing purchased/unpurchased:
- Background: #F9F7F4
- Padding: 8px 16px
- Text: "PURCHASED" - 11px, weight 600, #BDBDBD, uppercase
- Border-top: 1px solid #E0DDD8

---

## 5. Summary Section (Closed Runs Only)

### Summary Card
- Background: #F9F7F4
- Border: 1px solid #E0DDD8
- Border-radius: 8px
- Padding: 20px
- Margin-top: 16px

### Summary Stats
- Flex layout, gap 24px, justify center

**Stat item:**
- Text-align: center
- Number: 24px, Space Grotesk, weight 700
- Label: 12px, #888888

**Stats shown:**
- Purchased: number in #6B8E5E
- Skipped: number in #888888
- Total: number in #1A1A1A

### Summary Layout
```
┌─────────────────────────────────────────┐
│                                         │
│     5            2            7         │
│  Purchased    Skipped       Total       │
│                                         │
└─────────────────────────────────────────┘
```

---

## 6. Complete Modal

### Modal Container
- Standard modal (from 01-base-components)
- Max-width: 400px

### Header
- Icon: Checkmark circle, 24px, #6B8E5E
- Title: "Complete shopping trip?" - 18px, weight 700

### Body
- Text: "You've purchased X of Y items." - 14px, #1A1A1A
- Warning (if items remaining): "Z items will be marked as skipped." - 13px, #888888

### Actions
- "Cancel" button: Ghost
- "Complete Trip" button: Primary, success variant (#6B8E5E)

### Loading State
- Buttons disabled
- "Complete Trip" shows spinner

---

## 7. Cancel Modal

### Modal Container
- Standard modal
- Max-width: 400px

### Header
- Icon: X circle, 24px, #D84315
- Title: "Cancel shopping trip?" - 18px, weight 700

### Body
- Text: "This will discard your progress." - 14px, #1A1A1A
- Note: "X items were marked as purchased." - 13px, #888888

### Actions
- "Keep Shopping" button: Ghost
- "Cancel Trip" button: Primary, destructive variant (#D84315)

---

## 8. Empty State

### When list has no items
- Centered in card
- Icon: Shopping cart, 40px, #BDBDBD
- Title: "No items in this run" - 16px, weight 600
- Description: "This shopping list was empty when the trip started." - 13px, #888888

---

## 9. Loading State

### Skeleton
- Progress bar: shimmer animation
- 5 item rows with:
  - Checkbox placeholder: 24x24px rounded
  - Text placeholder: 60% width line
  - Meta placeholder: 30% width line

---

## 10. Mobile Adaptations (390px)

### Header
- Stack layout (title above buttons)
- Buttons: full width, stack vertically
- Gap: 8px between buttons

### Progress Card
- Padding: 12px 16px

### Item Rows
- Padding: 12px 14px
- Checkbox: 22x22px

### Summary Stats
- Smaller gap: 16px
- Number: 20px

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Shopping Run - Active | In progress, some purchased |
| Shopping Run - Active Empty | No items purchased yet |
| Shopping Run - Completed | Closed with summary |
| Shopping Run - Cancelled | Closed, cancelled state |
| Shopping Run - Loading | Skeleton state |
| Shopping Run - Complete Modal | Confirmation dialog |
| Shopping Run - Cancel Modal | Cancel confirmation |
| Shopping Run - Mobile Active | 390px, active state |
| Shopping Run - Mobile Completed | 390px, with summary |

---

## Expected Output

9 frames showing Shopping Run page states and modals.

---

## Implementation Reference

```
clients/web/src/routes/
├── ShoppingRun.tsx
└── ShoppingRun.css

Route: /households/:householdId/shopping-runs/:runId
```
