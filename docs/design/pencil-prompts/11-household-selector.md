# Pencil Prompt Pack: Household Selector

> Run after 10-invites-screens.md to create Household selection/landing page.

---

## Prompt

Create the Household Selector (Landing) page for HomeTusk.

---

## Page Layout

Full-page layout (not within app shell):
- Min-height: 100vh
- Background: #F5F2ED

### Header Bar
- Height: 64px
- Background: #FFFFFF
- Border-bottom: 1px solid #E0DDD8
- Padding: 0 48px

**Left side:**
- Logo mark: 28x28px, background #C45A3B, border-radius 4px
- Brand: "HOMETUSK" - Space Grotesk, 16px, weight 700, letter-spacing 1px

**Right side:**
- Avatar: 32x32px circle, background #1A1A1A, color #C45A3B, initials
- Name: "John Doe" - 13px, weight 500
- Chevron icon

---

## Main Content (Two Columns)

Flex layout, gap 48px, padding 48px

### Left Column (Households List)
- Flex: 1

**Title:**
- "Your Households" - 20px, Space Grotesk, weight 600

**Household Cards:**
- Vertical stack, gap 12px

**Household Item Card:**
- Height: 72px
- Background: #FFFFFF
- Border: 2px solid #1A1A1A
- Border radius: 8px
- Padding: 16px 20px
- Flex layout, gap 16px
- Hover: border-color #C45A3B
- Cursor: pointer

**Icon:**
- 40x40px, border-radius 8px
- Background: #1A1A1A, color #C45A3B
- Home icon

**Info:**
- Name: 14px, Space Grotesk, weight 600
- Meta: "3 members" - 12px, #888888

**Open button:**
- "Open" - Primary, SM

---

### Right Column (Actions)
- Width: 360px

**Create Household Card:**
- Background: #FFFFFF
- Border: 2px solid #1A1A1A
- Border radius: 8px
- Padding: 24px
- Gap: 16px

**Header:**
- Icon: 36x36px, background #1A1A1A, color #C45A3B, plus icon
- Title: "Create Household" - 16px, weight 600

**Content:**
- Description: "Start a new household..." - 13px, #888888
- Input: "Household name" - height 40px, border 2px solid #1A1A1A
- Button: "Create" - Primary, height 44px

**Join Household Card:**
- Same styling

**Header:**
- Icon: background #E8F1F8, color #5B8FC4, key icon
- Title: "Join Household"

**Content:**
- Description: "Have an invite code?..." - 13px, #888888
- Input: "Enter code" - monospace font
- Button: "Join"

---

## Empty State (No Households)

Single-column centered layout:

**Icon:**
- 72x72px circle
- Background: #1A1A1A
- Home icon, color #C45A3B

**Text:**
- Title: "Welcome to HomeTusk" - 24px, weight 600
- Subtitle: "Create or join a household..." - 14px, #888888, max-width 400px

**Action Cards:**
- Two cards side by side (Create + Join)
- Max-width 480px

---

## Loading State

- Household list area shows skeleton cards
- 3 skeleton items with animated background

---

## Error State

- Warning box (background #FDF4E8, border #E8A055)
- Title: "Unable to load households"
- Message: error details
- Retry button

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Household Selector - Desktop | With households |
| Household Selector - Empty | Welcome state |
| Household Selector - Loading | Skeleton |
| Household Selector - Error | Error state |
| Household Selector - Tablet | 1024px |
| Household Selector - Mobile | 390px, stacked layout |

---

## Responsive Notes

**Tablet (1024px):**
- Padding: 32px
- Right column: 320px

**Mobile (390px):**
- Single column layout
- Header height: 56px
- Action cards first (order: -1)
- Households list below
- Cards stack vertically in empty state

---

## Expected Output

6 frames showing Household Selector states and breakpoints.
