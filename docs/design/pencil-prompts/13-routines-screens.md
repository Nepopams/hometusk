# Pencil Prompt Pack: Routines Screens (EP-010)

> Run after 12-commands-screen.md to create Routines management screens.
> Covers: Routines List, Create/Edit Form, Delete/Pause Modals, Upcoming Instances.

---

## Prompt

Create Routines screens for HomeTusk using the established design system.

---

## 1. Routines List Page

### Page Layout
- Background: #F5F2ED (within app shell)
- Content centered, max-width 900px
- Padding: 32px 24px

### Section Header
- Title: "ROUTINES" - uppercase, Space Grotesk, 16px, weight 700, letter-spacing 0.5px
- Right side: "Create routine" button (Primary, SM)

### Routines Card Container
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px
- Box shadow: subtle
- Overflow: hidden

### Routine Item Row
- Padding: 12px 16px
- Flex layout, align center, justify space-between, gap 12px
- Divider between rows: 1px solid #E0DDD8

**Main Content (left):**
- Expand button: ▶/▼, 12px, #888888, hover #1A1A1A
- Title: 14px, Inter, weight 500, min-width 150px
- Zone: 14px, #888888, min-width 100px (or "—" if no zone)
- Frequency: 14px, #888888, min-width 100px
- Policy: 14px, #888888, min-width 100px

**Frequency display formats:**
- DAILY: "Daily"
- WEEKLY: "Mon, Wed, Fri" (comma-separated short day names)
- MONTHLY: "15th of month" (with ordinal suffix)
- EVERY_N_DAYS: "Every 3 days"

**Policy display:**
- ROUND_ROBIN: "Round-robin"
- FIXED: "Fixed: {name}"
- MANUAL: "Manual"

**Status Badge:**
- Pill shape, padding 2px 8px, border-radius 4px
- Font: 12px, weight 500, uppercase, letter-spacing 0.06em
- ACTIVE: background #E8F5E9, color #166534
- PAUSED: background #FEF9C3, color #854D0E

**Actions (right):**
- Pause/Resume button (Secondary, SM)
- Edit button: padding 4px 12px, border 1px solid #E0DDD8, border-radius 4px
- Delete button: padding 4px 12px, color #DC2626, border 1px solid #FECACA, border-radius 4px

### Empty State
- Centered, padding 32px 16px
- Title: "No routines yet" - 18px, weight 600
- Description: "No routines yet. Create your first routine to automate recurring tasks." - 14px, #888888
- CTA: "Create routine" button (Primary, MD)

### Loading State (Skeleton)
- 3 skeleton rows
- Each row: 2 animated lines (100% width, 60% width)
- Line height: 12px
- Animation: shimmer gradient

### Error State
- Flex layout with content and action
- Title: "Unable to load routines" - 16px, weight 600
- Message: "Check your connection and try again." - 14px, #888888
- Retry button (Primary, SM)

### 403 Access Denied
- Title: "Access Denied" - 16px, weight 600
- Message: "You do not have access to this household." - 14px, #888888
- "Back to Households" button (Primary, SM)

### List Footer (when 10+ routines)
- Hint text: "{count} routines in this household" - 12px, #888888

---

## 2. Upcoming Instances (Expandable)

### Container
- Background: #F9F7F4
- Border-top: 1px solid #E0DDD8
- Padding: 20px 24px

### Title
- "UPCOMING TASKS" - 12px, uppercase, weight 700, #888888, letter-spacing 0.08em
- Margin-bottom: 16px

### Instance List
- Vertical stack, gap 12px

### Instance Item
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px
- Padding: 16px 20px
- Flex layout, justify space-between

**Date:**
- Font: 14px, weight 500
- "Today", "Tomorrow", or "Wed, Jan 15"

**Assignee:**
- Font: 14px, #888888
- Fixed: "{name}"
- Round-robin: "Rotating"
- Manual: "Unassigned"

### Paused State
- Background: #FEF9C3 (warning soft)
- Message: "Routine is paused. Resume to see upcoming tasks." - 14px, #888888, italic

### Empty State
- Message: "No upcoming tasks scheduled." - 14px, #888888, italic

### Loading State
- 3 skeleton rows, height 40px each, shimmer animation

---

## 3. Create/Edit Routine Modal

### Modal
- Size: LG
- Title: "Create routine" or "Edit routine"

