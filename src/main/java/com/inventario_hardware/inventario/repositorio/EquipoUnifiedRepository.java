package com.inventario_hardware.inventario.repositorio;

import com.inventario_hardware.inventario.model.EquipoUnified;
import com.inventario_hardware.inventario.model.EquipoUnifiedId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipoUnifiedRepository extends JpaRepository<EquipoUnified, EquipoUnifiedId> {

    // /api/equipos/unificado?tipo=xxx  (filtra por el campo "tipo")
    List<EquipoUnified> findByTipoIgnoreCase(String tipo);

    // /api/equipos/unificado?q=...  (ahora busca también en tipoDeProducto)
    List<EquipoUnified> findByMarcaContainingIgnoreCaseOrModeloContainingIgnoreCaseOrNumeroDeSerieContainingIgnoreCaseOrTipoDeProductoContainingIgnoreCase(
            String marca, String modelo, String numeroDeSerie, String tipoDeProducto
    );
}
