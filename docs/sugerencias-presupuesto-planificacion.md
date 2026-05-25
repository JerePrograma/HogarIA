# Sugerencias de presupuesto y planificación mensual

## Conceptos

- Movimiento real: operación importada o cargada manualmente que representa algo que ya ocurrió. Debe estar confirmado para alimentar sugerencias.
- Presupuesto: límite o referencia mensual por categoría. No representa una obligación concreta ni crea movimientos.
- Planificación mensual: compromisos esperados para un período, como ingresos recurrentes, cuotas, deudas, alquiler, salud, suscripciones o recuperos esperados.

El motor de sugerencias nunca crea presupuesto ni planificación desde el preview. El flujo correcto es:

1. Generar preview.
2. Revisar y editar importes, títulos, fechas, cuentas y categorías.
3. Aplicar explícitamente al presupuesto, a la planificación o a ambos.

## Qué se incluye

Para presupuesto se toman movimientos reales confirmados y clasificados:

- gastos de consumo real;
- ahorro o inversión presupuestable;
- deuda cuando la categoría corresponde;
- categorías activas con `budgetable=true` y `technical=false`.

Para planificación se toman patrones que puedan representar compromisos:

- ingresos recurrentes;
- gastos fijos o recurrentes;
- deudas, tarjetas y créditos;
- salud mensual, alquiler, expensas y suscripciones;
- Banco Provincia préstamos importados como deuda estimada;
- Mercado Crédito cuando aparece como deuda o compromiso.

## Qué se excluye

Se excluyen:

- transferencias internas;
- movimientos no confirmados;
- movimientos sin categoría o ignorados por regla;
- movimientos en revisión, salvo inclusión explícita;
- posibles transferencias internas;
- duplicados cross-source;
- desembolsos o recuperables de CJPrestamos;
- capital CJ ya ejecutado o recuperado;
- categorías técnicas o no presupuestables;
- fondeos y gastos aislados pequeños que no forman patrón recurrente.

## Cálculo

Modos disponibles:

- Mes actual: suma del mes seleccionado.
- Promedio últimos 3 meses: promedio de los meses con datos dentro de la ventana.
- Promedio últimos 6 meses: promedio de los meses con datos dentro de la ventana.

El importe sugerido se redondea al múltiplo configurado en el preview, por defecto 1.000. Si se detecta un gasto muy alto de una sola vez frente al historial cercano, se marca como `outlier` y queda sin aplicación automática.

## Confianza

- Alta: hay varios meses consistentes.
- Media: hay más de un movimiento, un compromiso claro o una deuda reconocible.
- Baja: hay poca historia, posible duplicado u outlier.
- Ninguna: no hay base suficiente para sugerir.

La confianza no reemplaza la revisión del usuario. Sólo ayuda a priorizar qué revisar primero.

## Revisión y commit

En la pantalla de sugerencias se puede:

- activar o desactivar cada fila;
- editar importes sugeridos del presupuesto;
- editar título, tipo, fecha, monto, categoría y cuenta de planificación;
- omitir duplicados;
- decidir si un presupuesto existente se conserva o se reemplaza.

El commit sólo crea o actualiza:

- `BudgetCategoryItem`;
- `MonthlyPlanItem`.

No crea `MoneyTransaction`.

## Corrección o reversión

Si se aplicó una sugerencia incorrecta:

- presupuesto: editar o eliminar el item de la categoría en el mes correspondiente;
- planificación: editar, cancelar o eliminar el `MonthlyPlanItem`;
- movimientos reales: corregir clasificación, categoría o estado desde movimientos/importación y volver a generar preview.

Cuando se corrige el origen, el siguiente preview usa las reglas actualizadas y evita arrastrar errores clasificados como transferencias, duplicados o recuperables.
