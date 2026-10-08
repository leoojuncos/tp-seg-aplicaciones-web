import { useNavigate } from 'react-router-dom';

// Boton Volver, gris. Props: to (con to navega ahi; sin to vuelve en el historial, o a / si no hay a
// donde volver dentro del SGM) y label ('Volver').
export default function BackButton({ to, label = 'Volver' }) {
  const navigate = useNavigate();

  const goBack = () => {
    if (to) {
      navigate(to);
    } else if ((window.history.state?.idx ?? 0) > 0) {
      // idx es la posicion que guarda BrowserRouter en el historial: 0 es la primera pantalla del SGM en
      // esta pestana, aunque se haya llegado con un redirect (replace), y volver desde ahi saldria de la app.
      navigate(-1);
    } else {
      navigate('/');
    }
  };

  return (
    <button type="button" className="btn btn-outline-secondary" onClick={goBack}>
      {label}
    </button>
  );
}
