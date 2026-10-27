# La Cocha API (Java)

Backend offline-first del proyecto de grado: recibe desde la app del cuidador las lecturas de
pH y temperatura, los conteos de alevinos, la mortalidad, la alimentación y las biometrías;
genera alertas y calcula el resumen de cada lote.

**Stack:** Java 17 · Spring Boot 3.5 · Spring Data JPA · Flyway · PostgreSQL (Neon) · Docker en Render

## Estructura

```
src/main/java/com/lacocha/backend/
├── config/        Base de datos (Neon), clave X-API-Key, CORS y Swagger
├── modelo/        Entidades JPA (tablas)
├── repositorio/   Consultas a la base de datos (Spring Data)
├── dto/           Lo que entra y sale de la API (records)
├── servicio/      Lógica: sincronización, resumen del lote y reglas (sistema experto)
│                  PisciculturaFacade es la única puerta que usan los controladores
└── controlador/   Rutas REST y manejo de errores
src/main/resources/db/
├── migration/     Migraciones de Flyway comunes a los dos motores
├── motor/         Migraciones que solo entiende PostgreSQL o solo H2
└── semilla/       Datos de ejemplo (no los aplica Flyway)
```

## Patrones de diseño

`SyncService.push()` hacía cuatro trabajos en un solo método: registrar el dispositivo, fusionar
estanques, fusionar lotes y guardar los eventos con sus alertas. Los cinco patrones están puestos
en esas costuras, no de adorno. Hoy ese método tiene 21 líneas.

| Patrón | Dónde | Qué problema resuelve |
|---|---|---|
| **Facade** | `servicio/PisciculturaFacade.java` | Los cinco controladores inyectaban cinco servicios distintos. Ahora inyectan uno. Un controlador solo tiene que saber su ruta, su JSON y a qué caso de uso llamar; en qué servicio vive ese caso de uso no es asunto suyo. |
| **Adapter** | `servicio/AdaptadorJsonPush.java` | El celular y el ESP32 suben los eventos como JSON sin tipo: una trama del contador llega como un objeto cuyo campo `tipo` es la única pista de qué es. Este es el único lugar que la convierte en un record ya validado, y el que nombra el campo que falló como lo nombra el JSON y no como lo nombra Java. Los estanques y los lotes no pasan por aquí: esos llegan ya tipados en la petición. |
| **Factory Method** | `servicio/IngestoresEventos.java` | Antes un `instanceof LecturaAguaEntrada` enterrado en el push decidía quién dispara alertas. Ahora cada subclase de `Fabrica` responde por un `tipo` del protocolo y decide quién lo atiende. Aceptar una clase nueva de evento es agregar una subclase y registrarla. Es además la **única** lista de las etiquetas del protocolo: `Eventos.TIPOS` desapareció. |
| **Bridge** | `servicio/AlmacenEventos.java` + `AlmacenEventosJpa.java` | `IngestaEventos` tiene las reglas de qué se acepta, qué es duplicado y qué se rechaza, y sostiene un `AlmacenEventos` sin saber nunca cuál es. Nunca ve un `EntityManager`. Eso es lo que permite probar esas reglas sin base de datos (ver *Pruebas automáticas*). |
| **Decorator** | `servicio/DecoradoresAlmacenEventos.java` | Dos capacidades transversales que se envuelven desde fuera y se apilan. `ConteoPorOrigen` cuenta lo guardado por `origen` y lo deja en el log (`eventos de cel-cuidador-1: contador=120, manual=3`): la tesis compara el contador de alevinos contra el conteo a mano, y eso lo responde sin una consulta. `Rastro` escribe una línea por evento y **solo se envuelve si su nivel está en DEBUG**, porque un push trae hasta 500. |

Los decoradores se construyen en cada push, no una sola vez: una cuenta por `origen` que sumara dos
pushes distintos no diría nada.

## Pruebas automáticas

```powershell
.\mvnw test
```

