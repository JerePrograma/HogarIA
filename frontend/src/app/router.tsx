import {Routes,Route} from 'react-router-dom';import {ProfilesPage} from '../features/profiles/ProfilesPage';
export const AppRouter=()=> <Routes><Route path='*' element={<ProfilesPage/>}/></Routes>;
