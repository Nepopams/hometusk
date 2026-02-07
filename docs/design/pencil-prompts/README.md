# HomeTusk Pencil Prompt Packs

> Prompts for recreating the HomeTusk design system in Pencil.

---

## Overview

These prompt packs are designed to be run sequentially in Pencil to recreate the HomeTusk design system and screens. Each pack builds on the previous ones.

## Execution Order

Run the prompts in this order:

| # | File | Description | Frames |
|---|------|-------------|--------|
| 0 | `00-design-tokens.md` | Design variables and tokens | ~1 |
| 1 | `01-base-components.md` | Reusable UI components | ~12 |
| 2 | `02-login-screens.md` | Login page variants | 6 |
| 3 | `03-register-screens.md` | Registration page variants | 6 |
| 4 | `04-error-pages.md` | 401, 403, Session screens | 11 |
| 5 | `05-progress-gamification.md` | Progress & badges | 4 |
| 6 | `06-dashboard-app-shell.md` | App shell & dashboard | 4 |
| 7 | `07-tasks-screens.md` | Tasks List & Task Detail | 10 |
| 8 | `08-shopping-screens.md` | Shopping Lists & Items | 9 |
| 8a | `08a-shopping-enhancements.md` | Share/Export, Marketplace, Start Trip | 6 |
| 9 | `09-members-zones-screens.md` | Members & Zones | 9 |
| 10 | `10-invites-screens.md` | Invites management | 8 |
| 11 | `11-household-selector.md` | Household selection | 6 |
| 12 | `12-commands-screen.md` | Natural language input | 9 |
| 13 | `13-routines-screens.md` | Routines management | 8 |
| 14 | `14-voice-input.md` | Voice input components | 9 |
| 15 | `15-shopping-run.md` | Shopping Run checklist | 9 |

**Total: ~127 frames**

---

## How to Use

### Option 1: Copy Prompt to Pencil AI

1. Open Pencil
2. Create a new document or open existing
3. Open the AI assistant (Cmd+K or similar)
4. Copy the "Prompt" section from the desired .md file
5. Paste into AI assistant and run
6. Review and adjust the generated design

### Option 2: Reference While Manually Building

Use these files as specifications while manually building in Pencil:
- Color values, spacing, typography from tokens
- Component specifications from base components
- Screen layouts from screen-specific files

---

## Design System Summary

### Brand Colors
- **Primary (Terracotta):** #C45A3B
- **Hover:** #A84A30
- **Background:** #F5F2ED (warm cream)
- **Cards:** #FFFFFF

### Text Colors
- **Primary:** #1A1A1A
- **Secondary:** #888888
- **Muted:** #BDBDBD

### Semantic Colors
- **Success:** #6B8E5E / #E8F0E5
- **Warning:** #E8A055 / #FDF4E8
- **Error:** #D84315 / #FFEBE6
- **Info:** #5B8FC4 / #E8F1F8

### Typography
- **Headings:** Space Grotesk (bold)
- **Body:** Inter (regular/medium)
- **Mono:** IBM Plex Mono (code/invite codes)

### Key Measurements
- **Border radius:** 8px (inputs/buttons), 12px (cards)
- **Card padding:** 48px desktop, 40px tablet, 24px mobile
- **Input height:** 48px
- **Button heights:** SM=36px, MD=44px, LG=48px

### Breakpoints
- **Desktop:** 1200px+
- **Tablet:** 1024px
- **Mobile:** 390px

---

## Complete Screen Inventory

### Auth Screens (02-04)
- Login: Default, Validation Errors, Wrong Credentials, Loading, Tablet, Mobile
- Register: Default, Validation Errors, Password Error, Loading, Tablet, Mobile
- Session Expired: Warning Modal, Expired Modal, Re-auth Failed, Mobile
- 401 Not Signed In: Desktop, Tablet, Mobile
- 403 Access Denied: Desktop, Tablet, Mobile

