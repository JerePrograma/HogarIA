import { BrowserRouter } from 'react-router-dom';import {AppRouter} from './router';import {Providers} from './providers';
export const App=()=> <Providers><BrowserRouter><AppRouter/></BrowserRouter></Providers>;
