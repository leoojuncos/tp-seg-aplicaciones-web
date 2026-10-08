import { controlProps, FieldError, FieldLabel } from '../Field/parts.jsx';

// Desplegable con etiqueta y espacio de error. Props: label, name, id (el name si no se pasa), value,
// onChange, options (lista de { value, label }), emptyOption (texto de una primera opcion vacia, con
// value ""), required, disabled y error (el mensaje).
export default function Select({
  label,
  name,
  id = name,
  value,
  onChange,
  options,
  emptyOption,
  required = false,
  disabled = false,
  error,
}) {
  return (
    <div className="field">
      {label && (
        <FieldLabel htmlFor={id} required={required}>
          {label}
        </FieldLabel>
      )}
      <select
        id={id}
        name={name}
        value={value ?? ''}
        onChange={onChange}
        disabled={disabled}
        className={`form-select${error ? ' is-invalid' : ''}`}
        {...controlProps(id, error, required)}
      >
        {emptyOption !== undefined && <option value="">{emptyOption}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <FieldError id={`${id}-error`} message={error} />
    </div>
  );
}
