import type { Surface } from './types';

export const surfaces: Surface[] = [
  {
    key: 'home',
    label: 'Home',
    title: 'Household home',
    summary: 'Members, zones, notifications, and the daily household pulse.',
    accent: '#1d7f68',
  },
  {
    key: 'tasks',
    label: 'Tasks',
    title: 'Tasks and zones',
    summary: 'Household-scoped work, assignees, zones, and completion state.',
    accent: '#2b66c3',
  },
  {
    key: 'shopping',
    label: 'Shopping',
    title: 'Shopping lists',
    summary: 'Shared lists, item state, purchase counts, and task links.',
    accent: '#946200',
  },
  {
    key: 'command',
    label: 'Command',
    title: 'Command chat',
    summary: 'Context-ready command surface for the selected household.',
    accent: '#7a3db8',
  },
];
