import { Component } from 'react';
import BackButton from '../BackButton/index.jsx';

// Atrapa un error al mostrar una pantalla y en su lugar muestra "Algo salio mal" con Volver; el error
// queda en la consola. AppLayout lo monta con la ruta como key, asi al navegar se reinicia. Props:
// children.
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    console.error(error, info.componentStack);
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }
    return (
      <section className="generic-page">
        <span className="material-symbols-outlined generic-page-icon" aria-hidden="true">
          error
        </span>
        <h2>Algo salió mal</h2>
        <p>Ocurrió un error inesperado al mostrar esta pantalla.</p>
        <div className="generic-page-actions">
          <BackButton />
        </div>
      </section>
    );
  }
}
