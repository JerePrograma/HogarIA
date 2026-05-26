import { BrowserRouter } from 'react-router-dom';
import { Providers } from './providers/Providers';
import { AppRouter } from './router/AppRouter';

export function App() {
  return (
    <Providers>
      <BrowserRouter>
        <AppRouter />
      </BrowserRouter>
    </Providers>
  );
}
