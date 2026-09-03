package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;
import org.uteq.backend.academico.pago.repository.PagoRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PagoRepositoryTest {

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private TestEntityManager em;

    private Estudiante estudiante;
    private Estudiante estudiante2;
    private Usuario registrador;

    @BeforeEach
    void setUp() {
        EstadoGeneral estadoActivo = em.persist(EstadoGeneral.builder()
                .idEstadoGeneral(1L).nombre("Activo").build());

        Categoria categoria = em.persist(Categoria.builder()
                .nombre("SUB-14").edadMin((short) 12).edadMax((short) 14).activo(true).build());

        Persona persona1 = em.persist(Persona.builder()
                .nombre("Juan").apellido("Perez").cedula("0501234567")
                .correo("juan@test.com").fechaNacimiento(LocalDate.of(2010, 5, 15)).build());

        Persona persona2 = em.persist(Persona.builder()
                .nombre("Maria").apellido("Lopez").cedula("0501234568")
                .correo("maria@test.com").fechaNacimiento(LocalDate.of(2011, 3, 20)).build());

        Persona personaAdmin = em.persist(Persona.builder()
                .nombre("Admin").apellido("Sistema").cedula("0509999999")
                .correo("admin@test.com").fechaNacimiento(LocalDate.of(1990, 1, 1)).build());

        registrador = em.persist(Usuario.builder()
                .persona(personaAdmin).estadoGeneral(estadoActivo)
                .username("admin@sged.test").password_Hash("hash123").build());

        estudiante = em.persist(Estudiante.builder()
                .persona(persona1).categoria(categoria).estadoGeneral(estadoActivo)
                .codigoEstudiante("EST-001").fechaIngreso(LocalDate.of(2026, 1, 15))
                .activo(true).build());

        estudiante2 = em.persist(Estudiante.builder()
                .persona(persona2).categoria(categoria).estadoGeneral(estadoActivo)
                .codigoEstudiante("EST-002").fechaIngreso(LocalDate.of(2026, 2, 10))
                .activo(true).build());

        // Pago vigente: membresía agosto 2026, $30
        em.persist(Pago.builder()
                .estudiante(estudiante).tipo(TipoPago.MEMBRESIA)
                .anio((short) 2026).mes((short) 8).monto(new BigDecimal("30.00"))
                .fechaPago(LocalDate.of(2026, 8, 1)).registradoPor(registrador).build());

        // Pago vigente: membresía septiembre 2026, $30
        em.persist(Pago.builder()
                .estudiante(estudiante).tipo(TipoPago.MEMBRESIA)
                .anio((short) 2026).mes((short) 9).monto(new BigDecimal("30.00"))
                .fechaPago(LocalDate.of(2026, 9, 1)).registradoPor(registrador).build());

        // Pago diario: $5 el 15 de agosto
        em.persist(Pago.builder()
                .estudiante(estudiante2).tipo(TipoPago.DIARIO)
                .monto(new BigDecimal("5.00"))
                .fechaPago(LocalDate.of(2026, 8, 15)).registradoPor(registrador).build());

        // Pago anulado: membresía julio 2026, $30 (anulado)
        em.persist(Pago.builder()
                .estudiante(estudiante).tipo(TipoPago.MEMBRESIA)
                .anio((short) 2026).mes((short) 7).monto(new BigDecimal("30.00"))
                .fechaPago(LocalDate.of(2026, 7, 10)).registradoPor(registrador)
                .anuladoEn(OffsetDateTime.now()).anuladoPor(registrador)
                .motivoAnulacion("Error de registro").build());

        // Pago de estudiante 2: membresía agosto 2026, $25
        em.persist(Pago.builder()
                .estudiante(estudiante2).tipo(TipoPago.MEMBRESIA)
                .anio((short) 2026).mes((short) 8).monto(new BigDecimal("25.00"))
                .fechaPago(LocalDate.of(2026, 8, 5)).registradoPor(registrador).build());

        em.flush();
        em.clear();
    }

    // --- sumarMontoEntreFechas ---

    @Test
    @DisplayName("sumarMontoEntreFechas suma solo pagos vigentes del rango")
    void sumarMontoEntreFechas_suma_vigentes() {
        BigDecimal total = pagoRepository.sumarMontoEntreFechas(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        // 30 (memb est1 ago) + 5 (diario est2 ago) + 25 (memb est2 ago) = 60
        assertThat(total).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("sumarMontoEntreFechas devuelve 0 si no hay pagos en el rango")
    void sumarMontoEntreFechas_vacio_devuelve_cero() {
        BigDecimal total = pagoRepository.sumarMontoEntreFechas(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        assertThat(total).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("sumarMontoEntreFechas excluye pagos anulados")
    void sumarMontoEntreFechas_excluye_anulados() {
        // Julio tiene un pago anulado de 30, no debe sumarse
        BigDecimal total = pagoRepository.sumarMontoEntreFechas(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        assertThat(total).isEqualByComparingTo("0");
    }

    // --- countByFechaPagoBetweenAndAnuladoEnIsNull ---

    @Test
    @DisplayName("countByFechaPago cuenta solo pagos vigentes en el rango")
    void countByFechaPago_vigentes() {
        long count = pagoRepository.countByFechaPagoBetweenAndAnuladoEnIsNull(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        assertThat(count).isEqualTo(3); // 3 pagos vigentes en agosto
    }

    // --- idsConMembresiaCubierta ---

    @Test
    @DisplayName("idsConMembresiaCubierta retorna IDs de estudiantes con mes cubierto")
    void idsConMembresiaCubierta_retorna_ids() {
        List<Long> ids = pagoRepository.idsConMembresiaCubierta(
                TipoPago.MEMBRESIA, (short) 2026, (short) 8);
        // est1 y est2 tienen membresía agosto 2026 vigente
        assertThat(ids).containsExactlyInAnyOrder(
                estudiante.getIdEstudiante(), estudiante2.getIdEstudiante());
    }

    @Test
    @DisplayName("idsConMembresiaCubierta excluye anulados")
    void idsConMembresiaCubierta_excluye_anulados() {
        List<Long> ids = pagoRepository.idsConMembresiaCubierta(
                TipoPago.MEMBRESIA, (short) 2026, (short) 7);
        // El mes 7 solo tiene un pago anulado → vacío
        assertThat(ids).isEmpty();
    }

    // --- totalesPorMesDeCobro ---

    @Test
    @DisplayName("totalesPorMesDeCobro agrupa correctamente por año y mes")
    void totalesPorMesDeCobro_agrupacion() {
        List<Object[]> filas = pagoRepository.totalesPorMesDeCobro(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));
        // Julio: 0 vigentes; Agosto: 60; Septiembre: 30
        assertThat(filas).hasSize(2); // solo meses con datos vigentes
        // Verificar que agosto tiene total 60
        Object[] agosto = filas.stream()
                .filter(r -> ((Number) r[1]).intValue() == 8)
                .findFirst().orElseThrow();
        assertThat(((BigDecimal) agosto[2])).isEqualByComparingTo("60.00");
        assertThat(((Number) agosto[3]).longValue()).isEqualTo(3);
    }

    // --- existsByEstudiante... ---

    @Test
    @DisplayName("existsByEstudiante retorna true para mes cubierto vigente")
    void existsBy_verdadero() {
        boolean existe = pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                estudiante.getIdEstudiante(), TipoPago.MEMBRESIA, (short) 2026, (short) 8);
        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("existsByEstudiante retorna false para mes anulado")
    void existsBy_falso_anulado() {
        boolean existe = pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                estudiante.getIdEstudiante(), TipoPago.MEMBRESIA, (short) 2026, (short) 7);
        assertThat(existe).isFalse();
    }

    // --- findByEstudiante_IdEstudianteOrderByFechaPagoDesc ---

    @Test
    @DisplayName("findByEstudiante ordena por fecha descendente")
    void findByEstudiante_orden_desc() {
        List<Pago> pagos = pagoRepository.findByEstudiante_IdEstudianteOrderByFechaPagoDesc(
                estudiante.getIdEstudiante());
        assertThat(pagos).hasSize(3); // 2 vigentes + 1 anulado
        assertThat(pagos.get(0).getFechaPago()).isAfterOrEqualTo(pagos.get(1).getFechaPago());
    }

    // --- buscarConFiltros ---

    @Test
    @DisplayName("buscarConFiltros sin filtros devuelve todos los pagos paginados")
    void buscarConFiltros_sin_filtros() {
        Page<Pago> page = pagoRepository.buscarConFiltros(
                null, null, null, null, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("buscarConFiltros filtra por idEstudiante")
    void buscarConFiltros_por_estudiante() {
        Page<Pago> page = pagoRepository.buscarConFiltros(
                estudiante.getIdEstudiante(), null, null, null, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).allMatch(p ->
                p.getEstudiante().getIdEstudiante().equals(estudiante.getIdEstudiante()));
    }

    @Test
    @DisplayName("buscarConFiltros filtra por tipo DIARIO")
    void buscarConFiltros_por_tipo() {
        Page<Pago> page = pagoRepository.buscarConFiltros(
                null, TipoPago.DIARIO, null, null, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTipo()).isEqualTo(TipoPago.DIARIO);
    }

    @Test
    @DisplayName("buscarConFiltros filtra por rango de fechas")
    void buscarConFiltros_por_fechas() {
        Page<Pago> page = pagoRepository.buscarConFiltros(
                null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getMes()).isEqualTo((short) 9);
    }

    @Test
    @DisplayName("buscarConFiltros filtra solo anulados")
    void buscarConFiltros_anulados() {
        Page<Pago> page = pagoRepository.buscarConFiltros(
                null, null, null, null, true, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getAnuladoEn()).isNotNull();
    }

    @Test
    @DisplayName("buscarConFiltros filtra solo vigentes")
    void buscarConFiltros_vigentes() {
        Page<Pago> page = pagoRepository.buscarConFiltros(
                null, null, null, null, false, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent()).allMatch(p -> p.getAnuladoEn() == null);
    }

    @Test
    @DisplayName("buscarConFiltros combina estudiante + tipo + vigente")
    void buscarConFiltros_combinados() {
        Page<Pago> page = pagoRepository.buscarConFiltros(
                estudiante.getIdEstudiante(), TipoPago.MEMBRESIA, null, null, false, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(2); // ago y sept, excluyendo el anulado de julio
    }

    @Test
    @DisplayName("buscarConFiltros paginación funciona correctamente")
    void buscarConFiltros_paginacion() {
        Page<Pago> page1 = pagoRepository.buscarConFiltros(
                null, null, null, null, null, PageRequest.of(0, 2));
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalPages()).isEqualTo(3); // 5 elementos / 2 por página

        Page<Pago> page2 = pagoRepository.buscarConFiltros(
                null, null, null, null, null, PageRequest.of(1, 2));
        assertThat(page2.getContent()).hasSize(2);
    }
}
