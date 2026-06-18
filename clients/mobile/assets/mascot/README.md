# Mobile Mascot Assets

Final mascot artwork is intentionally not generated in code. Export approved static PNG assets to this directory with transparent backgrounds and consistent framing.

Required files:

- `mascot_idle.png`
- `mascot_hello.png`
- `mascot_thinking.png`
- `mascot_success.png`
- `mascot_confirm.png`
- `mascot_confused.png`
- `mascot_reject.png`
- `mascot_degraded.png`

Optional empty-state assets:

- `empty_tasks.png`
- `empty_shopping.png`
- `empty_commands.png`

Until these PNGs are present, `src/shared/ui/Mascot.tsx` renders a deterministic placeholder labeled with the expected filename.
