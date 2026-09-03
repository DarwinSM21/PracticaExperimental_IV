package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.entity.Alineacion;
import org.uteq.backend.deportivo.evaluacion.entity.AlineacionJugador;
import org.uteq.backend.deportivo.evaluacion.repository.AlineacionRepository;
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AlineacionRepositoryTest {

    @Autowired
    private AlineacionRepository alineacionRepository;

    @Autowired
    private TestEntityManager em;

    private Partido partido1;
    private Partido partido2;
    private Alineacion alineacion1;

    @BeforeEach
    void setUp() {
        EstadoGeneral activo = em.persist(EstadoGeneral.builder().idEstadoGeneral(1L).nombre("Activo").build());

        Categoria sub14 = em.persist(Categoria.builder()
                .nombre("SUB-14").edadMin((short) 12).edadMax((short) 14).activo(true).build());

        Posicion posDelantero = em.persist(Posicion.builder().abreviatura("DEL").nombre("Delantero").build());
        Posicion posArquero = em.persist(Posicion.builder().abreviatura("POR").nombre("Portero").build());

        Persona p1 = em.persist(Persona.builder()
                .nombre("Jugador").apellido("Uno").cedula("0509991111")
                .correo("j1@test.com").fechaNacimiento(LocalDate.of(2011, 1, 1)).build());

        Persona p2 = em.persist(Persona.builder()
                .nombre("Jugador").apellido("Dos").cedula("0509992222")
                .correo("j2@test.com").fechaNacimiento(LocalDate.of(2011, 2, 2)).build());

        Estudiante est1 = em.persist(Estudiante.builder()
                .persona(p1).categoria(sub14).estadoGeneral(activo)
                .codigoEstudiante("EST-A1").fechaIngreso(LocalDate.of(2026, 1, 1)).activo(true).build());

        Estudiante est2 = em.persist(Estudiante.builder()
                .persona(p2).categoria(sub14).estadoGeneral(activo)
                .codigoEstudiante("EST-A2").fechaIngreso(LocalDate.of(2026, 1, 1)).activo(true).build());

        partido1 = em.persist(Partido.builder()
                .categoria(sub14).fecha(LocalDate.of(2026, 8, 20)).hora(LocalTime.of(10, 0))
                .observacion("Amistoso contra Academia A").cerrado(false).build());

        partido2 = em.persist(Partido.builder()
                .categoria(sub14).fecha(LocalDate.of(2026, 8, 27)).hora(LocalTime.of(10, 0))
                .observacion("Amistoso contra Academia B").cerrado(false).build());

        alineacion1 = Alineacion.builder()
                .partido(partido1)
                .observacion("Estrategia 4-3-3")
                .build();

        AlineacionJugador j1 = AlineacionJugador.builder()
                .alineacion(alineacion1).estudiante(est1).posicion(posDelantero)
                .titular(true).build();

        AlineacionJugador j2 = AlineacionJugador.builder()
                .alineacion(alineacion1).estudiante(est2).posicion(posArquero)
                .titular(false).build();

        alineacion1.getJugadores().add(j1);
        alineacion1.getJugadores().add(j2);

        em.persist(alineacion1);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByPartido_IdPartido carga alineacion con grafo de jugadores")
    void findByPartido_cargaAlineacionConJugadores() {
        Optional<Alineacion> opt = alineacionRepository.findByPartido_IdPartido(partido1.getIdPartido());

        assertThat(opt).isPresent();
        assertThat(opt.get().getJugadores()).hasSize(2);
        assertThat(opt.get().getJugadores()).extracting(AlineacionJugador::getTitular)
                .containsExactlyInAnyOrder(true, false);
    }

    @Test
    @DisplayName("existsByPartido_IdPartido verifica existencia de alineacion en un partido")
    void existsByPartido_verificaCorrectamente() {
        boolean existeP1 = alineacionRepository.existsByPartido_IdPartido(partido1.getIdPartido());
        boolean existeP2 = alineacionRepository.existsByPartido_IdPartido(partido2.getIdPartido());

        assertThat(existeP1).isTrue();
        assertThat(existeP2).isFalse();
    }

    @Test
    @DisplayName("contarTitularesPorPartido cuenta la cantidad de jugadores titulares por partido")
    void contarTitularesPorPartido_sumaTitulares() {
        List<Object[]> resultados = alineacionRepository.contarTitularesPorPartido(List.of(partido1.getIdPartido()));

        assertThat(resultados).hasSize(1);
        Object[] fila = resultados.get(0);
        assertThat(fila[0]).isEqualTo(partido1.getIdPartido());
        assertThat(fila[1]).isEqualTo(1L); // solo j1 es titular
    }
}
