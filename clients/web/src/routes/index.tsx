import { Navigate, createBrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import HouseholdLayout from './HouseholdLayout';
import HouseholdSelector from './HouseholdSelector';
import Login from './Login';
import Notifications from './Notifications';
import NotFound from './NotFound';
import TaskDetail from './TaskDetail';
import TasksList from './TasksList';
import ZonesList from './ZonesList';

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <Login /> },
  {
    path: '/households',
    element: <ProtectedRoute />,
    children: [{ index: true, element: <HouseholdSelector /> }],
  },
  {
    path: '/households/:householdId',
    element: <ProtectedRoute requireHousehold />,
    children: [
      {
        element: <HouseholdLayout />,
        children: [
          { index: true, element: <Navigate to="tasks" replace /> },
          { path: 'tasks', element: <TasksList /> },
          { path: 'tasks/:taskId', element: <TaskDetail /> },
          { path: 'zones', element: <ZonesList /> },
          { path: 'notifications', element: <Notifications /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFound /> },
]);
