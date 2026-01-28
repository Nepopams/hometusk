# Pencil Prompt Pack: Invites Screens

> Run after 09-members-zones-screens.md to create Invites management screens.

---

## Prompt

Create Invites screens for HomeTusk using the established design system.

---

## Invites Page

### Page Layout
- Background: #F5F2ED
- Content centered, max-width 720px
- Padding: 32px 24px
- Gap between sections: 24px

---

## Section 1: Create Invite

### Section Title
- "CREATE INVITE" - uppercase, Space Grotesk, 16px, weight 700

### Card
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px
- Padding: 20px
- Gap: 16px

### Content
- Description: "Generate an invite code..." - 13px, #888888
- Primary button: "Generate Invite Code"

### Result (after generating)
- Container: background #F9F7F4, padding 16px, border-radius 8px

**Code row:**
- Label: "Code:" - 12px, weight 600, #888888
- Value: "ABC123XY" - monospace font, 14px, weight 700
- Copy button (Ghost, SM)

**Link row:**
- Label: "Link:" - 12px, weight 600, #888888
- Value: URL (truncated) - 12px, #5B8FC4
- Copy button (Ghost, SM)

**Meta:**
- Expiry badge: "Expires in 7 days" - success badge style
- "Share this code..." hint text

---

## Section 2: Active Invites

### Section Title
- "ACTIVE INVITES" - uppercase

### Card with list
- Same card styling

### Invite Item Row
- Padding: 14px
- Flex layout, justify space-between

**Info:**
- Code: monospace, 14px, weight 700
- Used code: #BDBDBD color
- Meta row:
  - "Created Jan 15" - 12px, #888888
  - Status badge

**Status Badges:**
- ACTIVE: background #E8F5E9, color #6B8E5E
- USED: background #FAFAFA, color #888888, border
- EXPIRED: background #FAFAFA, color #888888, border

**Actions:**
- Copy button (Ghost, SM)
- Revoke button (Ghost, SM, destructive - only for active)

### Empty State
- "No active invites"
- "Generate an invite code above..."

---

## Section 3: Join Household

### Section Title
- "JOIN HOUSEHOLD" - uppercase

### Card
- Padding: 20px
- Gap: 16px

### Content
- Label: "Enter invite code" - 13px, weight 600
- Input: invite code input, monospace placeholder
- Primary button: "Join"

### Error State
- Error banner below input
- Background #FFEBE6, border #D84315
- "Invalid or expired invite code"

### Success State
- Success banner
- "Successfully joined [Household Name]!"
- Button: "Go to household"

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Invites - Desktop | All sections |
| Invites - With Generated | After generating code |
| Invites - With Active List | Multiple invites |
| Invites - Join Error | Invalid code |
| Invites - Join Success | After joining |
| Invites - Empty | No active invites |
| Invites - Loading | Skeleton |
| Invites - Mobile | 390px breakpoint |

---

## Expected Output

8 frames showing Invites page states.
