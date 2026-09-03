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
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.horario.entity.Horario;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SesionEntrenamientoRepositoryTest {

    @Autowired
    private SesionEntrenamientoRepository sesionRepository;

    @Autowired
    private TestEntityManager em;

    private Entrenador entrenador;
    private Categoria sub14;
    private Horario horario;
    private SesionEntrenamiento sesion1;
    private SesionEntrenamiento sesion2;

    @BeforeEach
    void setUp() {
        EstadoGeneral activo = em.persist(EstadoGeneral.builder().idEstadoGeneral(1L).nombre("Activo").build());

        Persona pDT = em.persist(Persona.builder()
                .nombre("Roberto").apellido("Perez").cedula("0504445556")
                .correo("roberto.dt@test.com").fechaNacimiento(LocalDate.of(1982, 5, 12)).build());

        Usuario uDT = em.persist(Usuario.builder()
                .persona(pDT).estadoGeneral(activo)
                .username("roberto.dt@sged.test").password_Hash("hash").build());

        entrenador = em.persist(Entrenador.builder().persona(pDT).usuario(uDT).activo(true).build());

        sub14 = em.persist(Categoria.builder()
                .nombre("SUB-14").edadMin((short) 12).edadMax((short) 14).activo(true).build());

        horario = em.persist(Horario.builder()
                .entrenador(entrenador).categoria(sub14).diaSemana((short) 2)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(11, 0))
                .campo("Cancha A").activo(true).build());

        // Sesión 1: 2026-08-10 09:00 - 11:00
        sesion1 = em.persist(SesionEntrenamiento.builder()
                .entrenador(entrenador).categoria(sub14).horario(horario)
                .fecha(LocalDate.of(2026, 8, 10))
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(11, 0))
                .campo("Cancha A").estado("FINALIZADA").build());

        // Sesión 2: 2026-08-12 15:00 - 17:00
        sesion2 = em.persist(SesionEntrenamiento.builder()
                .entrenador(entrenador).categoria(sub14)
                .fecha(LocalDate.of(2026, 8, 12))
                .horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(17, 0))
                .campo("Cancha B").estado("PROGRAMADA").build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByFechaOrderByHoraInicioAsc retorna sesiones del dia ordenadas por hora")
    void findByFecha_retornaSesionesOrdenadas() {
        List<SesionEntrenamiento> sesiones = sesionRepository
                .findByFechaOrderByHoraInicioAsc(LocalDate.of(2026, 8, 10));

        assertThat(sesiones).hasSize(1);
        assertThat(sesiones.get(0).getCampo()).isEqualTo("Cancha A");
    }

    @Test
    @DisplayName("existsByHorario_IdHorarioAndFecha verifica si ya existe una sesion instanciada")
    void existsByHorario_verificaExistencia() {
        boolean existe = sesionRepository.existsByHorario_IdHorarioAndFecha(
                horario.getIdHorario(), LocalDate.of(2026, 8, 10));
        boolean noExiste = sesionRepository.existsByHorario_IdHorarioAndFecha(
                horario.getIdHorario(), LocalDate.of(2026, 8, 17));

        assertThat(existe).isTrue();
        assertThat(noExiste).isFalse();
    }

    @Test
    @DisplayName("existeSolape detecta superposicion horaria para una categoria")
    void existeSolape_detectaSuperposicion() {
        // En 2026-08-10 de 10:00 a 12:00 hay solape con 09:00-11:00
        boolean solapa = sesionRepository.existeSolape(
                sub14.getIdCategoria(), LocalDate.of(2026, 8, 10),
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        // En 2026-08-10 de 12:00 a 14:00 no hay solape
        boolean noSolapa = sesionRepository.existeSolape(
                sub14.getIdCategoria(), LocalDate.of(2026, 8, 10),
                LocalTime.of(12, 0), LocalTime.of(14, 0));

        assertThat(solapa).isTrue();
        assertThat(noSolapa).isFalse();
    }

    @Test
    @DisplayName("sesionesDelEntrenador pagina sesiones del entrenador")
    void sesionesDelEntrenador_paginado() {
        Page<SesionEntrenamiento> page = sesionRepository.sesionesDelEntrenador(
                entrenador.getIdEntrenador(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
