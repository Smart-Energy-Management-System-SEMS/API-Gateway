package com.sems.apigateway.application.queryservices;

import com.sems.apigateway.domain.model.entities.GatewayRoute;
import com.sems.apigateway.domain.model.valueobjects.RouteStatus;
import com.sems.apigateway.infrastructure.gateway.InMemoryGatewayRouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * Servicio de consulta por defecto para rutas de puerta de enlace (Gateway Routes).
 * 
 * Esta clase implementa la interfaz {@link GatewayRouteQueryService} y proporciona
 * operaciones de lectura para gestionar las rutas activas del sistema de gestión
 * de energía inteligente (SEMS).
 * 
 * Utiliza el patrón Query Service (CQRS) para separar las operaciones de consulta
 * de las de comando, mejorando la escalabilidad y mantenibilidad del sistema.
 * 
 * @author SEMS Team
 * @version 1.0
 */
@Service
public class DefaultGatewayRouteQueryService implements GatewayRouteQueryService {

    private final InMemoryGatewayRouteRepository repository;
/**
     * Constructor que inicializa el servicio de consulta con un repositorio inyectado.
     * 
     * Utiliza inyección de dependencias de Spring para obtener la instancia del
     * repositorio, asegurando que el ciclo de vida está gestionado por el contenedor
     * de IoC de Spring.
     * 
     * @param repository el repositorio en memoria de rutas de gateway
     *                   (no puede ser null)
     */
    public DefaultGatewayRouteQueryService(InMemoryGatewayRouteRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<GatewayRoute> getActiveRoutes() {
        return repository.findAll().stream()
                .filter(route -> route.routeStatus() == RouteStatus.ACTIVE)
                .toList();
    }
}
