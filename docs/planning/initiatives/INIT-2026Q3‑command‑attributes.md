Initiative: INIT-2026Q3‑command‑attributes — Structured Command Attributes & Scheduling
Status

Proposed — Ready for epic decomposition (2026‑Q3)

Sources of Truth
Existing command UX: INIT‑2026Q2‑command‑ux sets out the initial web command box and traceability【turn38file0†L20-L24】.
ADR‑002 and ADR‑012 describe the MVP text command scenario and idempotency guardrails, focusing on reliability rather than expressiveness【turn40file0†L23-L33】【turn39file0†L20-L28】.
MVP closure hardening plan documents emphasise idempotency, AI resilience and schema validation without expanding the domain【turn41file0†L7-L11】.
1. Problem / Opportunity

The initial command pipeline proves that natural‑language instructions can create or adjust tasks via AI. However, commands are currently “fire‑and‑forget”: they always result in an immediate task with inferred parameters. Users cannot specify key attributes such as due date, assignee, zone or priority at command time, nor can they schedule execution for later. Routines already support scheduling, but commands do not. This limitation creates friction:

Ambiguity: AI guesses deadlines and assignees; users cannot override them before execution.
No manual control: There is no form or UI to adjust due date, zone or assignee at confirmation.
Lack of scheduling: Users cannot queue commands to run in the future or repeat at intervals.
Missing zone context: Commands cannot target specific household zones (e.g. “garage” vs “kitchen”), which tasks and routines support.

Without richer attributes, commands remain less useful than the structured task UI. Extending commands with explicit scheduling and assignment will improve adoption and reduce mis‑assignments.

2. Outcome (what changes for user)

Users issuing commands will be able to:

Specify when the resulting task should occur — immediate, on a date/time (“tomorrow evening”), or after a delay (e.g. “in two weeks”).
Choose who is responsible (self or another household member) when the AI cannot confidently infer it.
Pick a zone (kitchen, bathroom, garden, etc.) to provide context for the task.
See a confirmation form that exposes and allows editing of these attributes before committing the task.
Optionally schedule commands to run later or recur (e.g. “remind me every Monday”).

This empowers users to control the AI‑generated tasks while still leveraging natural language for ease of use.

3. Scope (Now / Next / Later)
NOW — Minimal structured attributes
API additions: Extend POST /api/v1/commands contract with optional fields dueDate, assigneeId, zoneId and scheduleAt. Preserve backwards compatibility (all fields optional).
Command confirmation UI: Enhance the web command confirmation card to display these attributes with editable controls (date picker, assignee dropdown, zone selector).
Backend support: Store the fields on the Command domain model and propagate them into created tasks. Maintain idempotency semantics from ADR‑012【turn39file0†L20-L28】.
Validation: Enforce that the chosen assignee belongs to the current household and that zones exist; fall back to AI‑inferred values if fields are omitted.
Scheduling: Allow a simple “execute later” timestamp; commands scheduled in the future remain pending until the scheduled time.
NEXT — Rich forms & recurrence
Recurrence: Allow commands to define recurrence rules (daily, weekly, monthly) similar to routines.
Priority and reminders: Support priority levels and reminder notifications.
AI suggestions: Provide suggested values (assignee, zone, due date) in the confirmation form with confidence indicators.
Bulk commands: Enable multiple commands in a single request with shared or distinct attributes.
LATER — Deep integration
Voice input & context extraction (depends on ADR‑001).
Explainable decisions: Display AI reasoning about why an assignee/date was selected.
Cross‑household delegation (requires future roles/permissions work).
4. In Scope
Contract updates (OpenAPI) for new fields.
Front‑end work on the command input and confirmation UI.
Backend storage and task creation logic to respect user‑provided attributes.
Scheduling mechanism (e.g., Quartz or cron in backend) to execute future commands.
Migration to add new columns if necessary.
Update DoR/DoD and success metrics for command creation.
5. Out of Scope
Major changes to AI intent resolution or additional AI intents (should rely on existing AI platform).
Complex rule‑based assignment or dynamic zone suggestions (these can be added later).
Full recurrence UI (NEXT) or voice commands (LATER).
Expanding RBAC or cross‑household actions.
6. Success Metrics
≥70 % of commands resulting in tasks have user‑edited attributes (due date/assignee/zone) in early testing.
<3 % of commands fail due to invalid attribute values.
User satisfaction score on command flow increases vs baseline.
p95 command processing latency remains <2 s as per previous performance metric【turn38file0†L56-L58】.
7. Constraints / Guardrails
Contract‑first: All changes must be reflected in OpenAPI and backwards compatible.
Idempotency: Must not break existing idempotency semantics; repeated requests with the same Idempotency‑Key must respect the attribute set.
Security: Only members of a household may assign tasks within that household; zone selection must not leak cross‑household information.
Fallback: If AI fails or user omits attributes, tasks fall back to AI‑inferred defaults.
Performance: Scheduling must not degrade command responsiveness for immediate commands.
8. Dependencies
Completion of INIT‑2026Q2‑command‑ux (web command box and traceability)【turn38file0†L31-L37】.
Existing zone and assignment structures in the task domain.
Availability of household membership information via GET /users/me【turn44file0†L25-L31】.
Infrastructure for scheduled tasks (cron/Quartz) — may require new service or extension of task scheduler.
9. Risks & Mitigations
User confusion when confronted with additional fields in a natural‑language flow → Keep fields optional, provide sensible defaults and progressive disclosure; run user testing.
Scheduling complexity may delay time to market → Phase in simple scheduleAt first; postpone full recurrence to NEXT.
Overlap with routines could blur the user model → Clearly differentiate one‑off scheduled commands from recurring routines and evaluate merging flows in later initiatives.
Backend changes break existing clients → Adopt contract‑first approach; keep new fields optional; version API if needed.
Assignee/zone lists become stale → Load options dynamically based on current household context via API.
10. Epic Candidates
ID	Title	Scope	Status
EP‑011	Command Attribute API & Storage	NOW	Proposed
EP‑012	Command Confirmation UI & Scheduling	NOW	Proposed
EP‑013	Command Recurrence & Advanced UX	NEXT	Proposed
11. Exit Criteria (NOW)
New optional fields (dueDate, assigneeId, zoneId, scheduleAt) accepted by API and persisted.
Command confirmation UI displays and allows editing of these fields.
Created tasks reflect provided attributes.
Idempotency semantics preserved (same key + same payload returns cached response).
Documentation (OpenAPI, service catalog) updated.