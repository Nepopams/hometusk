import { Outlet } from 'react-router-dom';
import { CommandHistory, CommandInput } from '../components/commands';
import Layout from '../components/Layout/Layout';

export default function HouseholdLayout() {
  return (
    <Layout>
      <CommandInput />
      <CommandHistory />
      <Outlet />
    </Layout>
  );
}
