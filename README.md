# Post-contenido Unidad 12: despliegue y CI/CD

## Descripción
Repositorio del laboratorio de la Unidad 12 de Programación Web (séptimo semestre). Contiene una API REST de catálogo de productos con Spring Boot, JPA y pruebas automatizadas, dockerizada con un Dockerfile multi-stage y con perfiles dev y prod (Parte 1). En la Parte 2 se extiende con un pipeline de integración continua en GitHub Actions y monitoreo con Spring Boot Actuator.

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

## Decisiones de diseño
1. Placeholders con valor por defecto en vez de placeholders estrictos. Un placeholder estricto obliga a definir todas las variables antes de arrancar, incluso en local y en las pruebas del pipeline. Con un valor por defecto razonable, el mismo artefacto arranca sin configuración en desarrollo y en producción se sobrescribe con valores reales.
2. `application-dev.properties` y `application-prod.properties` como archivos separados, en lugar de un único archivo con bloques condicionales. El nombre del archivo deja claro qué configuración corresponde a cada ambiente y reduce el riesgo de editar por error una propiedad de producción.
3. Imagen final `eclipse-temurin` JRE Alpine en lugar de distroless. Reduce el tamaño frente a un JDK completo y conserva un shell mínimo para depurar con `docker exec`.
4. `ddl-auto=update` en producción en lugar de `validate`. Con `validate`, una base de datos recién creada, sin tablas, hace fallar el arranque. `update` crea el esquema en el primer despliegue. Un proyecto real usaría una herramienta de migraciones, que queda fuera del alcance del laboratorio.

## Ajustes sobre el enunciado
- El Dockerfile compila con la imagen `maven:3.9-eclipse-temurin-17-alpine`. La imagen `eclipse-temurin` sola no incluye Maven, así que el comando `mvn` del enunciado fallaría.
- Las imágenes y el pipeline usan Java 17, la versión que declara el proyecto, en lugar de Java 21.
- Se agregó el driver de PostgreSQL al `pom.xml`, necesario para el perfil prod.
- El proyecto no usa JWT, así que se omite la propiedad del secreto.

## Cómo compilar y ejecutar
Requisitos: Docker.

1. Clonar el repositorio: `git clone https://github.com/julianejurado-rgb/jurado-post1-u12.git`
2. Construir la imagen: `docker build -t mi-spring-app:local .`
3. Ejecutar con los valores por defecto (perfil dev): `docker run -p 8080:8080 mi-spring-app:local`
4. Probar la API: `curl "http://localhost:8080/api/productos?categoria=ELECTRONICA"`
5. Ejecutar con el perfil prod y una base PostgreSQL:
   `docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod -e DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/appdb -e DATASOURCE_USER=appuser -e DATASOURCE_PASSWORD=apppass mi-spring-app:local`
6. Los logs indican el perfil activo con la línea `The following 1 profile is active`.
