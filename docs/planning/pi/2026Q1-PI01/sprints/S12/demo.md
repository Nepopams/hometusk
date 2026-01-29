# Sprint S12 Demo Plan

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S12/sprint.md`
- Scope: `docs/planning/pi/2026Q1-PI01/sprints/S12/scope.md`

---

## Demo Goal

Demonstrate the complete Routines feature including web UI, lifecycle control (pause/resume), and upcoming instances preview.

---

## Demo Flow

### 1. Navigation
- Show sidebar with new "Routines" link
- Navigate to Routines page

### 2. Empty State
- Show empty state message (if no routines)
- "No routines yet. Create your first routine..."

### 3. Create Routine - Daily
- Click "Create Routine"
- Fill title: "Помыть посуду"
- Select frequency: Daily
- Select policy: Round-robin
- Save and show in list

### 4. Create Routine - Weekly
- Create another routine
- Title: "Вынести мусор"
- Select frequency: Weekly
- Select days: Monday, Thursday
- Select policy: Fixed (select specific user)
- Save and show in list

### 5. List View
- Show multiple routines in list
- Point out: title, zone, frequency text, status badge, policy indicator

### 6. Edit Routine
- Click edit on existing routine
- Show pre-populated form
- Change title or frequency
- Save and verify update

### 7. Pause Routine
- Click pause on ACTIVE routine
- Show confirmation dialog
- Confirm pause
- Show status badge change to PAUSED

### 8. Resume Routine
- Click resume on PAUSED routine
- Show immediate status change to ACTIVE

### 9. Upcoming Instances
- Expand routine or navigate to detail
- Show "Upcoming Tasks" section
- Point out: scheduled dates, existing tasks (with links), projected assignees

### 10. Delete Routine
- Click delete on routine
- Show confirmation dialog
- Confirm deletion
- Show removal from list

---

## Demo Checklist

- [ ] Routines page accessible
- [ ] Create routine works (multiple frequencies)
- [ ] Edit routine works
- [ ] Delete routine works with confirmation
- [ ] Pause/resume buttons functional
- [ ] Upcoming instances displayed
- [ ] Status badges correct
- [ ] Validation errors shown (optional demo)

---

## Environment

- [ ] Backend running with routines data
- [ ] Web client running
- [ ] Test household with:
  - At least 2 members (for round-robin demo)
  - At least 2 routines (ACTIVE and PAUSED)
  - Some existing tasks from scheduler

---

## Fallback

If live demo fails:
- Screenshots prepared in advance
- Video recording available
