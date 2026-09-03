package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.evaluacion.repository.AlineacionRepository;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.CrearPartidoRequest;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.PartidoPageResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.ResultadoRequest;
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.partido.repository.PartidoRepository;
import org.uteq.backend.deportivo.partido.service.PartidoService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartidoServiceTest {
    @Mock private PartidoRepository partidoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private AlineacionRepository alineacionRepository;

    @InjectMocks private PartidoService servicio;

    private static final Long ID_PARTIDO = 7L;
    private final Categoria categoria = Categoria.builder()
            .idCategoria(3L).nombre("SUB-14").activo(true).build();

    private Partido conMarcador(Short favor, Short contra) {
        return Partido.builder().idPartido(ID_PARTIDO).categoria(categoria)
                .fecha(LocalDate.of(2026, 8, 29)).golesFavor(favor).golesContra(contra).build();
    }

    private void devuelve(Partido p) {
        when(partidoRepository.findWithCategoriaByIdPartido(ID_PARTIDO)).thenReturn(Optional.of(p));
        when(alineacionRepository.contarTitularesPorPartido(List.of(ID_PARTIDO))).thenReturn(List.of());
    }

    @Test
    @DisplayName("sin marcador el partido esta PENDIENTE, no empatado 0-0")
    void sinMarcadorEsPendiente() {
        devuelve(conMarcador(null, null));

        assertEquals("PENDIENTE", servicio.buscarPorId(ID_PARTIDO).resultado());
    }

    @Test
    @DisplayName("el resultado se deduce del marcador")
    void resultadoSeDeduce() {
        devuelve(conMarcador((short) 3, (short) 1));
        assertEquals("GANADO", servicio.buscarPorId(ID_PARTIDO).resultado());

        reset(partidoRepository, alineacionRepository);
        devuelve(conMarcador((short) 2, (short) 2));
        assertEquals("EMPATADO", servicio.buscarPorId(ID_PARTIDO).resultado());

        reset(partidoRepository, alineacionRepository);
        devuelve(conMarcador((short) 0, (short) 1));
        assertEquals("PERDIDO", servicio.buscarPorId(ID_PARTIDO).resultado());
    }

    @Test
    @DisplayName("no se agenda un partido de una categoria dada de baja")
    void categoriaInactiva() {
        Categoria inactiva = Categoria.builder().idCategoria(4L).nombre("SUB-9").activo(false).build();
        when(categoriaRepository.findById(4L)).thenReturn(Optional.of(inactiva));

        var request = new CrearPartidoRequest(4L, LocalDate.of(2026, 8, 29), null, null);

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(request));
        verify(partidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cargar el resultado de un partido que no existe da 404")
    void resultadoDePartidoInexistente() {
        when(partidoRepository.findWithCategoriaByIdPartido(99L)).thenReturn(Optional.empty());
        var request = new ResultadoRequest((short) 1, (short) 0, null);
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.registrarResultado(99L, request));
    }

    @Test
    @DisplayName("la lista cuenta los titulares de toda la pagina en una consulta, no una por fila")
    void titularesEnUnaConsulta() {
        Partido p1 = conMarcador(null, null);
        Partido p2 = Partido.builder().idPartido(8L).categoria(categoria)
                .fecha(LocalDate.of(2026, 8, 22)).build();
        when(partidoRepository.buscarConFiltros(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 20), 2));
        when(alineacionRepository.contarTitularesPorPartido(List.of(ID_PARTIDO, 8L)))
                .thenReturn(List.<Object[]>of(new Object[]{ID_PARTIDO, 11L}));

        var respuesta = servicio.listar(null, null, null, null, 0, 20);

        verify(alineacionRepository, times(1)).contarTitularesPorPartido(any());
        assertTrue(respuesta.contenido().get(0).tieneAlineacion());
        assertEquals(11, respuesta.contenido().get(0).titulares());
        assertFalse(respuesta.contenido().get(1).tieneAlineacion());
    }

    @Test
    @DisplayName("listar sin filtros consulta con todos los parametros nulos")
    void listarSinFiltros() {
        when(partidoRepository.buscarConFiltros(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(Page.empty());

        PartidoPageResponse resp = servicio.listar(null, null, null, null, 0, 20);

        assertNotNull(resp);
        assertTrue(resp.contenido().isEmpty());
        verify(partidoRepository).buscarConFiltros(isNull(), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("listar con filtro por categoria envia el id de categoria")
    void listarFiltroCategoria() {
        when(partidoRepository.buscarConFiltros(eq(3L), isNull(), isNull(), isNull(), any()))
                .thenReturn(Page.empty());

        servicio.listar(3L, null, null, null, 0, 20);

        verify(partidoRepository).buscarConFiltros(eq(3L), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("listar con filtro cerrado o abierto envia el booleano correspondiente")
    void listarFiltroCerrado() {
        when(partidoRepository.buscarConFiltros(isNull(), eq(true), isNull(), isNull(), any()))
                .thenReturn(Page.empty());
        when(partidoRepository.buscarConFiltros(isNull(), eq(false), isNull(), isNull(), any()))
                .thenReturn(Page.empty());

        servicio.listar(null, true, null, null, 0, 20);
        servicio.listar(null, false, null, null, 0, 20);

        verify(partidoRepository).buscarConFiltros(isNull(), eq(true), isNull(), isNull(), eq(PageRequest.of(0, 20)));
        verify(partidoRepository).buscarConFiltros(isNull(), eq(false), isNull(), isNull(), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("listar con fecha desde o fecha hasta pasa cada parametro correctamente")
    void listarFiltroFechasIndividuales() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        LocalDate hasta = LocalDate.of(2026, 8, 31);

        when(partidoRepository.buscarConFiltros(isNull(), isNull(), eq(desde), isNull(), any()))
                .thenReturn(Page.empty());
        when(partidoRepository.buscarConFiltros(isNull(), isNull(), isNull(), eq(hasta), any()))
                .thenReturn(Page.empty());

        servicio.listar(null, null, desde, null, 0, 20);
        servicio.listar(null, null, null, hasta, 0, 20);

        verify(partidoRepository).buscarConFiltros(isNull(), isNull(), eq(desde), isNull(), eq(PageRequest.of(0, 20)));
        verify(partidoRepository).buscarConFiltros(isNull(), isNull(), isNull(), eq(hasta), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("listar con rango de fechas valido pasa ambas fechas al repositorio")
    void listarRangoFechasValido() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        LocalDate hasta = LocalDate.of(2026, 8, 31);

        when(partidoRepository.buscarConFiltros(isNull(), isNull(), eq(desde), eq(hasta), any()))
                .thenReturn(Page.empty());

        servicio.listar(null, null, desde, hasta, 0, 20);

        verify(partidoRepository).buscarConFiltros(isNull(), isNull(), eq(desde), eq(hasta), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("listar con fechaDesde posterior a fechaHasta lanza IllegalArgumentException")
    void listarRangoFechasInvalido() {
        LocalDate desde = LocalDate.of(2026, 8, 31);
        LocalDate hasta = LocalDate.of(2026, 8, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.listar(null, null, desde, hasta, 0, 20));

        assertTrue(ex.getMessage().contains("fecha desde no puede ser posterior"));
        verify(partidoRepository, never()).buscarConFiltros(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("listar combinando todos los filtros pasa todos los parametros al repositorio")
    void listarCombinacionFiltros() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        LocalDate hasta = LocalDate.of(2026, 8, 15);

        when(partidoRepository.buscarConFiltros(eq(5L), eq(false), eq(desde), eq(hasta), any()))
                .thenReturn(Page.empty());

        servicio.listar(5L, false, desde, hasta, 1, 10);

        verify(partidoRepository).buscarConFiltros(eq(5L), eq(false), eq(desde), eq(hasta), eq(PageRequest.of(1, 10)));
    }

    @Test
    @DisplayName("listar resultados vacios devuelve lista vacia y no consulta titulares")
    void listarResultadosVacios() {
        when(partidoRepository.buscarConFiltros(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PartidoPageResponse resp = servicio.listar(null, null, null, null, 0, 20);

        assertEquals(0, resp.total());
        assertEquals(0, resp.totalPaginas());
        assertTrue(resp.contenido().isEmpty());
        verify(alineacionRepository, never()).contarTitularesPorPartido(any());
    }

    @Test
    @DisplayName("listar normaliza pagina negativa a cero y tamano fuera de rango a limites")
    void listarPaginacionLimites() {
        when(partidoRepository.buscarConFiltros(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        servicio.listar(null, null, null, null, -5, 500);

        verify(partidoRepository).buscarConFiltros(any(), any(), any(), any(), eq(PageRequest.of(0, 100)));
    }

    @Test
    @DisplayName("metodo sobrecargado listar(idCategoria, pagina, tamano) delega con filtros nulos")
    void listarSobrecargaCompatibilidad() {
        when(partidoRepository.buscarConFiltros(eq(2L), isNull(), isNull(), isNull(), any()))
                .thenReturn(Page.empty());

        servicio.listar(2L, 0, 15);

        verify(partidoRepository).buscarConFiltros(eq(2L), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 15)));
    }
}
