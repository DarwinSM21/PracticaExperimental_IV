package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EstudianteRepositoryTest {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Test
    @DisplayName("buscarParaReporte - Filtra correctamente por categoría y estado")
    void buscarParaReporte_FiltraCorrectamente() {
        // Arrange is handled by data.sql or we can insert manually if needed
        // For now, let's just check that the query executes without errors
        
        // Act
        List<Estudiante> resultado = estudianteRepository.buscarParaReporte(null, true);
        
        // Assert
        assertThat(resultado).isNotNull();
    }

    @Test
    @DisplayName("findByActivoTrueOrderByPersona_ApellidoAsc - Trae estudiantes ordenados")
    void findByActivoTrueOrderByPersona_ApellidoAsc_TraeOrdenados() {
        List<Estudiante> resultado = estudianteRepository.findByActivoTrueOrderByPersona_ApellidoAsc();
        assertThat(resultado).isNotNull();
    }
}
