# Design Foundation

> How to use the HomeTusk design system primitives.
> Source: Pencil designs (`untitled.pen`) via PR0 Design Intake.

---

## Quick Start

```tsx
import { Button, TextField, Card, Modal } from '@/components/ui';
import { AuthLayout, BrandHeader } from '@/components/auth';
import { useIsMobile, useBreakpoint } from '@/hooks';
```

---

## 1. Design Tokens

All design values live in `src/styles/tokens.css` as CSS custom properties.

### Color Tokens

| Token | Usage |
|-------|-------|
| `--color-bg-page` | Page background (#F5F2ED) |
| `--color-bg-card` | Card/surface background (#FFFFFF) |
| `--color-brand` | Primary action color (#C45A3B terracotta) |
| `--color-brand-hover` | Primary hover (#A84A30) |
| `--color-text-primary` | Main text (#1A1A1A) |
| `--color-text-secondary` | Muted text (#888888) |
| `--color-border-subtle` | Default borders (#E0DDD8) |
| `--color-error` | Error states (#D84315) |
| `--color-success` | Success states (#6B8E5E) |
| `--color-focus-ring` | Focus outline (rgba(196, 90, 59, 0.25)) |

### Spacing Scale

Use `--spacing-{n}` tokens (not hardcoded px):

| Token | Value | Usage |
|-------|-------|-------|
| `--spacing-2` | 4px | Tight spacing |
| `--spacing-4` | 8px | Small gaps |
| `--spacing-6` | 16px | Default gap |
| `--spacing-8` | 24px | Section spacing |
| `--spacing-10` | 32px | Large gaps |
| `--spacing-12` | 48px | Card padding (desktop) |

### Typography

```css
font-family: var(--font-family-body);      /* Inter */
font-family: var(--font-family-heading);   /* Space Grotesk */

font-size: var(--font-size-sm);   /* 13px */
font-size: var(--font-size-base); /* 14px */
font-size: var(--font-size-md);   /* 16px */
font-size: var(--font-size-xl);   /* 20px */
font-size: var(--font-size-2xl);  /* 24px */
```

### Shadows & Radius

```css
border-radius: var(--radius-sm);  /* 4px */
border-radius: var(--radius-md);  /* 8px - inputs, buttons */
border-radius: var(--radius-lg);  /* 12px - cards */

box-shadow: var(--shadow-sm);     /* subtle */
box-shadow: var(--shadow-md);     /* dropdowns */
box-shadow: var(--shadow-lg);     /* cards */
box-shadow: var(--shadow-xl);     /* modals */
```

---

## 2. Base Primitives

### Button

```tsx
import { Button } from '@/components/ui';

// Variants: primary | secondary | ghost | text
// Sizes: sm | md | lg
<Button variant="primary" size="lg">Sign in</Button>
<Button variant="secondary">Cancel</Button>
<Button variant="ghost">Learn more</Button>
<Button loading>Processing...</Button>
<Button fullWidth>Create account</Button>
```

States: default, hover, focus (ring), disabled, loading (spinner).

### TextField

```tsx
import { TextField } from '@/components/ui';

<TextField
  label="Email"
  type="email"
  placeholder="you@example.com"
  hint="We'll never share your email"
/>

// With error
<TextField
  label="Email"
  error="Please enter a valid email address"
/>
```

### PasswordField

```tsx
import { PasswordField } from '@/components/ui';

<PasswordField
  label="Password"
  hint="At least 8 characters"
/>
```

Built-in show/hide toggle with accessible labels.

### Card

```tsx
import { Card } from '@/components/ui';

// Padding: none | sm (24px) | md (40px) | lg (48px)
<Card padding="lg">
  <h2>Card title</h2>
  <p>Card content</p>
</Card>

// Flat variant (no shadow)
<Card padding="md" flat>...</Card>
```

### ErrorBanner (form-level)

```tsx
import { ErrorBanner } from '@/components/ui';

<ErrorBanner title="Unable to sign in">
  The email or password you entered is incorrect.
</ErrorBanner>
```

### Divider

```tsx
import { Divider } from '@/components/ui';

<Divider />
<Divider text="or" />
<Divider text="or continue with" />
```

### TextLink

```tsx
import { TextLink } from '@/components/ui';

// React Router link
<TextLink to="/register">Create account</TextLink>

// External anchor
<TextLink href="https://example.com" external>Help</TextLink>

// Centered
<TextLink to="/forgot-password" centered>Forgot password?</TextLink>
```

---

## 3. Overlays

### Modal

```tsx
import { Modal } from '@/components/ui';

<Modal
  open={isOpen}
  onClose={() => setIsOpen(false)}
  title="Session Expired"
  size="md" // sm | md | lg
>
  <p>Your session has expired.</p>
  <Button onClick={handleSignIn}>Sign in again</Button>
</Modal>
```

Features:
- Focus trap and Escape key
- Backdrop click to close
- Responsive: slides up from bottom on mobile

### Sheet (mobile bottom sheet)

```tsx
import { Sheet, SheetItem } from '@/components/ui';

<Sheet
  open={isOpen}
  onClose={() => setIsOpen(false)}
  title="Select household"
>
  <SheetItem onClick={() => select('home')} selected={current === 'home'}>
    My Home
  </SheetItem>
  <SheetItem onClick={() => select('cabin')}>
    Beach Cabin
  </SheetItem>
</Sheet>
```

Use Sheet instead of Menu/Dropdown on mobile viewports.

### Menu (dropdown)

```tsx
import { Menu, MenuItem, MenuDivider } from '@/components/ui';

<Menu
  trigger={<Button variant="ghost">Options ▼</Button>}
  aria-label="User options"
>
  <MenuItem onClick={handleProfile}>Profile</MenuItem>
  <MenuItem onClick={handleSettings}>Settings</MenuItem>
  <MenuDivider />
  <MenuItem onClick={handleLogout} destructive>Sign out</MenuItem>
</Menu>
```

Features:
- Keyboard navigation (Arrow keys, Home, End, Escape)
- Transforms to bottom sheet on mobile

---

## 4. Feedback

### Spinner

```tsx
import { Spinner } from '@/components/ui';

<Spinner />           // Default size
<Spinner size="sm" /> // Small
<Spinner size="lg" /> // Large
```

### Skeleton

```tsx
import { Skeleton } from '@/components/ui';

// Text placeholder
<Skeleton variant="text" width="80%" />

// Multiple lines
<Skeleton variant="text" lines={3} />

// Avatar
<Skeleton variant="circular" width={48} height={48} />

// Card
<Skeleton variant="rectangular" height={120} />
```

### Snackbar (toast)

```tsx
import { Snackbar } from '@/components/ui';

<Snackbar
  open={showToast}
  onClose={() => setShowToast(false)}
  variant="success" // default | success | error | warning | info
  duration={4000}   // ms, 0 = no auto-dismiss
>
  Changes saved successfully
</Snackbar>

// With action
<Snackbar
  open={showUndo}
  onClose={() => setShowUndo(false)}
  action={<button onClick={handleUndo}>Undo</button>}
>
  Item deleted
</Snackbar>
```

---

## 5. Layout

### AuthLayout

```tsx
import { AuthLayout } from '@/components/auth';
import { BrandHeader } from '@/components/auth';

<AuthLayout>
  <Card padding="lg">
    <BrandHeader tagline="Welcome back" />
    <form>...</form>
  </Card>
</AuthLayout>
```

Responsive behavior:
- **Desktop (≥1200px):** Centered card, 420px width, 48px padding
- **Tablet (≥1024px):** Card 400px width, 40px padding
- **Mobile (<480px):** Full-width, no card background, 24px padding

### Container

```css
<div class="container">
  <!-- Max-width 1200px, auto margins, responsive padding -->
</div>
```

---

## 6. Responsive Utilities

### CSS Classes

```html
<span class="hide-mobile">Desktop only</span>
<span class="show-mobile">Mobile only</span>
```

### React Hooks

```tsx
import { useIsMobile, useIsTablet, useBreakpoint } from '@/hooks';

function MyComponent() {
  const isMobile = useIsMobile();      // <= 480px
  const isTablet = useIsTablet();      // <= 1024px
  const breakpoint = useBreakpoint();  // 'mobile' | 'tablet' | 'desktop'

  return isMobile ? <Sheet /> : <Menu />;
}
```

### Breakpoint Values

| Name | CSS | Hook |
|------|-----|------|
| Mobile | `max-width: 480px` | `useIsMobile()` |
| Tablet | `max-width: 1024px` | `useIsTablet()` |
| Desktop | `min-width: 1200px` | `useIsDesktop()` |

---

## 7. Accessibility Checklist

All primitives include:

- [x] **Focus ring:** 3px outline with `--color-focus-ring`
- [x] **Touch targets:** Minimum 44x44 CSS px
- [x] **ARIA attributes:** `role`, `aria-label`, `aria-expanded`, etc.
- [x] **Keyboard navigation:** Tab, Escape, Arrow keys where applicable
- [x] **Reduced motion:** Respects `prefers-reduced-motion`

```tsx
// Check user preference
import { usePrefersReducedMotion } from '@/hooks';

const prefersReducedMotion = usePrefersReducedMotion();
```

---

## 8. Adding a New Screen (PR Workflow)

### Step 1: Plan the screen

Reference existing designs in Pencil (`untitled.pen`) via MCP:
- Frame IDs are listed in `docs/design/intake/PR0_pencil_inventory.md`
- Use `mcp__pencil__get_screenshot` to verify visual

### Step 2: Compose with primitives

```tsx
import { AuthLayout, BrandHeader } from '@/components/auth';
import { Card, Button, TextField, ErrorBanner } from '@/components/ui';

export default function LoginPage() {
  return (
    <AuthLayout>
      <Card padding="lg">
        <BrandHeader tagline="Welcome back" />
        {error && <ErrorBanner>{error}</ErrorBanner>}
        <form>
          <TextField label="Email" ... />
          <PasswordField label="Password" ... />
          <Button type="submit" fullWidth loading={isLoading}>
            Sign in
          </Button>
        </form>
      </Card>
    </AuthLayout>
  );
}
```

### Step 3: Handle responsive

Use `useBreakpoint()` for conditional rendering:
```tsx
const breakpoint = useBreakpoint();
const isMobile = breakpoint === 'mobile';

// Show Sheet on mobile, Menu on desktop
{isMobile ? <Sheet ... /> : <Menu ... />}
```

### Step 4: Test

Manual checklist:
- [ ] 1200px viewport (desktop)
- [ ] 1024px viewport (tablet)
- [ ] 390px viewport (mobile)
- [ ] Keyboard navigation (Tab through all interactive elements)
- [ ] Screen reader (VoiceOver/NVDA basic check)

---

## 9. PR Sequence

| PR | Focus | Status |
|----|-------|--------|
| PR0 | Design Intake (docs) | ✅ Done |
| PR1 | Tokens + Base Primitives | ✅ Done |
| PR2 | Login Screen Refactor | 🔜 Next |
| PR3 | Register Screen | Planned |
| PR4 | Session Expired + 401/403 | Planned |

---

## 10. Sources of Truth

| Document | Path |
|----------|------|
| Pencil Design | `untitled.pen` |
| Token Proposal | `docs/design/intake/PR0_tokens_proposal.md` |
| Component Mapping | `docs/design/intake/PR0_component_mapping.md` |
| Implementation Plan | `docs/design/intake/PR0_implementation_plan.md` |
| Product Goal | `docs/planning/strategy/product-goal.md` |