### Form Layout
- Vertical stack, gap 28px

### Form Sections

**1. Title Section:**
- Label: "Title" + red asterisk for required
- Input: height 40px, border 1px solid #E0DDD8, border-radius 8px
- Placeholder: "e.g. Clean kitchen"
- Error state: red border, light red background, error message below

**2. Description Section:**
- Label: "Description (optional)"
- Textarea: 3 rows, resize vertical, same styling as input
- Placeholder: "Add a helpful note"

**3. Zone Section:**
- Label: "Zone (optional)"
- Select dropdown: same styling as input
- Default option: "No zone"

**4. Frequency Section:**
- Label: "Frequency"
- Radio button group (horizontal wrap, gap 16px):
  - Daily (default)
  - Weekly
  - Monthly
  - Every N days

**Weekly sub-section (when selected):**
- Helper text: "Select days of the week" - 12px, #888888
- Checkbox grid: 4 columns (2 on mobile)
- Day labels: Mon, Tue, Wed, Thu, Fri, Sat, Sun

**Monthly sub-section (when selected):**
- Label: "Day of month"
- Select dropdown: 1-31

**Every N days sub-section (when selected):**
- Label: "Interval (days)"
- Number input: min 1, max 365

**5. Assignment Policy Section:**
- Label: "Assignment policy"
- Radio button group:
  - Round-robin (default)
  - Fixed
  - Manual

**Fixed sub-section (when selected):**
- Label: "Fixed assignee"
- Select dropdown with household members
- Error state if not selected

### Form Actions
- Flex layout, justify end, gap 16px
- Cancel button (Ghost, MD)
- Submit button (Primary, MD): "Create routine" or "Save changes"
- Loading state on submit

### Error Banner
- Background: light red
- Padding: 16px
- Border radius: 8px
- Error text: 14px, red

---

## 4. Delete Routine Modal

### Modal
- Size: SM
- Title: "Delete routine"

### Content
- Message: "Delete routine? Pending tasks will remain." - 14px
- Routine name in quotes: "{title}" - 14px, weight 500

### Error Display (if delete fails)
- Red background, padding, red text

### Actions
- Cancel button (Ghost, MD)
- Delete button (Primary, MD) - loading state during deletion

---

## 5. Pause Routine Modal

### Modal
- Size: SM
- Title: "Pause routine"

### Content
- Message: "Pause routine? No new tasks will be generated while paused." - 14px

### Actions
- Cancel button (Ghost, MD)
- Pause routine button (Primary, MD)

---

## 6. Status Badge Component

### Badge Variants
- ACTIVE: background #E8F5E9, color #166534, text "Active"
- PAUSED: background #FEF9C3, color #854D0E, text "Paused"
- DELETED: background #FEE2E2, color #991B1B, text "Deleted" (rare, usually hidden)

### Styling
- Display: inline-block
- Padding: 2px 8px
- Border radius: 4px
- Font: 12px, weight 500, uppercase, letter-spacing 0.06em

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Routines List - Desktop | With multiple routines, various statuses |
| Routines List - Empty | No routines state with CTA |
| Routines List - Loading | Skeleton state |
| Routines List - Error | API error with retry |
| Routines List - Access Denied | 403 state |
| Routines List - Mobile | 390px breakpoint |
| Routine Row - Expanded | With upcoming instances visible |
| Routine Row - Paused | Paused status, resume button |
| Upcoming Instances - Active | With 3-5 scheduled dates |
| Upcoming Instances - Paused | Yellow warning state |
| Upcoming Instances - Empty | No upcoming dates |
| Upcoming Instances - Loading | Skeleton state |
| Create Routine Modal - Empty | Initial form state |
| Create Routine Modal - Weekly | With day checkboxes shown |
| Create Routine Modal - Monthly | With day-of-month dropdown |
| Create Routine Modal - Every N Days | With interval input |
| Create Routine Modal - Fixed Policy | With assignee dropdown |
| Create Routine Modal - Errors | Validation errors shown |
| Edit Routine Modal | Pre-filled form |
| Delete Routine Modal | Confirmation state |
| Delete Routine Modal - Error | With error message |
| Pause Routine Modal | Confirmation state |
| Status Badge - All States | ACTIVE, PAUSED, DELETED variants |

---

## Expected Output

23 frames showing Routines management screens with all variants.
