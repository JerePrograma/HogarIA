import { useQuery } from "@tanstack/react-query";
import { useEffect, type ReactNode } from "react";
import { Navigate, useParams } from "react-router-dom";
import { listProfiles } from "../../api/profilesApi";
import { queryKeys } from "../../domain/queryKeys";
import { routePaths } from "./routePaths";

type GuardProps = {
  children: ReactNode;
};

export function DevGuard({ children }: GuardProps) {
  return localStorage.getItem("devUserId") ? (
    <>{children}</>
  ) : (
    <Navigate to={routePaths.devUser} replace />
  );
}

export function ProfileGuard({ children }: GuardProps) {
  const { profileId } = useParams();

  const profilesQuery = useQuery({
    queryKey: queryKeys.profiles,
    queryFn: listProfiles,
  });

  useEffect(() => {
    if (profileId) {
      localStorage.setItem("selectedProfileId", profileId);
    }
  }, [profileId]);

  const selectedProfileId =
    profileId ?? localStorage.getItem("selectedProfileId");

  if (!selectedProfileId) {
    return <Navigate to={routePaths.profiles} replace />;
  }

  if (profilesQuery.isLoading) {
    return <p className="muted">Cargando perfiles...</p>;
  }

  if (profilesQuery.isSuccess) {
    const profileExists = profilesQuery.data.some(
      (profile) => profile.id === selectedProfileId,
    );

    if (!profileExists) {
      localStorage.removeItem("selectedProfileId");
      return <Navigate to={routePaths.profiles} replace />;
    }
  }

  return <>{children}</>;
}

export function ProtectedRoute({ children }: GuardProps) {
  return (
    <DevGuard>
      <ProfileGuard>{children}</ProfileGuard>
    </DevGuard>
  );
}

export function protectedElement(element: JSX.Element) {
  return <ProtectedRoute>{element}</ProtectedRoute>;
}
