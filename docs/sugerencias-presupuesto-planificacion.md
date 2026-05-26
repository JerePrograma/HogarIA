# Sugerencias de presupuesto y planificación mensual

## Conceptos base

- Movimiento real: operación importada o cargada manualmente que representa algo que ya ocurrió. Debe estar confirmado para alimentar sugerencias.
- Presupuesto: límite o referencia mensual por categoría. No representa una obligación concreta y no crea movimientos.
- Planificación mensual: compromisos esperados para un período, como ingresos recurrentes, cuotas, deudas, alquiler, salud, suscripciones o recuperos esperados.

El flujo separa estrictamente preview y commit:

1. El preview calcula sugerencias y totales, pero no crea ni modifica datos persistentes.
2. El usuario revisa, edita, activa o desactiva cada fila.
3. El commit aplica sólo las filas activas y sólo puede crear o actualizar `BudgetCategoryItem` y crear `MonthlyPlanItem`.
4. Este flujo nunca crea `MoneyTransaction`.

## Preview

El preview lee movimientos del perfil y genera sugerencias según el mes seleccionado, el modo histórico y los filtros enviados. Es una operación de sólo lectura (`readOnly`) y no asegura estructuras de presupuesto ni planificación.

Para presupuesto, el período destino siempre es el mes seleccionado (`year`/`month`).

Para planificación, el período destino es:

- el mes seleccionado si `nextMonth=false`;
- el mes siguiente si `nextMonth=true`.

La pantalla muestra el período destino de planificación junto a los totales para evitar confundir presupuesto del mes elegido con planificación del mes siguiente.

## Commit

El commit es transaccional y procesa únicamente filas activas. Si una fila desactivada contiene datos incompletos, no debe bloquear el commit.

Presupuesto:

- valida primero todas las sugerencias activas;
- no crea `BudgetYear` ni `BudgetMonth` si todas las sugerencias activas son inválidas;
- valida que la categoría exista, pertenezca al perfil o sea global permitida, esté activa, sea presupuestable y no sea técnica;
- crea la estructura presupuestaria sólo cuando existe al menos una sugerencia válida;
- crea o actualiza `BudgetCategoryItem` según `overwriteExistingBudgetItems`.

Planificación:

- crea `MonthlyPlanItem` con `status=ESTIMATED`;
- usa `source=SYSTEM` si no se informa source;
- valida con las mismas reglas base de `MonthlyPlanService`;
- no crea movimientos reales.

La respuesta puede ser completa o parcial:

- sin errores: cambios aplicados;
- con cambios y errores: aplicación parcial;
- sin cambios y con errores: no se aplicaron cambios.

Warnings y errores se muestran separados.

## Inclusión

Para presupuesto se incluyen:

- movimientos reales confirmados;
- gastos de consumo real;
- ahorro o inversión presupuestable;
- deuda cuando la categoría corresponde;
- categorías activas con `budgetable=true` y `technical=false`.

Para planificación se incluyen patrones que puedan representar compromisos:

- ingresos recurrentes;
- gastos fijos o recurrentes;
- deudas, tarjetas y créditos;
- salud mensual, alquiler, expensas y suscripciones;
- Banco Provincia préstamos importados como deuda estimada;
- Mercado Crédito cuando aparece como deuda o compromiso.

## Exclusión

Se excluyen siempre:

- transferencias internas o posibles transferencias internas;
- movimientos no confirmados;
- movimientos sin categoría;
- movimientos ignorados por regla;
- movimientos técnicos;
- duplicados cross-source;
- desembolsos o recuperables de CJPrestamos;
- capital CJ ya ejecutado o recuperado;
- categorías técnicas;
- categorías inactivas;
- categorías no presupuestables para presupuesto;
- fondeos;
- gastos aislados pequeños sin patrón recurrente.

La selección explícita por `selectedTransactionIds` no permite saltarse estas reglas duras. Sí permite incluir un movimiento en `REVIEW` aunque `includeReview=false`, siempre que cumpla el resto de condiciones.

## Filtros

`includeImportedOnly=true` significa sólo importados. Tiene prioridad sobre `includeManual`; si ambos vienen en `true`, los manuales se ignoran.

`includeImportedOnly=false` y `includeManual=false` también significa sólo importados. Es la forma explícita de apagar manuales sin forzar el modo "sólo importados".

`includeImportedOnly=false` y `includeManual=true` incluye importados y manuales.

`includeReview=true` incluye movimientos en revisión, salvo que violen una regla dura.

`selectedTransactionIds` trae sólo movimientos existentes del perfil. IDs inexistentes o de otro perfil no participan.

## Outliers

El motor separa tres conceptos:

- `outlierDetected`: se detectó un gasto atípico de una sola vez frente al resto de la ventana.
- `outlierAffectsSuggestedAmount`: ese mes atípico se excluyó del cálculo del importe sugerido.
- `applyByDefault`: la fila queda activa por defecto sólo si la sugerencia es confiable y el outlier no afecta el importe.

En promedios de 3 o 6 meses, un mes excepcional anterior también se excluye para que no contamine el promedio. Un patrón alto pero recurrente no se trata como outlier.

## Duplicados de planificación

Antes de crear `MonthlyPlanItem`, el commit revisa el período destino. Un ítem existente bloquea la creación cuando:

- no está `CANCELLED`;
- tiene la misma categoría;
- el monto exacto o rango es similar dentro de la tolerancia;
- y el título es igual o comparte tokens significativos.

La source distinta no evita el dedupe si el compromiso parece el mismo. Una categoría distinta o un monto fuera de tolerancia no bloquean una creación legítima.

Si `skipDuplicates=true`, los duplicados se omiten con warning. Si es `false`, el usuario puede forzar la creación.

## Revisión y edición en UI

La pantalla permite:

- activar o desactivar cada sugerencia;
- editar importes de presupuesto;
- editar título, tipo, prioridad, fecha, monto exacto, rango, categoría y cuenta de planificación tanto en desktop como en mobile;
- ver confianza, duplicados y gastos atípicos en español;
- impedir aplicar una planificación activa sin monto exacto ni rango válido;
- decidir si se omiten duplicados y si se reemplaza presupuesto existente.

## Corrección o reversión

Si se aplicó una sugerencia incorrecta:

- presupuesto: editar o eliminar el item de la categoría en el mes correspondiente;
- planificación: editar, cancelar o eliminar el `MonthlyPlanItem`;
- movimientos reales: corregir clasificación, categoría o estado desde movimientos/importación y volver a generar preview.

Si el commit devolvió errores, revisar primero las filas indicadas. Si fue parcial, las filas válidas ya quedaron aplicadas y las inválidas pueden corregirse y volver a enviarse.
