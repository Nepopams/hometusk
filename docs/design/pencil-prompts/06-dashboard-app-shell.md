# Pencil Prompt Pack: Dashboard & App Shell

> Run after 05-progress-gamification.md to create the main app layout.

---

## Prompt

Create the main application shell and dashboard for HomeTusk.

---

## App Shell Layout

### Desktop (1200px+)

**Structure:**
- Full viewport height
- Sidebar on left: 240px fixed width
- Header on top of content area: 64px height
- Main content: fills remaining space

**Sidebar:**
- Background: #FFFFFF
- Border-right: 1px solid #E0DDD8
- Padding: 24px

**Sidebar Header:**
- Logo mark: 28x28px, background #C45A3B, border-radius 4px
- Brand: "HomeTusk" - Space Grotesk, 18px, weight 700
- Gap: 12px

**Sidebar Navigation:**
- Vertical list
- Gap: 4px between items
- Margin-top: 32px

**Nav Item:**
- Padding: 12px 16px
- Border-radius: 8px
- Font: Inter, 14px, weight 500
- Color: #888888 (default), #1A1A1A (active)
- Background: transparent (default), #F5F2ED (active)
- Hover: background #F9F7F4

**Nav Items:**
1. Dashboard (icon: home)
2. Tasks (icon: checklist)
3. Shopping (icon: cart)
4. Zones (icon: grid)
5. Members (icon: users)
6. Progress (icon: trophy/chart)
7. Commands (icon: terminal)
8. Invites (icon: mail)

**Sidebar Footer:**
- Household switcher dropdown
- Current household name
- User info section

### Header

- Background: #FFFFFF
- Border-bottom: 1px solid #E0DDD8
- Padding: 0 24px
- Height: 64px
- Flex layout: space-between

**Left side:**
- Page title: H2 (20px, weight 700)

**Right side:**
- Notification bell (with badge for unread count)
- User avatar/menu trigger

### Main Content Area

- Background: #F5F2ED
- Padding: 24px
- Overflow-y: auto

---

## Dashboard Page

### Stats Cards Row

4 cards in a row (grid, gap 20px):

**Card Template:**
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border-radius: 8px
- Padding: 20px

**Cards:**
1. "Open Tasks" - count + "tasks to do"
2. "Completed Today" - count + "tasks done"
3. "Shopping Items" - count + "items needed"
4. "Your Points" - count + "total points"

### Recent Tasks Section

- Title: "Recent Tasks" (18px, weight 600)
- "View all" link on right

**Task Table:**
- Headers: Task, Zone, Assignee, Status, Due
- Rows with task data
- Status badges (OPEN, DONE, etc.)

### Quick Actions Section

- Title: "Quick Actions" (18px, weight 600)

**Action Cards (grid, 3 columns):**
1. "Add Task" - icon + label, hover effect
2. "Add Shopping Item" - icon + label
3. "View Progress" - icon + label

---

## Mobile App Shell (390px)

**No sidebar** - bottom navigation instead

**Bottom Nav:**
- Fixed to bottom
- Background: #FFFFFF
- Border-top: 1px solid #E0DDD8
- Height: 64px
- 5 icons: Dashboard, Tasks, Shopping, Progress, More

**Header:**
- Hamburger menu on left (opens sheet with full nav)
- Logo/title center
- Notifications on right

---

## Frame Names

| Frame Name | Description |
|------------|-------------|
| App Shell - Desktop | Empty shell layout |
| Dashboard - Desktop | With content |
| Dashboard - Mobile | 390px bottom nav |
| App Shell - Mobile | Bottom nav layout |

---

## Expected Output

4 frames showing app shell and dashboard layouts.
