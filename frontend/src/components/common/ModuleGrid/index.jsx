import { Link, Navigate } from 'react-router-dom';
import Message from '../Message/index.jsx';
import './index.css';

// Grilla de botones grandes, de a dos por fila (si queda uno solo, va al medio), con icono y titulo,
// angosta y centrada en la pantalla. Props: items, lista de { title, icon, path } (icon es un
// Material Symbol); redirectSingle (con un solo item va directo a su path) y emptyMessage (lo que se
// muestra si no hay items).
export default function ModuleGrid({ items, redirectSingle = false, emptyMessage = 'No hay opciones para mostrar.' }) {
  if (items.length === 0) {
    return (
      <div className="module-grid-empty">
        <Message>{emptyMessage}</Message>
      </div>
    );
  }
  if (redirectSingle && items.length === 1) {
    return <Navigate to={items[0].path} replace />;
  }
  return (
    <div className="row g-4 justify-content-center module-grid">
      {items.map((item) => (
        <div key={item.path} className="col-12 col-md-6">
          <Link to={item.path} className="module-grid-item">
            <span className="material-symbols-outlined module-grid-icon" aria-hidden="true">
              {item.icon}
            </span>
            <span className="module-grid-title">{item.title}</span>
          </Link>
        </div>
      ))}
    </div>
  );
}
