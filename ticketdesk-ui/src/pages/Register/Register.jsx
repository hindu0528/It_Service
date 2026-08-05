import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../../services/authService';
import { UserPlus, AlertCircle, Sparkles } from 'lucide-react';
import Toast from '../../components/Toast/Toast';
import styles from './Register.module.css';

const Register = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('ROLE_USER');
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const fillSampleData = () => {
    const randomNum = Math.floor(100 + Math.random() * 900);
    setUsername(`user_${randomNum}`);
    setEmail(`user${randomNum}@ticketdesk.com`);
    setPassword('Password@123');
    setRole('ROLE_USER');
    setFieldErrors({});
    setToast({ message: 'Sample registration data filled!', type: 'info' });
  };

  const validateClientSide = () => {
    const errs = {};

    // Username validation
    if (!username.trim()) {
      errs.username = 'Username is required';
    } else if (username.length < 3) {
      errs.username = 'Username must be at least 3 characters';
    }

    // Email validation
    if (!email.trim()) {
      errs.email = 'Email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errs.email = 'Please enter a valid email address (e.g. user@example.com)';
    }

    // Password validation (simple min 6 chars)
    if (!password) {
      errs.password = 'Password is required';
    } else if (password.length < 6) {
      errs.password = 'Password must be at least 6 characters long';
    }

    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});

    if (!validateClientSide()) {
      setToast({ message: 'Please fix the highlighted field errors', type: 'error' });
      return;
    }

    setLoading(true);
    try {
      await authService.register({ username, email, password, role });
      setToast({ message: `Account '${username}' created successfully! Redirecting to login...`, type: 'success' });
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      const data = err.response?.data;
      const newFieldErrs = {};

      if (data && data.fieldErrors && Array.isArray(data.fieldErrors)) {
        data.fieldErrors.forEach(fe => {
          newFieldErrs[fe.field] = fe.message;
        });
        setFieldErrors(newFieldErrs);
        setToast({ message: 'Validation failed. Please correct the highlighted fields.', type: 'error' });
      } else if (data && data.message) {
        if (data.message.toLowerCase().includes('admin')) {
          newFieldErrs.role = data.message;
        }
        setFieldErrors(newFieldErrs);
        setToast({ message: data.message, type: 'error' });
      } else {
        setToast({ message: 'Registration failed. Please check your network connection.', type: 'error' });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.header}>
          <div className={styles.logo}>
            <UserPlus size={26} />
          </div>
          <h1 className={styles.title}>Create Account</h1>
          <p className={styles.subtitle}>Sign up to submit and track support tickets</p>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <button type="button" className={styles.sampleBtn} onClick={fillSampleData}>
            <Sparkles size={14} style={{ display: 'inline', marginRight: '4px' }} />
            Fill Sample Registration Data
          </button>

          <div className={styles.formGroup}>
            <label className={styles.label}>Username</label>
            <input
              type="text"
              className={`${styles.input} ${fieldErrors.username ? styles.inputError : ''}`}
              placeholder="e.g. alex_smith"
              value={username}
              onChange={(e) => { setUsername(e.target.value); setFieldErrors(prev => ({ ...prev, username: null })); }}
              disabled={loading}
            />
            {fieldErrors.username && (
              <span className={styles.fieldError}><AlertCircle size={12} /> {fieldErrors.username}</span>
            )}
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Email Address</label>
            <input
              type="email"
              className={`${styles.input} ${fieldErrors.email ? styles.inputError : ''}`}
              placeholder="e.g. alex@example.com"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setFieldErrors(prev => ({ ...prev, email: null })); }}
              disabled={loading}
            />
            {fieldErrors.email && (
              <span className={styles.fieldError}><AlertCircle size={12} /> {fieldErrors.email}</span>
            )}
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Password</label>
            <input
              type="password"
              className={`${styles.input} ${fieldErrors.password ? styles.inputError : ''}`}
              placeholder="Min 6 characters"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setFieldErrors(prev => ({ ...prev, password: null })); }}
              disabled={loading}
            />
            {fieldErrors.password && (
              <span className={styles.fieldError}><AlertCircle size={12} /> {fieldErrors.password}</span>
            )}
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Role</label>
            <select
              className={`${styles.select} ${fieldErrors.role ? styles.inputError : ''}`}
              value={role}
              onChange={(e) => { setRole(e.target.value); setFieldErrors(prev => ({ ...prev, role: null })); }}
              disabled={loading}
            >
              <option value="ROLE_USER">User (Standard Support Client)</option>
              <option value="ROLE_AGENT">Support Agent</option>
              <option value="ROLE_ADMIN">Administrator (Restricted Admin Registration Test)</option>
            </select>
            {fieldErrors.role && (
              <span className={styles.fieldError}><AlertCircle size={12} /> {fieldErrors.role}</span>
            )}
          </div>

          <button type="submit" className={styles.submitBtn} disabled={loading}>
            {loading ? 'Creating Account...' : 'Register Account'}
          </button>
        </form>

        <div className={styles.footer}>
          Already have an account? <Link to="/login">Sign In</Link>
        </div>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
};

export default Register;
