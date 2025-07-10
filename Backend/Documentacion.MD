## indice de Flujos1. Autenticacion y Gestion de Familias
2. Gestion de Inventario
3. Registro de Gastos y Gastos Recurrentes
4. Reportes y Forecast
5. Configuracion de Notificaciones
6. Cambio de Contexto (Casa / Familia)---## 1. Autenticacion y Gestion de Familias```mermaid
flowchart TD
  A[Inicio] --> B[Registro de Usuario]
  B --> C{Token JWT generado}
  C --> D[Ir a Dashboard]
  D --> E[Creacion de Casa]
  E --> F[Creacion de Familia en Casa]
  F --> G[Invitar Miembros]
  G --> H[Asignacion de Roles]
  H --> I[Fin]
  D --> J[Login existente]
  J --> D
```**Pantallas UX/UI**:1. **Registro/Login** (email, contraseña)
2. **Dashboard Inicial** (seleccion de Casa/Familia o creacion)
3. **Crear Casa** (nombre, direccion)
4. **Administrar Familias** (lista, crear, describir)
5. **Gestionar Miembros** (invitar por email, asignar rol)---## 2. Gestion de Inventario```mermaid
flowchart LR
  A[Dashboard Familia] --> B[Ver Inventario]
  B --> C[Boton “Agregar item”]
  C --> D{Metodo de ingreso}
  D -->|Manual| E[Formulario Alta Manual]
  D -->|Escaneo QR/Barcode| F[Escaner + Autollenado]
  E --> G[Guardar item]
  F --> G
  G --> B
  B --> H[Ver Detalle item]
  H --> I[Editar o Eliminar]
  H --> J{Stock < Threshold?}
  J -->|Si| K[Notificar Alerta]
  J -->|No| B
```**Pantallas UX/UI**:1. **Inventario** (lista con busqueda, filtros por unidad y stock)
2. **Alta de item** (manual y escaneo)
3. **Detalle de item** (historial de movimientos, threshold)
4. **Edicion de item**
5. **Notificacion de Stock Bajo** (modal o push)---## 3. Registro de Gastos y Gastos Recurrentes```mermaid
flowchart TD
  A[Dashboard Familia] --> B[Ver Gastos]
  B --> C[Boton “Agregar Gasto”]
  C --> D[Formulario Gasto]
  D --> E[Asignar Categoria/Subcategoria]
  E --> F[Guardar Gasto]
  F --> B
  B --> G[Marcar como Recurrente]
  G --> H[Configurar Frecuencia]
  H --> I[Guardar Configuracion]
  I --> B
```**Pantallas UX/UI**:1. **Listado de Gastos** (filtros por categoria, fecha)
2. **Formulario Gasto** (cantidad, fecha, categoria, descripcion)
3. **Detalle de Gasto** (vista, editar, eliminar)
4. **Configurar Gasto Recurrente** (frecuencia, next\_due\_date)
5. **Calendario de Vencimientos**---## 4. Reportes y Forecast```mermaid
flowchart LR
  A[Dashboard Familia] --> B[Seccion Reportes]
  B --> C[Resumen Periodo]
  C --> D[Seleccionar Fechas]
  D --> E[Mostrar Graficos (Ingresos/Egresos)]
  B --> F[Forecast]
  F --> G[Elegir Horizonte]
  G --> H[Generar Proyeccion]
  H --> I[Ver Resultados]
```**Pantallas UX/UI**:1. **Resumen de Periodo** (graficos de barras y lineas)
2. **Forecast** (selector de horizonte, grafico)
3. **Exportar Informe** (PDF/Excel)---## 5. Configuracion de Notificaciones```mermaid
flowchart TD
  A[Dashboard Usuario] --> B[Perfil & Configuracion]
  B --> C[Notificaciones]
  C --> D[Listado de Eventos]
  D --> E[Seleccionar Canales y Frecuencia]
  E --> F[Guardar Preferencias]
  F --> G[Fin]
```**Pantallas UX/UI**:1. **Perfil de Usuario**
2. **Configuracion de Notificaciones** (checkbox por evento y canal)---## 6. Cambio de Contexto (Casa / Familia)```mermaid
flowchart TD
  A[Dashboard Principal] --> B[Selector Casa]
  B --> C[Selector Familia]
  C --> D[Cambiar Contexto]
  D --> E[Dashboard Familia]
```**Pantallas UX/UI**:1. **Selector de Casa/Familia** (dropdown o modal)
2. **Indicador de Contexto Actual** (barra superior)
