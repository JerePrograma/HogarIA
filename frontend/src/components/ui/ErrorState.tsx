type Props = {
  message: string;
  title?: string;
};

export function ErrorState({ message, title = 'No se pudo cargar la información' }: Props) {
  return (
    <section className="mensaje-error">
      <strong>{title}</strong>
      <p className="m-0 mt-1">{message}</p>
    </section>
  );
}