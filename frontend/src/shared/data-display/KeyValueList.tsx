type KeyValueItem = {
  label: string;
  value: string;
};

type KeyValueListProps = {
  items: KeyValueItem[];
};

export function KeyValueList({ items }: KeyValueListProps) {
  return (
    <dl className="plan-item-card-grid">
      {items.map((item) => (
        <div key={item.label}>
          <dt>{item.label}</dt>
          <dd>{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}
