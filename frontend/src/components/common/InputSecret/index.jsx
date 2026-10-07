import { useState } from 'react';
import { controlProps, enterHandler, FieldError, FieldLabel } from '../Field/parts.jsx';
import './index.css';

// Campo de contrasena con un boton de ojo para mostrarla u ocultarla. Props: las de Field (label,
// name, id, value, onChange, required, disabled, error, onEnter y el resto para el <input>);
// autoComplete es 'current-password' si no se pasa.
export default function InputSecret({
  label,
  name,
  id = name,
  value,
  onChange,
  required = false,
  disabled = false,
  error,
  onEnter,
  autoComplete = 'current-password',
  ...inputProps
}) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="field">
      {label && (
        <FieldLabel htmlFor={id} required={required}>
          {label}
        </FieldLabel>
      )}
      <div className="input-secret">
        <input
          id={id}
          name={name}
          type={visible ? 'text' : 'password'}
          value={value ?? ''}
          onChange={onChange}
          disabled={disabled}
          autoComplete={autoComplete}
          className={`form-control${error ? ' is-invalid' : ''}`}
          onKeyDown={enterHandler(onEnter)}
          {...controlProps(id, error, required)}
          {...inputProps}
        />
        <button
          type="button"
          className="input-secret-toggle"
          onClick={() => setVisible((current) => !current)}
          disabled={disabled}
          title={visible ? 'Ocultar' : 'Mostrar'}
          aria-label={visible ? 'Ocultar la contraseña' : 'Mostrar la contraseña'}
        >
          <span className="material-symbols-outlined" aria-hidden="true">
            {visible ? 'visibility' : 'visibility_off'}
          </span>
        </button>
      </div>
      <FieldError id={`${id}-error`} message={error} />
    </div>
  );
}
