import { useCallback, useRef, useState } from 'react';

// Estado de un formulario: [values, handleChange, reset, set]. handleChange sirve de onChange para
// Field, Select, InputSecret y Checkbox: toma el name del control y guarda checked si es un checkbox y
// value si no. reset vuelve a los valores iniciales y set reemplaza todos los valores.
export function useForm(initialValues = {}) {
  const initial = useRef(initialValues);
  const [values, setValues] = useState(initialValues);

  const handleChange = useCallback(({ target }) => {
    setValues((previous) => ({
      ...previous,
      [target.name]: target.type === 'checkbox' ? target.checked : target.value,
    }));
  }, []);

  const reset = useCallback(() => setValues(initial.current), []);

  return [values, handleChange, reset, setValues];
}
