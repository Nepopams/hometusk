# UI Components

Design-system-aligned components for HomeTusk, based on Pencil designs.

## Setup

Components use CSS custom properties from `styles/tokens.css`. This file is automatically imported via `styles/index.css`.

Fonts (Space Grotesk + Inter) are loaded from Google Fonts.

## Components

### Button

Primary action button with multiple variants.

```tsx
import { Button } from '@/components/ui';

// Primary (default) - terracotta background
<Button onClick={handleSubmit}>Sign in</Button>

// Secondary - white with dark border
<Button variant="secondary">Cancel</Button>

// Ghost - transparent with subtle border
<Button variant="ghost">Skip</Button>

// Text - no background, brand color text
<Button variant="text">Forgot password?</Button>

// Loading state
<Button loading>Processing...</Button>

// Full width
<Button fullWidth>Create account</Button>

// With icon
<Button iconLeft={<PlusIcon />}>Add item</Button>

// Sizes: sm (36px), md (44px), lg (48px - default)
<Button size="sm">Small</Button>
```

### TextField

Text input with label and error handling.

```tsx
import { TextField } from '@/components/ui';

// Basic
<TextField
  label="Email"
  type="email"
  placeholder="you@example.com"
/>

// With error
<TextField
  label="Email"
  type="email"
  error="Please enter a valid email address"
/>

// With hint
<TextField
  label="Username"
  hint="3-20 characters, letters and numbers only"
/>

// Required field
<TextField label="Email" required />
```

### PasswordField

Password input with show/hide toggle.

```tsx
import { PasswordField } from '@/components/ui';

// Basic
<PasswordField label="Password" />

// With hint (for registration)
<PasswordField
  label="Password"
  hint="At least 8 characters"
/>

// With error
<PasswordField
  label="Password"
  error="Password must be at least 8 characters"
/>
```

### Card

Container with padding variants.

```tsx
import { Card } from '@/components/ui';

// Default (lg padding - 48px)
<Card>Content</Card>

// Medium padding (40px)
<Card padding="md">Content</Card>

// Small padding (24px)
<Card padding="sm">Content</Card>

// Flat (no shadow)
<Card flat>Content</Card>
```

### ErrorBanner

Full-width banner for form-level errors.

```tsx
import { ErrorBanner } from '@/components/ui';

// Simple message
<ErrorBanner>
  Incorrect email or password. Please try again.
</ErrorBanner>

// With title
<ErrorBanner title="Authentication failed">
  We couldn't verify your credentials.
</ErrorBanner>
```

### Divider

Horizontal separator with optional text.

```tsx
import { Divider } from '@/components/ui';

// Simple line
<Divider />

// With text
<Divider text="or" />
```

### TextLink

Styled link for forms.

```tsx
import { TextLink } from '@/components/ui';

// React Router link
<TextLink to="/register">Create account</TextLink>

// External link
<TextLink href="/forgot-password">Forgot password?</TextLink>

// Muted variant
<TextLink to="/login" variant="muted">Sign in</TextLink>

// Centered
<TextLink to="/register" centered>Create account</TextLink>
```

### Spinner

Loading indicator.

```tsx
import { Spinner } from '@/components/ui';

// Default
<Spinner />

// With visible label
<Spinner label="Loading tasks..." showLabel />

// Sizes: sm (16px), md (24px - default), lg (32px)
<Spinner size="sm" />
```

## Auth Components

Shared layouts for authentication screens.

```tsx
import { AuthLayout, BrandHeader } from '@/components/auth';
import { Card, TextField, PasswordField, Button } from '@/components/ui';

function LoginPage() {
  return (
    <AuthLayout>
      <Card>
        <BrandHeader tagline="Welcome back" />

        <form>
          <TextField label="Email" type="email" />
          <PasswordField label="Password" />
          <Button fullWidth>Sign in</Button>
        </form>
      </Card>
    </AuthLayout>
  );
}
```

## Responsive Behavior

- **Desktop (1200px+)**: Card with shadow, max-width 420px, 48px padding
- **Tablet (1024px)**: Card with shadow, max-width 400px, 40px padding
- **Mobile (390px)**: Full-width, no card background, 24px padding

## Accessibility

- All inputs have associated labels (visible or sr-only)
- Focus states use visible ring (`--focus-ring-color`)
- Minimum touch targets are 44x44 where applicable
- Error messages use `role="alert"` for screen readers
- Loading states use `role="status"`

## Design Tokens

All components use CSS custom properties. See `styles/tokens.css` for the full list.

Key tokens:

| Category | Token | Value |
|----------|-------|-------|
| Brand | `--color-brand` | #C45A3B |
| Background | `--color-bg-page` | #F5F2ED |
| Background | `--color-bg-card` | #FFFFFF |
| Text | `--color-text-primary` | #1A1A1A |
| Text | `--color-text-secondary` | #888888 |
| Error | `--color-error` | #D84315 |
| Border | `--color-border-subtle` | #E0DDD8 |
| Radius | `--radius-md` | 8px |
| Radius | `--radius-lg` | 12px |
