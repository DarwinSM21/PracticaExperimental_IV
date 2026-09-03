package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;
import org.uteq.backend.academico.pago.repository.PagoRepository;
import org.uteq.backend.academico.pago.service.PagoService;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {
    @Mock private PagoRepository pagoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private PagoService service;

    private static final Long ID_EST = 1L;
    private static final String USERNAME = "recepcion@sged.test";

    private Estudiante estudiante() {
        return Estudiante.builder().idEstudiante(ID_EST)
                .persona(Persona.builder().nombre("Juan").apellido("Perez").build())
                .activo(true)
                .build();
    }

    private Estudiante estudianteInactivo() {
        return Estudiante.builder().idEstudiante(ID_EST)
                .persona(Persona.builder().nombre("Juan").apellido("Perez").build())
                .activo(false)
                .build();
    }

    private Usuario registrador() {
        return Usuario.builder().idUsuario(9L).username(USERNAME)
                .persona(Persona.builder().nombre("Ana").apellido("Admin").build())
                .build();
    }

    private void existenEstudianteYUsuario() {
        when(estudianteRepository.findById(ID_EST)).thenReturn(Optional.of(estudiante()));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(registrador()));
    }

    private void existenEstudianteInactivoYUsuario() {
        when(estudianteRepository.findById(ID_EST)).thenReturn(Optional.of(estudianteInactivo()));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(registrador()));
    }

    @Test
    @DisplayName("ingresosDelMes suma por fecha real de pago del mes calendario vigente en Ecuador")
    void ingresosDelMes_suma_el_mes_calendario_vigente() {
        YearMonth hoy = YearMonth.now(Zonas.ECUADOR);
        LocalDate inicio = hoy.atDay(1);
        LocalDate fin = hoy.atEndOfMonth();

        when(pagoRepository.sumarMontoEntreFechas(inicio, fin)).thenReturn(new BigDecimal("150.00"));
        when(pagoRepository.countByFechaPagoBetweenAndAnuladoEnIsNull(inicio, fin)).thenReturn(3L);

        var response = service.ingresosDelMes();

        assertThat(response.anio()).isEqualTo(hoy.getYear());
        assertThat(response.mes()).isEqualTo(hoy.getMonthValue());
        assertThat(response.total()).isEqualByComparingTo("150.00");
        assertThat(response.cantidadPagos()).isEqualTo(3L);
    }

    @Test
    @DisplayName("ingresosDelMes devuelve cero, no null, cuando no hay pagos este mes")
    void ingresosDelMes_sin_pagos_devuelve_cero() {
        when(pagoRepository.sumarMontoEntreFechas(any(), any())).thenReturn(BigDecimal.ZERO);
        when(pagoRepository.countByFechaPagoBetweenAndAnuladoEnIsNull(any(), any())).thenReturn(0L);

        var response = service.ingresosDelMes();

        assertThat(response.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.cantidadPagos()).isZero();
    }

    @Test
    @DisplayName("registrarMembresia guarda un pago por cada mes, sin duplicados y en orden")
    void registrarMembresia_guarda_un_pago_por_mes_distinto_y_ordenado() {
        existenEstudianteYUsuario();
        when(pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                any(), any(), any(), any())).thenReturn(false);
        when(pagoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        var meses = List.of(3, 1, 2, 1);
        var pagos = service.registrarMembresia(ID_EST, 2026, meses, new BigDecimal("30.00"), null, USERNAME);

        assertThat(pagos).extracting(p -> p.getMes().intValue()).containsExactly(1, 2, 3);
        assertThat(pagos).allSatisfy(p -> {
            assertThat(p.getTipo()).isEqualTo(TipoPago.MEMBRESIA);
            assertThat(p.getAnio()).isEqualTo((short) 2026);
            assertThat(p.getFechaPago()).isEqualTo(LocalDate.now(Zonas.ECUADOR));
        });
    }

    @Test
    @DisplayName("registrarMembresia usa la fecha dada en vez de la de hoy si se especifica")
    void registrarMembresia_usa_la_fecha_dada() {
        existenEstudianteYUsuario();
        when(pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                any(), any(), any(), any())).thenReturn(false);
        when(pagoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        var fechaFija = LocalDate.of(2026, 1, 15);
        var pagos = service.registrarMembresia(ID_EST, 2026, List.of(1), new BigDecimal("30.00"), fechaFija, USERNAME);

        assertThat(pagos).singleElement().satisfies(p -> assertThat(p.getFechaPago()).isEqualTo(fechaFija));
    }

    @Test
    @DisplayName("registrarMembresia rechaza si algun mes solicitado ya esta cubierto")
    void registrarMembresia_rechaza_mes_ya_cubierto() {
        existenEstudianteYUsuario();
        when(pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                ID_EST, TipoPago.MEMBRESIA, (short) 2026, (short) 1)).thenReturn(false);
        when(pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                ID_EST, TipoPago.MEMBRESIA, (short) 2026, (short) 2)).thenReturn(true);

        var e = assertThrows(IllegalArgumentException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(1, 2), new BigDecimal("30.00"), null, USERNAME));

        assertThat(e.getMessage()).contains("2/2026").contains("ya está cubierto");
        verify(pagoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registrarMembresia responde 404 si el estudiante no existe")
    void registrarMembresia_estudiante_inexistente() {
        when(estudianteRepository.findById(ID_EST)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(1), new BigDecimal("30.00"), null, USERNAME));

        verify(pagoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registrarMembresia falla si el usuario autenticado no tiene fila propia")
    void registrarMembresia_usuario_registrador_inexistente() {
        when(estudianteRepository.findById(ID_EST)).thenReturn(Optional.of(estudiante()));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(1), new BigDecimal("30.00"), null, USERNAME));

        verify(pagoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registrarDiario guarda un pago DIARIO con la fecha dada")
    void registrarDiario_usa_la_fecha_dada() {
        existenEstudianteYUsuario();
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));

        var fechaFija = LocalDate.of(2026, 3, 10);
        var pago = service.registrarDiario(ID_EST, new BigDecimal("5.00"), fechaFija, USERNAME);

        assertThat(pago.getTipo()).isEqualTo(TipoPago.DIARIO);
        assertThat(pago.getFechaPago()).isEqualTo(fechaFija);
        assertThat(pago.getAnio()).isNull();
        assertThat(pago.getMes()).isNull();
    }

    @Test
    @DisplayName("registrarDiario usa la fecha de hoy en Ecuador si no se especifica")
    void registrarDiario_usa_fecha_de_hoy_si_no_se_da() {
        existenEstudianteYUsuario();
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));

        var pago = service.registrarDiario(ID_EST, new BigDecimal("5.00"), null, USERNAME);

        assertThat(pago.getFechaPago()).isEqualTo(LocalDate.now(Zonas.ECUADOR));
    }

    @Test
    @DisplayName("historialDe responde 404 si el estudiante no existe")
    void historialDe_estudiante_inexistente() {
        when(estudianteRepository.existsById(ID_EST)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> service.historialDe(ID_EST));

        verify(pagoRepository, never()).findByEstudiante_IdEstudianteOrderByFechaPagoDesc(any());
    }

    @Test
    @DisplayName("historialDe devuelve los pagos del estudiante ordenados por fecha descendente")
    void historialDe_devuelve_pagos_del_estudiante() {
        when(estudianteRepository.existsById(ID_EST)).thenReturn(true);
        var esperado = List.of(Pago.builder().idPago(1L).tipo(TipoPago.DIARIO).build());
        when(pagoRepository.findByEstudiante_IdEstudianteOrderByFechaPagoDesc(ID_EST)).thenReturn(esperado);

        var pagos = service.historialDe(ID_EST);

        assertThat(pagos).isEqualTo(esperado);
    }

    @Test
    @DisplayName("anular deja constancia de quien, cuando y por que")
    void anular_registra_la_trazabilidad() {
        Pago pago = Pago.builder()
                .idPago(9L).tipo(Pago.TipoPago.DIARIO)
                .monto(new BigDecimal("250.00")).fechaPago(LocalDate.now())
                .build();
        when(pagoRepository.findById(9L)).thenReturn(Optional.of(pago));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(registrador()));
        when(pagoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Pago resultado = service.anular(9L, "Monto mal digitado", USERNAME);

        assertThat(resultado.getAnuladoEn()).isNotNull();
        assertThat(resultado.getAnuladoPor()).isNotNull();
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Monto mal digitado");

        assertThat(resultado.getMonto()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("anular dos veces se rechaza en vez de pasar en silencio")
    void anular_dos_veces_falla() {
        Pago yaAnulado = Pago.builder()
                .idPago(9L).tipo(Pago.TipoPago.DIARIO)
                .monto(new BigDecimal("25.00")).fechaPago(LocalDate.now())
                .anuladoEn(java.time.OffsetDateTime.now())
                .motivoAnulacion("ya estaba")
                .build();
        when(pagoRepository.findById(9L)).thenReturn(Optional.of(yaAnulado));

        assertThrows(IllegalArgumentException.class,
                () -> service.anular(9L, "otra vez", USERNAME));

        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("anular un pago que no existe da recurso no encontrado")
    void anular_inexistente() {
        when(pagoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.anular(404L, "motivo", USERNAME));
    }

    // --- Validaciones de estudiante inactivo ---

    @Test
    @DisplayName("registrarMembresia rechaza estudiante inactivo")
    void registrarMembresia_estudiante_inactivo() {
        existenEstudianteInactivoYUsuario();

        var e = assertThrows(IllegalArgumentException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(1), new BigDecimal("30.00"), null, USERNAME));

        assertThat(e.getMessage()).contains("estudiante inactivo");
        verify(pagoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registrarDiario rechaza estudiante inactivo")
    void registrarDiario_estudiante_inactivo() {
        existenEstudianteInactivoYUsuario();

        var e = assertThrows(IllegalArgumentException.class, () ->
                service.registrarDiario(ID_EST, new BigDecimal("5.00"), null, USERNAME));

        assertThat(e.getMessage()).contains("estudiante inactivo");
        verify(pagoRepository, never()).save(any());
    }

    // --- Validaciones de monto ---

    @Test
    @DisplayName("registrarMembresia rechaza monto cero")
    void registrarMembresia_monto_cero() {
        existenEstudianteYUsuario();

        var e = assertThrows(IllegalArgumentException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(1), BigDecimal.ZERO, null, USERNAME));

        assertThat(e.getMessage()).contains("monto");
    }

    @Test
    @DisplayName("registrarMembresia rechaza monto negativo")
    void registrarMembresia_monto_negativo() {
        existenEstudianteYUsuario();

        var e = assertThrows(IllegalArgumentException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(1), new BigDecimal("-10.00"), null, USERNAME));

        assertThat(e.getMessage()).contains("monto");
    }

    @Test
    @DisplayName("registrarMembresia rechaza monto null")
    void registrarMembresia_monto_null() {
        existenEstudianteYUsuario();

        assertThrows(IllegalArgumentException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(1), null, null, USERNAME));
    }

    @Test
    @DisplayName("registrarDiario rechaza monto cero")
    void registrarDiario_monto_cero() {
        existenEstudianteYUsuario();

        assertThrows(IllegalArgumentException.class, () ->
                service.registrarDiario(ID_EST, BigDecimal.ZERO, null, USERNAME));
    }

    // --- Validaciones de meses ---

    @Test
    @DisplayName("registrarMembresia rechaza lista de meses vacia")
    void registrarMembresia_meses_vacia() {
        existenEstudianteYUsuario();

        var e = assertThrows(IllegalArgumentException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(), new BigDecimal("30.00"), null, USERNAME));

        assertThat(e.getMessage()).contains("al menos un mes");
    }

    @Test
    @DisplayName("registrarMembresia rechaza mes fuera de rango (13)")
    void registrarMembresia_mes_fuera_rango() {
        existenEstudianteYUsuario();

        var e = assertThrows(IllegalArgumentException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(13), new BigDecimal("30.00"), null, USERNAME));

        assertThat(e.getMessage()).contains("inválido");
    }

    @Test
    @DisplayName("registrarMembresia rechaza mes 0")
    void registrarMembresia_mes_cero() {
        existenEstudianteYUsuario();

        var e = assertThrows(IllegalArgumentException.class, () ->
                service.registrarMembresia(ID_EST, 2026, List.of(0), new BigDecimal("30.00"), null, USERNAME));

        assertThat(e.getMessage()).contains("inválido");
    }

    // --- listar ---

    @Test
    @DisplayName("listar delega al repositorio y devuelve PagoPageResponse")
    void listar_delega_correctamente() {
        var pago = Pago.builder()
                .idPago(1L).tipo(TipoPago.DIARIO).monto(new BigDecimal("5.00"))
                .fechaPago(LocalDate.of(2026, 8, 15))
                .estudiante(Estudiante.builder().idEstudiante(1L)
                        .persona(Persona.builder().nombre("Juan").apellido("Perez").build()).build())
                .registradoPor(Usuario.builder().idUsuario(9L)
                        .persona(Persona.builder().nombre("Ana").apellido("Admin").build()).build())
                .build();
        var page = new PageImpl<>(List.of(pago), PageRequest.of(0, 20), 1);
        when(pagoRepository.buscarConFiltros(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(page);

        var result = service.listar(null, null, null, null, null, 0, 20);

        assertThat(result.contenido()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.pagina()).isZero();
    }

    @Test
    @DisplayName("listar rechaza fecha desde posterior a fecha hasta")
    void listar_fechas_invalidas() {
        var e = assertThrows(IllegalArgumentException.class, () ->
                service.listar(null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1), null, 0, 20));

        assertThat(e.getMessage()).contains("fecha desde");
    }
}
