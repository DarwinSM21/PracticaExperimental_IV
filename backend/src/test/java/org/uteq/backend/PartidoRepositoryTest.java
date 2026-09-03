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
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.partido.repository.PartidoRepository;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PartidoRepositoryTest {

    @Autowired
    private PartidoRepository partidoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Categoria catSub14;
    private Categoria catSub16;
    private Partido partido1;
    private Partido partido2;
    private Partido partido3;

    @BeforeEach
    void setUp() {
        catSub14 = entityManager.persist(Categoria.builder()
                .nombre("SUB-14")
                .edadMin((short) 12)
                .edadMax((short) 14)
                .activo(true)
                .build());

        catSub16 = entityManager.persist(Categoria.builder()
                .nombre("SUB-16")
                .edadMin((short) 14)
                .edadMax((short) 16)
                .activo(true)
                .build());

        // Partido 1: Sub-14, 2026-08-10 09:00, abierto
        partido1 = entityManager.persist(Partido.builder()
                .categoria(catSub14)
                .fecha(LocalDate.of(2026, 8, 10))
                .hora(LocalTime.of(9, 0))
                .cerrado(false)
                .build());

        // Partido 2: Sub-14, 2026-08-20 16:00, cerrado
        partido2 = entityManager.persist(Partido.builder()
                .categoria(catSub14)
                .fecha(LocalDate.of(2026, 8, 20))
                .hora(LocalTime.of(16, 0))
                .cerrado(true)
                .build());

        // Partido 3: Sub-16, 2026-08-30 11:00, abierto
        partido3 = entityManager.persist(Partido.builder()
                .categoria(catSub16)
                .fecha(LocalDate.of(2026, 8, 30))
                .hora(LocalTime.of(11, 0))
                .cerrado(false)
                .build());

        entityManager.flush();
    }

    @Test
    @DisplayName("buscarConFiltros - Sin filtros retorna todos los partidos ordenados por fecha desc")
    void buscarConFiltros_SinFiltros() {
        Page<Partido> page = partidoRepository.buscarConFiltros(
                null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Partido::getIdPartido)
                .containsExactly(partido3.getIdPartido(), partido2.getIdPartido(), partido1.getIdPartido());
    }

    @Test
    @DisplayName("buscarConFiltros - Filtrar por categoría retorna solo los de esa categoría")
    void buscarConFiltros_PorCategoria() {
        Page<Partido> pageSub14 = partidoRepository.buscarConFiltros(
                catSub14.getIdCategoria(), null, null, null, PageRequest.of(0, 10));

        assertThat(pageSub14.getTotalElements()).isEqualTo(2);
        assertThat(pageSub14.getContent()).allMatch(p -> p.getCategoria().getIdCategoria().equals(catSub14.getIdCategoria()));

        Page<Partido> pageSub16 = partidoRepository.buscarConFiltros(
                catSub16.getIdCategoria(), null, null, null, PageRequest.of(0, 10));

        assertThat(pageSub16.getTotalElements()).isEqualTo(1);
        assertThat(pageSub16.getContent().get(0).getIdPartido()).isEqualTo(partido3.getIdPartido());
    }

    @Test
    @DisplayName("buscarConFiltros - Filtrar por cerrado retorna abiertos o cerrados según corresponda")
    void buscarConFiltros_PorCerrado() {
        Page<Partido> cerrados = partidoRepository.buscarConFiltros(
                null, true, null, null, PageRequest.of(0, 10));

        assertThat(cerrados.getTotalElements()).isEqualTo(1);
        assertThat(cerrados.getContent().get(0).getIdPartido()).isEqualTo(partido2.getIdPartido());

        Page<Partido> abiertos = partidoRepository.buscarConFiltros(
                null, false, null, null, PageRequest.of(0, 10));

        assertThat(abiertos.getTotalElements()).isEqualTo(2);
        assertThat(abiertos.getContent()).extracting(Partido::getIdPartido)
                .containsExactly(partido3.getIdPartido(), partido1.getIdPartido());
    }

    @Test
    @DisplayName("buscarConFiltros - Filtrar por rango de fechas")
    void buscarConFiltros_PorRangoFechas() {
        Page<Partido> enRango = partidoRepository.buscarConFiltros(
                null, null, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 25), PageRequest.of(0, 10));

        assertThat(enRango.getTotalElements()).isEqualTo(1);
        assertThat(enRango.getContent().get(0).getIdPartido()).isEqualTo(partido2.getIdPartido());
    }

    @Test
    @DisplayName("buscarConFiltros - Combinación de categoría, estado y fechas")
    void buscarConFiltros_Combinado() {
        Page<Partido> resultado = partidoRepository.buscarConFiltros(
                catSub14.getIdCategoria(), false, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15),
                PageRequest.of(0, 10));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getIdPartido()).isEqualTo(partido1.getIdPartido());
    }

    @Test
    @DisplayName("buscarConFiltros - Paginación respeta límites de página")
    void buscarConFiltros_Paginacion() {
        Page<Partido> pagina0 = partidoRepository.buscarConFiltros(
                null, null, null, null, PageRequest.of(0, 2));

        assertThat(pagina0.getTotalElements()).isEqualTo(3);
        assertThat(pagina0.getTotalPages()).isEqualTo(2);
        assertThat(pagina0.getContent()).hasSize(2);
        assertThat(pagina0.getContent()).extracting(Partido::getIdPartido)
                .containsExactly(partido3.getIdPartido(), partido2.getIdPartido());

        Page<Partido> pagina1 = partidoRepository.buscarConFiltros(
                null, null, null, null, PageRequest.of(1, 2));

        assertThat(pagina1.getContent()).hasSize(1);
        assertThat(pagina1.getContent().get(0).getIdPartido()).isEqualTo(partido1.getIdPartido());
    }

    @Test
    @DisplayName("buscarConFiltros - Sin coincidencias retorna página vacía")
    void buscarConFiltros_SinCoincidencias() {
        Page<Partido> vacio = partidoRepository.buscarConFiltros(
                catSub16.getIdCategoria(), true, null, null, PageRequest.of(0, 10));

        assertThat(vacio.getTotalElements()).isZero();
        assertThat(vacio.getContent()).isEmpty();
    }
}
