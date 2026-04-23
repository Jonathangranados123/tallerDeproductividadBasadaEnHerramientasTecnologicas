package com.inventario_hardware.inventario.repositorio;
import com.inventario_hardware.inventario.model.Pantallas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface PantallaRepository extends JpaRepository<Pantallas, Long> {
    List<Pantallas> findByMarcaContainingIgnoreCaseOrModeloContainingIgnoreCaseOrNumeroDeSerieContainingIgnoreCase(
            String marca, String modelo, String serie
    );
}
