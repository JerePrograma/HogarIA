# Diseño de sincronización contable cjprestamos ↔ HogarIA

## 1) Objetivo de sincronización

Evolucionar desde el bridge read-only actual (`GET /api/profiles/{profileId}/external-loans/summary`) hacia una sincronización contable **controlada, idempotente y auditable** entre cjprestamos y HogarIA.

El objetivo no es “copiar datos”, sino traducir eventos de préstamos externos a impactos contables correctos dentro de HogarIA, respetando:

- ownership por `profileId` + `userId`,
- consistencia contable,
- trazabilidad de origen externo,
- capacidad de reintento sin duplicados,
- operación inicial manual con camino claro a automatización.

---

## 2) Por qué un pago de préstamo **NO** debe registrarse como ingreso simple

Un pago de cuota mezcla componentes económicos distintos. Tratarlo como `INCOME` único distorsiona KPIs y decisiones:

- **Capital recuperado**: no es ganancia nueva; es recuperación de un activo previamente entregado.
- **Interés cobrado**: sí es ingreso económico real.
- **(Opcional) punitorios/comisiones**: pueden tener tratamiento contable separado.

Si se registra todo como ingreso simple:

- se sobreestima la “rentabilidad” mensual,
- se falsea el flujo operativo real,
- se pierde capacidad de medir rendimiento de cartera (yield),
- se rompe comparabilidad entre períodos.

---

## 3) Por qué un préstamo otorgado **NO** debe registrarse como gasto común

Otorgar un préstamo no equivale a consumir dinero definitivamente (como supermercado, servicios o transporte). Es una **conversión de caja en derecho de cobro**.

Modelarlo como gasto común genera errores:

- reduce artificialmente el ahorro y resultados del período,
- impide distinguir inversión crediticia de consumo,
- dificulta analizar mora, recupero y retorno,
- contamina presupuestos de gasto familiar.

Conclusión: el desembolso debe representar salida de caja + aumento de activo de préstamo, no “gasto económico” ordinario.

---

## 4) Modelo recomendado

### 4.1 Componentes contables

1. **Capital prestado**
   - Representa principal entregado (activo por cobrar).
   - Impacto: baja caja disponible, sube activo de cartera.

2. **Capital recuperado**
   - Representa devolución del principal.
   - Impacto: sube caja disponible, baja activo de cartera.
   - **No** impacta como ingreso económico.

3. **Interés cobrado**
   - Representa ganancia real por financiamiento.
   - Impacto: sube caja y sube ingreso económico.

4. **Caja disponible**
   - Refleja movimientos monetarios reales en cuentas (bank/cash/etc.).
   - Debe actualizarse solo cuando existe evento confirmable (desembolso/pago).

### 4.2 Principio de diseño

Cada evento externo relevante (alta de préstamo, pago) debe descomponerse en asientos/movimientos con semántica explícita, evitando agregaciones ambiguas de “monto total”.

---

## 5) Qué se debe crear en HogarIA

> Sin implementar todavía. Este apartado define los artefactos requeridos.

### 5.1 Configuración por `profileId`

Crear configuración de integración por perfil para que cada tenant decida cómo mapear cjprestamos a su contabilidad.

Campos sugeridos:

- `profileId`
- `enabled`
- `manualSyncEnabled`
- `autoSyncEnabled` (futuro)
- `lastSyncAt`
- `lastSyncStatus`
- `lastSyncCursor`/`lastExternalEventAt` (si aplica)

### 5.2 Cuenta destino/origen

Definir cuentas obligatorias para materializar movimientos:

- cuenta de **desembolso** (préstamo otorgado),
- cuenta de **cobro** (pago recibido),
- opcionalmente cuenta técnica de conciliación.

Regla: sin cuenta configurada, no se crea `MoneyTransaction`.

### 5.3 Categorías necesarias

Como mínimo, categorías separadas para:

- `LOAN_CAPITAL_OUT` (capital prestado),
- `LOAN_CAPITAL_RECOVERY` (capital recuperado),
- `LOAN_INTEREST_INCOME` (interés cobrado).

Recomendación: categorías explícitas por perfil (o globales controladas), evitando mezclar con gasto/ingreso genérico.

### 5.4 Tabla de idempotencia

Crear tabla dedicada para garantizar que un evento externo no se procese dos veces.

Campos sugeridos:

- `id`
- `profileId`
- `sourceSystem` (`CJPRESTAMOS`)
- `eventType` (`LOAN_CREATED`, `PAYMENT_RECORDED`, etc.)
- `externalEventId` (o hash determinístico)
- `processedAt`
- `resultStatus`
- `moneyTransactionId` / referencias creadas
- índice único por (`profileId`, `sourceSystem`, `eventType`, `externalEventId`)

### 5.5 Mapping `externalLoanId` / `externalPaymentId`

Crear mapeo persistente entre IDs externos y entidades/artefactos internos.

Mínimo requerido:

- `externalLoanId` ↔ `internalLoanRef` (o entidad puente),
- `externalPaymentId` ↔ movimiento(s) interno(s),
- estado de sincronización por item,
- metadatos de timestamp y versión.

