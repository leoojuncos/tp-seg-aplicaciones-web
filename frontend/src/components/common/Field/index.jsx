import { controlProps, enterHandler, FieldError, FieldLabel } from './parts.jsx';

// Campo de texto con etiqueta y espacio de error. Props: label, name, id (el name si no se pasa),
// value, onChange, type ('text'), required, disabled (sirve para mostrar un detalle de solo lectura),
// error (el mensaje), onEnter (se llama al apretar Enter) y el resto, que pasa al <input>
// (placeholder, autoComplete, maxLength...).
export default function Field({
  label,
  name,
  id = name,
  value,
  onChange,
  type = 'text',
  required = false,
  disabled = false,
  error,
  onEnter,
  ...inputProps
}) {
  return (
    <div className="field">
      {label && (
        <FieldLabel htmlFor={id} required={required}>
          {label}
        </FieldLabel>
      )}
      <input
        id={id}
        name={name}
        type={type}
        value={value ?? ''}
        onChange={onChange}
        disabled={disabled}
        className={`form-control${error ? ' is-invalid' : ''}`}
        onKeyDown={enterHandler(onEnter)}
        {...controlProps(id, error, required)}
        {...inputProps}
      />
      <FieldError id={`${id}-error`} message={error} />
    </div>
  );
}
