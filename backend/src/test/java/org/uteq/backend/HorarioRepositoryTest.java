package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.horario.entity.Horario;
import org.uteq.backend.deportivo.horario.repository.HorarioRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class HorarioRepositoryTest {

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private TestEntityManager em;

    private Entrenador entrenador;
    private Categoria sub14;
    private Horario horarioLunesManana;
    private Horario horarioLunesTarde;
    private Horario horarioInactivo;

    @BeforeEach
    void setUp() {
        EstadoGeneral activo = em.persist(EstadoGeneral.builder().idEstadoGeneral(1L).nombre("Activo").build());

        Persona pEntrenador = em.persist(Persona.builder()
                .nombre("Carlos").apellido("DT").cedula("0502223334")
                .correo("carlos.dt@test.com").fechaNacimiento(LocalDate.of(1985, 4, 10)).build());

        Usuario uEntrenador = em.persist(Usuario.builder()
                .persona(pEntrenador).estadoGeneral(activo)
                .username("carlos.dt@sged.test").password_Hash("hash").build());

        entrenador = em.persist(Entrenador.builder()
                .persona(pEntrenador).usuario(uEntrenador).activo(true).build());

        sub14 = em.persist(Categoria.builder()
                .nombre("SUB-14").edadMin((short) 12).edadMax((short) 14).activo(true).build());

        // Lunes 08:00 - 10:00 (diaSemana = 1)
        horarioLunesManana = em.persist(Horario.builder()
                .entrenador(entrenador).categoria(sub14).diaSemana((short) 1)
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(10, 0))
                .campo("Cancha Principal").activo(true).build());

        // Lunes 16:00 - 18:00 (diaSemana = 1)
        horarioLunesTarde = em.persist(Horario.builder()
                .entrenador(entrenador).categoria(sub14).diaSemana((short) 1)
                .horaInicio(LocalTime.of(16, 0)).horaFin(LocalTime.of(18, 0))
                .campo("Cancha 2").activo(true).build());

        // Inactivo
        horarioInactivo = em.persist(Horario.builder()
                .entrenador(entrenador).categoria(sub14).diaSemana((short) 1)
                .horaInicio(LocalTime.of(10, 0)).horaFin(LocalTime.of(12, 0))
                .campo("Cancha Principal").activo(false).build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByEntrenador lista solo horarios activos ordenados por dia y hora de inicio")
    void findByEntrenador_activosOrdenados() {
        List<Horario> horarios = horarioRepository
                .findByEntrenador_IdEntrenadorAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(entrenador.getIdEntrenador());

        assertThat(horarios).hasSize(2);
        assertThat(horarios.get(0).getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(horarios.get(1).getHoraInicio()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    @DisplayName("findByActivoTrueAndDiaSemana retorna solo los activos de ese dia")
    void findByActivoTrueAndDiaSemana_filtraPorDia() {
        List<Horario> lunes = horarioRepository.findByActivoTrueAndDiaSemana((short) 1);
        List<Horario> martes = horarioRepository.findByActivoTrueAndDiaSemana((short) 2);

        assertThat(lunes).hasSize(2);
        assertThat(martes).isEmpty();
    }

    @Test
    @DisplayName("cruzadosCon detecta solapamiento en el mismo dia para el entrenador")
    void cruzadosCon_detectaSolape() {
        // Solape parcial con 08:00 - 10:00 (09:00 - 11:00)
        List<Horario> solapes = horarioRepository.cruzadosCon(
                entrenador.getIdEntrenador(), (short) 1,
                LocalTime.of(9, 0), LocalTime.of(11, 0), -1L);

        assertThat(solapes).hasSize(1);
        assertThat(solapes.get(0).getIdHorario()).isEqualTo(horarioLunesManana.getIdHorario());
    }

    @Test
    @DisplayName("cruzadosCon excluye idExcluir al verificar colisiones (util para updates)")
    void cruzadosCon_excluyeIdPropio() {
        List<Horario> solapes = horarioRepository.cruzadosCon(
                entrenador.getIdEntrenador(), (short) 1,
                LocalTime.of(8, 0), LocalTime.of(10, 0), horarioLunesManana.getIdHorario());

        assertThat(solapes).isEmpty();
    }
}