`ApiTest` recorre la API completa contra H2 en memoria. `IngestaEventosTest` prueba las reglas del
push **sin base de datos y sin contexto de Spring**: el `AlmacenEventos` es un mapa. Eso es lo que
compra el Bridge. Cubre que un conteo se acepta con su dispositivo y su hora de llegada, que el
mismo evento dos veces vuelve como duplicado y se guarda una sola vez, que un evento huérfano se
rechaza solo y el resto de la cola pasa, que el error nombra el campo como el JSON, que sin
`origen` se guarda como `manual`, que una lectura fuera de rango levanta alerta y que un conteo
nunca la levanta. Tarda milisegundos; el mismo terreno por HTTP cuesta levantar la aplicación.

Una de esas pruebas deja escrito algo que hoy es verdad: **este backend no descarta una alerta igual
que siga sin atender**, así que cada lectura fuera de rango levanta la suya. El sensor mide cada
pocos segundos, así que vale la pena decidir si eso se queda así; si algún día se agrega el filtro,
la prueba lo dirá en voz alta en vez de que el cambio pase inadvertido.

## Correr en local

No hace falta Neon: en local usa una base H2 en la carpeta `data/`.

```powershell
.\mvnw spring-boot:run
```

- Documentación interactiva: http://localhost:8080/docs → botón **Authorize** → `cambia-esta-clave`
- Pruebas automáticas: `.\mvnw test`

También se puede abrir la carpeta en IntelliJ o VS Code y ejecutar `LaCochaApplication`
con el perfil `local` (`--spring.profiles.active=local`).

## Probar las migraciones contra PostgreSQL

Las pruebas corren en H2 y producción en PostgreSQL (Neon). Una migración puede pasar en H2 y
romperse en Neon, así que lo que toque el esquema se verifica antes contra un PostgreSQL de verdad:

