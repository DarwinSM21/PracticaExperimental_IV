package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PersonaRepositoryTest {

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private TestEntityManager em;

    private Persona p1;
    private Persona p2Inactiva;

    @BeforeEach
    void setUp() {
        p1 = em.persist(Persona.builder()
                .nombre("Andrea").apellido("Salazar").cedula("0508887771")
                .correo("andrea.salazar@test.com").fechaNacimiento(LocalDate.of(1995, 3, 14))
                .activo(true).build());

        p2Inactiva = em.persist(Persona.builder()
                .nombre("Jorge").apellido("Morales").cedula("0508887772")
                .correo("jorge.inactivo@test.com").fechaNacimiento(LocalDate.of(1992, 7, 22))
                .activo(false).build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByCedulaAndActivoTrue encuentra persona activa por cedula")
    void findByCedulaAndActivoTrue_encuentraSoloActiva() {
        Optional<Persona> optActiva = personaRepository.findByCedulaAndActivoTrue("0508887771");
        Optional<Persona> optInactiva = personaRepository.findByCedulaAndActivoTrue("0508887772");

        assertThat(optActiva).isPresent();
        assertThat(optActiva.get().getNombre()).isEqualTo("Andrea");
        assertThat(optInactiva).isEmpty();
    }

    @Test
    @DisplayName("existsByCedulaAndActivoTrue solo es true para personas activas")
    void existsByCedulaAndActivoTrue_comportamiento() {
        assertThat(personaRepository.existsByCedulaAndActivoTrue("0508887771")).isTrue();
        assertThat(personaRepository.existsByCedulaAndActivoTrue("0508887772")).isFalse();
        assertThat(personaRepository.existsByCedulaAndActivoTrue("0000000000")).isFalse();
    }

    @Test
    @DisplayName("existeOtraPersonaConCedula detecta colision excluyendo el propio ID")
    void existeOtraPersonaConCedula_detectaColision() {
        // Misma cédula de p1, pero excluyendo id de p1 -> false (no hay otra)
        boolean otraConMismoId = personaRepository.existeOtraPersonaConCedula("0508887771", p1.getIdPersona());
        // Misma cédula de p1, pero excluyendo id ficticio -> true (p1 colisiona)
        boolean otraConOtroId = personaRepository.existeOtraPersonaConCedula("0508887771", -1L);

        assertThat(otraConMismoId).isFalse();
        assertThat(otraConOtroId).isTrue();
    }

    @Test
    @DisplayName("existeOtraPersonaConCorreo detecta colision de correo excluyendo propio ID")
    void existeOtraPersonaConCorreo_detectaColision() {
        boolean colisionPropia = personaRepository.existeOtraPersonaConCorreo("andrea.salazar@test.com", p1.getIdPersona());
        boolean colisionTercero = personaRepository.existeOtraPersonaConCorreo("andrea.salazar@test.com", -1L);

        assertThat(colisionPropia).isFalse();
        assertThat(colisionTercero).isTrue();
    }
}
