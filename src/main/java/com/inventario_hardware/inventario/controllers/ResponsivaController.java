package com.inventario_hardware.inventario.controllers;

import com.inventario_hardware.inventario.dto.CommonEquipo;
import com.inventario_hardware.inventario.dto.CrearResponsivaRequest;
import com.inventario_hardware.inventario.services.DocumentoResponsivaService;
import com.inventario_hardware.inventario.services.EquipoRouterService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Controlador REST para gestionar "Responsivas" (documentos .docx) a partir de equipos
 * existentes en la base de datos. Expone endpoints para:
 *  - Crear/usar una responsiva.
 *  - Agregar 1 o varios equipos a una responsiva.
 *  - Quitar filas por número de serie.
 *  - Actualizar metadatos de persona/área/departamento/puesto/clave/fecha.
 *  - Listar y descargar archivos .docx generados.
 */
@RestController
@RequestMapping("/api/responsivas")
@CrossOrigin(origins = "*") // Permite CORS desde cualquier origen (útil para frontends locales)
public class ResponsivaController {

    // Servicio que encapsula la manipulación del .docx (crear, buscar, escribir tablas/marcadores).
    private final DocumentoResponsivaService doc;

    // Servicio que resuelve cualquier "tipo" de equipo y lo normaliza a CommonEquipo.
    private final EquipoRouterService router;

    public ResponsivaController(DocumentoResponsivaService doc, EquipoRouterService router) {
        this.doc = doc;
        this.router = router;
    }

    /**
     * POST /api/responsivas/agregar
     * Agrega un solo equipo (de cualquier tipo) a una responsiva.
     * - Si la responsiva existe => la usa.
     * - Si no existe y crearNueva=true => la crea y agrega el equipo.
     * - Si no existe y crearNueva=false => 404.
     *
     * Body esperado (ejemplo):
     * {
     *   "tipo": "pantallas",
     *   "id": 1,
     *   "nombreResponsiva": "JUAN_PEREZ.docx",
     *   "crearNueva": true
     * }
     *
     * Respuesta: success, rutaResponsiva, equipo (mapa normalizado), mensaje opcional.
     */
    @PostMapping("/agregar")
    public ResponseEntity<Map<String, Object>> agregar(@RequestBody Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        try {
            // 1) Parseo de parámetros de entrada
            String tipo = String.valueOf(req.get("tipo"));
            Long id = Long.parseLong(String.valueOf(req.get("id")));
            String nombre = String.valueOf(req.get("nombreResponsiva"));
            boolean crearNueva = req.get("crearNueva") != null &&
                    Boolean.parseBoolean(String.valueOf(req.get("crearNueva")));

            // 2) Buscar el equipo en el "router" por tipo+id
            var opt = router.findByTipoAndId(tipo, id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Equipo no encontrado"));
            }

            // 3) Resolver ruta de la responsiva destino (crear si aplica)
            String ruta = doc.buscarResponsiva(nombre);
            if (ruta == null) {
                if (!crearNueva) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("success", false, "error", "Responsiva no encontrada"));
                }
                ruta = doc.crearNuevaResponsiva(nombre);
                res.put("mensaje", "Responsiva creada");
            } else {
                res.put("mensaje", "Usando responsiva existente");
            }

            // 4) Normalizar el equipo a mapa y agregarlo a la tabla del documento
            CommonEquipo c = router.toCommon(tipo, opt.get());
            Map<String, String> datos = doc.convertirCommonAMapa(c);
            doc.agregarEquipoAResponsiva(ruta, datos);

