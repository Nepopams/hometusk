# Auth UI Implementation Plan

> Based on Pencil MCP design analysis (`untitled.pen`)

## 1. Auth Frames Inventory

### Login Screens
| Frame ID | Name | Viewport | State |
|----------|------|----------|-------|
| `4vJMN` | Login - Default | 1200px | Default |
| `KK0dY` | Login - Validation Errors | 1200px | Field errors |
| `dmO63` | Login - Wrong Credentials | 1200px | API error banner |
| `FVSmW` | Login - Loading | 1200px | Button loading |
| `m70Ad` | Login - 1024px Tablet | 1024px | Default |
| `XILe3` | Login - 390px Mobile | 390px | Default (no card bg) |

### Register Screens
| Frame ID | Name | Viewport | State |
|----------|------|----------|-------|
| `mPFZL` | Register - Default | 1200px | Default |
| `NR3BE` | Register - Validation Errors | 1200px | Field errors |
| `RrcBM` | Register - Password Rule Error | 1200px | Password hint |
| `1h222` | Register - Loading | 1200px | Button loading |
| `Qj8fk` | Register - 1024px Tablet | 1024px | Default |
| `gUKML` | Register - 390px Mobile | 390px | Default |

### Session Expired
| Frame ID | Name | Viewport | Type |
|----------|------|----------|------|
| `fmlqQ` | Session Warning - Before Expiration | 1200px | Modal |
| `8CHBm` | Session Expired - Modal | 1200px | Modal |
| `5IASX` | Session Expired - Modal | 1024px | Modal |
| `kvKxQ` | Session Expired - Mobile | 390px | Full-page |
| `G7gtU` | Session Expired - Re-auth Failed | 1200px | Modal |
| `lw1ZF` | Session Expired - Full-page | 1200px | Full-page |

### 401/403 Error Pages
| Frame ID | Name | Viewport | Type |
|----------|------|----------|------|
| `4dLWc` | 401 - Not Signed In | 1200px | Full-page |
| `h3VbD` | 401 - Not Signed In | 1024px | Full-page |
| `yKRmp` | 401 - Not Signed In | 390px | Full-page |
| `e0Goo` | 403 - Access Denied | 1200px | Full-page |
| `ymcdH` | 403 - Access Denied | 1024px | Full-page |
| `pjCdq` | 403 - Access Denied | 390px | Full-page |

---

## 2. Design Token Set (from Pencil)

### Colors
```css
/* Background */
--bg-page: #F5F2ED;
--bg-card: #FFFFFF;
--bg-dark: #1A1A1A;
--bg-hover: #E8E5E0;
--bg-hover-subtle: #F9F7F4;
--bg-pressed: #EBE8E3;

/* Text */
--text-primary: #1A1A1A;
--text-secondary: #888888;
--text-muted: #BDBDBD;
--text-inverse: #F5F2ED;

/* Brand */
--terracotta: #C45A3B;        /* Primary action */
--terracotta-hover: #A84A30;

/* Border */
--border-primary: #1A1A1A;
--border-subtle: #E0DDD8;
--border-muted: #F0EDE8;

/* Semantic */
--error: #D84315;
--error-soft: #FFEBE6;
--success: #6B8E5E;
--success-soft: #E8F0E5;
--warning: #E8A055;
--warning-soft: #FDF4E8;
--info: #5B8FC4;
--info-soft: #E8F1F8;
--neutral-soft: #F5F2ED;

/* Focus */
--focus-ring: #C45A3B40;
--focus-ring-width: 3px;
```

### Spacing Scale
```css
--spacing-2: 2px;
--spacing-4: 4px;
--spacing-6: 6px;
--spacing-8: 8px;
--spacing-12: 12px;
--spacing-16: 16px;
--spacing-20: 20px;
--spacing-24: 24px;
--spacing-28: 28px;
--spacing-32: 32px;
--spacing-40: 40px;
--spacing-48: 48px;
--spacing-56: 56px;
--spacing-64: 64px;
```

### Border Radius
```css
--radius-none: 0;
--radius-sm: 4px;
--radius-md: 8px;
--radius-lg: 12px;
--radius-full: 999px;
```

### Border Width
```css
--border-thin: 1px;
--border-standard: 2px;
--border-emphasis: 4px;
```