Esto evita reconciliaciones heurísticas por texto/fecha/monto.

---

## 6) Flujo propuesto

### 6.1 Préstamo creado en cjprestamos

1. cjprestamos registra nuevo préstamo.
2. HogarIA detecta/consulta evento en próxima sincronización.
3. Se valida configuración del `profileId` (cuentas + categorías + permisos).
4. Se registra mapping del `externalLoanId`.
5. Se crea impacto contable inicial de capital prestado (si aplica política de contabilización al otorgamiento).

### 6.2 Cuotas generadas

1. Se sincroniza cronograma de cuotas externas.
2. HogarIA proyecta cuotas futuras como planificación (p. ej. `MonthlyPlanItem` tipo `RECOVERY`, según diseño ya acordado).
3. Se actualiza estado de cuota sincronizada sin crear ingresos reales aún.

### 6.3 Pago registrado en cjprestamos

1. Se detecta `externalPaymentId`.
2. Se descompone pago en componentes: capital vs interés (y otros si existieran).
3. Se aplica idempotencia antes de generar movimientos.
4. Se crean movimientos contables según reglas:
   - capital recuperado: movimiento de caja + categoría de recupero de capital (no ingreso económico),
   - interés cobrado: ingreso real.
5. Se marca evento como procesado con referencias internas.

### 6.4 Sincronización manual desde HogarIA (fase inicial)

1. Usuario dispara acción “Sincronizar movimientos”.
2. Backend corre proceso por `profileId`.
3. Se devuelve resumen:
   - eventos leídos,
   - movimientos creados,
   - omitidos por idempotencia,
   - errores accionables.
4. UI muestra resultado y timestamp de última ejecución.

### 6.5 Posterior automatización

Evolución gradual:

- job programado por perfil,
- reintentos con backoff,
- eventual consumo por webhook/event stream,
- observabilidad (métricas, trazas, alertas).

---

## 7) Reglas de negocio y seguridad

1. **No duplicar movimientos**: todo evento externo pasa por control idempotente estricto.
2. **No crear `MoneyTransaction` sin cuenta/categoría configurada**: fallar con error accionable.
3. **No registrar capital recuperado como ingreso económico**.
4. **Registrar interés cobrado como ingreso real**.
5. Validar ownership en cada operación sensible por `profileId` + `userId`.
6. Mantener trazabilidad completa (evento externo → movimiento interno).

---

## 8) UX propuesta

### 8.1 Botón “Sincronizar movimientos”

Ubicación sugerida: módulo external-loans existente.

Comportamiento:

- acción manual explícita,
- feedback de progreso,
- bloqueo de doble ejecución concurrente por perfil.

### 8.2 Estado de última sincronización

Mostrar:

- fecha/hora última ejecución,
- estado (`OK`, `PARTIAL`, `FAILED`),
- cantidad de eventos procesados,
- cantidad de errores.

### 8.3 Tabla de movimientos sincronizados

Columnas sugeridas:

- fecha evento externo,
- tipo (`PRINCIPAL_OUT`, `PRINCIPAL_RECOVERY`, `INTEREST_INCOME`),
- `externalLoanId`, `externalPaymentId`,
- monto,
- cuenta destino/origen,
- categoría,
- estado (`CREATED`, `SKIPPED_IDEMPOTENT`, `ERROR`).

### 8.4 Errores accionables

Errores deben ser comprensibles y resolubles por usuario funcional:

- “Falta configurar cuenta de cobro para perfil X”.
- “Falta categoría LOAN_INTEREST_INCOME”.
- “Evento externo duplicado omitido”.
- “Pago sin desglose capital/interés en origen”.

Cada error debería incluir recomendación concreta de corrección.

---

## 9) Riesgos y decisiones pendientes

## 9.1 Riesgos

1. **Riesgo de clasificación incorrecta** si cjprestamos no entrega desglose confiable de capital/interés.
2. **Riesgo de duplicación** ante reintentos sin idempotencia fuerte.
3. **Riesgo de UX confusa** si no se explican bien “omitido” vs “error”.
4. **Riesgo de desalineación temporal** entre fecha de pago externo y fecha contable interna.
5. **Riesgo de seguridad** si no se aplica consistentemente validación `profileId` + `userId`.

## 9.2 Decisiones pendientes (antes de implementar)

1. Política exacta de contabilización del otorgamiento (inmediata vs diferida).
2. Contrato canónico de eventos con cjprestamos (campos, versionado, timezone, consistencia).
3. Estrategia de reconciliación cuando cambian datos históricos en origen.
4. Definición de “fuente de verdad” para estados de cuota.
5. Diseño final de entidades internas (`loans` bounded context + puentes contables).
6. Política de retry, timeout y manejo de fallas parciales.
7. Reglas de cierre de período: cómo tratar sincronizaciones tardías.

---

## Cierre

Este diseño establece una transición segura desde lectura externa hacia sincronización contable robusta, preservando semántica financiera y preparando el camino para automatización progresiva. No incluye implementación ni migraciones; define lineamientos y decisiones arquitectónicas previas necesarias.
