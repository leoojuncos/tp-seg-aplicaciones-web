import './index.css';

// Tabla simple, sin orden ni paginado. Props: columns, lista de { key, header, render, width, align }
// (render recibe la fila y devuelve el contenido de la celda; sin render se muestra row[key]; align
// 'end' o 'center'); rows; rowKey, el campo o la funcion que da la clave de cada fila ('id'); loading,
// que muestra "Cargando…"; y emptyMessage, para cuando no hay filas. Las acciones de una fila van en
// una columna con render y botones .btn-icon.
export default function Table({
  columns,
  rows,
  rowKey = 'id',
  loading = false,
  emptyMessage = 'No se encontraron resultados',
}) {
  const keyOf = typeof rowKey === 'function' ? rowKey : (row) => row[rowKey];
  const placeholder = loading ? 'Cargando…' : rows.length === 0 ? emptyMessage : null;

  return (
    <div className="table-responsive">
      <table className="sgm-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                className={alignClass(column.align)}
                style={column.width ? { width: column.width } : undefined}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {placeholder ? (
            <tr>
              <td colSpan={columns.length} className="sgm-table-empty">
                {placeholder}
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={keyOf(row)}>
                {columns.map((column) => (
                  <td key={column.key} className={alignClass(column.align)}>
                    {column.render ? column.render(row) : row[column.key]}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

function alignClass(align) {
  if (align === 'end') {
    return 'text-end';
  }
  return align === 'center' ? 'text-center' : undefined;
}
