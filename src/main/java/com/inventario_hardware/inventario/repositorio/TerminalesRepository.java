package com.inventario_hardware.inventario.repositorio;

import com.inventario_hardware.inventario.model.Telefonos;
import com.inventario_hardware.inventario.model.Terminales_pv;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalesRepository extends JpaRepository<Terminales_pv, Long> {
}
