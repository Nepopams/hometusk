# Pencil Prompt Pack: Commands Screen

> Run after 11-household-selector.md to create the Commands/NL input page.

---

## Prompt

Create the Commands page for HomeTusk natural language input interface.

---

## Page Layout

Two-column layout within app shell:
- Max-width: 1200px
- Gap: 32px
- Left column: flex 1 (composer + result)
- Right column: 320px fixed (history)

---

## Left Column

### Section: Command Composer

**Section Title:**
- "COMMAND" - uppercase, Space Grotesk, 16px, weight 700

**Composer Card:**
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px
- Padding: 20px
- Gap: 16px

**Textarea wrapper:**
- Border: 1px solid #E0DDD8
- Border radius: 4px
- Padding: 12px
- Focus: border-color #C45A3B

**Textarea:**
- Min-height: 60px
- No border
- Font: 14px Inter
- Placeholder: "Type a command like 'Clean the kitchen tomorrow'..." (#888888)
- Resize: vertical

**Actions row:**
- Flex layout, gap 12px
- Help button: Ghost style, "? Help" with icon
- Submit button: Primary, "Send Command"

---

### Section: Result

**Empty State:**
- Centered, padding 40px 20px
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px
- Icon: terminal/command, 24px, #888888
- Title: "No command yet" - 16px, weight 600
- Description: "Enter a command above..." - 12px, #888888

**Result Card:**
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px
- Padding: 20px
- Gap: 16px

### Result Banners

**Success (EXECUTED):**
- Background: #E8F0E5
- Icon: checkmark circle, #6B8E5E
- Title: "Command executed" - 14px, weight 700, #6B8E5E
- Subtitle: interpreted command - 13px, #1A1A1A

**Warning (DEGRADED):**
- Background: #FDF4E8
- Icon: warning, #E8A055
- Title: "Degraded mode" - #E8A055
- Subtitle: explanation

**Info (NEEDS_INPUT):**
- Background: #E8F1F8
- Icon: question, #5B8FC4
- Title: "More information needed" - #5B8FC4
- Question below in larger text

**Error (REJECTED):**
- Background: #FFEBE6
- Icon: X circle, #D84315
- Title: "Command rejected" - #D84315
- Error details below

**Loading:**
- Background: #E8F1F8
- Spinner: 24px animated circle
- Title: "Processing..."

### Changes Section (after success)
- Title: "Changes made:" - 13px, weight 600
- List of changes with checkmark icons
- Each change: label (14px, weight 600) + detail (12px, #888888)

### Result Actions
- Border-top: 1px solid #E0DDD8
- Padding-top: 8px
- "New Command" button (Ghost)
- "View Details" collapse toggle

---

## Right Column

### Section: History

**Section Title:**
- "HISTORY" - uppercase

**History Card:**
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px

**History Item:**
- Padding: 14px
- Border-bottom: 1px solid #E0DDD8

**Item header:**
- Title: command text (truncated) - 13px, weight 500
- Status badge: 10px, uppercase, pill shape
  - SUCCESS: #E8F0E5 bg, #6B8E5E text
  - WARNING: #FDF4E8 bg, #E8A055 text
  - INFO: #E8F1F8 bg, #5B8FC4 text
  - ERROR: #FFEBE6 bg, #D84315 text

**Item time:**
- "2 minutes ago" - 12px, #888888

**Empty history:**
- "No commands yet" - 13px, #888888
- "Commands will appear here..." - 12px

**Clear button:**
- Footer with border-top
- "Clear History" - 12px, weight 500, #888888
- Hover: background #F9F7F4

---

## Mobile (768px and below)

- Single column layout
- History hidden, replaced with toggle button
- "Show History" button in top right
- History opens as full-screen sheet
- Sheet header: "History" + "Done" button

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Commands - Desktop | Empty state |
| Commands - Processing | Loading spinner |
| Commands - Executed | Success result |
| Commands - Needs Input | Question shown |
| Commands - Degraded | Warning state |
| Commands - Rejected | Error state |
| Commands - With History | Multiple history items |
| Commands - Mobile | 390px, toggle button |
| Commands - Mobile History | History sheet open |

---

## Expected Output

9 frames showing Commands page states.
