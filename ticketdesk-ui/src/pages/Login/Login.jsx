import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { authService } from '../../services/authService';
import { Ticket, AlertCircle } from 'lucide-react';
import Toast from '../../components/Toast/Toast';
import styles from './Login.module.css';

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});

    const errs = {};
    if (!username.trim()) errs.username = 'Please enter your username';
    if (!password) errs.password = 'Please enter your password';

    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
      setToast({ message: 'Username and password are required', type: 'error' });
      return;
    }

    setLoading(true);
    try {
      const data = await authService.login({ username, password });
      login(data);
      setToast({ message: `Welcome back, ${data.username}!`, type: 'success' });
      setTimeout(() => {
        if (data.roles && (data.roles.includes('ROLE_ADMIN') || data.roles.includes('ROLE_AGENT'))) {
          navigate('/dashboard');
        } else {
          navigate('/tickets');
        }
      }, 500);
    } catch (err) {
      const data = err.response?.data;
      const msg = data?.message || 'Authentication failed. Please check your credentials.';
      setFieldErrors({ username: 'Invalid credentials', password: 'Check username and password' });
      setToast({ message: msg, type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = (userType) => {
    setFieldErrors({});
    if (userType === 'admin') {
      setUsername('admin');
      setPassword('Admin@1234');
    } else if (userType === 'agent') {
      setUsername('agent');
      setPassword('Agent@1234');
    } else if (userType === 'user') {
      setUsername('john_doe');
      setPassword('Password@123');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.header}>
          <div className={styles.logo}>
            <Ticket size={28} />
          </div>
          <h1 className={styles.title}>Sign in to TicketDesk</h1>
          <p className={styles.subtitle}>Enter your credentials to access your support dashboard</p>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.formGroup}>
            <label className={styles.label}>Username</label>
            <input
              type="text"
              className={`${styles.input} ${fieldErrors.username ? styles.inputError : ''}`}
              placeholder="e.g. admin or john_doe"
              value={username}
              onChange={(e) => { setUsername(e.target.value); setFieldErrors(prev => ({ ...prev, username: null })); }}
              disabled={loading}
            />
            {fieldErrors.username && (
              <span className={styles.fieldError}><AlertCircle size={12} /> {fieldErrors.username}</span>
            )}
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Password</label>
            <input
              type="password"
              className={`${styles.input} ${fieldErrors.password ? styles.inputError : ''}`}
              placeholder="••••••••"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setFieldErrors(prev => ({ ...prev, password: null })); }}
              disabled={loading}
            />
            {fieldErrors.password && (
              <span className={styles.fieldError}><AlertCircle size={12} /> {fieldErrors.password}</span>
            )}
          </div>

          <button type="submit" className={styles.submitBtn} disabled={loading}>
            {loading ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        <div className={styles.quickSection}>
          <div className={styles.quickTitle}>Quick Credentials Preset</div>
          <div className={styles.quickBtns}>
            <button className={styles.quickBtn} onClick={() => handleQuickLogin('admin')}>
              Admin
            </button>
            <button className={styles.quickBtn} onClick={() => handleQuickLogin('agent')}>
              Agent
            </button>
            <button className={styles.quickBtn} onClick={() => handleQuickLogin('user')}>
              User
            </button>
          </div>
        </div>

        <div className={styles.footer}>
          Don't have an account? <Link to="/register">Create Account</Link>
        </div>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
};

export default Login;
