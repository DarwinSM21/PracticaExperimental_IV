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
import org.uteq.backend.inventario.articulo.entity.Articulo;
import org.uteq.backend.inventario.articulo.entity.Articulo.TipoArticulo;
import org.uteq.backend.inventario.articulo.repository.ArticuloRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ArticuloRepositoryTest {

    @Autowired
    private ArticuloRepository articuloRepository;

    @Autowired
    private TestEntityManager em;

    private Articulo balon;
    private Articulo uniforme;
    private Articulo conoInactivo;
    private Articulo chalecoStockBajo;

    @BeforeEach
    void setUp() {
        balon = em.persist(Articulo.builder()
                .nombre("Balón Golty #4")
                .tipo(TipoArticulo.BALON)
                .stockActual(20)
                .stockMinimo(5)
                .unidadMedida("unidad")
                .activo(true)
                .build());

        uniforme = em.persist(Articulo.builder()
                .nombre("Uniforme Titular M")
                .tipo(TipoArticulo.UNIFORME)
                .talla("M")
                .stockActual(12)
                .stockMinimo(5)
                .unidadMedida("unidad")
                .activo(true)
                .build());

        chalecoStockBajo = em.persist(Articulo.builder()
                .nombre("Chalecos Entrenamiento")
                .tipo(TipoArticulo.IMPLEMENTO)
                .stockActual(3)
                .stockMinimo(10)
                .unidadMedida("unidad")
                .activo(true)
                .build());

        conoInactivo = em.persist(Articulo.builder()
                .nombre("Conos de agilidad rotos")
                .tipo(TipoArticulo.IMPLEMENTO)
                .stockActual(0)
                .stockMinimo(5)
                .unidadMedida("unidad")
                .activo(false)
                .build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByActivoTrue lista solo articulos activos")
    void findByActivoTrue_retornaSoloActivos() {
        List<Articulo> activos = articuloRepository.findByActivoTrue();

        assertThat(activos).hasSize(3)
                .extracting(Articulo::getNombre)
                .containsExactlyInAnyOrder("Balón Golty #4", "Uniforme Titular M", "Chalecos Entrenamiento");
    }

    @Test
    @DisplayName("findByActivoTrue paginado pagina correctamente los activos")
    void findByActivoTrue_paginado() {
        Page<Articulo> pagina = articuloRepository.findByActivoTrue(PageRequest.of(0, 2));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
        assertThat(pagina.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findConStockBajo retorna articulos activos con stockActual <= stockMinimo")
    void findConStockBajo_retornaArticulosConStockIgualOMenor() {
        List<Articulo> bajos = articuloRepository.findConStockBajo();

        assertThat(bajos).hasSize(1);
        assertThat(bajos.get(0).getNombre()).isEqualTo("Chalecos Entrenamiento");
        assertThat(bajos.get(0).getStockActual()).isLessThanOrEqualTo(bajos.get(0).getStockMinimo());
    }

    @Test
    @DisplayName("findConStockBajo excluye articulos inactivos aunque tengan stock bajo")
    void findConStockBajo_excluyeInactivos() {
        List<Articulo> bajos = articuloRepository.findConStockBajo();

        assertThat(bajos).noneMatch(a -> !a.getActivo());
    }
}
