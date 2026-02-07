import { Navigate, createBrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import AcceptInvite from './AcceptInvite';
import Analytics from './Analytics';
import Callback from './Callback';
import Commands from './Commands';
import Dashboard from './Dashboard';
import HouseholdLayout from './HouseholdLayout';
import HouseholdSelector from './HouseholdSelector';
import Invites from './Invites';
import Login from './Login';
import Register from './Register';
import Members from './Members';
import Notifications from './Notifications';
import NotFound from './NotFound';
import Progress from './Progress';
import SessionExpired from './SessionExpired';
import ShoppingDetail from './ShoppingDetail';
import ShoppingRun from './ShoppingRun';
import ShoppingLists from './ShoppingLists';
import Unauthorized from './Unauthorized';
import AccessDenied from './AccessDenied';
import TaskDetail from './TaskDetail';
import TasksList from './TasksList';
import Routines from './Routines';
import ZonesList from './ZonesList';

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <Login /> },
  { path: '/register', element: <Register /> },
  { path: '/callback', element: <Callback /> },
  {
    path: '/households',
    element: <ProtectedRoute />,
    children: [
      { index: true, element: <HouseholdSelector /> },
      // Legacy: redirect to landing page (create via modal or inline form)
      { path: 'new', element: <Navigate to="/households" replace /> },
    ],
  },
  {
    path: '/households/:householdId',
    element: <ProtectedRoute requireHousehold />,
    children: [
      {
        element: <HouseholdLayout />,
        children: [
          { index: true, element: <Dashboard /> },
          { path: 'commands', element: <Commands /> },
          { path: 'invites', element: <Invites /> },
          { path: 'tasks', element: <TasksList /> },
          { path: 'tasks/:taskId', element: <TaskDetail /> },
          { path: 'routines', element: <Routines /> },
          { path: 'shopping', element: <ShoppingLists /> },
          { path: 'shopping/:listId', element: <ShoppingDetail /> },
          { path: 'shopping-runs/:runId', element: <ShoppingRun /> },
          { path: 'analytics', element: <Analytics /> },
          { path: 'progress', element: <Progress /> },
          { path: 'zones', element: <ZonesList /> },
          { path: 'notifications', element: <Notifications /> },
          { path: 'members', element: <Members /> },
        ],
      },
    ],
  },
  {
    path: '/invite',
    element: <ProtectedRoute />,
    children: [{ index: true, element: <AcceptInvite /> }],
  },
  { path: '/session-expired', element: <SessionExpired /> },
  { path: '/unauthorized', element: <Unauthorized /> },
  { path: '/access-denied', element: <AccessDenied /> },
  { path: '*', element: <NotFound /> },
]);
