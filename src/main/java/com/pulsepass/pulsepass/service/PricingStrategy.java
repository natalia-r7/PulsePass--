package com.pulsepass.pulsepass.service;

import com.pulsepass.pulsepass.entity.TicketType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Estrategia de precios encapsulada (BR-TICKET-009).
 *
 * Precios base:
 *   GENERAL   → $50.00
 *   STUDENT   → $35.00 (30 % de descuento sobre GENERAL)
 *   VIP       → $150.00 (3× GENERAL)
 *   BACKSTAGE → $250.00 (5× GENERAL)
 *
 * Al crecer PulsePass, este componente puede evolucionar hacia
 * un PricingService completo con promociones, descuentos y
 * precios múltiples, sin tocar TicketServiceImpl.
 */
@Component
public class PricingStrategy {

    public BigDecimal calculate(TicketType type) {
        return switch (type) {
            case GENERAL   -> new BigDecimal("50.00");
            case STUDENT   -> new BigDecimal("35.00");
            case VIP       -> new BigDecimal("150.00");
            case BACKSTAGE -> new BigDecimal("250.00");
        };
    }
}
