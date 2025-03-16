package com.alibaba.csp.sentinel.cluster;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockedStatic;

import com.alibaba.csp.sentinel.cluster.client.ClusterTokenClient;
import com.alibaba.csp.sentinel.cluster.client.TokenClientProvider;
import com.alibaba.csp.sentinel.cluster.server.EmbeddedClusterTokenServer;
import com.alibaba.csp.sentinel.cluster.server.EmbeddedClusterTokenServerProvider;
import com.alibaba.csp.sentinel.property.DynamicSentinelProperty;
import com.alibaba.csp.sentinel.property.SentinelProperty;

public class ClusterStateManagerTests {

    @Mock
    private ClusterTokenClient mockClient;

    @Mock
    private EmbeddedClusterTokenServer mockServer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Reset state
        ClusterStateManager.applyState(ClusterStateManager.CLUSTER_NOT_STARTED);

        // Mock dependencies
        try (MockedStatic<TokenClientProvider> clientProvider = mockStatic(TokenClientProvider.class);
             MockedStatic<EmbeddedClusterTokenServerProvider> serverProvider = mockStatic(EmbeddedClusterTokenServerProvider.class)) {

            clientProvider.when(TokenClientProvider::getClient).thenReturn(mockClient);
            serverProvider.when(EmbeddedClusterTokenServerProvider::getServer).thenReturn(mockServer);
        }
    }

    @Test
    public void testRegisterProperty() {
        SentinelProperty<Integer> property = new DynamicSentinelProperty<>();
        ClusterStateManager.registerProperty(property);

        property.updateValue(ClusterStateManager.CLUSTER_CLIENT);
        assertEquals(ClusterStateManager.CLUSTER_CLIENT, ClusterStateManager.getMode());
    }

    @Test
    public void testGetMode() {
        assertEquals(ClusterStateManager.CLUSTER_NOT_STARTED, ClusterStateManager.getMode());

        ClusterStateManager.setToClient();
        assertEquals(ClusterStateManager.CLUSTER_CLIENT, ClusterStateManager.getMode());

        ClusterStateManager.setToServer();
        assertEquals(ClusterStateManager.CLUSTER_SERVER, ClusterStateManager.getMode());
    }

    @Test
    public void testIsClient() {
        assertFalse(ClusterStateManager.isClient());

        ClusterStateManager.setToClient();
        assertTrue(ClusterStateManager.isClient());

        ClusterStateManager.setToServer();
        assertFalse(ClusterStateManager.isClient());
    }

    @Test
    public void testIsServer() {
        assertFalse(ClusterStateManager.isServer());

        ClusterStateManager.setToServer();
        assertTrue(ClusterStateManager.isServer());

        ClusterStateManager.setToClient();
        assertFalse(ClusterStateManager.isServer());
    }

    @Test
    public void testSetToClient() throws Exception {
        try (MockedStatic<TokenClientProvider> clientProvider = mockStatic(TokenClientProvider.class);
             MockedStatic<EmbeddedClusterTokenServerProvider> serverProvider = mockStatic(EmbeddedClusterTokenServerProvider.class)) {

            clientProvider.when(TokenClientProvider::getClient).thenReturn(mockClient);
            serverProvider.when(EmbeddedClusterTokenServerProvider::getServer).thenReturn(mockServer);

            assertTrue(ClusterStateManager.setToClient());
            verify(mockServer).stop();
            verify(mockClient).start();
            assertEquals(ClusterStateManager.CLUSTER_CLIENT, ClusterStateManager.getMode());

            // Test idempotent
            assertTrue(ClusterStateManager.setToClient());
        }
    }

    @Test
    public void testSetToServer() throws Exception {
        try (MockedStatic<TokenClientProvider> clientProvider = mockStatic(TokenClientProvider.class);
             MockedStatic<EmbeddedClusterTokenServerProvider> serverProvider = mockStatic(EmbeddedClusterTokenServerProvider.class)) {

            clientProvider.when(TokenClientProvider::getClient).thenReturn(mockClient);
            serverProvider.when(EmbeddedClusterTokenServerProvider::getServer).thenReturn(mockServer);

            assertTrue(ClusterStateManager.setToServer());
            verify(mockClient).stop();
            verify(mockServer).start();
            assertEquals(ClusterStateManager.CLUSTER_SERVER, ClusterStateManager.getMode());

            // Test idempotent
            assertTrue(ClusterStateManager.setToServer());
        }
    }

    @Test
    public void testApplyState() throws Exception {
        try (MockedStatic<TokenClientProvider> clientProvider = mockStatic(TokenClientProvider.class);
             MockedStatic<EmbeddedClusterTokenServerProvider> serverProvider = mockStatic(EmbeddedClusterTokenServerProvider.class)) {

            clientProvider.when(TokenClientProvider::getClient).thenReturn(mockClient);
            serverProvider.when(EmbeddedClusterTokenServerProvider::getServer).thenReturn(mockServer);

            // Test client mode
            ClusterStateManager.applyState(ClusterStateManager.CLUSTER_CLIENT);
            verify(mockServer).stop();
            verify(mockClient).start();
            assertEquals(ClusterStateManager.CLUSTER_CLIENT, ClusterStateManager.getMode());

            // Test server mode
            ClusterStateManager.applyState(ClusterStateManager.CLUSTER_SERVER);
            verify(mockClient, times(2)).stop();
            verify(mockServer).start();
            assertEquals(ClusterStateManager.CLUSTER_SERVER, ClusterStateManager.getMode());

            // Test stop
            ClusterStateManager.applyState(ClusterStateManager.CLUSTER_NOT_STARTED);
            verify(mockClient, times(3)).stop();
            verify(mockServer, times(2)).stop();
            assertEquals(ClusterStateManager.CLUSTER_NOT_STARTED, ClusterStateManager.getMode());

            // Test invalid state
            ClusterStateManager.applyState(-2);
            assertEquals(ClusterStateManager.CLUSTER_NOT_STARTED, ClusterStateManager.getMode());
        }
    }

    @Test
    public void testSetStop() throws Exception {
        try (MockedStatic<TokenClientProvider> clientProvider = mockStatic(TokenClientProvider.class);
             MockedStatic<EmbeddedClusterTokenServerProvider> serverProvider = mockStatic(EmbeddedClusterTokenServerProvider.class)) {

            clientProvider.when(TokenClientProvider::getClient).thenReturn(mockClient);
            serverProvider.when(EmbeddedClusterTokenServerProvider::getServer).thenReturn(mockServer);

            // Set to client first
            ClusterStateManager.setToClient();

            // Then stop
            ClusterStateManager.applyState(ClusterStateManager.CLUSTER_NOT_STARTED);
            verify(mockClient).stop();
            verify(mockServer).stop();
            assertEquals(ClusterStateManager.CLUSTER_NOT_STARTED, ClusterStateManager.getMode());
        }
    }

    @Test
    public void testGetLastModified() throws InterruptedException {
        long before = ClusterStateManager.getLastModified();
        Thread.sleep(10);

        ClusterStateManager.setToClient();
        long after = ClusterStateManager.getLastModified();

        assertTrue(after > before);
    }
}
