import React from 'react';
import ReactDOM from 'react-dom/client';
import 'bootstrap/dist/css/bootstrap.min.css';
import '@fontsource-variable/montserrat';
import 'material-symbols/outlined.css';
import '../src/styles/theme.css';
import PortalApp from '../src/PortalApp.jsx';

// Entrada del portal del contribuyente (ver vite.portal.config.js). Carga los mismos estilos base que
// src/main.jsx porque los componentes comunes, como Table, usan la paleta de theme.css.
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <PortalApp />
  </React.StrictMode>,
);
