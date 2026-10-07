import { Fragment } from 'react';
import { Link } from 'react-router-dom';
import './index.css';

// Encabezado de una pantalla: las migas de los niveles de arriba (12px, con enlace si traen url), el
// titulo de la pantalla actual y, a la derecha, sus acciones; cierra con una linea. Props: titles,
// lista de { title, url } o strings donde el ultimo es la pantalla actual, y actions (opcional, por
// ejemplo un boton).
export default function SectionHeading({ titles, actions = null }) {
  const items = titles.map((item) => (typeof item === 'string' ? { title: item } : item));
  const current = items[items.length - 1];
  const trail = items.slice(0, -1);

  return (
    <div className="section-heading">
      {trail.length > 0 && (
        <nav className="section-heading-trail" aria-label="Ubicación">
          {trail.map((item, index) => (
            <Fragment key={`${index}-${item.title}`}>
              {item.url ? <Link to={item.url}>{item.title}</Link> : <span>{item.title}</span>}
              <span aria-hidden="true">/</span>
            </Fragment>
          ))}
        </nav>
      )}
      <div className="section-heading-row">
        <h1 className="section-heading-title">{current?.title}</h1>
        {actions && <div className="section-heading-actions">{actions}</div>}
      </div>
      <hr className="section-heading-line" />
    </div>
  );
}
