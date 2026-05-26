type ProgressBarProps = {
  value: number;
  label?: string;
};

export function ProgressBar({ value, label }: ProgressBarProps) {
  const safeValue = Math.min(Math.max(value, 0), 100);

  return (
    <div
      className="progress-track"
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={safeValue}
      aria-label={label}
    >
      <div className="progress-fill" style={{ width: `${safeValue}%` }} />
    </div>
  );
}
