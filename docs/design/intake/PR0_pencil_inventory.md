# PR0: Pencil Design Inventory

> Design intake from Pencil MCP for HomeTusk Auth screens
> Source file: `untitled.pen`

## MCP Connection Status

| Tool | Status | Notes |
|------|--------|-------|
| `mcp__pencil__get_editor_state` | ✅ Connected | Document loaded |
| `mcp__pencil__batch_get` | ✅ Working | Pattern search + node read |
| `mcp__pencil__get_variables` | ✅ Working | 52 design tokens extracted |
| `mcp__pencil__get_screenshot` | ✅ Working | Visual verification available |

---

## Auth Screen Inventory

### Login Screens

| Frame ID | Name | Width | States/Notes |
|----------|------|-------|--------------|
| `4vJMN` | Login - Default | 1200px | Default state |
| `KK0dY` | Login - Validation Errors | 1200px | Field-level errors |
| `dmO63` | Login - Wrong Credentials | 1200px | Error banner visible |
| `FVSmW` | Login - Loading | 1200px | Button spinner, 0.7 opacity |
| `m70Ad` | Login - 1024px Tablet | 1024px | Card width=400, padding=40 |
| `XILe3` | Login - 390px Mobile | 390px | No card bg, padding=24 |
| `BD5y7` | Login - Click Map & Component Tree | 1200px | Dev handoff doc |

### Register Screens

| Frame ID | Name | Width | States/Notes |
|----------|------|-------|--------------|
| `mPFZL` | Register - Default | 1200px | Name + Email + Password |
| `NR3BE` | Register - Validation Errors | 1200px | Field-level errors |
| `RrcBM` | Register - Password Rule Error | 1200px | Password hint highlighted |
| `1h222` | Register - Loading | 1200px | Button spinner |
| `Qj8fk` | Register - 1024px Tablet | 1024px | Card width=400, padding=40 |
| `gUKML` | Register - 390px Mobile | 390px | No card bg, padding=24 |
| `AsnKa` | Register - Click Map & Component Tree | 1200px | Dev handoff doc |

### Session Expired

| Frame ID | Name | Width | Type |
|----------|------|-------|------|
| `fmlqQ` | Session Warning - Before Expiration | 1200px | Modal (5-min warning) |
| `8CHBm` | Session Expired - Modal | 1200px | Modal w/ backdrop |
| `5IASX` | Session Expired - Modal | 1024px | Modal w/ backdrop |
| `kvKxQ` | Session Expired - 390px Mobile | 390px | Full-page (no modal) |
| `G7gtU` | Session Expired - Re-auth Failed | 1200px | Modal + error state |
| `lw1ZF` | Session Expired - Full-page | 1200px | Alternative full-page |
| `YPsPr` | Session Expired - Click Map | 600px | Dev handoff doc |

### 401 - Not Signed In

| Frame ID | Name | Width | Notes |
|----------|------|-------|-------|
| `4dLWc` | 401 - Not Signed In | 1200px | Content width=360 |
| `h3VbD` | 401 - Not Signed In | 1024px | Content width=340 |
| `yKRmp` | 401 - Not Signed In | 390px | Full-width + padding=24 |

### 403 - Access Denied

| Frame ID | Name | Width | Notes |
|----------|------|-------|-------|
| `e0Goo` | 403 - Access Denied | 1200px | Content width=400, support link |
| `ymcdH` | 403 - Access Denied | 1024px | Content width=380 |
| `pjCdq` | 403 - Access Denied | 390px | Full-width + padding=24 |

---

## Breakpoint Summary

| Breakpoint | Width | Card Behavior | Padding |
|------------|-------|---------------|---------|
| Desktop | 1200px | Card w/ shadow, width=420px | 48px |
| Tablet | 1024px | Card w/ shadow, width=400px | 40px |
| Mobile | 390px | No card, full-width | 24px |

---

## Shared Components (Reusable in Pencil)

| Component ID | Name | Usage in Auth |
|--------------|------|---------------|
| `WN5CV` | Button/Primary | Sign in, Create account, Sign in again |
| `IY72S` | Button/Secondary | Cancel, Switch household |
| `WmAbf` | Button/Ghost | Enter invite code |
| `9JTAW` | Button/Icon | — |
| `GG9sQ` | Input | Email, Name fields |
| `6LLgq` | TextArea | — |
| `VUwvD` | Select | — |
| `8QEUu` | Badge/Success | "Input preserved" hint |
| `MPoek` | Badge/Warning | — |
| `ep1s4` | Badge/Info | — |
| `afGKG` | Badge/Neutral | — |
| `NcX9C` | Card | Login/Register cards |
| `brKdu` | Toast | Session warning? |
| `AL046` | ErrorState | Wrong credentials banner |
| `ilzTy` | Skeleton | — |

---

## Design System Frame

| Section | Frame ID | Contents |
|---------|----------|----------|
| Tokens Section | `0shZ9` | Typography, Spacing, Radii, Sizes |
| Components Section | `QPyh5` | Buttons, Inputs, Badges, Cards, States |

Typography card (`hRXYF`) defines:
- H1: 32px/700 (Page Title)
- H2: 24px/700 (Section Title)
- H3: 20px/700 (Card Title)
- Body: 15px/500
- Body Small: 13px/500
- Caption: 12px/400
- Label: 13px/600

---

## State Matrix

| Screen | Default | Validation | Server Error | Loading |
|--------|---------|------------|--------------|---------|
| Login | ✅ `4vJMN` | ✅ `KK0dY` | ✅ `dmO63` | ✅ `FVSmW` |
| Register | ✅ `mPFZL` | ✅ `NR3BE` | ✅ `RrcBM` | ✅ `1h222` |
| Session Expired | ✅ `8CHBm` | — | ✅ `G7gtU` | — |
| 401 | ✅ `4dLWc` | — | — | — |
| 403 | ✅ `e0Goo` | — | — | — |
