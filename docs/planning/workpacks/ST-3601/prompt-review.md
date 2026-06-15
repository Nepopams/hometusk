# ST-3601 Review Prompt

Review the ST-3601 diff read-only.

Must-fix focus:

- any mobile behavior change not documented as a bug fix;
- any direct mobile AI Platform call;
- any backend/API/AI Platform contract change;
- any sensitive token stored outside SecureStore;
- command request builder or continuation parser behavior drift;
- task/shopping/push/deep-link/session flow regression;
- missing `npm run typecheck` evidence;
- missing Spotless/CI formatting fix if backend checks still fail.

Report GO/NO-GO with file/line evidence and residual risks.
