import { Link } from "react-router-dom";

interface Props {
  profileId: string;
  accountsLoading: boolean;
  categoriesLoading: boolean;
  hasAccounts: boolean;
  hasCategories: boolean;
}

export function TransactionSetupWarnings({
  profileId,
  accountsLoading,
  categoriesLoading,
  hasAccounts,
  hasCategories,
}: Props) {
  return (
    <>
      {!accountsLoading && !hasAccounts ? (
        <section className="mensaje-warning transactions-setup-warning">
          <strong>No hay cuentas cargadas.</strong>
          <span>Necesitás al menos una cuenta para registrar movimientos.</span>
          <Link
            className="boton-secundario"
            to={`/profiles/${profileId}/accounts`}
          >
            Crear cuenta
          </Link>
        </section>
      ) : null}

      {!categoriesLoading && !hasCategories ? (
        <section className="mensaje-warning transactions-setup-warning">
          <strong>No hay categorías cargadas.</strong>
          <span>
            Necesitás al menos una categoría para clasificar movimientos.
          </span>
          <Link
            className="boton-secundario"
            to={`/profiles/${profileId}/categories`}
          >
            Crear categoría
          </Link>
        </section>
      ) : null}
    </>
  );
}
