import './index.css';

// Piezas comunes de Field, Select, InputSecret y Checkbox: la etiqueta, con un punto si el campo es
// obligatorio, y el espacio del mensaje de error, de alto fijo para que aparecer un mensaje no mueva la
// fila.
export function FieldLabel({ htmlFor, required = false, children }) {
  return (
    <label htmlFor={htmlFor} className="form-label">
      {children}
      {required && <span className="field-required" title="Campo obligatorio" aria-hidden="true" />}
    </label>
  );
}

export function FieldError({ id, message }) {
  return (
    <div id={id} className="invalid-feedback d-block field-error">
      {message}
    </div>
  );
}

// Atributos de accesibilidad del control: si tiene error, apunta al mensaje.
export function controlProps(id, error, required) {
  return {
    'aria-invalid': error ? true : undefined,
    'aria-describedby': error ? `${id}-error` : undefined,
    'aria-required': required || undefined,
  };
}

// onKeyDown que llama a onEnter al apretar Enter, o undefined si no hay onEnter.
export function enterHandler(onEnter) {
  if (!onEnter) {
    return undefined;
  }
  return (event) => {
    if (event.key === 'Enter') {
      onEnter();
    }
  };
}
