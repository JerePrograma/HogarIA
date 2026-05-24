# Importación Banco Provincia - Mis Préstamos

## Alcance
Permite importar un Excel de “Mis Préstamos” para previsualizar deudas y crear compromisos mensuales estimados en planificación.

## Formato esperado
Archivo XLS/XLSX con columnas: Tipo, Identificación, Número de cuenta, Deuda a la fecha, Importe original y Vencimiento (dd/MM/yyyy).

## Qué se crea
En commit se crean `MonthlyPlanItem` de tipo `DEBT`, estado `ESTIMATED`, prioridad `ESSENTIAL`, source `IMPORT` y moneda `ARS`.

## Qué NO se crea
No se crean `MoneyTransaction` reales ni se impacta gasto ejecutado del dashboard.

## Limitación de cuota estimada
La cuota se estima linealmente: `deudaActual / mesesRestantes`. El banco no informa sistema de amortización, tasa ni próxima cuota real.

## Validación manual
1. Subir archivo y ejecutar preview.
2. Verificar warnings y duplicados.
3. Confirmar commit.
4. Revisar en planificación mensual que existan los ítems y no existan movimientos reales nuevos.
