import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import AuthLayout from '../components/AuthLayout.jsx';
import Alert from '../components/Alert.jsx';

const EMPTY = { fullName: '', email: '', phone: '', username: '', password: '', confirm: '' };

export default function Register() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setErrors({});
    if (form.password !== form.confirm) {
      setErrors({ confirm: 'Passwords do not match' });
      return;
    }
    setBusy(true);
    try {
      const { confirm, ...body } = form;
      await register(body);
      navigate('/dashboard');
    } catch (err) {
      setError(Object.keys(err.fieldErrors).length ? 'Please fix the highlighted fields.' : err.message);
      setErrors(err.fieldErrors);
    } finally {
      setBusy(false);
    }
  };

  const field = (key, label, props = {}) => (
    <label className={errors[key] ? 'field has-error' : 'field'}>
      <span>{label}</span>
      <input value={form[key]} onChange={set(key)} required {...props} />
      {errors[key] && <small className="field-error">{errors[key]}</small>}
    </label>
  );

  return (
    <AuthLayout title="Open your account" subtitle="It takes a minute. A savings account is created for you.">
      <form onSubmit={submit} className="form">
        <Alert>{error}</Alert>
        {field('fullName', 'Full name', { autoFocus: true, autoComplete: 'name' })}
        <div className="row-2">
          {field('email', 'Email', { type: 'email', autoComplete: 'email' })}
          {field('phone', 'Mobile number', { inputMode: 'tel', placeholder: '9876543210' })}
        </div>
        {field('username', 'Username', { autoComplete: 'username', placeholder: 'letters, numbers, . or _' })}
        <div className="row-2">
          {field('password', 'Password', { type: 'password', autoComplete: 'new-password' })}
          {field('confirm', 'Confirm password', { type: 'password', autoComplete: 'new-password' })}
        </div>
        <p className="muted small">At least 8 characters with a letter and a number.</p>
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="auth-switch">
        Already a customer? <Link to="/login">Log in</Link>
      </p>
    </AuthLayout>
  );
}
