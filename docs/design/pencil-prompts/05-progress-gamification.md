# Pencil Prompt Pack: Progress & Gamification

> Run after 04-error-pages.md to create Progress/Gamification screens.

---

## Prompt

Create the Progress page design for HomeTusk gamification features.

---

## Page Layout

- Background: #F5F2ED (within app shell - has sidebar/header)
- Content max-width: 900px
- Centered horizontally with auto margins
- Vertical gap between sections: 24px

### Page Header

- Title: "Progress" - H1 (32px, weight 700)
- Subtitle: "Track your achievements and household team progress." - Body (16px, #888888)
- Gap between title and subtitle: 8px

---

## Card Grid (2-column on desktop)

Grid layout:
- 2 columns on desktop (>768px)
- 1 column on mobile
- Gap: 20px

### 1. Personal Progress Card

**Card styling:**
- Background: #FFFFFF
- Border: 1px solid #E0DDD8
- Border radius: 8px
- Padding: 24px

**Content:**
- Title: "Your Progress" - H3 (18px, weight 700)
- Gap below title: 16px

**Stats section:**
- Total Points: Large number (48px, weight 700), label below (13px, #888888 "Total points")
- This Week: Row layout - label "This week:" (13px, #888888) + value "+25" (28px, weight 600)

**Streak section:**
- Container: background #F9F7F4, border 1px solid #E0DDD8, border-radius 8px, padding 16px
- Centered content
- Streak value: 24px, weight 600, color #C45A3B
- Label: "day streak" (13px, #888888)
- Message: "Day 5! Keep it up!" (13px, #1A1A1A)
- Best streak (if applicable): "Best: 12 days" (12px, #BDBDBD)

**Badges section:**
- Title: "Your Badges" (16px, weight 500)
- Badge grid: flex wrap, gap 12px
- Empty state: "Complete tasks to earn badges!" (13px, #BDBDBD)

**Encouragement:**
- "Keep it up!" (14px, weight 500, color #6B8E5E success)

### 2. Household Aggregate Card

**Card styling:** Same as Personal Progress

**Content:**
- Title: "Household Stats" - H3 (18px, weight 700)
- Helper text: "See how your household is doing together" (13px, #888888)

**Stats:**
- Total tasks completed: Large number (36px, weight 700) + label
- Total points earned: Large number + label
- Horizontal layout with 32px gap

---

## Badge Card Component

Individual badge display:
- Container: padding 12px 16px, border-radius 8px
- Background: #F9F7F4 (not earned), linear-gradient(135deg, #fff9c4, #ffe082) (earned)
- Opacity: 0.5 + grayscale(0.8) when not earned
- Centered content, vertical layout

**Content:**
- Icon: 24px emoji or icon
- Name: 12px, weight 500
- Criteria: 10px, #BDBDBD

**Badge Icons (emoji mapping):**
- STAR: star
- TROPHY: trophy
- FIRE: fire
- CHECK: checkmark
- MEDAL: medal

---

## Badge Catalog Section

Full-width section below the grid:
- Title: "All Badges" (16px, weight 500)
- Grid of all available badges (earned and not earned)
- Empty state: "No badges available." (13px, #BDBDBD)

---

## Privacy Settings Card

**Card styling:** Same as other cards

**Content:**
- Title: "Privacy Settings" - H3 (18px, weight 700)

**Toggle options (checkbox style):**
1. "Show my progress to household members"
2. "Show my streak to household members"
3. "Enable gamification"

Each toggle:
- Checkbox: 18x18px
- Label: 14px, gap 12px from checkbox
- Cursor: pointer

**Warning text (when gamification disabled):**
- "You will not earn points or badges while gamification is disabled."
- Font: 13px, color #E8A055 (warning)
- Margin-left: 30px (aligned with label text)

**Saving state:**
- "Saving..." (13px, #888888, italic)

---

## States

### Empty State
When user has 0 points and no badges:
- Personal Progress Card shows:
- Centered text: "Start completing tasks to earn points!" (16px, #888888)
- Padding: 24px

### Loading State
- Page title visible
- Spinner centered below title
- `.progress__loading` alignment

---

## Responsive (Mobile <768px)

- Grid becomes 1 column
- Stats in Personal Progress card: vertical layout
- Large numbers slightly smaller (36px instead of 48px)
- Card padding: 16px

---

## Frame Names

| Frame Name | Description |
|------------|-------------|
| Progress - Desktop | Full content with data |
| Progress - Empty | No points/badges yet |
| Progress - Loading | Spinner state |
| Progress - Mobile | 390px breakpoint |

---

## Expected Output

4 frames showing Progress page states and breakpoints.
