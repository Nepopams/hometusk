# Pencil Prompt Pack: Tasks Screens

> Run after 06-dashboard-app-shell.md to create Tasks List and Task Detail screens.

---

## Prompt

Create Tasks screens for HomeTusk using the established design system.

---

## 1. Tasks List Page

### Page Layout
- Background: #F5F2ED (within app shell)
- Content centered, max-width 900px
- Padding: 32px 24px

### Section Header
- Title: "TASKS" - uppercase, Space Grotesk, 16px, weight 700, letter-spacing 0.5px
- Right side: "Add Task" button (Primary, SM)

### Filter Bar
- Horizontal layout, gap 12px, flex-wrap
- Each filter:
  - Label above: 12px, weight 500, #888888
  - Select dropdown: height 36px, border 1px solid #1A1A1A, border-radius 8px
  - Font: 13px Inter
  - Chevron icon on right

**Filters:**
- Status (All, Open, In Progress, Done)
- Zone (All Zones, Kitchen, Living Room, etc.)
- Assignee (Everyone, specific members)

### Tasks Card Container
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px
- Overflow: hidden

### Task Item Row
- Padding: 14px
- Flex layout, align center, gap 12px
- Hover: background #F9F7F4
- Cursor: pointer

**Checkbox:**
- 20x20px
- Border: 2px solid #1A1A1A
- Border radius: 4px
- Done state: background #6B8E5E, checkmark white

**Content:**
- Title: 13px, Space Grotesk, weight 600
- Done title: #888888, line-through
- Meta row: flex wrap, gap 8px
  - Zone: 12px, #888888
  - Assignee: 12px, #888888
  - Deadline: 12px, #888888 (overdue: #D84315)

**Status Badge:**
- Pill shape, padding 3px 8px, border-radius 10px
- Font: 10px, weight 600, capitalize
- OPEN: background #E3F2FD, color #5B8FC4
- IN_PROGRESS: background #FFF3E0, color #E8A055
- DONE: background #E8F5E9, color #6B8E5E
- CANCELLED: background #FAFAFA, color #888888, border 1px solid #E0DDD8

### Divider between items
- Height: 1px, background #E0DDD8

### Empty State
- Centered, padding 32px 24px
- Icon: 24px, #888888
- Title: "No tasks yet" - 16px, weight 600
- Description: "Create your first task..." - 13px, #888888

### Loading State (Skeleton)
- Same layout as task item
- Checkbox: 20x20 animated background
- Lines: animated #F9F7F4, border-radius 4px

---

## 2. Task Detail Page

### Page Layout
- Background: #F5F2ED
- Content centered, max-width 720px
- Padding: 32px 24px

### Back Link
- "← Back to tasks" - 13px, #888888
- Hover: color #C45A3B

### Header
- Title: 24px, Space Grotesk, weight 700
- Done title: #888888, line-through
- Status badge next to title (larger: padding 6px 12px, 12px font)
- Description below: 14px, #888888, line-height 1.6

### Detail Card
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px

### Detail Rows
Each row:
- Padding: 14px
- Flex layout, gap 12px, align center
- Divider between rows

**Row structure:**
- Icon container: 36x36px, background #F9F7F4, border-radius 8px
- Content:
  - Label: 12px, uppercase, #BDBDBD, letter-spacing 0.3px
  - Value: 13px, Space Grotesk, weight 500

**Rows:**
1. Zone - grid icon
2. Assignee - user icon + avatar (36px circle, #C45A3B bg, white initials)
3. Deadline - calendar icon (overdue: value color #D84315)
4. Created - clock icon
5. Points - star icon

### Metadata Section
- Background: #F9F7F4
- Padding: 14px
- Font: 12px, #BDBDBD
- "Created by ... on ..."
- "Last updated ..."

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Tasks List - Desktop | With multiple tasks |
| Tasks List - Empty | No tasks state |
| Tasks List - Loading | Skeleton state |
| Tasks List - Filtered | Active filter indicator |
| Tasks List - Mobile | 390px, stacked filters |
| Task Detail - Desktop | Full task info |
| Task Detail - Done | Completed task |
| Task Detail - Loading | Skeleton state |
| Task Detail - Not Found | 404 state |
| Task Detail - Mobile | 390px breakpoint |

---

## Expected Output

10 frames showing Tasks List and Task Detail screens.
