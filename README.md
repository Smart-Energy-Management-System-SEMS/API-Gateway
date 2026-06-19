# API Gateway SEMS

API Gateway de SEMS en Java/Spring Boot, preparado para ejecución local y despliegue como contenedor en Azure Container Apps.

## Requisitos

- Java 21
- Maven 3.9+
- Docker (opcional para ejecución en contenedor)

## Health checks

- `GET /actuator/health`
- `GET /gateway/health` (forward interno a Actuator Health)

## Configuración por variables de entorno

El servicio lee variables desde `application.yaml` (sin hardcodear `localhost` para rutas de microservicios en despliegue):

- `PORT` / `SERVER_PORT` para el puerto (`server.port: ${SERVER_PORT:${PORT:8080}}`)
- `CONFIG_SERVICE_URL`
- `IAM_SERVICE_URL`
- `DEVICE_MANAGEMENT_SERVICE_URL`
- `ALERT_SERVICE_URL`
- `SUBSCRIPTIONS_SERVICE_URL`
- `PAYMENTS_SERVICE_URL`
- `ANALYTICS_SERVICE_URL`
- `ENERGY_MONITORING_SERVICE_URL`
- Variables opcionales de seguridad/CORS y timeouts

Usa `.env.example` como plantilla.

## Ejecución local (sin Docker)

1. Configura variables de entorno (puedes copiar `.env.example` a `.env` para referencia local).
2. Ejecuta:

```bash
mvn spring-boot:run
```

Por defecto queda en `http://localhost:8080` si no defines `PORT`.

## Docker

### Build de imagen

```bash
docker build -t sems-api-gateway:latest .
```

### Run de contenedor

```bash
docker run --rm -p 8080:8080 \
  -e PORT=8080 \
  -e CONFIG_SERVICE_URL=http://config-service:8090 \
  -e IAM_SERVICE_URL=http://iam-service:8080 \
  -e DEVICE_MANAGEMENT_SERVICE_URL=http://device-management-service:8080 \
  -e ALERT_SERVICE_URL=http://alert-service:8080 \
  -e SUBSCRIPTIONS_SERVICE_URL=http://subscriptions-service:8080 \
  -e PAYMENTS_SERVICE_URL=http://payments-service:8080 \
  -e ANALYTICS_SERVICE_URL=http://analytics-service:8080 \
  -e ENERGY_MONITORING_SERVICE_URL=http://energy-monitoring-service:8080 \
  sems-api-gateway:latest
```

## Ejemplo Azure Container Apps

Ejemplo base (ajusta `RESOURCE_GROUP`, `ENVIRONMENT_NAME`, `ACR_LOGIN_SERVER` e imagen):

```bash
az containerapp create \
  --name sems-api-gateway \
  --resource-group <RESOURCE_GROUP> \
  --environment <ENVIRONMENT_NAME> \
  --image <ACR_LOGIN_SERVER>/sems-api-gateway:latest \
  --target-port 8080 \
  --ingress external \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    CONFIG_SERVICE_URL=https://config-service.<domain> \
    IAM_SERVICE_URL=https://iam-service.<domain> \
    DEVICE_MANAGEMENT_SERVICE_URL=https://device-management-service.<domain> \
    ALERT_SERVICE_URL=https://alert-service.<domain> \
    SUBSCRIPTIONS_SERVICE_URL=https://subscriptions-service.<domain> \
    PAYMENTS_SERVICE_URL=https://payments-service.<domain> \
    ANALYTICS_SERVICE_URL=https://analytics-service.<domain> \
    ENERGY_MONITORING_SERVICE_URL=https://energy-monitoring-service.<domain>
```

## Notas

- No se cambió lógica de negocio ni contratos de endpoints de negocio.
- Para local puedes seguir usando URLs locales mediante variables de entorno.
- En Azure define siempre URLs desplegadas, no `localhost`.
