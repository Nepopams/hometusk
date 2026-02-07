# Sprint S18 — Demo Plan

## Demo Goal
Demonstrate end-to-end Shopping Run experience: from list to completed trip.

---

## Prerequisites
- Shopping list with 5+ unpurchased items
- Backend running with ST-1302 endpoints
- Web client with ST-1307 + ST-1308 changes

---

## Demo Scenarios

### Scenario 1: Happy Path — Complete Shopping Trip
1. Navigate to shopping list with items
2. Click "Start Shopping Trip" button
3. Review confirmation modal (item count, list name)
4. Click "Start" → redirect to run page
5. Progress shows "0 of 5 purchased"
6. Check off 4 items (optimistic update, progress increments)
7. Click "Complete Trip"
8. Confirm dialog: "4 items purchased, 1 skipped"
9. View summary: run marked COMPLETED, read-only checkboxes

### Scenario 2: Cancel Trip
1. Start new shopping trip
2. Check 1 item
3. Click "Cancel Trip"
4. Confirm cancellation
5. Verify run is CANCELLED
6. Redirect to shopping list

### Scenario 3: Empty List Prevention
1. Navigate to list with all items purchased (or empty)
2. Verify "Start Shopping Trip" button is disabled
3. Verify helpful message shown

### Scenario 4: Error Recovery
1. Start trip
2. (Simulate network error)
3. Click checkbox
4. Verify optimistic update, then rollback
5. Verify error snackbar
6. Retry → success

### Scenario 5: Closed Run View
1. Navigate to completed run via direct URL
2. Verify status badge (COMPLETED/CANCELLED)
3. Verify checkboxes are read-only
4. Verify no action buttons

---

## Success Criteria
- [ ] Run created successfully from list
- [ ] Items checkable with optimistic updates
- [ ] Progress indicator accurate
- [ ] Complete/cancel flows work
- [ ] Closed runs are read-only
- [ ] Error states handled gracefully

---

## Notes
- EP-013 exit criteria should be fully verified after demo
- INIT-2026Q2-shopping-marketplaces ready to close
