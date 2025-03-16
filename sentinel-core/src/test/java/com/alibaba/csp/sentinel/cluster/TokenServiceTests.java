package com.alibaba.csp.sentinel.cluster;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TokenServiceTests {

    @Mock
    private TokenService tokenService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRequestToken() {
        when(tokenService.requestToken(anyLong(), anyInt(), anyBoolean()))
            .thenReturn(new TokenResult());

        tokenService.requestToken(1L, 1, true);
        tokenService.requestToken(1L, 1, false);

        verify(tokenService, times(2)).requestToken(anyLong(), anyInt(), anyBoolean());
    }

    @Test
    public void testRequestParamToken() {
        Collection<Object> params = Arrays.asList("param1", "param2");
        when(tokenService.requestParamToken(anyLong(), anyInt(), any()))
            .thenReturn(new TokenResult());

        tokenService.requestParamToken(1L, 1, params);

        verify(tokenService).requestParamToken(anyLong(), anyInt(), any());
    }

    @Test
    public void testRequestConcurrentToken() {
        when(tokenService.requestConcurrentToken(anyString(), anyLong(), anyInt()))
            .thenReturn(new TokenResult());

        tokenService.requestConcurrentToken("localhost", 1L, 1);

        verify(tokenService).requestConcurrentToken(anyString(), anyLong(), anyInt());
    }

    @Test
    public void testReleaseConcurrentToken() {
        doNothing().when(tokenService).releaseConcurrentToken(anyLong());

        tokenService.releaseConcurrentToken(1L);

        verify(tokenService).releaseConcurrentToken(anyLong());
    }
}
