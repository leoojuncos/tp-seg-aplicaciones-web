import { useState } from 'react';
import { Field, InputSecret, Message } from '../../components/common/index.js';
import { errorMessage, postJson } from '../../api/http.js';
import { useForm } from '../../hooks/useForm.js';
import { useSession } from '../../hooks/useSession.js';

export default function LoginView() {
  const [values, handleChange] = useForm({ username: '', password: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const { refresh } = useSession();

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const response = await postJson('/api/auth/login', values, { authRedirect: false });
      if (response.ok) {
        const session = await refresh();
        if (!session) {
          // 200 del login pero GET /api/auth/session no trajo sesion (red, 5xx, cookie no
          // guardada): sin esto el formulario quedaba deshabilitado para siempre, sin aviso.
          setError('No se pudo iniciar la sesión. Probá de nuevo.');
          setSubmitting(false);
        }
        // Si hay sesion, PublicRoute redirige solo: no hace falta reactivar el formulario, el
        // componente se desmonta.
      } else {
        setError(errorMessage(response, 'Usuario o clave incorrectos'));
        setSubmitting(false);
      }
    } catch {
      setError('No se pudo conectar con el servidor.');
      setSubmitting(false);
    }
  }

  return (
    <section className="generic-page">
      <span className="material-symbols-outlined generic-page-icon" aria-hidden="true">
        login
      </span>
      <h2>Ingresar al SGM</h2>
      <form onSubmit={handleSubmit}>
        <Field
          label="Usuario"
          name="username"
          value={values.username}
          onChange={handleChange}
          autoComplete="username"
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
