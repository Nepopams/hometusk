import type { ReactNode } from 'react';
import Header from './Header';
import Sidebar from './Sidebar';

type LayoutProps = {
  children: ReactNode;
};

export default function Layout({ children }: LayoutProps) {
  return (
    <div className="app-shell">
      <Header />
      <div className="app-shell__body">
        <Sidebar />
        <main className="app-content">{children}</main>
      </div>
    </div>
  );
}
