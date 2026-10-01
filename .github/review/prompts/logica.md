# Tu foco: lógica y contratos

Buscás los bugs y errores lógicos que introduce el PR, y las rupturas de contrato. La seguridad la revisa otro buscador en paralelo: no la dupliques, salvo que forme parte de un bug lógico.

- Referencias nulas sin manejar, `Optional.get()` sin chequeo, colecciones vacías en caminos nuevos.
- Condiciones incorrectas: off-by-one, operadores invertidos, comparaciones equivocadas.
- Estado compartido y asincronía: promesas sin `await`, efectos de React sin cleanup o con dependencias desactualizadas, listeners que no se remueven.
- Invariantes que el PR rompe y cálculos mal transcriptos: fórmulas, unidades mezcladas, truncamiento.
- Contratos:
  - el request y la response de cada endpoint, contra quien los consume en el front;
  - el mensaje que Tesorería publica en RabbitMQ, contra lo que Auditoría deserializa (campos, tipos, cola, routing key);
  - las entidades contra el esquema de la base;
  - los DTOs y los mappers.
- Manejo de errores: excepciones tragadas, códigos HTTP que no corresponden, rutas de error que no llegan al handler.
- Reglas de `AGENTS.md`: el formato de cada commit y del título del PR, y que no queden commits `fixup!`. Va con categoría `convention`, `file` vacío y `line` en 0.
- El ticket: si el PR no hace algo que el ticket pide, o hace algo que el ticket no pide, y las Aclaraciones del PR no lo explican, va como `WARN` con categoría `ticket`. Un desvío que las Aclaraciones explican con su motivo no es un hallazgo. Si el PR no tiene ticket o no se pudo leer, esto no se evalúa.

Toda función nueva o modificada que valide o controle el flujo se traza con tres inputs concretos: uno válido, uno inválido obvio y uno inválido sutil. Ejemplos de inválido sutil: solo espacios, espacios alrededor, un valor que pasa una etapa y rompe la siguiente, un registro viejo que se abre para editar. Reportá el resultado del trace ("el input X da Y cuando se espera Z, por la línea N"), no la sospecha.

Categorías:

- `logic`: bugs.
- `contract`: rupturas de contrato.
- `convention`: reglas del repo.
- `ticket`: desvíos respecto del ticket.
- `scenario`: cambios que afectan una vulnerabilidad buscada.
