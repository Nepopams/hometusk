# Pencil Prompt Pack: Shopping Enhancements

> Addendum to 08-shopping-screens.md — adds Share/Export, Marketplace links, and Start Trip features.

---

## Prompt

Enhance the Shopping Items page with Share/Export dropdown, Marketplace link-out buttons, and Start Trip functionality.

---

## 1. Enhanced Header Actions

### Header Layout Update
- Title row now includes action buttons on right side
- Flex layout, align center, gap 8px

### Start Trip Button
- Position: Right side of header
- Variant: Primary (#C45A3B)
- Size: Medium (height 40px)
- Icon: Play/arrow icon, 16px, left of text
- Text: "Start Trip"
- Disabled state: when list is empty (opacity 0.5, cursor not-allowed)

### Share/Export Dropdown Button
- Position: Left of Start Trip button
- Variant: Ghost
- Size: Medium (height 40px)
- Icon: Share/export icon, 16px
- Text: "Share"
- Chevron down icon on right

### Header Layout
```
┌─────────────────────────────────────────────────────────┐
│ ← Back to lists                                         │
│                                                         │
│ Weekly Groceries                    [Share ▼] [▶ Start] │
│ 12 items                                                │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Share/Export Dropdown Menu

### Dropdown Container
- Position: Below button, right-aligned
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border-radius: 8px
- Box-shadow: 0 4px 12px rgba(0,0,0,0.1)
- Min-width: 180px
- Padding: 4px 0

### Menu Items
- Padding: 10px 14px
- Hover: background #F9F7F4
- Cursor: pointer
- Flex layout, align center, gap 10px

**Copy as Text:**
- Icon: Clipboard, 16px, #888888
- Text: "Copy as text" - 13px, #1A1A1A

**Download CSV:**
- Icon: Download, 16px, #888888
- Text: "Download CSV" - 13px, #1A1A1A

### Divider
- Height: 1px
- Background: #E0DDD8
- Margin: 4px 0

### Success Feedback (Copy)
- After click: "Copied!" text replaces "Copy as text"
- Icon changes to checkmark, #6B8E5E
- Reverts after 2 seconds

---

## 3. Start Trip Modal

### Modal Container
- Standard modal (from 01-base-components)
- Max-width: 420px

### Header
- Icon: Shopping cart with play overlay, 24px, #C45A3B
- Title: "Start shopping trip?" - 18px, weight 700

### Body Content

**Summary line:**
- "You're about to start a trip with:" - 14px, #1A1A1A
- Margin-bottom: 12px

**Item count card:**
- Background: #F9F7F4
- Border-radius: 8px
- Padding: 16px
- Text-align: center
- Number: 32px, Space Grotesk, weight 700, #1A1A1A
- Label: "items to purchase" - 13px, #888888

**Note:**
- Margin-top: 12px
- "A snapshot of the current list will be created." - 13px, #888888

### Actions
- "Cancel" button: Ghost
- "Start Trip" button: Primary (#C45A3B)

### Loading State
- Buttons disabled
- "Start Trip" shows spinner

### Modal Layout
```
┌─────────────────────────────────────────┐
│  🛒▶  Start shopping trip?              │
├─────────────────────────────────────────┤
│                                         │
│  You're about to start a trip with:     │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │             12                  │    │
│  │      items to purchase          │    │
│  └─────────────────────────────────┘    │
│                                         │
│  A snapshot of the current list will    │
│  be created.                            │
│                                         │
├─────────────────────────────────────────┤
│           [Cancel]  [Start Trip]        │
└─────────────────────────────────────────┘
```

---

## 4. Marketplace Link-Out Buttons

### Position
- Each shopping item row
- Right side, before delete button
- Visible on hover (desktop) or always (mobile)

### Button Group Container
- Flex layout, gap 4px
- Opacity: 0 by default (desktop)
- Row hover: opacity 1
- Transition: opacity 0.15s

### Marketplace Button
- Size: 28x28px
- Background: transparent
- Border: 1px solid #E0DDD8
- Border-radius: 6px
- Hover: background #F9F7F4, border-color #C45A3B

### Marketplace Icons
- Size: 16x16px
- Can be:
  - Generic external link icon
  - Or marketplace logo (Ozon blue, YandexMarket yellow)

### Tooltip
- On hover: show marketplace name
- "Open in Ozon" / "Open in Yandex Market"
- Position: above button
- Background: #1A1A1A
- Color: white
- Font: 11px
- Padding: 4px 8px
- Border-radius: 4px

### Item Row with Marketplace
```
┌────────────────────────────────────────────────────────┐
│ ☐  Молоко 2.5%                      [O] [Y]  🗑       │
│    Added by Alex                                       │
└────────────────────────────────────────────────────────┘
     ^checkbox  ^name           ^marketplace ^delete
```

### Mobile Behavior
- Buttons always visible (no hover state)
- Slightly smaller: 26x26px

---

## 5. Enhanced Item Row Layout

### Desktop Layout (with all features)
```
┌──────────────────────────────────────────────────────────────┐
│ [☐]  Item Name                              [O][Y]    [🗑]  │
│      quantity • Added by Name                                │
└──────────────────────────────────────────────────────────────┘
```

### Spacing
- Checkbox: 22x22px
- Gap after checkbox: 12px
- Item content: flex 1
- Marketplace buttons: gap 4px
- Gap before delete: 8px
- Delete button: 32x32px

---

## 6. Mobile Adaptations

### Header
- Stack buttons below title
- Full width buttons
- Share button: 48% width
- Start Trip button: 48% width
- Flex layout, gap 8px

### Marketplace Buttons
- Always visible (no hover)
- Size: 26x26px

### Share Dropdown
- Full width on mobile
- Position: below button, full width of card

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Shopping Items - With Actions | Header with Share + Start Trip |
| Shopping Items - Share Dropdown | Dropdown menu open |
| Shopping Items - Start Trip Modal | Confirmation modal |
| Shopping Items - Start Trip Loading | Modal with spinner |
| Shopping Items - Marketplace Hover | Item with marketplace buttons |
| Shopping Items - Mobile Actions | Mobile header layout |

---

## Expected Output

6 frames showing enhanced Shopping Items features.

---

## Implementation Reference

```
clients/web/src/routes/
├── ShoppingDetail.tsx (enhanced)
└── ShoppingDetail.css (enhanced)

API functions:
- exportShoppingList(householdId, listId, format)
- getMarketplaceTemplates()
- createShoppingRun(householdId, listId)
```