### App Shell (06)
- Desktop layout with sidebar
- Mobile layout with bottom nav
- Dashboard with stats cards

### Tasks (07)
- Tasks List: Default, Empty, Loading, Filtered, Mobile
- Task Detail: Default, Done, Loading, Not Found, Mobile

### Shopping (08, 08a, 15)
- Shopping Lists: Default, Empty, Loading, Mobile
- Shopping Items: Default, With Purchased, Empty, Loading, Mobile
- Shopping Items Enhanced: With Actions, Share Dropdown, Start Trip Modal, Marketplace Hover, Mobile
- Shopping Run: Active, Empty, Completed, Cancelled, Loading, Complete Modal, Cancel Modal, Mobile

### Members & Zones (09)
- Members List: Default, Empty, Loading, Mobile
- Zones List: Default, Empty, Loading, Mobile
- Create Zone Modal

### Invites (10)
- Invites page: All sections, Generated code, Active list
- Join states: Error, Success
- Empty, Loading, Mobile

### Household Selector (11)
- With households, Empty/Welcome, Loading, Error
- Tablet, Mobile

### Gamification (05)
- Progress page: With data, Empty, Loading, Mobile

### Commands (12, 14)
- Empty, Processing, Executed, Needs Input
- Degraded, Rejected, With History
- Mobile, Mobile History Sheet
- Voice: Idle, Hover, Recording, Uploading, Transcribing, Ready
- Voice Errors: Permission, Rate Limit, Mobile Recording

---

## File Locations in Web Client

```
clients/web/src/
├── styles/
│   ├── tokens.css         # CSS custom properties (source of truth)
│   └── index.css          # Global styles
├── components/
│   ├── ui/                # Base components
│   │   ├── Button.tsx/.css
│   │   ├── TextField.tsx/.css
│   │   ├── Card.tsx/.css
│   │   ├── Modal.tsx/.css
│   │   ├── Spinner.tsx/.css
│   │   └── ...
│   ├── auth/              # Auth-specific
│   │   ├── AuthLayout.tsx/.css
│   │   └── BrandHeader.tsx/.css
│   ├── gamification/      # Progress/badges
│   │   ├── PersonalProgressCard.tsx
│   │   ├── BadgeGrid.tsx
│   │   └── PrivacySettingsCard.tsx
│   ├── tasks/             # Task components
│   ├── notifications/     # Notification components
│   └── commands/          # Command result components
└── routes/
    ├── Login.tsx/.css
    ├── Register.tsx/.css
    ├── Progress.tsx/.css
    ├── TasksList.tsx/.css
    ├── TaskDetail.tsx/.css
    ├── ShoppingLists.tsx/.css
    ├── ShoppingDetail.tsx/.css
    ├── Members.tsx/.css
    ├── ZonesList.tsx/.css
    ├── Invites.tsx/.css
    ├── HouseholdSelector.tsx/.css
    ├── Commands.tsx/.css
    └── ...
```

---

## Notes

- **Not pixel-perfect:** These prompts will generate designs similar to the original, but AI-generated output varies. Manual adjustments may be needed.
- **Component reusability:** Mark components as "reusable" in Pencil to enable instances across screens.
- **Variables:** Use Pencil's variables feature for colors and spacing to enable easy theming.
- **Responsive:** Each screen should have Desktop/Tablet/Mobile variants where applicable.
- **States:** Don't forget empty, loading, and error states for each screen.

---

## Sources of Truth

| Document | Path |
|----------|------|
| Design Tokens (CSS) | `clients/web/src/styles/tokens.css` |
| Design Foundation | `docs/design/FOUNDATION.md` |
| Original Inventory | `docs/design/intake/PR0_pencil_inventory.md` |
| Token Proposal | `docs/design/intake/PR0_tokens_proposal.md` |
| Component Mapping | `docs/design/intake/PR0_component_mapping.md` |
