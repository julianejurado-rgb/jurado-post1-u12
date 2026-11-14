# Post-contenido Unidad 12: despliegue y CI/CD

![CI](https://github.com/julianejurado-rgb/jurado-post1-u12/actions/workflows/ci.yml/badge.svg)

## Descripción
Repositorio del laboratorio de la Unidad 12 de Programación Web (séptimo semestre). Contiene una API REST de catálogo de productos con Spring Boot, JPA y pruebas automatizadas. La Parte 1 la dockeriza con un Dockerfile multi-stage y perfiles dev y prod. La Parte 2 agrega Spring Boot Actuator y un pipeline de integración continua en GitHub Actions que compila, ejecuta las pruebas, construye la imagen Docker y comprueba que el contenedor arranca y responde saludable.

## Parte 1: dockerización con perfiles y variables de entorno
El Dockerfile multi-stage compila el proyecto con Maven en una etapa con JDK y ejecuta el JAR en una etapa con JRE Alpine, con un usuario sin privilegios de root. La configuración sensible se externaliza en `application.properties` con placeholders `${VARIABLE:valor-por-defecto}`. Sin variables de entorno, la aplicación arranca con el perfil dev y una base H2 en memoria. Con `SPRING_PROFILES_ACTIVE=prod` y las variables `DATASOURCE_URL`, `DATASOURCE_USER` y `DATASOURCE_PASSWORD`, arranca con la configuración de producción sobre PostgreSQL.

| Variable | Perfil | Para qué sirve | Valor por defecto |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ambos | Elige el perfil | `dev` |
| `PORT` | ambos | Puerto del servidor | `8080` |
| `DATASOURCE_URL` | ambos | URL JDBC de la base de datos | H2 en memoria (dev) o `jdbc:postgresql://localhost:5432/appdb` (prod) |
| `DATASOURCE_USER` | ambos | Usuario de la base de datos | `sa` (dev) o `appuser` (prod) |
| `DATASOURCE_PASSWORD` | ambos | Contraseña de la base de datos | vacía |
| `DDL_AUTO` | prod | Estrategia de Hibernate para el esquema | `update` |

## Parte 2: pipeline CI/CD y monitoreo con Actuator
Spring Boot Actuator expone `/actuator/health`. La exposición cambia según el perfil: en dev se publican todos los endpoints, y en prod solo `health` e `info`, para no mostrar información sensible. Con el perfil prod, `/actuator/beans` responde 404 y `/actuator/health` devuelve solo el estado, sin detalles.

El workflow `.github/workflows/ci.yml` se ejecuta en cada push y pull request hacia `main`, y tiene dos jobs encadenados con `needs`:

1. `build-and-test` compila el proyecto con JDK 17, con la caché de Maven de `actions/setup-java`, y ejecuta `mvn clean verify`: las pruebas y la verificación de cobertura. Publica el reporte de Surefire como artefacto.
2. `docker-build` solo corre si el job anterior pasó. Construye la imagen con el mismo Dockerfile de la Parte 1, la levanta dentro del runner y consulta `/actuator/health` hasta 15 veces, con 2 segundos entre intentos. Si no responde `UP`, muestra los logs del contenedor y falla. Al terminar siempre elimina el contenedor de prueba.

La imagen no se publica en ningún registro, y el pipeline no necesita secretos de ninguna cuenta externa.

## Despliegue en Railway (paso final manual)
Railway construye la imagen a partir del mismo Dockerfile, sin que el workflow intervenga.

1. Entrar a railway.app con la cuenta de GitHub, crear un proyecto y elegir "Deploy from GitHub repo" con este repositorio.
2. Agregar un servicio PostgreSQL al mismo proyecto.
3. En las variables del servicio de la aplicación definir `SPRING_PROFILES_ACTIVE=prod` y los datos de conexión de PostgreSQL: `DATASOURCE_URL` (en formato JDBC, `jdbc:postgresql://<host>:<puerto>/<base>`), `DATASOURCE_USER` y `DATASOURCE_PASSWORD`. Railway define `PORT` por su cuenta.
4. Generar el dominio público y comprobar que `GET https://<dominio>/actuator/health` devuelve `{"status":"UP"}`.

## Decisiones de diseño
Parte 1
1. Placeholders con valor por defecto en vez de placeholders estrictos. Un placeholder estricto obliga a definir todas las variables antes de arrancar, incluso en local y en las pruebas del pipeline. Con un valor por defecto razonable, el mismo artefacto arranca sin configuración en desarrollo y en producción se sobrescribe con valores reales.
2. `application-dev.properties` y `application-prod.properties` como archivos separados, en lugar de un único archivo con bloques condicionales. El nombre del archivo deja claro qué configuración corresponde a cada ambiente y reduce el riesgo de editar por error una propiedad de producción.
3. Imagen final `eclipse-temurin` JRE Alpine en lugar de distroless. Reduce el tamaño frente a un JDK completo y conserva un shell mínimo para depurar con `docker exec`.
4. `ddl-auto=update` en producción en lugar de `validate`. Con `validate`, una base de datos recién creada, sin tablas, hace fallar el arranque. `update` crea el esquema en el primer despliegue. Un proyecto real usaría una herramienta de migraciones, que queda fuera del alcance del laboratorio.

Parte 2
5. Prueba mínima de contexto además de las pruebas existentes. `ApplicationContextTest` arranca el contexto completo con el perfil dev, y así el pipeline valida que la configuración nueva de perfiles y Actuator carga sin errores antes de construir la imagen.
6. El job `docker-build` construye y verifica la imagen, pero no la publica. Publicar exigiría que cada estudiante configure secretos de una cuenta externa solo para aprobar el laboratorio. El smoke test comprueba lo esencial: que la imagen arranca y expone el endpoint de monitoreo.
7. Exposición de Actuator diferenciada por perfil: todos los endpoints en dev y solo `health` e `info` en prod, para no revelar información sensible en producción.

## Ajustes sobre el enunciado
- El Dockerfile compila con la imagen `maven:3.9-eclipse-temurin-17-alpine`. La imagen `eclipse-temurin` sola no incluye Maven, así que el comando `mvn` del enunciado fallaría.
- Las imágenes y el pipeline usan Java 17, la versión que declara el proyecto, en lugar de Java 21.
- Se agregó el driver de PostgreSQL al `pom.xml`, necesario para el perfil prod.
- El proyecto no usa JWT, así que se omite la propiedad del secreto.
- El workflow declara `permissions: contents: read`, porque solo necesita leer el código.
- La propiedad `management.*` de cada perfil se agregó junto con la dependencia de Actuator, en la Parte 2, y no en la Parte 1. Antes de agregar la dependencia no tenían efecto.

## Cómo compilar y ejecutar
Requisitos: Docker.

1. Clonar el repositorio: `git clone https://github.com/julianejurado-rgb/jurado-post1-u12.git`
2. Construir la imagen: `docker build -t mi-spring-app:local .`
3. Ejecutar con los valores por defecto (perfil dev): `docker run -p 8080:8080 mi-spring-app:local`
4. Probar la API: `curl "http://localhost:8080/api/productos?categoria=ELECTRONICA"`
5. Comprobar el estado: `curl http://localhost:8080/actuator/health`
6. Ejecutar con el perfil prod y una base PostgreSQL:
   `docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod -e DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/appdb -e DATASOURCE_USER=appuser -e DATASOURCE_PASSWORD=apppass mi-spring-app:local`
7. Los logs indican el perfil activo con la línea `The following 1 profile is active`.
8. Para ejecutar las pruebas sin Docker: `./mvnw clean verify`. El pipeline se consulta en la pestaña Actions del repositorio.
