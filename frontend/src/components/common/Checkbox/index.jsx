import { FieldError } from '../Field/parts.jsx';
import './index.css';

// Casilla en forma de chip con indicador, compatible con useForm: al cambiar emite { target: { name,
// type: 'checkbox', checked } }. Props: name, label (el texto del chip, en forma afirmativa), checked,
// onChange, disabled y title (la etiqueta de arriba; en un formulario va siempre, para que el chip no
// quede suelto entre campos con etiqueta).
export default function Checkbox({ name, label, checked, onChange, disabled = false, title }) {
  const toggle = () => onChange({ target: { name, type: 'checkbox', checked: !checked } });

  return (
    <div className="field">
      {title && <span className="form-label d-block">{title}</span>}
      <button
        type="button"
        role="checkbox"
        aria-checked={checked}
        className={`checkbox-chip${checked ? ' checkbox-chip-checked' : ''}`}
        onClick={toggle}
        disabled={disabled}
      >
        <span className="checkbox-chip-box" aria-hidden="true" />
        {label}
      </button>
      <FieldError />
    </div>
  );
}
