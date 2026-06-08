package com.sems.apigateway.application.commandservices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
/**
 * Servicio de comando para gestionar la configuración de seguridad del API Gateway.
 * 
 * Esta clase es responsable de determinar si la seguridad está habilitada o deshabilitada
 * en el gateway, basándose en la configuración de la aplicación.
 */
@Service
public class GatewaySecurityCommandService {

    private final boolean securityEnabled;
 /**
     * Constructor que inyecta la configuración de seguridad desde las propiedades de la aplicación.
     * 
     * @param securityEnabled valor booleano inyectado de la configuración 'gateway.security.enabled'
     *                        (por defecto: false)
     */
    public GatewaySecurityCommandService(@Value("${gateway.security.enabled:false}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }
}
