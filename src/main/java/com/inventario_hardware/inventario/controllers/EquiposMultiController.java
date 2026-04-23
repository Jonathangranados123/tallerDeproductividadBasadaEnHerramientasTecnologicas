package com.inventario_hardware.inventario.controllers;


import com.inventario_hardware.inventario.dto.CommonEquipo;
import com.inventario_hardware.inventario.model.EquipoUnified;
import com.inventario_hardware.inventario.repositorio.EquipoUnifiedRepository;
import com.inventario_hardware.inventario.services.EquipoRouterService;
import com.inventario_hardware.inventario.util.QRGenerator;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

/**
 * Controlador genérico para CRUD de equipos por "tipo".
 * Delegamos la lógica específica al EquipoRouterService
 * y añadimos la búsqueda unificada sobre EquipoUnified.
 */
@RestController
@RequestMapping("/api/equipos")
@CrossOrigin(origins = "*")
public class EquiposMultiController {

    private final EquipoRouterService router;
    private final EquipoUnifiedRepository repo; // ⬅️ NUEVO

    public EquiposMultiController(EquipoRouterService router,
                                  EquipoUnifiedRepository repo) { // ⬅️ NUEVO
        this.router = router;
        this.repo   = repo;
    }

    // =========================
    // LISTAR / CONSULTAR
    // =========================

    /**
     * GET /api/equipos/unificado?q=...  (ó ?tipo=...)
     * - Si llega "tipo", lista por tipo desde la vista unificada.
     * - Si llega "q", busca en marca/modelo/serie/tipo.
     * - Si no llega nada, devuelve [].
     */
    @GetMapping("/unificado")
    public List<EquipoUnified> buscarUnificado(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo
    ){
        if (tipo != null && !tipo.isBlank()) {
            return repo.findByTipoIgnoreCase(tipo);
        }
        if (q == null || q.isBlank()) {
            return Collections.emptyList();
        }
        // Busca en marca, modelo, numeroDeSerie y tipo (o cambia a tipoDeProducto si así se llama)
        // ...
        return repo.findByMarcaContainingIgnoreCaseOrModeloContainingIgnoreCaseOrNumeroDeSerieContainingIgnoreCaseOrTipoDeProductoContainingIgnoreCase(
                q, q, q, q
        );

    }

    /**
     * GET /api/equipos/{tipo}?q=texto
     */
    @GetMapping("/{tipo}")
    public List<?> listarPorTipo(
            @PathVariable String tipo,
            @RequestParam(required = false) String q
    ){
        return router.buscarPorTipo(tipo, q);
    }
    /**
     * GET /api/equipos/{tipo}/{id}
     */
    @GetMapping("/{tipo}/{id}")
    public ResponseEntity<?> obtener(@PathVariable String tipo, @PathVariable Long id){
        return router.obtenerPorTipoId(tipo, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // CREAR / ACTUALIZAR / ELIMINAR
    // =========================
    @PostMapping("/{tipo}")
    public ResponseEntity<?> crear(@PathVariable String tipo, @RequestBody CommonEquipo dto){
        Object saved = router.crear(tipo, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{tipo}/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String tipo, @PathVariable Long id, @RequestBody CommonEquipo dto){
        Object updated = router.actualizar(tipo, id, dto);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{tipo}/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String tipo, @PathVariable Long id){
        boolean ok = router.eliminar(tipo, id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // =========================
    // GENERACIÓN DE QR
    // =========================
    @GetMapping(value = "/{tipo}/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String tipo, @PathVariable Long id) {
        return router.findByTipoAndId(tipo, id).map(entity -> {
            try {
                var c = router.toCommon(tipo, entity);
                String payload = String.format(
                        "{\"id\":%d,\"tipo\":\"%s\",\"marca\":\"%s\",\"modelo\":\"%s\",\"serie\":\"%s\"}",
                        c.id, c.tipo != null ? c.tipo : "",
                        c.marca != null ? c.marca : "",
                        c.modelo != null ? c.modelo : "",
                        c.numeroDeSerie != null ? c.numeroDeSerie : ""
                );
                BufferedImage img = QRGenerator.generateQRImage(payload, 250, 250);
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(img, "png", baos);
                    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(baos.toByteArray());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
            }
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]));
    }
}
