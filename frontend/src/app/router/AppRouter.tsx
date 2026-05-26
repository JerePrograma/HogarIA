import { Navigate, Route, Routes } from "react-router-dom";
import { DevGuard, ProtectedRoute } from "./routeGuards";
import { routePaths } from "./routePaths";
import { appRoutes, type AppRoute } from "./routes";

export function AppRouter() {
  return (
    <Routes>
      {appRoutes.map((route) => renderRoute(route))}
      <Route path="*" element={<Navigate to={routePaths.profiles} replace />} />
    </Routes>
  );
}

function renderRoute(route: AppRoute) {
  const element = withGuard(route);

  if (!route.children?.length) {
    return <Route key={route.path} path={route.path} element={element} />;
  }

  return (
    <Route key={route.path} path={route.path} element={element}>
      {route.children.map((child) =>
        child.path === "" ? (
          <Route key="index" index element={child.element} />
        ) : (
          <Route key={child.path} path={child.path} element={child.element} />
        ),
      )}
    </Route>
  );
}

function withGuard(route: AppRoute) {
  if (route.protected) {
    return <ProtectedRoute>{route.element}</ProtectedRoute>;
  }

  if (route.devOnly) {
    return <DevGuard>{route.element}</DevGuard>;
  }

  return route.element;
}
