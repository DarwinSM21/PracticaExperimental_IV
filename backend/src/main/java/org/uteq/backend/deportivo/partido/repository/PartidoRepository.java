package org.uteq.backend.deportivo.partido.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.partido.entity.Partido;

import java.time.LocalDate;
import java.util.Optional;

public interface PartidoRepository extends JpaRepository<Partido, Long> {
    @EntityGraph(attributePaths = "categoria")
    Page<Partido> findAllByOrderByFechaDescHoraDesc(Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Partido> findByCategoria_IdCategoriaOrderByFechaDescHoraDesc(Long idCategoria, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Optional<Partido> findWithCategoriaByIdPartido(Long idPartido);

    @EntityGraph(attributePaths = "categoria")
    @Query("""
           SELECT p FROM Partido p
           WHERE (:idCategoria IS NULL OR p.categoria.idCategoria = :idCategoria)
             AND (:cerrado IS NULL OR p.cerrado = :cerrado)
             AND (cast(:fechaDesde as date) IS NULL OR p.fecha >= :fechaDesde)
             AND (cast(:fechaHasta as date) IS NULL OR p.fecha <= :fechaHasta)
           ORDER BY p.fecha DESC, p.hora DESC
           """)
    Page<Partido> buscarConFiltros(
            @Param("idCategoria") Long idCategoria,
            @Param("cerrado") Boolean cerrado,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            Pageable pageable);
}
