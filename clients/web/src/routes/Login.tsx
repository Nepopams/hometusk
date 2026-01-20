import { Link } from 'react-router-dom';

export default function Login() {
  return (
    <div className="page">
      <h1>Login</h1>
      <p>This is a placeholder login screen. Authentication will be added later.</p>
      <div className="card">
        <p>Use the demo route to preview the household layout.</p>
        <Link className="button" to="/households/demo/tasks">
          Continue to Demo Household
        </Link>
      </div>
    </div>
  );
}
