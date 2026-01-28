# Pencil Prompt Pack: Register Screens

> Run after 02-login-screens.md to create Registration screen variants.

---

## Prompt

Create Register screen designs for HomeTusk using the established design system.

### Screen Structure

Same layout as Login screens:
- Background: #F5F2ED
- Card on Desktop/Tablet, no card on Mobile
- Centered layout

### Brand Header

- Same as Login
- Tagline: "Create your account"

### Form Fields

1. **Name field:**
   - Label: "Name"
   - Input type: text
   - Placeholder: "Your name"

2. **Email field:**
   - Label: "Email"
   - Input type: email
   - Placeholder: "you@example.com"

3. **Password field:**
   - Label: "Password"
   - Input type: password with show/hide toggle
   - Hint below: "At least 8 characters"

Gap between fields: 20px

### Actions

- Primary button: "Create account" - full width, height 48px

Divider with "or" text

- "Already have an account? Sign in" - link centered

---

## Screen Variants

### 1. Register - Desktop (1200px)

- Frame: 1200x900px, background #F5F2ED
- Card: centered, width 420px, padding 48px
- Gap between form sections: 32px

### 2. Register - Tablet (1024px)

- Frame: 1024x900px
- Card: width 400px, padding 40px
- Gap: 28px

### 3. Register - Mobile (390px)

- Frame: 390x844px
- No card background
- Full width, padding 24px
- Gap: 24px

### 4. Register - Validation Errors

Same as Desktop but with:
- Name field: error state
- Error message: "Name is required"
- Email field: error state
- Error message: "Please enter a valid email"
- Password field: error state
- Error message: "Password must be at least 8 characters"

### 5. Register - Password Rule Error

Same as Desktop but with:
- Only password field has error
- Hint text turns red: "At least 8 characters"
- Error message below input

### 6. Register - Loading State

Same as Desktop but with:
- Form fields at 0.7 opacity
- "Create account" button shows spinner
- Button disabled

---

## Naming Convention

| Frame Name | Description |
|------------|-------------|
| Register - Default | Desktop 1200px |
| Register - Validation Errors | All field errors |
| Register - Password Rule Error | Password-specific error |
| Register - Loading | Button spinner state |
| Register - Tablet | 1024px breakpoint |
| Register - Mobile | 390px breakpoint |

---

## Expected Output

6 frames showing all Register screen states and breakpoints.