### Typography
```css
--font-heading: "Space Grotesk", sans-serif;
--font-body: "Inter", sans-serif;

/* Sizes from Design System */
/* H1: 32px/700 Space Grotesk */
/* H2: 24px/700 Space Grotesk */
/* H3: 20px/700 Space Grotesk */
/* Body: 16px/400 Inter */
/* Small: 14px/400 Inter */
/* Caption: 13px/400 Inter */
/* Label: 14px/600 Inter */
```

### Component Sizes (from design)
```css
/* Buttons */
--btn-height: 48px;
--btn-height-mobile: 52px;
--btn-padding-x: 24px;
--btn-radius: 6px;

/* Inputs */
--input-height: 48px;
--input-padding: 12px 16px;
--input-radius: 8px;

/* Cards */
--card-padding-desktop: 48px;
--card-padding-tablet: 40px;
--card-padding-mobile: 24px;
--card-radius: 12px;
--card-width-desktop: 420px;
--card-width-tablet: 400px;
--card-gap: 32px;
```

---

## 3. Component Mapping Table

| Pencil Component | ID | Existing UI Component | Action |
|------------------|----|-----------------------|--------|
| Button/Primary | `WN5CV` | `.button` (index.css) | **Extend** - add terracotta color, loading state |
| Button/Secondary | `IY72S` | `.ghost-button` (partial) | **Create** - outlined style with border-primary |
| Button/Ghost | `WmAbf` | `.ghost-button` (index.css) | **Extend** - add terracotta text color |
| Button/Icon | `9JTAW` | — | **Create** - icon-only button variant |
| Input | `GG9sQ` | `.create-household__field input` | **Extract** - create reusable `<Input>` component |
| TextArea | `6LLgq` | `<textarea>` (Login.tsx) | **Create** - styled `<TextArea>` component |
| Select | `VUwvD` | `<Select>` (ui/Select.tsx) | **Extend** - align with design tokens |
| Badge/Success | `8QEUu` | `.status-badge--success` | **Consolidate** into `<Badge>` component |
| Badge/Warning | `MPoek` | `.status-badge--warning` | Same as above |
| Badge/Info | `ep1s4` | `.status-badge--info` | Same as above |
| Badge/Neutral | `afGKG` | — | Add to Badge component |
| Card | `NcX9C` | `.card` (index.css) | **Extend** - add padding variants |
| EmptyState | `czrJN` | `.empty-state` (index.css) | **Create** component wrapper |
| Toast | `brKdu` | — | **Create** - for session warnings |
| ErrorState | `AL046` | `.command-result--error` | **Consolidate** into `<ErrorBanner>` |
| Skeleton | `ilzTy` | — | **Create** - for loading states |
| ErrorMessage | — | `<ErrorMessage>` (ui/) | **Extend** - add design tokens |
| Spinner | — | `<Spinner>` (ui/) | **Extend** - add terracotta color |

### New Components Needed
| Component | Purpose |
|-----------|---------|
| `AuthLayout` | Shared wrapper: centered card, responsive padding, brand header |
| `FormField` | Label + Input + Error message pattern |
| `PasswordInput` | Input with show/hide toggle |
| `ErrorBanner` | Full-width error banner (wrong credentials) |
| `Modal` | Overlay + panel for session expired |
| `IconCircle` | Colored circle with icon (401/403 pages) |
| `Divider` | "or" divider with lines |
| `TextLink` | Terracotta link style |

---

## 4. Implementation Plan (4 PRs)

### PR1: Design Tokens + Base Components
**Scope:** Foundation layer, no new screens

**Files to create/modify:**
```
clients/web/src/
├── styles/
│   ├── tokens.css          # NEW: CSS custom properties from Pencil
│   └── index.css           # MODIFY: import tokens, update existing classes
├── components/ui/
│   ├── Button.tsx          # NEW: Primary/Secondary/Ghost/Icon variants
│   ├── Button.css          # NEW: button styles
│   ├── Input.tsx           # NEW: styled input with error state
│   ├── Input.css           # NEW: input styles
│   ├── FormField.tsx       # NEW: label + input + error wrapper
│   ├── PasswordInput.tsx   # NEW: input with visibility toggle
│   ├── TextLink.tsx        # NEW: terracotta link
│   ├── Divider.tsx         # NEW: "or" divider
│   └── IconCircle.tsx      # NEW: icon in colored circle
```

