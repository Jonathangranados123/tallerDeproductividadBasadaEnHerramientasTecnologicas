package com.inventario_hardware.inventario.repositorio;

import com.inventario_hardware.inventario.model.Pantallas;
import com.inventario_hardware.inventario.model.Radio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RadioRepository extends JpaRepository<Radio, Long> {
}
