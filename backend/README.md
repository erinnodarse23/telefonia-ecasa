# ECASA API Java

Backend Spring Boot en Java 17 + Spring Boot 3.4.

## Estructura de paquetes

- `controllers`: controladores REST y endpoints HTTP.
- `service`: reglas de negocio y transacciones.
- `repository`: acceso a PostgreSQL mediante JDBC.
- `security`: JWT y autenticacion.
- `exceptions`: excepciones y manejador global de errores.
- `config`: configuracion transversal de Spring.

## Requisitos

- Java 17
- Maven 3.9+
- PostgreSQL con el esquema existente de ECASA

## Configuracion

Copiar `.env.example` como referencia y definir las variables de entorno antes de iniciar:

```powershell
$env:DB_HOST = 'localhost'
$env:DB_PORT = '5432'
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = 'e1r2i3n4'
$env:DB_NAME = 'ecasa_telefonica'
$env:JWT_SECRET = 'secreto-largo'
```

## Ejecutar

```powershell
mvn spring-boot:run
```

La API escucha por defecto en `http://localhost:5000/api`.

- Health: `GET /api/health`
- Swagger: `http://localhost:5000/api/docs`

Durante la desarrollo, `backend` Java permanece como backend de respaldo.

## Validar

```powershell
mvn test
```