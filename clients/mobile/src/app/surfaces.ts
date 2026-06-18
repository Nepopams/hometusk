import type { Surface } from './types';

export const surfaces: Surface[] = [
  {
    key: 'home',
    label: 'Дом',
    title: 'Дом',
    summary: 'Что происходит сегодня.',
    accent: '#2F7D6D',
  },
  {
    key: 'tasks',
    label: 'Задачи',
    title: 'Задачи',
    summary: 'Домашние дела, зоны и исполнители.',
    accent: '#5B8FC4',
  },
  {
    key: 'shopping',
    label: 'Покупки',
    title: 'Покупки',
    summary: 'Списки, товары и покупки для дома.',
    accent: '#E8A055',
  },
  {
    key: 'command',
    label: 'Команды',
    title: 'Команды',
    summary: 'Скажи обычными словами, что нужно сделать дома.',
    accent: '#2F7D6D',
  },
];