```powershell
docker compose up -d
.\mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Para revisar cómo quedó el esquema:

```powershell
docker exec lacocha-postgres psql -U lacocha -d lacocha -c "\d estanques"
docker exec lacocha-postgres psql -U lacocha -d lacocha -c "select version, description, success from flyway_schema_history order by installed_rank"
```

Al terminar, `docker compose down`. Con `docker compose down -v` se borra también la base, útil
para volver a aplicar las migraciones desde cero.

Las migraciones comunes van en `db/migration/`. Lo que solo entiende un motor (por ejemplo los
índices parciales, que H2 no soporta) va en `db/motor/postgresql/` y `db/motor/h2/`, y Flyway
escoge la carpeta según el motor al que esté conectado.

## Base de datos

13 tablas, 32 índices, 30 restricciones `CHECK`, 16 llaves foráneas y 2 `UNIQUE`. Las migraciones
se aplican solas al arrancar (Flyway) y están verificadas contra PostgreSQL 16, no solo contra H2.

**Catálogo** — se puede editar; gana el `actualizado_en` más reciente.

| Tabla | Para qué |
|---|---|
| `estanques` | Tanques, estanques y jaulas. Nombre único: el cuidador los distingue por ahí |
| `lotes` | Siembras. Código único dentro de cada estanque. Si está cerrado, dice cuándo |

**Eventos** — nunca se editan y el `id` lo genera el celular, por eso el push es idempotente.

| Tabla | Para qué |
|---|---|
| `lecturas_agua` | pH, temperatura, oxígeno y el voltaje crudo de la sonda |
| `conteos` | Lo que contó la máquina de alevinos |
| `mortalidades` | Peces muertos y la causa |
| `alimentaciones` | Kilos entregados |
| `biometrias` | Peso promedio y tamaño de la muestra |

**Decisiones del sistema experto** — son datos, no código, y cada fila lleva su fuente.

| Tabla | Para qué |
|---|---|
| `parametros_rango` | Rango óptimo y crítico de cada variable. Fuera del óptimo se genera alerta |
| `tasas_alimentacion` | Qué porcentaje de la biomasa se da por día según el peso del pez |
| `factores_temperatura` | Cuánto se corrige esa tasa según la temperatura. Sobre 18 °C suspende |
| `alertas` | Las genera el servidor. Deja quién la atendió y cuándo |
| `hallazgos_ia` | Lo que encuentra el servicio de análisis. Aparte de `alertas` a propósito: ver abajo |

**Operación**

| Tabla | Para qué |
|---|---|
| `dispositivos` | Celulares y nodos. `ultimo_visto_en` distingue uno sin señal de uno perdido |
| `sincronizaciones` | Qué pasó en cada push: aceptados, duplicados, obsoletos y rechazados |

La base no depende de que los datos lleguen por la API: los mismos límites que validan los DTO
están como `CHECK` y `UNIQUE` en el esquema, así que un `INSERT` por SQL directo o desde el panel
web tampoco puede guardar un pH de 20, un estado inventado ni dos lotes con el mismo código.

### Cargar datos de ejemplo

```powershell
docker compose up -d
docker exec -i lacocha-postgres psql -U lacocha -d lacocha < src/main/resources/db/semilla/piloto.sql
```

Siembra tres estanques con dos meses de historia (1.080 lecturas, conteos, mortalidad,
alimentación, biometrías, un lote cosechado y dos alertas pendientes). Se puede volver a correr:
borra lo que sembró antes y lo recrea. **No** es una migración de Flyway a propósito, para que
nunca llegue a la base de producción.

### Si Flyway se queja de un checksum

```
Migration checksum mismatch for migration version N
```

Pasa cuando se edita una migración que ya se aplicó. En desarrollo se arregla recreando la base
(`docker compose down -v`). En producción **no se edita una migración aplicada**: se escribe una
nueva con el número siguiente.

## Quién usa la app

Hasta la V23 la API se protegía con una sola clave compartida (`X-API-Key`): todos los celulares
mandaban la misma. Servía para el piloto, pero no dice quién hizo cada cosa y no se le puede quitar
el acceso a una persona sin cambiársela a toda la finca.

La **V24** agrega dos tablas:

- **`usuarios`** — documento (es lo que la gente de la finca se sabe de memoria; no se pide correo
  porque varios cuidadores no tienen uno), nombre, el hash BCrypt de la clave y el rol
  (`cuidador` o `productor`). No se borran: sus registros siguen apuntando ahí, se desactivan.
- **`sesiones`** — una fila por cada ingreso vivo. Se guarda el **SHA-256 del token**, no el token:
  una copia de la base no sirve para entrar.

**Por qué una tabla y no un JWT.** Lo que de verdad pasa en esta finca es que se pierde un celular.
Con la tabla se cierra esa sesión y listo; un token firmado seguiría sirviendo hasta que venza y no
hay cómo revocarlo.

**Las dos credenciales conviven.** Cualquiera de las dos abre la API: el token de sesión de una
persona, o la clave compartida que siguen usando el nodo ESP32 y un celular que todavía no ha
iniciado sesión. Por eso agregar personas no dejó por fuera a los dispositivos que ya sincronizaban.

## Lo que el umbral no puede ver

El sistema experto de `parametros_rango` compara cada lectura contra un umbral y genera su alerta.
Eso funciona y no cambia. La **V26** agrega `hallazgos_ia` para los casos en que un umbral fijo, por
bien afinado que esté, no puede ayudar:

| El caso | Por qué el umbral no lo ve |
|---|---|
| El estanque de manantial corre a 11 °C y hoy marcó 15 °C | 15 °C está dentro del rango **óptimo** de la trucha (10–16). El umbral no tiene nada que decir |
| El oxígeno baja de 8.5 a 6.2 en una semana | Nunca cruzó el mínimo crítico de 5.0. El umbral avisará el día que lo cruce, que es el día en que ya es tarde |
| Agua tibia con oxígeno justo | Cada valor está en rango. Juntos no: el agua tibia carga menos oxígeno y la trucha en agua tibia consume más |
| El lote crece la mitad de lo que el agua permite | No es un problema de agua, así que ningún umbral de agua lo detecta |

**Por qué una tabla aparte y no más columnas en `alertas`.** La tabla `alertas` ya creció una vez
para esto: la V20 le quitó el `NOT NULL` a `lectura_id` y le agregó `lote_id`, justo para que el
sistema experto pudiera avisar de cosas que no son del agua. Así que la pregunta es legítima. Tres
razones, y la tercera es la que decide:

1. `ck_alertas_coherente` exige que una alerta de agua traiga su lectura y su variable, y eso es un
   invariante de verdad: **cada alerta del sistema experto sale de comparar un valor contra un
   umbral**. Un hallazgo puede salir de la forma de treinta lecturas, de la pendiente de toda una
   ventana o de dos biometrías y la temperatura entre ellas. Meterlo ahí obliga a relajar ese CHECK
   para todos, y se pierde la garantía sobre las alertas que sí la cumplían.
2. Un hallazgo trae `puntaje`, que es la medida que lo disparó, y `alertas` no tiene dónde ponerlo.
3. **La tesis compara las dos cosas.** La pregunta que tiene que poder responder es cuántas veces la
   IA vio algo que el umbral no vio, y cuántas avisó de más. Con los dos en la misma tabla esa
   consulta depende de filtrar por `disparada_por` y de que nadie agregue un valor nuevo sin
   acordarse; con dos tablas es contar filas. **Lo que se mide se separa.**

Y para el cuidador también son dos cosas distintas: un umbral cruzado es un hecho que verifica
bajando el sensor otra vez, y un hallazgo del modelo es una sospecha que vale la pena confirmar.
Presentarlos iguales le quita credibilidad al primero y se la regala al segundo.

Lo que **sí** comparten es el vocabulario de urgencia: `nivel` usa las mismas palabras que `alertas`
(`advertencia`, `critica`) más `informe`, que es el que agrega el análisis para lo que vale saber
pero no amerita caminar hasta el estanque.

**Sin servicio de análisis la tabla queda vacía y nada se rompe.** El backend trabaja igual con su
sistema experto: la IA agrega, no sostiene. Es el mismo criterio que hace que la app funcione en un
estanque sin señal.

## Cómo funciona la sincronización

Todo lo que registra el celular es un **evento** con un `id` (UUID) que genera el propio celular.
Los eventos no se editan, por eso no hay conflictos entre dispositivos.

`POST /api/sync/push` (encabezado `X-API-Key`)

```json
{
  "dispositivo_id": "cel-cuidador-1",
  "estanques": [{"id": "…", "nombre": "Tanque 1", "tipo": "tanque", "actualizado_en": "2026-10-08T14:00:00Z"}],
  "lotes":     [{"id": "…", "estanque_id": "…", "codigo": "L12", "cantidad_inicial": 5000, "actualizado_en": "…"}],
  "eventos": [
    {"tipo": "lectura_agua", "id": "…", "estanque_id": "…", "temp_c": 14.2, "ph": 7.05, "mv": 1510, "origen": "sensor", "registrado_en": "…"},
    {"tipo": "conteo",       "id": "…", "lote_id": "…", "total": 5200, "cortes_multiples": 37, "origen": "contador", "registrado_en": "…"},
    {"tipo": "mortalidad",   "id": "…", "lote_id": "…", "cantidad": 3, "origen": "voz", "registrado_en": "…"},
    {"tipo": "alimentacion", "id": "…", "lote_id": "…", "kg": 0.5, "registrado_en": "…"},
    {"tipo": "biometria",    "id": "…", "lote_id": "…", "peso_promedio_g": 2.5, "muestra": 50, "registrado_en": "…"}
  ]
}
```

Las fechas van en formato ISO con zona horaria (`Z` o `-05:00`).

Respuesta: `aceptados`, `duplicados`, `obsoletos`, `rechazados` (con el error) y `alertas_generadas`.
El celular borra de su cola todo lo que salga en cualquiera de esas listas.

- **Reintentos seguros:** si la señal se cae y el celular reenvía, lo repetido sale en `duplicados` y no se guarda dos veces.
- **Un evento malo no frena la cola:** se rechaza solo ese y los demás se guardan.
- **Estanques y lotes** sí se editan: gana el `actualizado_en` más reciente; el cambio viejo sale en `obsoletos`.
- **Máximo** 500 eventos por envío.

`GET /api/sync/pull?desde=<servidor_en anterior>` devuelve estanques y lotes cambiados desde
el último pull y las alertas pendientes. El cursor es la hora del servidor, no la del celular,
para que un celular con el reloj atrasado no se pierda cambios.

## Otros endpoints

| Método | Ruta | Para qué |
|---|---|---|
| GET/POST | `/api/estanques` | Listar / crear estanques |
| PATCH | `/api/estanques/{id}` | Editar estanque |
| GET/POST | `/api/lotes` | Listar (filtros `estanque_id`, `estado`) / crear lotes |
| PATCH | `/api/lotes/{id}` | Editar lote |
| GET | `/api/estanques/{id}/lecturas` | Historial de pH y temperatura (`desde`, `hasta`, `limite`) |
| GET | `/api/lotes/{id}/resumen` | Población, supervivencia, biomasa y ración sugerida |
| GET | `/api/alertas` | Alertas (`pendientes`, `estanque_id`) |
| POST | `/api/alertas/{id}/atender` | Marcar alerta como atendida |
| GET | `/api/dispositivos` | Celulares que sincronizan, con la última vez que lo hicieron |
| PATCH | `/api/dispositivos/{id}` | Ponerle nombre o darlo de baja (`activo`) |
| GET | `/api/sincronizaciones` | Historial de envíos (`dispositivo_id`) |
| GET | `/api/parametros` | Umbrales y curva de alimentación, con su fuente |
| GET | `/salud` | Chequeo de Render (sin clave) |

Los umbrales y la curva de alimentación **ya no están en el código**: viven en las tablas
`parametros_rango`, `tasas_alimentacion` y `factores_temperatura`, cada fila con su fuente.
Ajustarlos con el productor es un `UPDATE`, no un redespliegue, y se consultan en
`GET /api/parametros`.

Siguen siendo **valores de referencia**: la columna `fuente` dice textualmente que están sin
citar. Antes del piloto hay que ajustarlos con el productor aliado y citarlos de AUNAP/FAO y de
la tabla del fabricante del alimento, y actualizar esa columna.

## Desplegar en Render + Neon

Render no tiene Java nativo, por eso se despliega con el `Dockerfile` incluido.

1. En **Neon** crea un proyecto `lacocha` y copia la cadena de conexión (`postgresql://…`).
   Se puede pegar tal cual: el backend la convierte a JDBC.
2. Sube esta carpeta a un repositorio de GitHub.
3. En **Render** → **New** → **Blueprint** → elige el repositorio. Render lee `render.yaml`.
4. Cuando pida las variables, llena:
   - `DATABASE_URL` = la cadena de Neon
   - `API_KEY` = una clave larga (la usará la app)
   - `ALLOWED_ORIGINS` = la URL del panel web, o `http://localhost:5173` por ahora
5. **Apply**. La primera compilación tarda unos minutos. Al terminar abre
   `https://<tu-servicio>.onrender.com/salud` → debe decir `{"estado":"ok"}`.

Las tablas se crean solas al arrancar (Flyway).

Nota: en el plan gratis Render apaga el servicio tras 15 min sin uso y el primer envío tarda
~1 minuto en despertarlo. La app debe tener un tiempo de espera largo en el primer intento.
