import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Login() {
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  if (authProvider !== 'dev') {
    return (
      <div className="page">
        <h1>Authentication Not Available</h1>
        <p>VITE_AUTH_PROVIDER must be &apos;dev&apos; for token paste login.</p>
        <p>Current: {authProvider || 'not set'}</p>
      </div>
    );
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(token.trim());
      navigate('/households');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>Login (Dev Mode)</h1>
      <p>Paste your JWT token to authenticate.</p>

      <form onSubmit={handleSubmit} className="card">
        <label>
          JWT Token:
          <textarea
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            rows={6}
            disabled={loading}
            required
          />
        </label>

        {error && <div className="error">{error}</div>}

        <button className="button" type="submit" disabled={loading || !token.trim()}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </div>
  );
}
