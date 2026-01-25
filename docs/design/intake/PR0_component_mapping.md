# PR0: Component Mapping

> Pencil components → HomeTusk codebase mapping
> Identifies existing patterns and gaps for PR1 implementation

---

## Repo Discovery

### Web UI Location
- **Root:** `clients/web/`
- **Routes:** `src/routes/` (Login.tsx, CreateHousehold.tsx, etc.)
- **Components:** `src/components/`
- **Styles:** `src/styles/index.css` (single global CSS file)

### Current Styling Approach
- **Type:** Plain CSS with BEM-like class names
- **No:** CSS modules, Tailwind, SCSS
- **Pattern:** Classes defined in `index.css`, applied directly

### Existing UI Components (`src/components/ui/`)
| File | Purpose | Notes |
|------|---------|-------|
| `Select.tsx` | Dropdown select | Basic wrapper |
| `Spinner.tsx` | Loading indicator | Text-only "Loading..." |
| `ErrorMessage.tsx` | Error display | Card with retry button |
| `CopyButton.tsx` | Copy to clipboard | Named export |

### Current CSS Patterns (from `index.css`)
| Class | Usage |
|-------|-------|
| `.card` | Container with shadow |
| `.button` | Primary button (green #0b3d3a) |
| `.ghost-button` | Dashed border button |
| `.chip` | Pill-shaped tag |
| `.status-badge--*` | Status indicators |
| `.empty-state` | Empty placeholder |
| `.create-household__field` | Form field layout |
| `.invite-modal__*` | Modal patterns |

---

## Component Mapping Table

### Buttons

| Pencil Component | ID | Existing | Action | Notes |
|------------------|----|---------|---------|----|
| Button/Primary | `WN5CV` | `.button` | **Modify** | Change to terracotta, add height/radius tokens |
| Button/Secondary | `IY72S` | `.ghost-button` (partial) | **Create** | Solid bg + dark border |
| Button/Ghost | `WmAbf` | `.ghost-button` | **Extend** | Add proper hover states |
| Button/Icon | `9JTAW` | — | **Create** | 40x40 icon-only button |

**Proposed Button Component:**
```tsx
interface ButtonProps {
  variant: 'primary' | 'secondary' | 'ghost' | 'text';
  size: 'sm' | 'md' | 'lg';
  loading?: boolean;
  disabled?: boolean;
  fullWidth?: boolean;
  iconLeft?: ReactNode;
  iconRight?: ReactNode;
}
```

### Form Inputs

| Pencil Component | ID | Existing | Action | Notes |
|------------------|----|---------|---------|----|
| Input | `GG9sQ` | `.create-household__field input` | **Create** | Extract to reusable component |
| TextArea | `6LLgq` | `<textarea>` inline | **Create** | Styled textarea |
| Select | `VUwvD` | `Select.tsx` | **Extend** | Add design tokens |
| Password Input | — | — | **Create** | Input + show/hide toggle |

**Proposed TextField Component:**
```tsx
interface TextFieldProps {
  label: string;
  hint?: string;
  error?: string;
  endAdornment?: ReactNode;
  // ...HTMLInputAttributes
}
```

**Proposed PasswordField Component:**
```tsx
interface PasswordFieldProps {
  label: string;
  hint?: string;
  error?: string;
  // Show/hide toggle built-in
}
```

### Containers

| Pencil Component | ID | Existing | Action | Notes |
|------------------|----|---------|---------|----|
| Card | `NcX9C` | `.card` | **Extend** | Add padding variants |
| Modal | — | `.invite-modal__*` | **Create** | Backdrop + panel |
| AuthLayout | — | — | **Create** | Centered wrapper |

**Proposed Card Component:**
```tsx
interface CardProps {
  padding: 'none' | 'sm' | 'md' | 'lg';
  flat?: boolean;
}
```

**Proposed AuthLayout Component:**
```tsx
interface AuthLayoutProps {
  children: ReactNode;
  // Centered, responsive, handles breakpoints
}
```

### Feedback

| Pencil Component | ID | Existing | Action | Notes |
|------------------|----|---------|---------|----|
| ErrorState | `AL046` | `.command-result--error` | **Create** | ErrorBanner for forms |
| Toast | `brKdu` | — | **Create** | Session warning toast |
| Badge/Success | `8QEUu` | `.status-badge--success` | **Consolidate** | Unified Badge component |
| Badge/Warning | `MPoek` | `.status-badge--warning` | Same | |
| Badge/Info | `ep1s4` | `.status-badge--info` | Same | |
| Skeleton | `ilzTy` | — | **Create** | Loading placeholder |

**Proposed ErrorBanner Component:**
```tsx
interface ErrorBannerProps {
  title?: string;
  children: ReactNode;
  action?: ReactNode;
}
```

### Navigation/Links

| Pencil Pattern | Existing | Action | Notes |
|----------------|---------|--------|-------|
| Terracotta link | — | **Create** | TextLink component |
| "or" divider | — | **Create** | Divider with text |

**Proposed TextLink Component:**
```tsx
interface TextLinkProps {
  to?: string;  // React Router
  href?: string; // Anchor
  variant: 'default' | 'muted';
  centered?: boolean;
}
```

### Auth-Specific

| Pattern | Existing | Action | Notes |
|---------|---------|--------|-------|
| Brand header (logo + name) | — | **Create** | BrandHeader component |
| Session expired modal | — | **Create** | SessionExpiredModal |
| 401/403 error page | — | **Create** | Unauthorized, Forbidden routes |

**Proposed BrandHeader Component:**
```tsx
interface BrandHeaderProps {
  tagline?: string; // "Welcome back" / "Create your account"
}
```

---

## Gap Analysis Summary

### Must Create (PR1 - Tokens & Primitives)
1. `tokens.css` - Design token definitions
2. `Button.tsx` + `Button.css` - Multi-variant button
3. `TextField.tsx` + `TextField.css` - Labeled input
4. `PasswordField.tsx` - Password with toggle
5. `Card.tsx` + `Card.css` - Padding variants
6. `ErrorBanner.tsx` - Form-level errors
7. `Divider.tsx` - "or" separator
8. `TextLink.tsx` - Styled links
9. `Spinner.tsx` - Update with tokens

### Must Create (PR2 - Auth Layout)
1. `AuthLayout.tsx` - Centered wrapper
2. `BrandHeader.tsx` - Logo + tagline
3. Refactor `Login.tsx` to use new components

### Must Create (PR3 - Register)
1. `Register.tsx` - Registration form
2. Password hint component

### Must Create (PR4 - Session/Errors)
1. `Modal.tsx` - Overlay + panel
2. `SessionExpiredModal.tsx`
3. `Unauthorized.tsx` (401)
4. `Forbidden.tsx` (403)

---

## Implementation Priority

| Priority | Component | Reason |
|----------|-----------|--------|
| P0 | tokens.css | Foundation for everything |
| P0 | Button | Used in all auth forms |
| P0 | TextField | Used in all auth forms |
| P0 | PasswordField | Login + Register |
| P1 | AuthLayout | Shared layout |
| P1 | BrandHeader | Shared branding |
| P1 | ErrorBanner | Wrong credentials state |
| P1 | Card | Form containers |
| P2 | Divider | "or" separator |
| P2 | TextLink | Navigation links |
| P2 | Spinner | Loading states |
| P3 | Modal | Session expired |
| P3 | 401/403 pages | Error states |

---

## File Structure (Proposed)

```
clients/web/src/
├── styles/
│   ├── tokens.css       # NEW: Design tokens
│   └── index.css        # MODIFIED: Import tokens
├── components/
│   ├── ui/
│   │   ├── index.ts     # NEW: Barrel export
│   │   ├── Button.tsx   # NEW
│   │   ├── Button.css   # NEW
│   │   ├── TextField.tsx # NEW
│   │   ├── TextField.css # NEW
│   │   ├── PasswordField.tsx # NEW
│   │   ├── Card.tsx     # NEW
│   │   ├── Card.css     # NEW
│   │   ├── ErrorBanner.tsx # NEW
│   │   ├── Divider.tsx  # NEW
│   │   ├── TextLink.tsx # NEW
│   │   ├── Spinner.tsx  # MODIFIED
│   │   └── Spinner.css  # NEW
│   ├── auth/
│   │   ├── index.ts     # NEW: Barrel export
│   │   ├── AuthLayout.tsx # NEW
│   │   ├── AuthLayout.css # NEW
│   │   ├── BrandHeader.tsx # NEW
│   │   └── BrandHeader.css # NEW
│   └── ...existing
└── routes/
    ├── Login.tsx        # MODIFIED
    ├── Register.tsx     # NEW (PR3)
    ├── Unauthorized.tsx # NEW (PR4)
    └── Forbidden.tsx    # NEW (PR4)
```
