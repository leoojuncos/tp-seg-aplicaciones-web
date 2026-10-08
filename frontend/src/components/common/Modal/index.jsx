import { useEffect, useId, useRef } from 'react';
import './index.css';

// Modal sobre un fondo gris. Props: show (true), title, size ('sm', 'md', 'lg' o 'xl'; 'md'), onClose
// (Escape lo llama; sin onClose, Escape no cierra), footer (los botones) y children (el cuerpo). Al
// abrir lleva el foco al primer control y bloquea el scroll de la pagina; al cerrar devuelve el foco a
// donde estaba.
export default function Modal({ show = true, title, size = 'md', onClose, footer, children }) {
  const dialogRef = useRef(null);
  const onCloseRef = useRef(onClose);
  const titleId = useId();

  useEffect(() => {
    onCloseRef.current = onClose;
  });

  useEffect(() => {
    if (!show) {
      return undefined;
    }
    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialogRef.current?.querySelector('input, select, textarea, button')?.focus();

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onCloseRef.current?.();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = previousOverflow;
      previousFocus?.focus?.();
    };
  }, [show]);

  if (!show) {
    return null;
  }
  return (
    <div className="modal d-block sgm-modal" role="dialog" aria-modal="true" aria-labelledby={title ? titleId : undefined}>
      <div className={`modal-dialog modal-dialog-centered${size === 'md' ? '' : ` modal-${size}`}`} ref={dialogRef}>
        <div className="modal-content">
          <div className="modal-header">
            {title && (
              <h2 id={titleId} className="modal-title">
                {title}
              </h2>
            )}
          </div>
          <div className="modal-body">{children}</div>
          {footer && <div className="modal-footer">{footer}</div>}
        </div>
      </div>
    </div>
  );
}
