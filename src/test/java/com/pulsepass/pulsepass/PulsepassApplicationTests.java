package com.pulsepass.pulsepass;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de humo que verifica que el contexto completo de Spring arranca.
 *
 * Requiere una instancia de PostgreSQL activa (local o en Docker).
 * Para ejecutar solo las pruebas de controladores y servicios (sin BD):
 *   mvn clean test -Dtest="*ControllerTest,GlobalExceptionHandlerTest,*ServiceImplTest"
 *
 * Para ejecutar esta prueba junto con las de integración (necesita Docker):
 *   mvn clean test
 */
@Disabled("Smoke test del contexto completo — requiere PostgreSQL activo. " +
        "Ejecutar manualmente con Docker iniciado: mvn test -Dtest=PulsepassApplicationTests")
@SpringBootTest
class PulsepassApplicationTests {

	@Test
	void contextLoads() {
	}

}
