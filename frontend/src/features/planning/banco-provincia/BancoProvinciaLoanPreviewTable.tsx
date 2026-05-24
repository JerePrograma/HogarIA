export function BancoProvinciaLoanPreviewTable({rows}:{rows:any[]}) {
  return <table><thead><tr><th>Cuenta</th><th>Deuda actual</th><th>Importe original</th><th>Vencimiento</th><th>Meses</th><th>Cuota estimada</th><th>Duplicado</th><th>Warnings</th></tr></thead><tbody>{rows.map((r) => <tr key={r.lineNumber}><td>{r.accountNumber}</td><td>{r.currentDebtAmount}</td><td>{r.originalAmount}</td><td>{r.dueDate}</td><td>{r.monthsRemaining}</td><td>{r.estimatedMonthlyAmount}</td><td>{r.duplicate ? 'Sí':'No'}</td><td>{(r.warnings||[]).join(' | ')}</td></tr>)}</tbody></table>;
}
