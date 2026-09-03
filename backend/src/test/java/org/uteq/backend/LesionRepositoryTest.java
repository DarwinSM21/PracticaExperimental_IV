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
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LesionRepositoryTest {

    @Autowired
    private LesionRepository lesionRepository;

    @Autowired
    private TestEntityManager em;

    private Estudiante est1;
    private Estudiante est2;
    private Entrenador entrenador;
    private Lesion lesionActivaEst1;
    private Lesion lesionCuradaEst1;
    private Lesion lesionActivaEst2;

    @BeforeEach
    void setUp() {
        EstadoGeneral activo = em.persist(EstadoGeneral.builder().idEstadoGeneral(1L).nombre("Activo").build());

        Categoria sub14 = em.persist(Categoria.builder()
                .nombre("SUB-14").edadMin((short) 12).edadMax((short) 14).activo(true).build());

        Persona pEntrenador = em.persist(Persona.builder()
                .nombre("Mario").apellido("Fisio").cedula("0503334445")
                .correo("mario.fisio@test.com").fechaNacimiento(LocalDate.of(1988, 6, 20)).build());

        Usuario uEntrenador = em.persist(Usuario.builder()
                .persona(pEntrenador).estadoGeneral(activo)
                .username("mario.fisio@sged.test").password_Hash("hash").build());

        entrenador = em.persist(Entrenador.builder()
                .persona(pEntrenador).usuario(uEntrenador).activo(true).build());

        Persona p1 = em.persist(Persona.builder()
                .nombre("Pedro").apellido("Gomez").cedula("0501112223")
                .correo("pedro@test.com").fechaNacimiento(LocalDate.of(2011, 2, 1)).build());

        Persona p2 = em.persist(Persona.builder()
                .nombre("Luis").apellido("Mora").cedula("0502221113")
                .correo("luis@test.com").fechaNacimiento(LocalDate.of(2011, 4, 15)).build());

        est1 = em.persist(Estudiante.builder()
                .persona(p1).categoria(sub14).estadoGeneral(activo)
                .codigoEstudiante("EST-101").fechaIngreso(LocalDate.of(2026, 1, 10)).activo(true).build());

        est2 = em.persist(Estudiante.builder()
                .persona(p2).categoria(sub14).estadoGeneral(activo)
                .codigoEstudiante("EST-102").fechaIngreso(LocalDate.of(2026, 1, 10)).activo(true).build());

        // Lesión activa est1
        lesionActivaEst1 = em.persist(Lesion.builder()
                .estudiante(est1).entrenador(entrenador)
                .descripcion("Esguince de tobillo derecho grado 2")
                .fechaLesion(LocalDate.of(2026, 8, 10))
                .fechaEstimadaRetorno(LocalDate.of(2026, 9, 10))
                .fechaAlta(null)
                .build());

        // Lesión pasada curada est1
        lesionCuradaEst1 = em.persist(Lesion.builder()
                .estudiante(est1).entrenador(entrenador)
                .descripcion("Contractura muscular gemelo")
                .fechaLesion(LocalDate.of(2026, 3, 5))
                .fechaEstimadaRetorno(LocalDate.of(2026, 3, 20))
                .fechaAlta(LocalDate.of(2026, 3, 18))
                .build());

        // Lesión activa est2
        lesionActivaEst2 = em.persist(Lesion.builder()
                .estudiante(est2).entrenador(entrenador)
                .descripcion("Distensión ligamento rodilla")
                .fechaLesion(LocalDate.of(2026, 8, 15))
                .fechaEstimadaRetorno(LocalDate.of(2026, 9, 30))
                .fechaAlta(null)
                .build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("buscarActivaPorEstudiante retorna la lesion activa actual sin fecha de alta")
    void buscarActivaPorEstudiante_encuentraActiva() {
        Optional<Lesion> opt = lesionRepository.buscarActivaPorEstudiante(est1.getIdEstudiante());

        assertThat(opt).isPresent();
        assertThat(opt.get().getIdLesion()).isEqualTo(lesionActivaEst1.getIdLesion());
        assertThat(opt.get().getFechaAlta()).isNull();
    }

    @Test
    @DisplayName("idsEstudiantesLesionados lista los IDs de todos los estudiantes con lesion activa")
    void idsEstudiantesLesionados_listaIdsActivos() {
        List<Long> ids = lesionRepository.idsEstudiantesLesionados();

        assertThat(ids).hasSize(2)
                .containsExactlyInAnyOrder(est1.getIdEstudiante(), est2.getIdEstudiante());
    }

    @Test
    @DisplayName("idsYLesionActivaPorEstudiante devuelve tuplas (idEstudiante, idLesion) para activos")
    void idsYLesionActivaPorEstudiante_tuplas() {
        List<Object[]> tuplas = lesionRepository.idsYLesionActivaPorEstudiante();

        assertThat(tuplas).hasSize(2);
        assertThat(tuplas).allMatch(t -> t.length == 2 && t[0] instanceof Long && t[1] instanceof Long);
    }

    @Test
    @DisplayName("listarActivas pagina las lesiones activas ordenadas por fecha de lesion descendente")
    void listarActivas_paginado() {
        Page<Lesion> pagina = lesionRepository.listarActivas(PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(Lesion::getIdLesion)
                .containsExactly(lesionActivaEst2.getIdLesion(), lesionActivaEst1.getIdLesion());
    }
}
