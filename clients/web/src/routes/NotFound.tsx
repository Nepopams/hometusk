import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="page">
      <h1>Not Found</h1>
      <p>The page you are looking for does not exist.</p>
      <Link className="button" to="/login">
        Go to Login
      </Link>
    </div>
  );
}
