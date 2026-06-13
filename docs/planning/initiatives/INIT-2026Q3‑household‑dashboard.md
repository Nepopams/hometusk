Initiative: INIT-2026Q3‑household‑dashboard — Unified Household Home & Navigation
Status

Proposed — Ready for epic decomposition (2026‑Q3)

Sources of Truth
Household lifecycle initiative delivered the ability to create, join and invite households【turn42file0†L20-L34】.
Story ST‑401 defined a household selector and empty state for switching contexts【turn44file0†L16-L31】.
Currently there is no dedicated “household home” page; tasks, shopping lists and routines live in separate sections, and navigation into a household context is fragmented.
1. Problem / Opportunity

Households are a core concept in HomeTusk: they group tasks, shopping lists, routines and members. After the MVP, users can create and switch households but they cannot view a single consolidated overview. The site lacks a “home” for each household:

Fragmented navigation: Users must visit separate pages to see tasks, lists and members.
No at‑a‑glance view: There is no dashboard summarising overdue tasks, upcoming routines or active shopping runs.
Hidden entry: The existing household selector reveals the current household but does not link to a dedicated household page; the Household route exists in code but is not exposed in navigation.
No context for triage: Without a central hub, planning and triaging household chores is hard; users cannot quickly tell what’s next.

Creating a unified household dashboard will make HomeTusk feel cohesive and help families coordinate.

2. Outcome (what changes for user)

Users will have a dedicated Household Dashboard accessible via the main navigation or household selector. On this page they can:

See a summary of tasks: overdue, due today, upcoming.
View shopping lists and active shopping runs, with quick links to checklists.
See scheduled routines and ability to pause or skip the next occurrence.
View household members and their roles (admin/member).
Create new tasks, shopping lists or invites directly from the dashboard.
On mobile: access the same information in a scrollable feed with clear sections.
3. Scope (Now / Next / Later)
NOW — Minimal dashboard
New route: /households/{householdId} becomes an accessible page; add link in header/side navigation when a household is selected.
Dashboard layout: Use responsive card layout: tasks summary card, shopping lists card, routines card and members card.
Data aggregation: Expose new backend endpoint GET /api/v1/households/{id}/dashboard returning aggregated counts and latest items. Alternatively, fetch existing endpoints client‑side in parallel.
Navigation: Each card links to its detailed page (tasks list, shopping lists page, routines page, members view).
Empty states: For new households with no data, show prompts such as “Create your first task”.
NEXT — Personalisation & analytics
Customisable dashboard: Allow users to hide or reorder sections.
Completion charts: Show weekly task completion rate, streaks or gamification badges.
Zone overview: Summarise cleanliness or task load per zone.
Notifications feed: List recent events (completed tasks, new invites, shopping run completions).
LATER — Integrations & Smart Insights
Sensor integrations: Display real‑world sensor data (e.g. fridge weight, light usage) on dashboard.
Smart suggestions: Use AI to recommend tasks or shopping items based on past behaviour.
Calendar integration: Show upcoming events from external calendars that may affect household planning.
4. In Scope
UI work to add a dashboard route and navigation.
Aggregation of tasks, lists and routines data per household.
Minor backend changes (new endpoint or aggregator service) to support efficient queries.
Security checks to ensure household boundaries (no cross‑household leaks).
Updating documentation (OpenAPI, service catalogue) and DoR/DoD.
5. Out of Scope
Creating new types of content (e.g., expenses or chat).
Deep analytics or machine learning (deferred to NEXT/LATER).
Redesigning individual tasks or lists pages (they remain as is).
Complex role management or permissions beyond admin/member.
6. Success Metrics
≥70 % of active users visit the household dashboard at least once per week after release.
Average time to find overdue tasks decreases (as measured via telemetry).
User satisfaction with navigation improves vs baseline.
No reported cross‑household data leaks; page load time ≤1 s on test data sets.
7. Constraints / Guardrails
Contract‑first: Any new endpoint must be defined in OpenAPI.
Privacy: Only members of a household may view the dashboard; aggregated data must not expose details of other households.
Performance: Aggregations must be efficient; consider caching or denormalised counts to avoid heavy queries.
Responsiveness: The page must be usable on mobile and desktop.
Cohesion: Follow existing design system (use card layouts similar to tasks and shopping lists screens).
8. Dependencies
Household lifecycle (create/join/invite) is delivered【turn42file0†L30-L34】.
Task list, shopping lists and routines features exist as separate routes.
Household context via selector (ST‑401) is implemented【turn44file0†L16-L31】.
Backend support to fetch tasks, lists and routines by household.
Design system components for cards.
9. Risks & Mitigations
Information overload: A dense dashboard may overwhelm users → Start with minimal sections; allow customisation in NEXT.
Performance bottleneck: Aggregating many queries per household may slow down page → Introduce a dedicated dashboard endpoint or caching layer.
Navigation confusion: Users may not discover the dashboard → Add explicit links in header and after household creation; provide onboarding tooltip.
Scope creep: Temptation to add analytics/notifications early → Separate phases (NEXT/LATER) and enforce strict scope for NOW.
Mobile usability issues → Design responsive layout and test on small screens.
10. Epic Candidates
ID	Title	Scope	Status
EP‑021	Household Dashboard UI & Navigation	NOW	Proposed
EP‑022	Backend Dashboard Endpoint	NOW	Proposed
EP‑023	Dashboard Personalisation & Analytics	NEXT	Proposed
11. Exit Criteria (NOW)
/households/{id} page reachable via UI.
Dashboard displays cards summarising tasks, shopping lists, routines and members (with counts and links).
Aggregated data retrieved efficiently and respects household boundaries.
Empty states and error handling implemented.
Documentation updated; DoR/DoD satisfied.