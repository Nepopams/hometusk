# Pencil Prompt Pack: Shopping Screens

> Run after 07-tasks-screens.md to create Shopping Lists and Shopping Items screens.

---

## Prompt

Create Shopping screens for HomeTusk using the established design system.

---

## 1. Shopping Lists Page

### Page Layout
- Background: #F5F2ED
- Content centered, max-width 720px
- Padding: 32px 24px

### Section Header
- Title: "SHOPPING LISTS" - uppercase, Space Grotesk, 16px, weight 700
- Right side: "New List" button (optional)

### Lists Card Container
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px

### List Item Row
- Padding: 14px
- Flex layout, align center, gap 12px
- Hover: background #F9F7F4
- Link (no text decoration)

**Icon container:**
- 36x36px
- Background: #1A1A1A
- Color: #C45A3B
- Border radius: 8px
- Cart/list icon

**Content:**
- Name: 13px, Space Grotesk, weight 600
- Meta: "X items" - 12px, #888888

**Right side:**
- Badge with count: min-width 20px, height 20px, padding 0 6px, background #C45A3B, color white, border-radius 10px, 12px font weight 600
- Chevron icon: #BDBDBD

### Empty State
- Centered, padding 24px
- Icon: cart/list, 24px, #888888
- Title: "No shopping lists" - 16px, weight 600
- Description: "Create a list..." - 13px, #888888

---

## 2. Shopping Items (Detail) Page

### Page Layout
- Background: #F5F2ED
- Content centered, max-width 720px
- Padding: 32px 24px

### Back Link
- "← Back to lists" - 13px, #888888

### Header
- Title: List name - 24px, Space Grotesk, weight 700
- Count: "X items" - 13px, #888888

### Items Card
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px

### Add Item Row (top)
- Padding: 12px 14px
- Border-bottom: 1px solid #E0DDD8
- Flex layout, gap 8px

**Input:**
- Flex: 1
- No border, transparent background
- Font: 13px Inter
- Placeholder: "Add item..." (#BDBDBD)

**Add button:**
- 32x32px
- Background: #C45A3B
- Color: white
- Border radius: 8px
- Plus icon

### Shopping Item Row
- Padding: 12px 14px
- Flex layout, gap 12px, align center
- Divider between items

**Checkbox:**
- 22x22px
- Border: 2px solid #E0DDD8
- Border radius: 4px
- Hover: border #C45A3B
- Checked: background #6B8E5E, border #6B8E5E, white checkmark

**Item info:**
- Name: 13px, Space Grotesk, weight 500
- Purchased name: line-through, #888888
- Meta: "Added by..." - 12px, #BDBDBD

**Delete button:**
- 32x32px, transparent, opacity 0 normally
- Hover on row: opacity 1
- Hover on button: background rgba(239,68,68,0.1), color #D84315
- Trash icon

### Purchased Section
- Background: #F9F7F4
- Section divider with label: "PURCHASED" - 12px, weight 600, #BDBDBD, uppercase

### Empty State
- Centered, padding 32px 24px
- Title: "No items yet"
- Description: "Add items to this list..."

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Shopping Lists - Desktop | Multiple lists |
| Shopping Lists - Empty | No lists |
| Shopping Lists - Loading | Skeleton |
| Shopping Lists - Mobile | 390px |
| Shopping Items - Desktop | With items |
| Shopping Items - With Purchased | Some items checked |
| Shopping Items - Empty | No items |
| Shopping Items - Loading | Skeleton |
| Shopping Items - Mobile | 390px |

---

## Expected Output

9 frames showing Shopping Lists and Shopping Items screens.
