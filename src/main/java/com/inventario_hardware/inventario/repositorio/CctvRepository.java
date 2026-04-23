package com.inventario_hardware.inventario.repositorio;

import com.inventario_hardware.inventario.model.Cctv;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CctvRepository   extends JpaRepository<Cctv, Long> {}