import { Outlet } from 'react-router-dom';
import Layout from '../components/Layout/Layout';

export default function HouseholdLayout() {
  return (
    <Layout>
      <Outlet />
    </Layout>
  );
}
