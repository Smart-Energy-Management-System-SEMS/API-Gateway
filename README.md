# API Gateway SEMS

API Gateway centralizado para SEMS construido con **Java 17**, **Spring Boot**, **Spring Cloud Gateway** y **Maven**.

Su responsabilidad es actuar como punto de entrada unico entre frontend y microservicios, aplicando politicas transversales:
- Enrutamiento HTTP por servicio
- Rewrites de paths
- CORS
- Health checks
- Logging global de requests
- Seguridad JWT con activacion por variable de entorno

## Arquitectura general

Este proyecto usa una estructura orientada a DDD para separar responsabilidades del Gateway (sin logica de negocio de microservicios):

```text
api-gateway/
|-- src/main/java/com/sems/apigateway/
|   |-- application/
|   |   |-- commandservices/
|   |   |-- eventhandlers/
|   |   |-- outboundservices/
|   |   `-- queryservices/
|   |-- domain/
|   |   |-- model/
|   |   |   |-- aggregates/
|   |   |   |-- commands/
|   |   |   |-- entities/
|   |   |   |-- queries/
|   |   |   `-- valueobjects/
|   |   |-- repositories/
|   |   `-- services/
|   |-- infrastructure/
|   |   |-- configuration/
|   |   |-- gateway/
|   |   |-- security/
|   |   |-- cors/
|   |   `-- filters/
|   |-- interfaces/
|   |   `-- rest/
|   |       |-- controllers/
|   |       |-- resources/
|   |       `-- transform/
|   `-- shared/
|       |-- constants/
|       |-- exceptions/
|       `-- utils/
|-- src/main/resources/application.yaml
|-- .env.example
|-- pom.xml
`-- README.md
```

## Microservicios integrados

- IAM Service
- Device Management Service
- Alert Service
- Subscriptions Service
- Payments Service
- Analytics Service
- Energy Monitoring Service (pendiente de despliegue)

## Variables de entorno

Definidas en `.env.example`.

Variables clave:
- `PORT`
- `FRONTEND_URL`
- `GATEWAY_ALLOWED_ORIGINS`
- `GATEWAY_SECURITY_ENABLED`
- `GATEWAY_JWT_JWK_SET_URI` o `GATEWAY_JWT_SECRET`
- `SUBSCRIPTIONS_SERVICE_URL`
- `ANALYTICS_SERVICE_URL`
- `DEVICE_MANAGEMENT_SERVICE_URL`
- `PAYMENTS_SERVICE_URL`
- `ALERT_SERVICE_URL`
- `IAM_SERVICE_URL`
- `ENERGY_MONITORING_SERVICE_URL`

## Rutas principales del Gateway

| Dominio | Ruta externa Gateway | Upstream interno |
|---|---|---|
| Gateway Health | `GET /gateway/health` | Respuesta local del gateway |
| IAM Auth | `/api/v1/auth/**` | `IAM_SERVICE_URL/api/v1/auth/**` |
| IAM Users | `/api/v1/users/**` | `IAM_SERVICE_URL/api/v1/users/**` |
| IAM Health | `GET /iam/health` | `IAM_SERVICE_URL/actuator/health` |
| Device Health | `GET /api/v1/health/device-management` | `/api/v1/device-management/health` |
| Devices | `/api/v1/devices/**` | `/api/v1/device-management/devices/**` |
| User Devices | `/api/v1/users/{userId}/devices/**` | `/api/v1/device-management/users/{userId}/devices/**` |
| User Bindings | `/api/v1/users/{userId}/bindings/**` | `/api/v1/device-management/users/{userId}/bindings/**` |
| Bindings | `/api/v1/bindings/**` | `/api/v1/device-management/bindings/**` |
| Configurations | `/api/v1/configurations/**` | `/api/v1/device-management/configurations/**` |
| Alert Service | `/api/v1/alerts-service/**` | `ALERT_SERVICE_URL/api/v1/**` |
| Subscriptions Health | `GET /api/v1/subscriptions/health` | `SUBSCRIPTIONS_SERVICE_URL/health` |
| Subscription Plans | `/api/v1/subscription-plans/**` | `SUBSCRIPTIONS_SERVICE_URL/api/v1/subscription-plans/**` |
| Subscriptions | `/api/v1/subscriptions/**` | `SUBSCRIPTIONS_SERVICE_URL/api/v1/subscriptions/**` |
| Subscriptions Webhook | `POST /api/v1/webhooks/stripe` | `SUBSCRIPTIONS_SERVICE_URL/api/v1/webhooks/stripe` |
| Payments Health | `GET /api/v1/payments/health` | `PAYMENTS_SERVICE_URL/health` |
| Payments Webhook | `POST /api/v1/payments/webhooks/stripe` | `PAYMENTS_SERVICE_URL/api/v1/webhooks/stripe` |
| Payment Methods | `/api/v1/payments/payment-methods/**` | `PAYMENTS_SERVICE_URL/api/v1/payment-methods/**` |
| Invoices | `/api/v1/payments/invoices/**` | `PAYMENTS_SERVICE_URL/api/v1/invoices/**` |
| Payments Core | `/api/v1/payments/process`, `/api/v1/payments/{id}`, `/api/v1/payments/user/**`, `/api/v1/payments/subscription/**` | `PAYMENTS_SERVICE_URL` |
| Analytics | `/api/v1/analytics/**` | `ANALYTICS_SERVICE_URL/api/v1/analytics/**` |
| Energy | `/api/v1/energy/**` | `ENERGY_MONITORING_SERVICE_URL/api/v1/energy/**` |

## CORS

CORS habilitado para:
- `http://localhost:5173`
- `http://localhost:4200`
- valor de `FRONTEND_URL` y `GATEWAY_ALLOWED_ORIGINS`

Metodos permitidos: `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

## Seguridad JWT

Controlada por `GATEWAY_SECURITY_ENABLED`.

- `false`: todas las rutas pasan sin validacion de token.
- `true`: se exige JWT para rutas de negocio.

Rutas publicas cuando seguridad esta activa:
- `/gateway/health`
- `/actuator/health/**`
- `/iam/health`
- `/api/v1/auth/**`
- `/api/v1/payments/health`
- `/api/v1/payments/webhooks/stripe`
- `/api/v1/subscriptions/health`
- `/api/v1/webhooks/stripe`
- `/api/v1/health/device-management`
- `/api/v1/alerts-service/health`
- `/api/v1/analytics/health`
- `/api/v1/energy/health`

## Logging

Filtro global de gateway (`RequestLoggingFilter`) registra por request:
- metodo HTTP
- path
- status code
- tiempo de respuesta en ms

## Ejecucion local

1. Configurar variables de entorno (puedes partir de `.env.example`).
2. Ejecutar:

```bash
mvn spring-boot:run
```

Gateway por defecto: `http://localhost:8089`.

## Compilacion

```bash
mvn clean package
```

## Despliegue en Render

1. Crear nuevo Web Service en Render con repo del gateway.
2. Runtime: Java 17.
3. Build command:

```bash
mvn clean package
```

4. Start command:

```bash
java -jar target/api-gateway-sems-0.0.1-SNAPSHOT.jar
```

5. Definir variables de entorno en Render:
- URLs de microservicios (`*_SERVICE_URL`)
- `GATEWAY_SECURITY_ENABLED`
- JWT (`GATEWAY_JWT_JWK_SET_URI` o `GATEWAY_JWT_SECRET` si aplica)
- CORS (`FRONTEND_URL`, `GATEWAY_ALLOWED_ORIGINS`)

## Importante

Este API Gateway **no** se conecta directamente a PostgreSQL, Neon, MongoDB, Stripe, Twilio, Gmail, Google OAuth ni Kafka.

Esas integraciones pertenecen a cada microservicio. El Gateway solo enruta y aplica politicas transversales.
