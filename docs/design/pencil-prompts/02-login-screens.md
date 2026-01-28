# Pencil Prompt Pack: Login Screens

> Run after 01-base-components.md to create Login screen variants.

---

## Prompt

Create Login screen designs for HomeTusk using the established design system.

### Screen Structure

**Page Layout:**
- Background: #F5F2ED (bg-page)
- Centered vertically and horizontally
- Content max-width varies by breakpoint

**Card Container (Desktop/Tablet only):**
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 12px
- Shadow: 0 10px 24px rgba(18,18,18,0.08)

### Brand Header

At top of form:
- Logo mark: 32x32px square, background #C45A3B, border-radius 4px
- Brand name: "HomeTusk" in Space Grotesk, 20px, weight 700, #1A1A1A
- Tagline: "Welcome back" in Inter, 16px, weight 400, #888888
- Gap between logo+name and tagline: 8px

### Form Fields

1. **Email field:**
   - Label: "Email"
   - Input type: email
   - Placeholder: "you@example.com"

2. **Password field:**
   - Label: "Password"
   - Input type: password with show/hide toggle
   - Placeholder: dots

Gap between fields: 20px

### Actions

- Primary button: "Sign in" - full width, height 48px
- Below button: "Forgot password?" link centered

Divider with "or" text

- "Create account" link centered

Gap between elements: 16px

---

## Screen Variants

### 1. Login - Desktop (1200px)

- Frame: 1200x800px, background #F5F2ED
- Card: centered, width 420px, padding 48px
- Gap between form sections: 32px

### 2. Login - Tablet (1024px)

- Frame: 1024x768px
- Card: width 400px, padding 40px
- Gap between form sections: 28px

### 3. Login - Mobile (390px)

- Frame: 390x844px
- NO card background (transparent)
- Full width with padding 24px on sides
- Content starts from top with 48px top padding
- Gap between form sections: 24px

### 4. Login - Validation Errors

Same as Desktop but with:
- Email field: error state with red border
- Error message below: "Please enter a valid email address"
- Password field: error state
- Error message: "Password is required"

### 5. Login - Wrong Credentials

Same as Desktop but with:
- ErrorBanner at top of form (after brand header)
- Title: "Unable to sign in"
- Description: "The email or password you entered is incorrect."

### 6. Login - Loading State

Same as Desktop but with:
- Form fields at 0.7 opacity
- "Sign in" button shows spinner instead of text
- Button disabled

---

## Naming Convention

| Frame Name | Description |
|------------|-------------|
| Login - Default | Desktop 1200px |
| Login - Validation Errors | Field-level errors |
| Login - Wrong Credentials | Server error banner |
| Login - Loading | Button spinner state |
| Login - Tablet | 1024px breakpoint |
| Login - Mobile | 390px breakpoint |

---

## Expected Output

6 frames showing all Login screen states and breakpoints.
