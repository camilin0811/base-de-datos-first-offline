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
└── controlador/   Rutas REST y manejo de errores
src/main/resources/db/migration/   Tablas (Flyway)
```

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
| GET | `/salud` | Chequeo de Render (sin clave) |

Los umbrales y la tabla de alimentación están en `servicio/Reglas.java`. Son **valores de referencia**:
hay que ajustarlos con el productor y citarlos de AUNAP/FAO y del fabricante del alimento.

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