            // 5) Respuesta OK
            res.put("success", true);
            res.put("rutaResponsiva", ruta);
            res.put("equipo", datos);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            // Cualquier excepción => 500 con mensaje
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/responsivas/agregar-multiples
     * Agrega varios equipos a una responsiva en una sola llamada.
     * - items: lista de objetos { "tipo": "...", "id": ... }
     * - Si la responsiva no existe y crearNueva=true => se crea.
     * - Si la responsiva no existe y crearNueva=false => 404.
     *
     * Body ejemplo:
     * {
     *   "nombreResponsiva": "JUAN.docx",
     *   "crearNueva": true,
     *   "items": [ {"tipo":"pantallas","id":1}, {"tipo":"laptops","id":7} ]
     * }
     *
     * Respuesta: success, agregados (count), rutaResponsiva.
     */
    @PostMapping("/agregar-multiples")
    public ResponseEntity<Map<String, Object>> agregarMultiples(@RequestBody Map<String, Object> req) {
        Map<String, Object> res = new HashMap<>();
        try {
            String nombre = String.valueOf(req.get("nombreResponsiva"));
            boolean crearNueva = req.get("crearNueva") != null &&
                    Boolean.parseBoolean(String.valueOf(req.get("crearNueva")));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) req.get("items");

            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "No se enviaron items"
                ));
            }

            String ruta = doc.buscarResponsiva(nombre);
            if (ruta == null) {
                if (!crearNueva) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("success", false, "error", "Responsiva no encontrada"));
                }
                ruta = doc.crearNuevaResponsiva(nombre);
            }

            int ok = 0;
            // Recorre los items, normaliza y agrega uno por uno
            for (Map<String, Object> it : items) {
                String tipo = String.valueOf(it.get("tipo"));
                Long id = Long.parseLong(String.valueOf(it.get("id")));
                var e = router.findByTipoAndId(tipo, id);
                if (e.isPresent()) {
                    CommonEquipo c = router.toCommon(tipo, e.get());
                    Map<String, String> datos = doc.convertirCommonAMapa(c);
                    doc.agregarEquipoAResponsiva(ruta, datos);
                    ok++;
                }
            }

            res.put("success", true);
            res.put("agregados", ok);
            res.put("rutaResponsiva", ruta);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/responsivas/quitar
     * Elimina una fila de la tabla principal de la responsiva buscando por el valor
     * de la columna "numero de serie" (encabezado normalizado en minúsculas).
     *
     * Params:
     * - nombreResponsiva: nombre del archivo .docx
     * - numeroSerie: valor a buscar para eliminar la fila
     *
     * Respuesta: { "success": true/false }
     */
    @DeleteMapping("/quitar")
    public ResponseEntity<Map<String, Object>> quitar(@RequestParam String nombreResponsiva,
                                                      @RequestParam String numeroSerie) {
        try {
            String ruta = doc.buscarResponsiva(nombreResponsiva);
            if (ruta == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Responsiva no encontrada"));
            }

            // Intenta eliminar la fila cuyo "numero de serie" coincida
            boolean ok = doc.eliminarFilaPorValor(ruta, "numero de serie", numeroSerie);
            return ResponseEntity.ok(Map.of("success", ok));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * PUT /api/responsivas/metadatos
     * Actualiza los metadatos de la responsiva (persona/área/departamento/puesto/clave/fecha).
     * Si el documento tiene una tabla con columnas "Nombre | Area | Departamento" la llena.
     * Si no existe, usa marcadores de texto: ${persona}, ${area}, ${departamento}, ${puesto}, ${clave}, ${fecha}.
     *
     * Body ejemplo:
     * {
     *   "nombreResponsiva":"JUAN_PEREZ.docx",
     *   "persona":"Juan Pérez",
     *   "area":"Sistemas",
     *   "departamento":"TI",
     *   "puesto":"Soporte",
     *   "clave":"ABC123",
     *   "fecha":"2025-10-27"
     * }
     */
    @PutMapping("/metadatos")
    public ResponseEntity<Map<String,Object>> actualizarMetadatos(@RequestBody Map<String,String> dto){
        try{
            // nombreResponsiva es obligatorio para localizar el archivo
            String nombre        = dto.get("nombreResponsiva");
            String persona       = dto.getOrDefault("persona", "");
            String area          = dto.getOrDefault("area", "");
            String departamento  = dto.getOrDefault("departamento", "");
            String puesto        = dto.getOrDefault("puesto", "");
            String clave         = dto.getOrDefault("clave", "");
            String fecha         = dto.getOrDefault("fecha", ""); // opcional

            String ruta = doc.buscarResponsiva(nombre);
            if (ruta == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Responsiva no encontrada"));

            // El servicio decide si escribe en tabla o reemplaza marcadores
            doc.actualizarPersonaEnDocumento(ruta, persona, area, departamento, puesto, clave, fecha);

            return ResponseEntity.ok(Map.of("success", true, "rutaResponsiva", ruta));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/responsivas
     * Devuelve la lista de nombres de archivos .docx disponibles en la carpeta configurada.
     */
    @GetMapping
    public ResponseEntity<List<String>> listar() throws IOException {
        return ResponseEntity.ok(doc.listarResponsivas());
    }

    /**
     * GET /api/responsivas/descargar?nombreArchivo=XYZ.docx
     * Descarga el archivo indicado por nombre.
     * - Content-Type: application/octet-stream
     * - Content-Disposition: attachment; filename=...
     */
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargar(@RequestParam String nombreArchivo) throws IOException {
        String ruta = doc.buscarResponsiva(nombreArchivo);
        if (ruta == null) return ResponseEntity.notFound().build();

        File f = new File(ruta);
        byte[] bytes = Files.readAllBytes(f.toPath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", nombreArchivo);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * POST /api/responsivas/crear-con-items
     * Crea (o usa) una responsiva y agrega un lote de equipos usando el DTO CrearResponsivaRequest.
     * Además, actualiza metadatos si vienen en el cuerpo.
     *
     * Body (parcial):
     * {
     *   "nombreResponsiva": "JUAN.docx",
     *   "crearNueva": true,
     *   "persona": "...",
     *   "area": "...",
     *   "departamento": "...",
     *   "puesto": "...",
     *   "clave": "...",
     *   "fecha": "YYYY-MM-DD",
     *   "items": [ {"tipo":"laptops","id":10}, ... ]
     * }
     *
     * Respuesta: success, rutaResponsiva, agregados (conteo).
     */
    @PostMapping("/crear-con-items")
    public ResponseEntity<Map<String, Object>> crearConItems(@RequestBody CrearResponsivaRequest req) {
        try {
            // Validación mínima
            if (req.items == null || req.items.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Debes enviar al menos un item"
                ));
            }

            // Crear/usar responsiva
            String ruta = doc.buscarResponsiva(req.nombreResponsiva);
            if (ruta == null) {
                if (!req.crearNueva) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "success", false,
                            "error", "Responsiva no encontrada y crearNueva=false"
                    ));
                }
                ruta = doc.crearNuevaResponsiva(req.nombreResponsiva);
            }

            // Metadatos (si vienen). El servicio decide tabla/marcadores.
            doc.actualizarPersonaEnDocumento(
                    ruta,
                    req.persona,
                    req.area,
                    req.departamento,
                    req.puesto,     // NUEVO
                    req.clave,      // NUEVO
                    req.fecha       // NUEVO
            );

            // Agregar cada item normalizado a la tabla principal
            int agregados = 0;
            for (Map<String, Object> it : req.items) {
                String tipo = String.valueOf(it.get("tipo"));
                Long id = Long.parseLong(String.valueOf(it.get("id")));
                var opt = router.findByTipoAndId(tipo, id);
                if (opt.isEmpty()) continue;

                var common = router.toCommon(tipo, opt.get());
                var datos = doc.convertirCommonAMapa(common);
                // Encabezados opcionales (si tu plantilla los espera)
                datos.putIfAbsent("cantidad", "1");
                datos.putIfAbsent("equipo", tipo);

                doc.agregarEquipoAResponsiva(ruta, datos);
                agregados++;
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "rutaResponsiva", ruta,
                    "agregados", agregados
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * DTO alternativo compacto para crear una responsiva desde un "lote" simple
     * {tipo,id}, más metadatos básicos. Útil para integraciones ligeras.
     */
    public static class CrearDesdeLoteDTO {
        public String nombreResponsiva;
        public String persona;
        public String area;
        public String departamento;
        public List<Item> items;

        public static class Item {
            public String tipo;
            public Long id;
        }
    }

    /**
     * POST /api/responsivas/crear-desde-lote
     * Variante compacta:
     * 1) Convierte items {tipo,id} en filas normalizadas (Map<String,String>).
     * 2) Aplica metadatos básicos (persona/area/departamento).
     * 3) Genera la responsiva en una sola llamada con ayuda del servicio.
     */
    @PostMapping("/crear-desde-lote")
    public ResponseEntity<?> crearDesdeLote(@RequestBody CrearDesdeLoteDTO body) {
        try {
            if (body.items == null || body.items.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No hay items"));
            }

            // 1) Generar filas normalizadas desde items
            List<Map<String, String>> filas = new ArrayList<>();
            for (CrearDesdeLoteDTO.Item it : body.items) {
                var opt = router.findByTipoAndId(it.tipo, it.id);
                if (opt.isEmpty()) continue;

                var common = router.toCommon(it.tipo, opt.get());
                Map<String, String> fila = doc.convertirCommonAMapa(common);
                fila.putIfAbsent("cantidad", "1");
                fila.putIfAbsent("equipo", it.tipo);
                filas.add(fila);
            }
            if (filas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Ningún item válido"));
            }

            // 2) Metadatos básicos
            Map<String, String> meta = new HashMap<>();
            if (body.persona != null) meta.put("persona", body.persona);
            if (body.area != null) meta.put("area", body.area);
            if (body.departamento != null) meta.put("departamento", body.departamento);

            // 3) Crear y llenar usando helper del servicio
            String ruta = doc.crearResponsivaDesdeLote(body.nombreResponsiva, meta, filas);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "rutaResponsiva", ruta,
                    "agregados", filas.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}

