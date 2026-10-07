import { BrowserRouter } from 'react-router-dom';
import { SessionProvider } from './context/SessionProvider.jsx';
import Router from './Router.jsx';

export default function App() {
  return (
    <BrowserRouter>
      <SessionProvider>
        <Router />
      </SessionProvider>
    </BrowserRouter>
  );
}
