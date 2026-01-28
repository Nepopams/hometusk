# Pencil Prompt Pack: Members & Zones Screens

> Run after 08-shopping-screens.md to create Members and Zones list screens.

---

## Prompt

Create Members and Zones screens for HomeTusk using the established design system.

---

## 1. Members List Page

### Page Layout
- Background: #F5F2ED
- Content centered, max-width 720px
- Padding: 32px 24px

### Section Header
- Title: "MEMBERS" - uppercase, Space Grotesk, 16px, weight 700
- Right side: "Invite" button (Primary, SM)

### Members Card Container
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px

### Member Item Row
- Padding: 14px
- Flex layout, align center, gap 12px
- Divider between items

**Avatar:**
- 40x40px circle
- Background: #C45A3B
- Color: white (inverse)
- Font: 13px, Space Grotesk, weight 700, uppercase
- Shows initials (e.g., "JD")

**Info:**
- Name: 13px, Space Grotesk, weight 600
- Meta row: flex wrap, gap 8px
  - Email: 12px, #888888
  - Joined: "Joined Jan 15" - 12px, #BDBDBD

**Role Badge:**
- Pill shape, padding 3px 8px, border-radius 10px
- Font: 10px, weight 600, capitalize
- OWNER: background rgba(196,90,59,0.1), color #C45A3B
- MEMBER: background #FAFAFA, color #888888, border 1px solid #E0DDD8

### Empty State
- Centered, padding 24px
- Icon: users, 24px, #888888
- Title: "No members"
- Description: "Invite someone..."

---

## 2. Zones List Page

### Page Layout
- Same as Members

### Section Header
- Title: "ZONES" - uppercase
- Right side: "Add Zone" button (Primary, SM)

### Zones Card Container
- Same styling as Members card

### Zone Item Row
- Padding: 14px
- Flex layout, align center, gap 12px

**Icon container:**
- 36x36px
- Background: #1A1A1A (or color-coded per zone)
- Color: #C45A3B
- Border radius: 8px
- Grid/room icon

**Info:**
- Name: "Kitchen", "Living Room", etc. - 13px, Space Grotesk, weight 600
- Meta: "X tasks" - 12px, #888888

**Actions (on hover/mobile):**
- Edit button (Ghost, SM)
- Delete button (Ghost, SM, destructive)

### Create Zone Modal
- Modal overlay
- Title: "Create Zone"
- Input: "Zone name"
- Buttons: "Cancel" (Ghost), "Create" (Primary)

### Empty State
- Icon: grid, 24px, #888888
- Title: "No zones"
- Description: "Create zones to organize tasks..."

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Members - Desktop | Multiple members |
| Members - Empty | No members |
| Members - Loading | Skeleton |
| Members - Mobile | 390px |
| Zones - Desktop | Multiple zones |
| Zones - Empty | No zones |
| Zones - Loading | Skeleton |
| Zones - Mobile | 390px |
| Create Zone Modal | Modal state |

---

## Expected Output

9 frames showing Members and Zones screens.
