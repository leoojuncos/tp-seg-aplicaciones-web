import { useState } from 'react';
import { errorMessage, postJson } from '../../api/http.js';
import { Field, InputSecret, Message } from '../../components/common/index.js';
import { useForm } from '../../hooks/useForm.js';
import { usePortalSession } from '../../hooks/usePortalSession.js';
import './index.css';

// Ingreso al portal del contribuyente con CUIT y clave. La clave la valida el monolito; con el ingreso
// hecho, la ruta (guestOnly) manda al inicio.
export default function PortalLoginView() {
  const [values, handleChange] = useForm({ cuit: '', password: '' });
  const [cuitError, setCuitError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const { login } = usePortalSession();

  async function handleSubmit(event) {
    event.preventDefault();
    // El CUIT va con sus 11 digitos, sin guiones, como lo recibe la API.
    const cuit = values.cuit.trim();
    if (!/^\d{11}$/.test(cuit)) {
      setCuitError('Ingresá los 11 dígitos del CUIT, sin guiones.');
      return;
    }
    setCuitError(null);
    setSubmitting(true);
    setError('');
    try {
      const response = await postJson('/api/vep/auth/login', { cuit, password: values.password }, { authRedirect: false });
      if (response.ok) {
        login(response.body);
      } else {
        setError(errorMessage(response, 'CUIT o clave incorrectos'));
        setSubmitting(false);
      }
    } catch {
      setError('No se pudo conectar con el servidor.');
      setSubmitting(false);
    }
  }

  return (
    <section className="portal-login">
      <form onSubmit={handleSubmit} className="portal-login-form">
        <h1>Ingresá al portal</h1>
        <Field
          label="CUIT/CUIL"
          name="cuit"
          value={values.cuit}
          onChange={handleChange}
          error={cuitError}
          autoComplete="username"
          placeholder="20123456789"
          required
          disabled={submitting}
        />
        <InputSecret
          label="Contraseña"
          name="password"
          value={values.password}
          onChange={handleChange}
          required
          disabled={submitting}
        />
        <Message type="error">{error}</Message>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          Ingresar
        </button>
      </form>
    </section>
  );
}
