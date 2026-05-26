type LoadingStateProps = {
  title?: string;
  message?: string;
};

export function LoadingState({
  title = "Cargando información",
  message = "Estamos consultando los datos necesarios.",
}: LoadingStateProps) {
  return (
    <section className="panel-muted" aria-busy="true">
      <p className="label-ui">Cargando</p>
      <h3 className="mb-1 mt-2 text-lg font-semibold">{title}</h3>
      <p className="texto-secundario m-0">{message}</p>
    </section>
  );
}
