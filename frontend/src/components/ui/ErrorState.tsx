type Props = { message: string };
export function ErrorState({ message }: Props) { return <p className='error-box'>{message}</p>; }
