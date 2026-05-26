type Props = {
  message: string;
  title?: string;
};

export function EmptyState({ message, title = 'Sin información para mostrar' }: Props) {
  return (
    <section className="panel-muted">
      <p className="label-ui">Estado vacío</p>
      <h3 className="mb-1 mt-2 text-lg font-semibold">{title}</h3>
      <p className="texto-secundario m-0">{message}</p>
    </section>
  );
}