**Acceptance Criteria:**
- [ ] All design tokens exported as CSS custom properties
- [ ] Button component with 4 variants + loading + disabled states
- [ ] Input component with default/error/disabled states
- [ ] PasswordInput with show/hide toggle
- [ ] Components use tokens, not hardcoded values
- [ ] Storybook stories or visual test for each (optional)

---

### PR2: Login + AuthLayout
**Scope:** Login screen refactor, shared auth layout

**Files to create/modify:**
```
clients/web/src/
├── components/
│   ├── auth/
│   │   ├── AuthLayout.tsx      # NEW: centered layout with brand
│   │   ├── AuthLayout.css      # NEW: responsive styles
│   │   ├── BrandHeader.tsx     # NEW: logo + tagline
│   │   └── ErrorBanner.tsx     # NEW: wrong credentials banner
├── routes/
│   └── Login.tsx               # MODIFY: use new components
├── styles/
│   └── auth.css                # NEW: auth-specific styles
```

**Responsive behavior:**
- 1200px+: Card with shadow, 420px width, 48px padding
- 1024px: Card 400px width, 40px padding
- 390px: No card bg, full-width, 24px padding

**States covered:**
- [ ] Default (email + password fields)
- [ ] Validation errors (field-level)
- [ ] Wrong credentials (error banner)
- [ ] Loading (button spinner)
- [ ] Redirecting to OIDC

---

### PR3: Register Screen
**Scope:** Register screen using shared components

**Files to create/modify:**
```
clients/web/src/
├── components/auth/
│   └── PasswordHint.tsx        # NEW: password requirements hint
├── routes/
│   └── Register.tsx            # NEW: registration form
├── lib/
│   └── validation.ts           # NEW: form validation utils
```

**Fields:**
- Name (optional label)
- Email
- Password (with requirements hint: "At least 8 characters")

**States covered:**
- [ ] Default
- [ ] Validation errors (field-level)
- [ ] Password rule error (hint highlight)
- [ ] Loading

**Notes:**
- Reuses AuthLayout, FormField, PasswordInput from PR2
- Links to Login via TextLink

---

### PR4: Session Expired + 401/403
**Scope:** Error states and session management UI

**Files to create/modify:**
```
clients/web/src/
├── components/
│   ├── ui/
│   │   └── Modal.tsx           # NEW: overlay + panel
│   ├── auth/
│   │   ├── SessionWarningModal.tsx   # NEW: "session expiring" modal
│   │   └── SessionExpiredModal.tsx   # NEW: "session expired" modal
├── routes/
│   ├── Unauthorized.tsx        # NEW: 401 full-page
│   └── Forbidden.tsx           # NEW: 403 full-page
├── context/
│   └── AuthContext.tsx         # MODIFY: trigger session modals
├── hooks/
│   └── useSessionWarning.ts    # NEW: countdown/warning logic
```

**Session Expired behavior:**
- Modal on 1200px/1024px
- Full-page on mobile (390px)
- Shows "Your unsaved input has been preserved" hint
- Buttons: Cancel, Sign in again

**401 - Not Signed In:**
- IconCircle: info-soft bg, log-in icon
- Title: "Please sign in to continue"
- Buttons: Sign in (primary), Create account (secondary)

**403 - Access Denied:**
- IconCircle: warning-soft bg, shield-alert icon
- Title: "You don't have access to this household"
- Buttons: Switch household (primary), Enter invite code (secondary)
- Link: "Need help? Contact support"

---

## 5. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Font loading (Space Grotesk, Inter) | Add to `<head>` via Google Fonts or self-host |
| Breaking existing styles | Use BEM or CSS modules, avoid global changes |
| Session modal timing | Use configurable threshold, default 5 min warning |
| OIDC integration | PR4 modifies AuthContext carefully, test both dev/keycloak modes |

---

## 6. Out of Scope
- Backend/API changes
- Email verification flow
- Password reset flow
- Social login buttons
- Two-factor authentication UI

---

## Sources of Truth
- Product goal: `docs/planning/strategy/product-goal.md`
- Scope anchor: `docs/planning/releases/MVP.md`
- Design: `untitled.pen` (Pencil MCP)
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
