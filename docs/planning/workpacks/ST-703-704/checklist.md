# DoD Checklist: ST-703 + ST-704

## Code Quality
- [ ] TypeScript strict mode passes
- [ ] ESLint clean (`npm run lint`)
- [ ] Consistent component patterns (hooks, props)
- [ ] No `any` types without justification

## Tests
- [ ] AnalyticsPage renders with mock data
- [ ] BalanceScoreCard shows score correctly
- [ ] BalanceScoreCard handles null (N/A)
- [ ] BalanceScoreCard expandable works
- [ ] MemberStatsList displays all members
- [ ] ZoneStatsList displays all zones
- [ ] Error state renders correctly
- [ ] Loading state renders correctly
- [ ] All tests pass: `npm test`

## UI/UX
- [ ] Loading spinner shown while fetching
- [ ] Error message user-friendly with retry option
- [ ] Balance score has visual indicator (color)
- [ ] "How calculated" expandable works
- [ ] Non-toxic wording throughout
- [ ] Navigation link visible and works

## Accessibility
- [ ] Keyboard navigable
- [ ] Screen reader labels (aria-labels on interactive elements)
- [ ] Color not only indicator (text + color)

## ST-704 (Stretch)
- [ ] Period toggle visible
- [ ] Switching reloads data
- [ ] URL reflects period selection
- [ ] Page reload preserves selection
