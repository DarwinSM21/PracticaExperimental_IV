package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.deportivo.partido.controller.PartidoController;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.CrearPartidoRequest;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.PartidoPageResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.PartidoResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.ResultadoRequest;
import org.uteq.backend.deportivo.partido.service.AlineacionService;
import org.uteq.backend.deportivo.partido.service.PartidoService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PartidoControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PartidoService partidoService;

    @Mock
    private AlineacionService alineacionService;

    @InjectMocks
    private PartidoController partidoController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(partidoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PartidoResponse crearPartidoResponse(Long id) {
        return new PartidoResponse(
                id,
                3L,
                "SUB-14",
                LocalDate.of(2026, 8, 20),
                LocalTime.of(15, 0),
                (short) 2,
                (short) 1,
                "Buen partido",
                "GANADO",
                true,
                11,
                false,
                null
        );
    }

    @Test
    @DisplayName("GET /api/partidos sin filtros devuelve 200 con pagina")
    void listarSinFiltros() throws Exception {
        PartidoPageResponse page = new PartidoPageResponse(
                List.of(crearPartidoResponse(1L)), 0, 20, 1, 1);
        when(partidoService.listar(isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/partidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].idPartido").value(1));
    }

    @Test
    @DisplayName("GET /api/partidos con filtros completos pasa parametros al servicio")
    void listarConFiltrosCompletos() throws Exception {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        LocalDate hasta = LocalDate.of(2026, 8, 31);
        PartidoPageResponse page = new PartidoPageResponse(
                List.of(crearPartidoResponse(1L)), 1, 10, 1, 1);

        when(partidoService.listar(eq(3L), eq(true), eq(desde), eq(hasta), eq(1), eq(10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/partidos")
                        .param("idCategoria", "3")
                        .param("cerrado", "true")
                        .param("fechaDesde", "2026-08-01")
                        .param("fechaHasta", "2026-08-31")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        verify(partidoService).listar(3L, true, desde, hasta, 1, 10);
    }

    @Test
    @DisplayName("GET /api/partidos con fechaDesde posterior a fechaHasta devuelve 400 Bad Request")
    void listarRangoFechasInvalidoDevuelve400() throws Exception {
        when(partidoService.listar(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta"));

        mockMvc.perform(get("/api/partidos")
                        .param("fechaDesde", "2026-08-31")
                        .param("fechaHasta", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La fecha desde no puede ser posterior a la fecha hasta"));
    }

    @Test
    @DisplayName("GET /api/partidos/{id} devuelve 200")
    void verPartido() throws Exception {
        when(partidoService.buscarPorId(1L)).thenReturn(crearPartidoResponse(1L));

        mockMvc.perform(get("/api/partidos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPartido").value(1))
                .andExpect(jsonPath("$.categoria").value("SUB-14"));
    }

    @Test
    @DisplayName("POST /api/partidos crea partido exitosamente")
    void crearPartido() throws Exception {
        CrearPartidoRequest request = new CrearPartidoRequest(
                3L, LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), "Amistoso");
        when(partidoService.crear(any())).thenReturn(crearPartidoResponse(5L));

        mockMvc.perform(post("/api/partidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPartido").value(5));
    }

    @Test
    @DisplayName("PUT /api/partidos/{id}/resultado actualiza resultado")
    void registrarResultado() throws Exception {
        ResultadoRequest request = new ResultadoRequest((short) 3, (short) 1, "Finalizado");
        when(partidoService.registrarResultado(eq(1L), any())).thenReturn(crearPartidoResponse(1L));

        mockMvc.perform(put("/api/partidos/1/resultado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPartido").value(1));
    }

    @Test
    @DisplayName("POST /api/partidos/{id}/reapertura reabre partido")
    void reabrirPartido() throws Exception {
        when(partidoService.reabrir(1L)).thenReturn(crearPartidoResponse(1L));

        mockMvc.perform(post("/api/partidos/1/reapertura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPartido").value(1));
    }

    @Test
    @DisplayName("DELETE /api/partidos/{id} elimina partido")
    void eliminarPartido() throws Exception {
        doNothing().when(partidoService).eliminar(1L);

        mockMvc.perform(delete("/api/partidos/1"))
                .andExpect(status().isNoContent());

        verify(partidoService).eliminar(1L);
    }
}
