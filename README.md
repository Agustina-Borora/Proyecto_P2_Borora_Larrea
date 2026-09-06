# Laboratorio LB

Sistema de escritorio para la gestión de un laboratorio de análisis clínicos: registro de pacientes, generación de órdenes de análisis, carga de resultados y seguimiento del estado de cada pedido, desde el ingreso del paciente hasta la entrega del informe.

Desarrollado en Java (Swing) con persistencia en MySQL, para la materia Práctica II.

## Índice

- [Funcionalidades](#funcionalidades)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Base de datos](#base-de-datos)
- [Requisitos previos](#requisitos-previos)
- [Instalación y configuración](#instalación-y-configuración)
- [Recuperar contraseña por email](#recuperar-contraseña-por-email)
- [Cómo compilar y ejecutar](#cómo-compilar-y-ejecutar)
- [Autores](#autores)

## Funcionalidades

La aplicación se organiza en un menú lateral con las siguientes secciones:

**Principal**
- **Escritorio** — panel de inicio con estadísticas del mes (análisis registrados, emitidos, en proceso y pendientes) y las últimas órdenes cargadas.
- **Pacientes** — alta, búsqueda y edición de pacientes.
- **Registros** — listado completo de órdenes generadas, con filtros por rango de fecha, cobertura y estado.

**Análisis**
- **Nuevo Análisis** — carga de una orden nueva: datos del paciente (nuevo o existente), médico derivante, cobertura y selección de las prestaciones/análisis solicitados desde el nomenclador.
- **Registrar Resultados** — selección de una orden pendiente y carga de los valores obtenidos para cada parámetro del análisis, con sus valores de referencia.
- **Catálogo de Exámenes** — consulta de los análisis disponibles.
- **Cotización** — cálculo de costos de análisis.

**Administración**
- **Pagos / Obra Social**, **Usuarios**, **Estadísticas** y **Configuración** — pantallas de gestión administrativa.

**Cuenta**
- **Recuperar contraseña** — desde el link "¿Olvidaste tu contraseña?" del Login: se manda un código de 6 dígitos al email registrado del usuario, se verifica y se permite elegir una contraseña nueva. Ver [Recuperar contraseña por email](#recuperar-contraseña-por-email).

## Tecnologías

| Componente | Detalle |
|---|---|
| Lenguaje | Java 8 |
| Interfaz gráfica | Java Swing, generada con el editor de formularios de NetBeans |
| Base de datos | MySQL (driver `mysql-connector-j`) |
| Build | Apache Ant (proyecto NetBeans) |
| Look & feel | [FlatLaf](https://www.formdev.com/flatlaf/) |
| Componentes adicionales | `AbsoluteLayout` (org.netbeans.lib.awtextra), `JCalendar`/`JDateChooser` (com.toedter.calendar) |
| Envío de email | JavaMail (`javax.mail`), para el código de "recuperar contraseña" |
| Hash de contraseñas | PBKDF2/HMAC-SHA256 (`javax.crypto`, incluido en el JDK — sin librería externa) |

## Arquitectura

El proyecto sigue el patrón **MVC (Modelo-Vista-Controlador)**, con una capa extra de acceso a datos (DAO) entre el Controlador y la base:

```
Vista (Swing)  →  Controlador  →  DAO  →  Base de datos (MySQL)
                        ↓
                     Modelo
```

- **`modelo/`** — clases POJO que representan las entidades del dominio (`Paciente`, `Sexo`, `Prestacion`, `OrdenResumen`, `PedidoCreado`, `Parametro`, `EstadisticasEscritorio`). No contienen lógica de base de datos ni de UI.
- **`dao/`** — una clase DAO por entidad/tabla (`PacienteDAO`, `PedidoDAO`, `RegistroDAO`, `SexoDAO`, `MedicoDAO`, `NomencladorDAO`, `AnalisisTipoDAO`, `AnalitoDAO`, `PedidoAnalitoResultadoDAO`, `EscritorioDAO`, `PasswordResetDAO`), responsables únicamente de las consultas SQL contra su tabla correspondiente. Reciben la `Connection` ya abierta; no gestionan conexiones.
- **`controlador/`** — intermediario entre la Vista y los DAO (`PacienteController`, `RegistroController`, `NuevoAnalisisController`, `ResultadosController`, `EscritorioController`, `LoginController`, `SexoController`, `NomencladorController`, `PasswordController`). Abren y cierran la conexión a través de `ConexionUtil`, manejan los errores de SQL con un cartel al usuario, y devuelven a la Vista datos ya listos para mostrar (nunca un `ResultSet` ni una `Connection`). `PasswordHasher` (también acá) hashea contraseñas y códigos de verificación.
- **Vista** (`formulariosPrincipales/`, `registros/`, `pacientes/`, `escritorio/`, `nuevoAnalisis/`, `registrarResultados/`) — pantallas Swing. No abren conexiones ni arman SQL: le piden los datos al Controlador correspondiente.
- **`conexiones/`** — `Conexion` (apertura de la conexión JDBC), `Sesion` (datos del usuario logueado, en memoria durante toda la ejecución), `Usuario` (autenticación, con contraseñas hasheadas) y `EmailService` (envío de mails por SMTP).
- **`vistas/`** — pantallas de cuenta de usuario (`CambiarContraseña`, `TokenDeConfirmacion`) y otras en desarrollo (`EscritorioDetalledeOrden`, `FormCatalogoExamenes`/`TablaCatalogoExamenes`). `RecuperarContrasenaFrame` coordina el flujo completo de recuperar contraseña sin tener su propio `.form` (reutiliza los paneles ya diseñados).
- **`panels/`** y **`interfaz/`** — componentes de UI reutilizables entre pantallas: menú lateral (`Menu`), panel contenedor con el estilo general (`PanelBorder`), buscador (`Busqueda`), estilo de tablas FlatLaf, campos de texto con validación de formato (`FiltroSoloDigitos`, `FiltroSoloLetras`, etc.), botones y campos redondeados.
- **`menu/`** — lógica del menú lateral de navegación (lista de opciones, ítem seleccionado, evento de selección).

Las clases de `modelo/`, `dao/` y `controlador/` tienen comentarios Javadoc explicando su responsabilidad; es el punto de partida recomendado para entender el código.

## Estructura del proyecto

```
Laboratorio_LB/
├── src/
│   ├── acciones/               Acciones/eventos varios de UI
│   ├── conexiones/             Conexión a la BD y sesión de usuario
│   ├── controlador/            Controladores (capa MVC)
│   ├── dao/                    Acceso a datos (una clase por tabla)
│   ├── escritorio/              Vista: tabla del Escritorio
│   ├── formulariosPrincipales/ Vista: pantallas principales (Login, Principal, Pacientes, Registros, etc.)
│   ├── interfaz/               Componentes Swing reutilizables (botones/campos redondeados)
│   ├── menu/                   Menú lateral de navegación
│   ├── modelo/                 Entidades del dominio (POJOs)
│   ├── nuevoAnalisis/           Vista: sub-paneles de "Nuevo Análisis"
│   ├── pacientes/               Vista: tabla de Pacientes
│   ├── panels/                  Componentes de UI reutilizables (menú, buscador, estilos)
│   ├── registrarResultados/     Vista: carga de resultados
│   ├── registros/               Vista: listado y filtros de órdenes
│   └── vistas/                  Vista: cuenta de usuario (recuperar contraseña) y pantallas en desarrollo
├── sql/                         Scripts de base de datos (ver más abajo)
├── build.xml                    Build de Ant (generado por NetBeans)
├── email.properties.example     Plantilla de configuración SMTP (ver más abajo)
└── nbproject/                   Configuración del proyecto NetBeans
```

## Base de datos

Motor: **MySQL**, base de datos `laboratorio`. Tablas principales utilizadas por la aplicación:

`pacientes`, `usuarios`, `roles`, `permisos_pantalla`, `medicos`, `obras_sociales`, `planes_obra_social`, `sexos`, `pedidos`, `pedido_analisis`, `pedido_analito_resultado`, `nomenclador`, `categorias_analisis`, `analisis_tipos`, `analitos`, `valores_referencia`, `valor_resultado`, `password_reset_tokens`.

La carpeta `sql/` incluye los scripts para poblar/actualizar el catálogo de análisis y resultados:

| Script | Contenido |
|---|---|
| `import_nomenclador.sql` | Carga del nomenclador de prestaciones |
| `resultados_analisis_schema.sql` | Estructura de tablas de resultados de análisis |
| `resultados_analisis_seed_hemograma.sql` | Datos de referencia de hemograma |
| `resultados_analisis_seed_quimica_clinica.sql` | Datos de referencia de química clínica |
| `migracion_01` a `migracion_05` | Migraciones incrementales aplicadas sobre la base |
| `migracion_06_password_reset.sql` | Amplía `usuarios.password_usuario` para guardar el hash, y crea `password_reset_tokens` (recuperar contraseña) |

> Estos scripts asumen que la base `laboratorio` y sus tablas base (`pacientes`, `usuarios`, `pedidos`, etc.) ya existen. Si estás levantando el proyecto desde cero y no tenés un dump completo de la base, pedile a quien la tenga un export (`mysqldump laboratorio > laboratorio.sql`) antes de aplicar estos scripts.

## Requisitos previos

- JDK 8 (o superior, configurado como Java 8 en el proyecto)
- MySQL Server (local, puerto por defecto 3306) o acceso a una instancia remota
- NetBeans (recomendado, el proyecto está armado para su editor de formularios) o Ant desde línea de comandos
- Los `.jar` externos referenciados por el proyecto (no incluidos en el repositorio): `mysql-connector-j`, FlatLaf, `JCalendar` (JDateChooser), `AbsoluteLayout`, JavaMail (`javax.mail`, solo necesario para "recuperar contraseña")

## Instalación y configuración

1. Cloná el repositorio y abrilo como proyecto existente en NetBeans (`File > Open Project`).
2. Agregá las librerías externas necesarias a la biblioteca del proyecto (Properties > Libraries) si no vienen ya configuradas: MySQL Connector/J, FlatLaf, JCalendar, AbsoluteLayout.
3. Creá la base `laboratorio` en tu MySQL local y cargá el esquema base (tablas de pacientes, usuarios, pedidos, etc.) más los scripts de `sql/`.
4. Revisá los datos de conexión en `src/conexiones/Conexion.java`:

   ```java
   private static final String URL = "jdbc:mysql://localhost:3306/laboratorio";
   private static final String USER = "root";
   private static final String PASSWORD = "";
   ```

   Ajustalos si tu usuario/contraseña de MySQL son distintos.

## Recuperar contraseña por email

El link "¿Olvidaste tu contraseña?" del Login abre un flujo de 3 pasos: pide el email de la cuenta, manda un código de 6 dígitos a ese email (`vistas.TokenDeConfirmacion`) y, si el código es correcto, deja elegir una contraseña nueva (`vistas.CambiarContraseña`). Todo lo coordina `vistas.RecuperarContrasenaFrame`, con la lógica en `controlador.PasswordController`.

Las contraseñas (y los códigos de verificación) se guardan hasheadas con PBKDF2/HMAC-SHA256 (`controlador.PasswordHasher`) — nunca en texto plano. Las contraseñas viejas, guardadas en texto plano antes de este cambio, se re-hashean solas la primera vez que ese usuario inicia sesión correctamente (no hace falta migrarlas a mano).

Para que el envío de email funcione hace falta:

1. **Agregar la librería JavaMail al proyecto.** En NetBeans: clic derecho sobre el proyecto → Properties → Libraries → Compile → Add Library/Add JAR, y agregar `javax.mail` (por ejemplo `com.sun.mail:javax.mail:1.6.2`, compatible con Java 8 — se puede descargar desde [Maven Central](https://mvnrepository.com/artifact/com.sun.mail/javax.mail/1.6.2)).
2. **Correr el script de migración** `sql/migracion_06_password_reset.sql` sobre la base `laboratorio`.
3. **Configurar una cuenta de correo para enviar los códigos.** Copiá `email.properties.example` como `email.properties` (en la raíz del proyecto, al lado de `build.xml`) y completá tus datos. Con Gmail:
   1. Entrá a [myaccount.google.com/security](https://myaccount.google.com/security) con la cuenta que va a mandar los correos.
   2. Activá la "Verificación en dos pasos" si no la tenías.
   3. Generá una "Contraseña de aplicación" (App Password) para "Correo" — un código de 16 caracteres, distinto de tu contraseña real de Gmail.
   4. Usá ese código como `smtp.password` en `email.properties`.

`email.properties` está en `.gitignore` a propósito: cada quien pone sus propias credenciales en su copia local, y nunca se sube al repositorio. `email.properties.example` sí se versiona, como plantilla.

## Cómo compilar y ejecutar

Desde NetBeans: abrir el proyecto y usar **Run > Run Project** (o F6).

Desde línea de comandos, con Ant:

```bash
ant clean jar   # compila y genera el .jar
ant run         # compila y ejecuta
```

La clase de entrada es `formulariosPrincipales.Login`.

## Autores

- Agus (Agustina Borora)
- Larrea

Trabajo práctico para la materia Práctica II.
