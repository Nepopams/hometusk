# Manual Test Cases: EP-008 — Analytics & Fairness Dashboard

## Prerequisites
- User authenticated and member of a household
- Household has at least 2 members
- Some tasks exist (mix of completed, open, overdue)

---

## TC-001: Analytics Page Access

**Steps:**
1. Login to web app
2. Select a household
3. Click "Analytics" in sidebar navigation

**Expected:**
- Analytics page loads at `/households/{id}/analytics`
- Page shows: Balance Score, Member Contributions, Zone Breakdown, Overdue Tasks
- Default period is "Last 7 days"

---

## TC-002: Balance Score Display

**Precondition:** Household has completed tasks in last 7 days

**Steps:**
1. Navigate to Analytics page
2. Observe Balance Score card

**Expected:**
- Score displays as number (0-100) or "N/A"
- Color matches score:
  - Green: score >= 70
  - Yellow: score 50-69
  - Red: score < 50
  - Gray: N/A (no completed tasks)
- Interpretation text shown below score
- "How is this calculated?" link present

---

## TC-003: Balance Score Expandable Formula

**Steps:**
1. Navigate to Analytics page
2. Click "How is this calculated?"

**Expected:**
- Formula section expands
- Shows: `Balance = 100 × (1 - Gini coefficient)`
- Click again hides formula

---

## TC-004: Member Contributions Table

**Precondition:** Household has 2+ members with tasks

**Steps:**
1. Navigate to Analytics page
2. Observe "Member Contributions" section

**Expected:**
- Table shows all household members
- Columns: Member, Completed, Open, Overdue
- Numbers are accurate per member
- Overdue count highlighted in red if > 0

---

## TC-005: Zone Breakdown Table

**Precondition:** Household has zones with tasks

**Steps:**
1. Navigate to Analytics page
2. Observe "Zone Breakdown" section

**Expected:**
- Table shows all zones with tasks
- Columns: Zone, Completed, Overdue
- Numbers match tasks in each zone

---

## TC-006: Overdue Tasks List

**Precondition:** Household has overdue tasks

**Steps:**
1. Navigate to Analytics page
2. Observe "Overdue Tasks" section

**Expected:**
- Shows max 5 overdue tasks
- Sorted by days overdue (most overdue first)
- Each item shows: title, assignee name, "X days overdue"

---

## TC-007: Period Toggle - 7 days

**Steps:**
1. Navigate to Analytics page
2. Click "Last 7 days" button (if not already selected)

**Expected:**
- Button shows as active/selected
- URL shows `?period=7d` or no period param
- Data reflects last 7 days
- Footer shows correct date range

---

## TC-008: Period Toggle - 30 days

**Steps:**
1. Navigate to Analytics page
2. Click "Last 30 days" button

**Expected:**
- Button shows as active/selected
- URL updates to `?period=30d`
- Data reflects last 30 days
- Completed counts may increase (more history)
- Footer shows correct date range

---

## TC-009: Period Persistence on Reload

**Steps:**
1. Navigate to Analytics page
2. Select "Last 30 days"
3. Refresh browser (F5)

**Expected:**
- Period remains "Last 30 days"
- URL still has `?period=30d`

---

## TC-010: Empty State - No Completed Tasks

**Precondition:** Household has no completed tasks in selected period

**Steps:**
1. Navigate to Analytics page
2. Ensure no tasks completed in last 7 days

**Expected:**
- Balance Score shows "N/A" (gray)
- Interpretation: "N/A — no tasks completed in this period"
- Member Contributions shows 0 for all members' Completed column

---

## TC-011: Empty State - No Overdue Tasks

**Precondition:** Household has no overdue tasks

**Steps:**
1. Navigate to Analytics page

**Expected:**
- Overdue Tasks section shows "No overdue tasks"

---

## TC-012: Non-Toxic Wording Verification

**Steps:**
1. Navigate to Analytics page
2. Review all text on page

**Expected:**
- NO text contains: "fairness score", "unfair", "winner", "loser", "failed"
- Uses: "Balance Score", "Contributions", "Breakdown", "Open tasks"

---

## TC-013: Security - Non-Member Access (API)

**Steps:**
1. Get householdId of a household you're NOT a member of
2. Call: `GET /api/v1/households/{householdId}/analytics`

**Expected:**
- Response: 403 Forbidden
- No data returned

---

## TC-014: Security - Non-Member Access (Web)

**Steps:**
1. Manually enter URL `/households/{other-household-id}/analytics`

**Expected:**
- Page shows "Access Denied" or redirects
- No analytics data visible

---

## TC-015: Loading State

**Steps:**
1. Navigate to Analytics page
2. Observe during data load (may need slow network simulation)

**Expected:**
- Spinner/loading indicator shown
- No broken UI during load

---

## TC-016: Error State

**Steps:**
1. Disconnect network
2. Navigate to Analytics page (or refresh)

**Expected:**
- Error message displayed
- "Retry" option available
- No crash or blank page

---

## Test Summary Checklist

| TC | Description | Pass/Fail |
|----|-------------|-----------|
| TC-001 | Analytics Page Access | |
| TC-002 | Balance Score Display | |
| TC-003 | Balance Score Expandable | |
| TC-004 | Member Contributions | |
| TC-005 | Zone Breakdown | |
| TC-006 | Overdue Tasks List | |
| TC-007 | Period Toggle - 7d | |
| TC-008 | Period Toggle - 30d | |
| TC-009 | Period Persistence | |
| TC-010 | Empty - No Completed | |
| TC-011 | Empty - No Overdue | |
| TC-012 | Non-Toxic Wording | |
| TC-013 | Security - API 403 | |
| TC-014 | Security - Web 403 | |
| TC-015 | Loading State | |
| TC-016 | Error State | |

**Tested by:** _______________
**Date:** _______________
**Environment:** _______________
