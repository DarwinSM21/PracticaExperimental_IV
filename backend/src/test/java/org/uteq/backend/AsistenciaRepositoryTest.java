package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AsistenciaRepositoryTest {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private TestEntityManager em;

    private Estudiante estudiante;
    private Estudiante estudiante2;
    private SesionEntrenamiento sesion1;
    private SesionEntrenamiento sesion2;

    @BeforeEach
    void setUp() {
        EstadoGeneral activo = em.persist(EstadoGeneral.builder().idEstadoGeneral(1L).nombre("Activo").build());

        Persona pDT = em.persist(Persona.builder()
                .nombre("DT").apellido("Profe").cedula("0509998881")
                .correo("dt.profe@test.com").fechaNacimiento(LocalDate.of(1980, 1, 1)).build());

        Usuario uDT = em.persist(Usuario.builder()
                .persona(pDT).estadoGeneral(activo)
                .username("dt.profe@sged.test").password_Hash("hash").build());

        Entrenador entrenador = em.persist(Entrenador.builder()
                .persona(pDT).usuario(uDT).activo(true).build());

        Categoria sub14 = em.persist(Categoria.builder()
                .nombre("SUB-14").edadMin((short) 12).edadMax((short) 14).activo(true).build());

        Persona pEst1 = em.persist(Persona.builder()
                .nombre("Estudiante").apellido("Uno").cedula("0501119991")
                .correo("est1@test.com").fechaNacimiento(LocalDate.of(2011, 5, 5)).build());

        Persona pEst2 = em.persist(Persona.builder()
                .nombre("Estudiante").apellido("Dos").cedula("0501119992")
                .correo("est2@test.com").fechaNacimiento(LocalDate.of(2011, 6, 6)).build());

        estudiante = em.persist(Estudiante.builder()
                .persona(pEst1).categoria(sub14).estadoGeneral(activo)
                .codigoEstudiante("EST-AS1").fechaIngreso(LocalDate.of(2026, 1, 1)).activo(true).build());

        estudiante2 = em.persist(Estudiante.builder()
                .persona(pEst2).categoria(sub14).estadoGeneral(activo)
                .codigoEstudiante("EST-AS2").fechaIngreso(LocalDate.of(2026, 1, 1)).activo(true).build());

        sesion1 = em.persist(SesionEntrenamiento.builder()
                .entrenador(entrenador).categoria(sub14)
                .fecha(LocalDate.of(2026, 8, 1))
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(10, 0))
                .campo("Cancha 1").estado("FINALIZADA").build());

        sesion2 = em.persist(SesionEntrenamiento.builder()
                .entrenador(entrenador).categoria(sub14)
                .fecha(LocalDate.of(2026, 8, 8))
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(10, 0))
                .campo("Cancha 1").estado("FINALIZADA").build());

        // Asistencia sesion 1: estudiante PRESENTE, estudiante2 AUSENTE
        em.persist(Asistencia.builder()
                .sesion(sesion1).estudiante(estudiante)
                .estado(Asistencia.ESTADO_PRESENTE)
                .horaEntrada(LocalTime.of(7, 55)).metodo(Asistencia.METODO_QR).build());

        em.persist(Asistencia.builder()
                .sesion(sesion1).estudiante(estudiante2)
                .estado(Asistencia.ESTADO_AUSENTE)
                .metodo(Asistencia.METODO_MANUAL).build());

        // Asistencia sesion 2: estudiante TARDE
        em.persist(Asistencia.builder()
                .sesion(sesion2).estudiante(estudiante)
                .estado(Asistencia.ESTADO_TARDE)
                .horaEntrada(LocalTime.of(8, 15)).metodo(Asistencia.METODO_MANUAL).build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findBySesionIdSesionAndEstudianteIdEstudiante obtiene asistencia puntual")
    void findBySesionAndEstudiante_encuentraAsistencia() {
        Optional<Asistencia> opt = asistenciaRepository
                .findBySesionIdSesionAndEstudianteIdEstudiante(sesion1.getIdSesion(), estudiante.getIdEstudiante());

        assertThat(opt).isPresent();
        assertThat(opt.get().getEstado()).isEqualTo(Asistencia.ESTADO_PRESENTE);
    }

    @Test
    @DisplayName("listarHabilitadosParaEvaluar solo retorna presentes o tardes")
    void listarHabilitadosParaEvaluar_filtraPresentesYTardes() {
        List<Asistencia> habilitados = asistenciaRepository.listarHabilitadosParaEvaluar(sesion1.getIdSesion());

        assertThat(habilitados).hasSize(1);
        assertThat(habilitados.get(0).getEstudiante().getIdEstudiante()).isEqualTo(estudiante.getIdEstudiante());
    }

    @Test
    @DisplayName("contarAsistenciasDesde cuenta solo asistencias PRESENTE y TARDE a partir de una fecha")
    void contarAsistenciasDesde_cuentaCorrectamente() {
        long cuentaDesdeAgosto1 = asistenciaRepository.contarAsistenciasDesde(
                estudiante.getIdEstudiante(), LocalDate.of(2026, 8, 1));
        long cuentaDesdeAgosto5 = asistenciaRepository.contarAsistenciasDesde(
                estudiante.getIdEstudiante(), LocalDate.of(2026, 8, 5));

        assertThat(cuentaDesdeAgosto1).isEqualTo(2); // 1 PRESENTE + 1 TARDE
        assertThat(cuentaDesdeAgosto5).isEqualTo(1); // solo la de 8 de agosto
    }
}
