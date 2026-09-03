package org.uteq.backend.academico.pago.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {
    boolean existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
            Long idEstudiante, TipoPago tipo, Short anio, Short mes);

    List<Pago> findByEstudiante_IdEstudianteOrderByFechaPagoDesc(Long idEstudiante);

    @Query("""
           SELECT COALESCE(SUM(p.monto), 0) FROM Pago p
            WHERE p.fechaPago BETWEEN :inicio AND :fin
              AND p.anuladoEn IS NULL
           """)
    BigDecimal sumarMontoEntreFechas(LocalDate inicio, LocalDate fin);

    long countByFechaPagoBetweenAndAnuladoEnIsNull(LocalDate inicio, LocalDate fin);

    @Query("""
           SELECT p.estudiante.idEstudiante FROM Pago p
           WHERE p.tipo = :tipo AND p.anio = :anio AND p.mes = :mes
             AND p.anuladoEn IS NULL
           """)
    List<Long> idsConMembresiaCubierta(
            @Param("tipo") TipoPago tipo, @Param("anio") Short anio, @Param("mes") Short mes);

    @Query("""
           SELECT year(p.fechaPago), month(p.fechaPago), SUM(p.monto), COUNT(p)
           FROM Pago p
           WHERE p.fechaPago BETWEEN :desde AND :hasta
             AND p.anuladoEn IS NULL
           GROUP BY year(p.fechaPago), month(p.fechaPago)
           """)
    List<Object[]> totalesPorMesDeCobro(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @EntityGraph(attributePaths = {"estudiante", "estudiante.persona", "registradoPor", "registradoPor.persona"})
    @Query("""
           SELECT p FROM Pago p
           WHERE (:idEstudiante IS NULL OR p.estudiante.idEstudiante = :idEstudiante)
             AND (:tipo IS NULL OR p.tipo = :tipo)
             AND (cast(:fechaDesde as date) IS NULL OR p.fechaPago >= :fechaDesde)
             AND (cast(:fechaHasta as date) IS NULL OR p.fechaPago <= :fechaHasta)
             AND (:anulado IS NULL OR (:anulado = true AND p.anuladoEn IS NOT NULL) OR (:anulado = false AND p.anuladoEn IS NULL))
           ORDER BY p.fechaPago DESC, p.idPago DESC
           """)
    Page<Pago> buscarConFiltros(
            @Param("idEstudiante") Long idEstudiante,
            @Param("tipo") TipoPago tipo,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("anulado") Boolean anulado,
            Pageable pageable);
}
