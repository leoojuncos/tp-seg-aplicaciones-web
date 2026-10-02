# Cómo buscar

Leé el diff completo antes de empezar. Para todo lo que necesites fuera del diff, usá Read, Grep y Glob sobre el repo. No supongas el contenido de un archivo que no leíste.

En la búsqueda el objetivo es la cobertura, no la precisión. Reportá todo lo que encuentres, aunque tengas poca confianza, con `confidence` y `severity` honestos. Después de vos hay un verificador que intenta refutar cada hallazgo, y dos modelos externos que votan los que bloquean. Un hallazgo que se cae en la verificación cuesta poco; uno que no reportás no lo recupera nadie.

Si no encontrás nada, devolvé `findings` vacío: es un resultado válido.
