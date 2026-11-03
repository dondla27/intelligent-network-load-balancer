

package com.network.loadbalancer;

import com.network.loadbalancer.model.BackendServer;
import com.network.loadbalancer.service.BackendServerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackendServerServiceTests {

    private BackendServerService service;

    @BeforeEach
    void setUp() {
        service = new BackendServerService();
    }
    
@Test
void shouldDistributeRequestsInRoundRobinOrder() {

    assertEquals("Backend-1",
            service.nextServer("round-robin", List.of()).getName());

    assertEquals("Backend-2",
            service.nextServer("round-robin", List.of()).getName());

    assertEquals("Backend-3",
            service.nextServer("round-robin", List.of()).getName());

    assertEquals("Backend-1",
            service.nextServer("round-robin", List.of()).getName());
}

@Test
void shouldSelectBackendWithFewestActiveConnections() {

    BackendServer backend1 = service.findByName("Backend-1");
    BackendServer backend2 = service.findByName("Backend-2");
    BackendServer backend3 = service.findByName("Backend-3");

    backend1.beginConnection();
    backend1.beginConnection();

    backend2.beginConnection();

    BackendServer selected =
            service.nextServer("least-connections", List.of());

    assertNotNull(selected);
    assertEquals("Backend-3", selected.getName());
}

    @Test
    void shouldExcludeSpecifiedBackend() {
        BackendServer server =
                service.nextServer("round-robin",
                        List.of("Backend-1", "Backend-2"));

        assertNotNull(server);
        assertEquals("Backend-3", server.getName());
    }

    @Test
    void shouldReturnNullWhenAllBackendsExcluded() {
        BackendServer server =
                service.nextServer("round-robin",
                        List.of("Backend-1",
                                "Backend-2",
                                "Backend-3"));

        assertNull(server);
    }
    
@Test
void shouldExcludeUnhealthyBackend() {

    BackendServer backend1 =
            service.findByName("Backend-1");

    backend1.setHealthy(false);

    BackendServer selected =
            service.nextServer("round-robin", List.of());

    assertNotNull(selected);
    assertNotEquals("Backend-1", selected.getName());
}

@Test
void shouldAllowRecoveredBackendToBeSelected() {

    BackendServer backend1 =
            service.findByName("Backend-1");

    backend1.setHealthy(false);

    assertFalse(backend1.isHealthy());

    backend1.setHealthy(true);

    assertTrue(backend1.isHealthy());

    BackendServer selected =
            service.nextServer("least-connections", List.of());

    assertNotNull(selected);
    assertEquals("Backend-1", selected.getName());
}



}
