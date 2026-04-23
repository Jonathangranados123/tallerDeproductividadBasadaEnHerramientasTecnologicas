package com.inventario_hardware.inventario.repositorio;

import com.inventario_hardware.inventario.model.Switch_aps;
import com.inventario_hardware.inventario.model.Tablets;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TabletsRepository extends JpaRepository<Tablets, Long> {
}
