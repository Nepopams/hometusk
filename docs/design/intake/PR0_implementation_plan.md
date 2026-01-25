# PR0: Implementation Plan

> Actionable plan for implementing Pencil designs in HomeTusk web client

---

## PR Sequence

```
PR0 (this) → PR1 → PR2 → PR3 → PR4
  Design      Tokens   Login   Register  Session/Errors
  Intake      + Base   Screen  Screen    + 401/403
```

---

## PR1: Design Tokens + Base Components

**Goal:** Establish design system foundation without breaking existing UI.

### Files to Create

| File | Purpose |
|------|---------|
| `styles/tokens.css` | CSS custom properties (colors, spacing, radius, typography) |
| `components/ui/Button.tsx` | Primary/Secondary/Ghost/Text variants |
| `components/ui/Button.css` | Button styles |
| `components/ui/TextField.tsx` | Labeled input with error state |
| `components/ui/TextField.css` | Input styles |
| `components/ui/PasswordField.tsx` | TextField + show/hide toggle |
| `components/ui/PasswordField.css` | Toggle button styles |
| `components/ui/Card.tsx` | Padding variants (none/sm/md/lg) |
| `components/ui/Card.css` | Card styles |
| `components/ui/ErrorBanner.tsx` | Form-level error banner |
| `components/ui/ErrorBanner.css` | Banner styles |
| `components/ui/Divider.tsx` | Horizontal line with optional text |
| `components/ui/Divider.css` | Divider styles |
| `components/ui/TextLink.tsx` | Styled link (React Router + anchor) |
| `components/ui/TextLink.css` | Link styles |
| `components/ui/Spinner.css` | Spinner with tokens |
| `components/ui/index.ts` | Barrel export |
| `components/auth/AuthLayout.tsx` | Centered layout wrapper |
| `components/auth/AuthLayout.css` | Responsive styles |
| `components/auth/BrandHeader.tsx` | Logo + brand name + tagline |
| `components/auth/BrandHeader.css` | Brand styles |
| `components/auth/index.ts` | Barrel export |
| `components/ui/README.md` | Usage documentation |

### Files to Modify

| File | Change |
|------|--------|
| `styles/index.css` | Add `@import './tokens.css'` + Google Fonts |
| `components/ui/Spinner.tsx` | Add CSS import, use tokens |

### Acceptance Criteria

- [ ] All tokens from Pencil variables are CSS custom properties
- [ ] Button works with 4 variants × 3 sizes + loading + disabled
- [ ] TextField shows label, hint, error states
- [ ] PasswordField has working show/hide toggle
- [ ] Card supports responsive padding (48/40/24)
- [ ] ErrorBanner renders with icon and message
- [ ] Components are accessible (focus ring, aria attributes)
- [ ] `npm run build` passes
- [ ] `npm run lint` passes
- [ ] No visual regressions in existing routes

---

## PR2: Login Screen Refactor

**Goal:** Apply new components to Login route.

### Files to Modify

| File | Change |
|------|--------|
| `routes/Login.tsx` | Rewrite with AuthLayout, Button, TextField, etc. |

### New States to Support

| State | Components Used |
|-------|-----------------|
| Default | TextField (email), PasswordField, Button, TextLink |
| Validation errors | TextField with error prop |
| Wrong credentials | ErrorBanner above form |
| Loading | Button with loading prop |
| Redirecting | Spinner |

### Responsive Behavior

| Breakpoint | Layout |
|------------|--------|
| ≥1200px | Card (420px, padding 48px) |
| ≥1024px | Card (400px, padding 40px) |
| <480px | No card, full-width, padding 24px |

---

## PR3: Register Screen

**Goal:** Create registration form using same components.

### Files to Create

| File | Purpose |
|------|---------|
| `routes/Register.tsx` | Registration form |
| `components/auth/PasswordHint.tsx` | "At least 8 characters" hint |

### Fields

1. Name (optional indicator)
2. Email
3. Password (with PasswordHint)

### States

- Default
- Validation errors (field-level)
- Password rule error
- Loading

---

## PR4: Session Expired + 401/403

**Goal:** Complete error state coverage.

### Files to Create

| File | Purpose |
|------|---------|
| `components/ui/Modal.tsx` | Overlay + panel |
| `components/ui/Modal.css` | Modal styles |
| `components/auth/SessionExpiredModal.tsx` | Session expired dialog |
| `components/auth/SessionWarningModal.tsx` | 5-minute warning |
| `routes/Unauthorized.tsx` | 401 full-page |
| `routes/Forbidden.tsx` | 403 full-page |
| `components/ui/IconCircle.tsx` | Icon in colored circle |

### Files to Modify

| File | Change |
|------|--------|
| `context/AuthContext.tsx` | Trigger session modals |
| `routes/index.tsx` | Add 401/403 routes |

### Session Expired Behavior

| Viewport | UI Type |
|----------|---------|
| ≥1024px | Modal with backdrop |
| <480px | Full-page |

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Font loading issues | Self-host or use Google Fonts |
| Breaking existing styles | New components use `card-ui` (not `card`), scoped CSS |
| Token conflicts | Use `--color-*` prefix, not `--bg-*` directly |
| Auth context complexity | PR4 changes are isolated, test both dev/keycloak |

---

## Out of Scope

- Backend/API changes
- Email verification flow
- Password reset flow
- Social login buttons
- 2FA UI
- Other screens beyond Auth

---

## Verification Commands

```bash
# Build
cd clients/web && npm run build

# Lint
npm run lint

# Dev server (manual test)
npm run dev
```

---

## Sources of Truth

| Document | Path |
|----------|------|
| Pencil Design | `untitled.pen` |
| Product Goal | `docs/planning/strategy/product-goal.md` |
| MVP Scope | `docs/planning/releases/MVP.md` |
| DoR | `docs/_governance/dor.md` |
| DoD | `docs/_governance/dod.md` |
