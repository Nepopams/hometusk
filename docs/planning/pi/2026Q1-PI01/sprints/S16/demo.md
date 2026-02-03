# Sprint S16 — Demo Plan

## Demo Date
TBD (end of sprint)

## Attendees
- Product Owner
- Development Team
- Stakeholders (optional)

---

## Demo Scenarios

### 1. ShoppingRun Entity (ST-1301)
**Presenter:** Backend Dev

**Setup:**
- Database with test household, shopping list, items

**Demo Steps:**
1. Show migration V025 applied successfully
2. Show entity classes (ShoppingRun, ShoppingRunItem)
3. Run repository tests demonstrating:
   - Create run from list (items snapshotted)
   - Query runs by household (boundary enforcement)
   - Status transitions (ACTIVE → COMPLETED)

**Success Criteria:**
- [ ] Entity persists correctly
- [ ] Household boundary enforced
- [ ] Tests pass

---

### 2. Export Shopping List (ST-1303)
**Presenter:** Backend Dev

**Setup:**
- Shopping list with various items (including special chars, Cyrillic)

**Demo Steps:**
1. Call `GET /export?format=text` — show plain text output
2. Call `GET /export?format=csv` — show CSV with proper escaping
3. Show Content-Type and Content-Disposition headers
4. Download CSV and open in spreadsheet app

**Success Criteria:**
- [ ] Text format readable
- [ ] CSV properly escaped (commas, quotes)
- [ ] Cyrillic characters correct
- [ ] File downloads with correct name

---

### 3. Marketplace Templates (ST-1304)
**Presenter:** Backend Dev

**Setup:**
- application.yml with Ozon and Yandex Market templates

**Demo Steps:**
1. Call `GET /api/v1/marketplace-templates`
2. Show response with both marketplaces
3. Show startup validation (try malformed template)
4. Demonstrate URL generation concept (client-side)

**Success Criteria:**
- [ ] Templates returned correctly
- [ ] Startup validation works
- [ ] {query} placeholder documented

---

### 4. Task-Shopping Navigation (ST-1309)
**Presenter:** Frontend Dev

**Setup:**
- Task with linked shopping items
- Shopping items with linked task

**Demo Steps:**
1. Open TaskDetail page — show "Shopping Items" section
2. Click item → navigate to ShoppingDetail
3. On ShoppingDetail, show "For task: X" link
4. Click task link → navigate back to TaskDetail
5. Show empty state (task without items)

**Success Criteria:**
- [ ] Navigation works both directions
- [ ] Empty states handled gracefully
- [ ] Links are clickable

---

### 5. Voice Input Polish (ST-1206, ST-1207, ST-1208)
**Presenter:** Frontend Dev

**Setup:**
- Web app with voice input enabled
- Multiple browsers ready (Chrome, Firefox)

**Demo Steps:**
1. **Error Handling (ST-1206):**
   - Deny microphone permission → show friendly error
   - Simulate network error → show retry option
   - Show fallback to text input

2. **Telemetry (ST-1207):**
   - Open browser console
   - Record voice → show telemetry events logged
   - Edit transcript → show edit tracking

3. **Cross-Browser (ST-1208):**
   - Demo in Chrome (primary)
   - Demo in Firefox (if supported)
   - Show keyboard navigation
   - Show screen reader labels (inspect ARIA)

**Success Criteria:**
- [ ] Errors are user-friendly
- [ ] Telemetry events fire correctly
- [ ] Works in Chrome and Firefox
- [ ] Keyboard accessible

---

## Demo Notes Template

| Scenario | Passed | Issues | Follow-up |
|----------|--------|--------|-----------|
| ShoppingRun Entity | | | |
| Export List | | | |
| Marketplace Templates | | | |
| Task-Shopping Nav | | | |
| Voice Polish | | | |

---

## Feedback Collection
- [ ] Capture stakeholder feedback
- [ ] Log improvement suggestions
- [ ] Note any scope questions for S17
