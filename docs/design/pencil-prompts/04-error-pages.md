# Pencil Prompt Pack: Error & Session Pages

> Run after 03-register-screens.md to create error screens.

---

## Prompt

Create error and session-related screens for HomeTusk.

---

## 1. 401 - Not Signed In

### Layout (all breakpoints)
- Background: #F5F2ED
- Content centered both vertically and horizontally
- No card wrapper

### Content (Desktop 1200px)
- Content container width: 360px
- Icon: Lock or user icon, 48px, color #888888
- Title: "Sign in required" - H2 (24px, weight 700)
- Description: "You need to sign in to access this page." - Body (16px, #888888)
- Primary button: "Sign in" - width fit-content, min-width 160px
- Gap between elements: 16px

### Breakpoints

**Tablet (1024px):**
- Content width: 340px
- Same layout

**Mobile (390px):**
- Full width with 24px padding
- Button full width

---

## 2. 403 - Access Denied

### Layout (all breakpoints)
- Background: #F5F2ED
- Content centered

### Content (Desktop 1200px)
- Content container width: 400px
- Icon: Shield or warning icon, 48px, color #D84315 (error)
- Title: "Access denied" - H2 (24px, weight 700, color #D84315)
- Description: "You don't have permission to view this page. If you believe this is a mistake, please contact support." - Body (16px, #888888)
- Secondary button: "Go back" - width fit-content
- Text link below: "Contact support" - centered

### Breakpoints

**Tablet (1024px):**
- Content width: 380px

**Mobile (390px):**
- Full width with 24px padding
- Button full width

---

## 3. Session Expired - Modal

### Modal Structure
- Backdrop: rgba(26, 26, 26, 0.5)
- Modal card: width 400px, padding 32px, border-radius 12px
- Shadow: 0 20px 40px rgba(18,18,18,0.12)

### Content
- Icon: Clock or timer icon, 40px, color #E8A055 (warning)
- Title: "Session expired" - H3 (20px, weight 700)
- Description: "Your session has expired. Please sign in again to continue." - Body (14px, #888888)
- Primary button: "Sign in again" - full width
- Text link: "Return to home" - centered below

### Variants

**Session Warning (before expiration):**
- Same modal but with:
- Title: "Session expiring soon"
- Description: "Your session will expire in 5 minutes. Would you like to stay signed in?"
- Two buttons: "Stay signed in" (Primary) and "Sign out" (Secondary/Ghost)

**Re-auth Failed:**
- Same modal but with ErrorBanner at top:
- "Unable to extend session. Please sign in again."

---

## 4. Session Expired - Mobile (390px)

On mobile, no modal - full page layout:
- Background: #F5F2ED
- Content centered with 24px padding
- Same content as modal but full-screen
- Button full width

---

## Naming Convention

| Frame Name | Description |
|------------|-------------|
| 401 - Not Signed In | Desktop 1200px |
| 401 - Not Signed In - Tablet | 1024px |
| 401 - Not Signed In - Mobile | 390px |
| 403 - Access Denied | Desktop 1200px |
| 403 - Access Denied - Tablet | 1024px |
| 403 - Access Denied - Mobile | 390px |
| Session Warning - Modal | 5-min warning |
| Session Expired - Modal | Desktop modal |
| Session Expired - Modal - Tablet | 1024px modal |
| Session Expired - Re-auth Failed | Error state |
| Session Expired - Mobile | 390px full-page |

---

## Expected Output

11 frames covering all 401, 403, and Session Expired variants.
