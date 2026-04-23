package com.inventario_hardware.inventario.services;

import com.inventario_hardware.inventario.model.Pantallas;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

@Service
public class DocumentoResponsivaService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoResponsivaService.class);

    // ---------------- Normalización ----------------

    /** Regex para eliminar acentos/diacríticos al normalizar encabezados/valores. */
    private static final Pattern NON_ASCII = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Normaliza texto para comparar encabezados/celdas:
     * - NFD (separa acentos) y los remueve
     * - minúsculas
     * - reemplaza signos típicos por espacio
     * - colapsa espacios
     */
    private static String norm(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD);
        t = NON_ASCII.matcher(t).replaceAll("");          // sin acentos
        t = t.toLowerCase(Locale.ROOT);                   // minúsculas
        t = t.replaceAll("[:.;,\\-_/\\\\]", " ");         // limpia signos típicos de encabezado
        t = t.trim().replaceAll("\\s+", " ");            // colapsa espacios
        return t;
    }

    // ---------------- Rutas configurables ----------------

    /** Ruta de la plantilla .docx (se puede sobreescribir por properties). */
    @Value("${app.plantilla.path:C:\\\\Users\\\\jesus\\\\Downloads\\\\carpeta de responsivas\\\\Formato_Responsiva.docx}")
    private String plantillaPath;

    /** Carpeta destino donde se guardan las responsivas generadas. */
    @Value("${app.responsivas.path:C:\\\\Users\\\\jesus\\\\Downloads\\\\carpeta de responsivas\\\\Nueva carpeta}")
    private String responsivasPath;

    public String getResponsivasPath() { return responsivasPath; }

    // =========================================================
    //               CREAR / BUSCAR / LISTAR ARCHIVOS
    // =========================================================

    /**
     * Crea una nueva responsiva (DOCX) desde la plantilla configurada.
     * Si nombreArchivo es null/vacío, genera un nombre con timestamp.
     * @return ruta absoluta del archivo creado
     */
    public String crearNuevaResponsiva(String nombreArchivo) throws IOException {
        Files.createDirectories(Paths.get(responsivasPath));

        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            nombreArchivo = "responsiva_" + ts + ".docx";
        }
        if (!nombreArchivo.endsWith(".docx")) nombreArchivo += ".docx";

        Path origen = Paths.get(plantillaPath);
        Path destino = Paths.get(responsivasPath, nombreArchivo);

        if (!Files.exists(origen)) {
            throw new FileNotFoundException("No se encontró la plantilla en: " + plantillaPath);
        }

        Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        log.info("Responsiva creada desde plantilla: {}", destino);
        return destino.toString();
    }

    /**
     * Devuelve la ruta absoluta de una responsiva por nombre (o null si no existe).
     * Asegura sufijo .docx si faltara.
     */
    public String buscarResponsiva(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return null;
        if (!nombreArchivo.endsWith(".docx")) nombreArchivo += ".docx";
        Path rutaCompleta = Paths.get(responsivasPath, nombreArchivo);
        return Files.exists(rutaCompleta) ? rutaCompleta.toString() : null;
    }

    /** Lista los archivos .docx disponibles en la carpeta configurada. */
    public List<String> listarResponsivas() throws IOException {
        List<String> out = new ArrayList<>();
        Path dir = Paths.get(responsivasPath);
        if (Files.exists(dir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.docx")) {
                for (Path p : stream) out.add(p.getFileName().toString());
            }
        }
        return out;
    }

    // =========================================================
    //                 TABLA PRINCIPAL DE EQUIPOS
    // =========================================================

    /**
     * Agrega una fila de equipo a la tabla principal (detectada por encabezados).
     * Busca primero una fila completamente vacía para reutilizarla; si no hay, crea nueva.
     */
    public void agregarEquipoAResponsiva(String rutaResponsiva, Map<String, String> datosEquipo) throws IOException {
        File archivo = new File(rutaResponsiva);
        if (!archivo.exists()) throw new FileNotFoundException("No se encontró la responsiva: " + rutaResponsiva);

        // Normaliza las llaves para hacer match con encabezados normalizados
        Map<String, String> datosNorm = new HashMap<>();
        for (Map.Entry<String, String> e : datosEquipo.entrySet()) datosNorm.put(norm(e.getKey()), e.getValue());

        try (FileInputStream fis = new FileInputStream(archivo);
             XWPFDocument document = new XWPFDocument(fis)) {

            XWPFTable tabla = localizarTablaPrincipal(document);
            if (tabla == null) throw new IllegalStateException("No se encontró una tabla con encabezados de responsiva.");

            // 1) intenta llenar fila completamente vacía
            if (!llenarFilaVaciaEnTabla(tabla, datosNorm)) {
                // 2) si no hay, crea nueva fila
                agregarNuevaFilaATabla(tabla, datosNorm);
            }

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                document.write(fos);
            }
            log.info("Equipo agregado a responsiva {}", rutaResponsiva);
        }
    }

    /**
     * Elimina la primera fila cuyo valor en la columna indicada coincida con el valor dado.
     * @return true si eliminó una fila; false si no encontró coincidencia.
     */
    public boolean eliminarFilaPorValor(String rutaResponsiva, String headerKey, String value) throws IOException {
        File archivo = new File(rutaResponsiva);
        if (!archivo.exists()) throw new FileNotFoundException("No se encontró la responsiva: " + rutaResponsiva);

        String keyN = norm(headerKey);
        String valN = norm(value);

        try (FileInputStream fis = new FileInputStream(archivo);
             XWPFDocument document = new XWPFDocument(fis)) {

            XWPFTable tabla = localizarTablaPrincipal(document);
            if (tabla == null || tabla.getRows().size() < 2) return false;

            XWPFTableRow headerRow = tabla.getRow(0);
            int targetIdx = indexColumna(headerRow, keyN);
            if (targetIdx < 0) return false;

            for (int rowIndex = 1; rowIndex < tabla.getNumberOfRows(); rowIndex++) {
                XWPFTableRow row = tabla.getRow(rowIndex);
                String cellVal = norm(getCellText(row, targetIdx));
                if (cellVal.equals(valN)) {
                    tabla.removeRow(rowIndex);
                    try (FileOutputStream fos = new FileOutputStream(archivo)) {
                        document.write(fos);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Intenta eliminar por cualquiera de los headers posibles (atajo de conveniencia).
     * Útil cuando el nombre del encabezado varía entre plantillas.
     */
    public boolean eliminarFilaPorCualquierClave(String rutaResponsiva, String value, String... posiblesHeaders) throws IOException {
        for (String h : posiblesHeaders) {
            if (eliminarFilaPorValor(rutaResponsiva, h, value)) return true;
        }
        return false;
    }

    // =========================================================
    //                    REEMPLAZO DE MARCADORES
    // =========================================================

    /**
     * Reemplaza marcadores tipo ${clave} por su valor, tanto en párrafos
     * fuera de tablas como dentro de celdas.
     */
    public void reemplazarMarcadores(String rutaResponsiva, Map<String, String> marcadores) throws IOException {
        File archivo = new File(rutaResponsiva);
        if (!archivo.exists()) throw new FileNotFoundException("No se encontró la responsiva: " + rutaResponsiva);

        try (FileInputStream fis = new FileInputStream(archivo);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Reemplazo en párrafos fuera de tablas
            for (XWPFParagraph p : document.getParagraphs()) reemplazarMarcadoresEnParrafo(p, marcadores);

            // Reemplazo en párrafos dentro de celdas de tablas
            for (XWPFTable t : document.getTables()) {
                for (XWPFTableRow r : t.getRows()) {
                    for (XWPFTableCell c : r.getTableCells()) {
                        for (XWPFParagraph p : c.getParagraphs()) reemplazarMarcadoresEnParrafo(p, marcadores);
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                document.write(fos);
            }
        }
    }

    /** Reemplaza ${clave} → valor dentro de un párrafo (maneja runs recreando el texto completo). */
    private void reemplazarMarcadoresEnParrafo(XWPFParagraph paragraph, Map<String, String> marcadores) {
        String texto = paragraph.getText();
        if (texto == null || texto.isEmpty()) return;

        String nuevo = texto;
        for (Map.Entry<String, String> e : marcadores.entrySet()) {
            String marcador = "${" + e.getKey() + "}";
            if (nuevo.contains(marcador)) nuevo = nuevo.replace(marcador, Objects.toString(e.getValue(), ""));
        }
        if (!nuevo.equals(texto)) {
            while (paragraph.getRuns().size() > 0) paragraph.removeRun(0);
            paragraph.createRun().setText(nuevo);
        }
    }

    // =========================================================
    //                          CONVERSORES
    // =========================================================

    /** Conversor específico para Pantallas → columnas esperadas en la tabla principal. */
    public Map<String, String> convertirPantallaAMapa(Pantallas p) {
        Map<String, String> datos = new HashMap<>();
        datos.put("cantidad", "1");
        datos.put("equipo", "PANTALLA");
        datos.put("marca", Objects.toString(p.getMarca(), ""));
        datos.put("modelo", Objects.toString(p.getModelo(), ""));
        datos.put("numero de serie", Objects.toString(p.getNumeroDeSerie(), ""));
        datos.put("estado del equipo", Objects.toString(p.getEstado(), ""));
        return datos;
    }

    /** Conversor genérico desde CommonEquipo → mapa de columnas para la tabla principal. */
    public Map<String, String> convertirCommonAMapa(com.inventario_hardware.inventario.dto.CommonEquipo c) {
        Map<String, String> datos = new HashMap<>();
        datos.put("cantidad", "1");
        datos.put("equipo", c.tipo != null ? c.tipo.toUpperCase(Locale.ROOT) : "EQUIPO");
        datos.put("marca", c.marca != null ? c.marca : "");
        datos.put("modelo", c.modelo != null ? c.modelo : "");
        datos.put("numero de serie", c.numeroDeSerie != null ? c.numeroDeSerie : "");
        datos.put("estado del equipo", c.estado != null ? c.estado : "");
        // extras (si no existen columnas en la plantilla, serán ignorados)
        datos.put("area", c.area != null ? c.area : "");
        datos.put("departamento", c.departamento != null ? c.departamento : "");
        datos.put("responsable", c.responsable != null ? c.responsable : "");
        datos.put("puesto", c.puesto != null ? c.puesto : "");
        datos.put("id", c.id != null ? String.valueOf(c.id) : "");
        datos.put("tipo", c.tipo != null ? c.tipo : "");
        return datos;
    }

    // =========================================================
    //                 HELPERS TABLAS / ENCABEZADOS
    // =========================================================

    /** Indica si una celda está vacía (normaliza espacios no separables). */
    private static boolean isEmptyCell(XWPFTableCell cell) {
        if (cell == null) return true;
        String txt = cell.getText();
        if (txt == null) return true;
        txt = txt.replace("\u00A0", " ").replace("\u2007", " ").replace("\u202F", " ");
        return txt.trim().isEmpty();
    }

    /** Devuelve el texto de una celda por índice de columna (seguro ante límites). */
    private static String getCellText(XWPFTableRow row, int idx) {
        if (row == null || idx < 0 || idx >= row.getTableCells().size()) return "";
        return row.getCell(idx).getText();
    }

    /**
     * Busca el índice de columna por nombre normalizado.
     * Tolerante a encabezados tipo "numero/número de serie".
     */
    private int indexColumna(XWPFTableRow headerRow, String nombre) {
        String objetivo = norm(nombre);
        for (int i = 0; i < headerRow.getTableCells().size(); i++) {
            String head = norm(getCellText(headerRow, i));
            if (head.equals(objetivo)) return i;
            if (objetivo.contains("numero de serie") && head.contains("numero") && head.contains("serie")) return i;
        }
        return -1;
    }

    /**
     * Localiza la tabla principal de equipos: exige que contenga al menos
     * las columnas “equipo” y “numero de serie”.
     */
    private XWPFTable localizarTablaPrincipal(XWPFDocument doc) {
        for (XWPFTable t : doc.getTables()) {
            if (t.getRows().isEmpty()) continue;
            XWPFTableRow header = t.getRow(0);
            if (header == null) continue;
            int idxEquipo = indexColumna(header, "equipo");
            int idxSerie  = indexColumna(header, "numero de serie");
            if (idxEquipo >= 0 && idxSerie >= 0) return t;
        }
        return null;
    }

    /**
     * Llena la primera fila completamente vacía con los datos dados (match por encabezado normalizado).
     * @return true si llenó una fila vacía, false si no había y se debe crear una nueva.
     */
    private boolean llenarFilaVaciaEnTabla(XWPFTable table, Map<String, String> datosNorm) {
        if (table.getRows().size() < 2) return false;

        XWPFTableRow headerRow = table.getRow(0);
        Map<Integer, String> columnHeaders = new HashMap<>();
        for (int i = 0; i < headerRow.getTableCells().size(); i++) {
            columnHeaders.put(i, norm(getCellText(headerRow, i)));
        }

        for (int r = 1; r < table.getRows().size(); r++) {
            XWPFTableRow row = table.getRow(r);
            boolean empty = true;
            for (XWPFTableCell c : row.getTableCells()) {
                if (!isEmptyCell(c)) { empty = false; break; }
            }
            if (empty) {
                for (int i = 0; i < row.getTableCells().size(); i++) {
                    String key = columnHeaders.get(i);
                    if (key == null) continue;
                    String val = datosNorm.get(key);
                    if (val != null) {
                        XWPFTableCell cell = row.getCell(i);
                        if (cell.getParagraphs().size() > 0) cell.removeParagraph(0);
                        cell.addParagraph().createRun().setText(val);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Crea una nueva fila (mismo número de celdas que el header) y la llena con los datos.
     * Si falta alguna columna en el mapa, escribe vacío.
     */
    private void agregarNuevaFilaATabla(XWPFTable table, Map<String, String> datosNorm) {
        if (table.getRows().isEmpty()) return;

        XWPFTableRow headerRow = table.getRow(0);
        Map<Integer, String> columnHeaders = new HashMap<>();
        for (int i = 0; i < headerRow.getTableCells().size(); i++) {
            columnHeaders.put(i, norm(getCellText(headerRow, i)));
        }

        XWPFTableRow newRow = table.createRow();
        while (newRow.getTableCells().size() < headerRow.getTableCells().size()) newRow.addNewTableCell();

        for (int i = 0; i < newRow.getTableCells().size(); i++) {
            String key = columnHeaders.get(i);
            XWPFTableCell cell = newRow.getCell(i);
            if (cell.getParagraphs().size() > 0) cell.removeParagraph(0);
            String val = (key == null) ? null : datosNorm.get(key);
            cell.addParagraph().createRun().setText(val != null ? val : "");
        }
    }

    // =========================================================
    //                  LOTE + METADATOS (CREAR)
    // =========================================================

    /**
     * Agrega una lista de filas (ya normalizadas a columnas) a la PRIMERA tabla del documento.
     * Si hay filas vacías las reutiliza; de lo contrario, crea nuevas filas.
     */
    public void agregarLoteATabla(String rutaResponsiva, List<Map<String,String>> filas) throws IOException {
        File archivo = new File(rutaResponsiva);
        if (!archivo.exists()) throw new FileNotFoundException("No se encontró la responsiva: " + rutaResponsiva);

        try (FileInputStream fis = new FileInputStream(archivo);
             XWPFDocument document = new XWPFDocument(fis)) {

            if (document.getTables().isEmpty())
                throw new IllegalStateException("La plantilla no contiene tablas");

            XWPFTable table = document.getTables().get(0);

            XWPFTableRow headerRow = table.getRow(0);
            Map<Integer, String> headerIdxToKey = new HashMap<>();
            for (int i = 0; i < headerRow.getTableCells().size(); i++) {
                headerIdxToKey.put(i, norm(headerRow.getCell(i).getText()));
            }

            for (Map<String, String> filaOriginal : filas) {
                // Normaliza llaves de la fila
                Map<String,String> fila = new HashMap<>();
                filaOriginal.forEach((k,v) -> fila.put(norm(k), v));

                if (!llenarFilaVaciaEnTabla(table, fila)) {
                    XWPFTableRow newRow = table.createRow();
                    while (newRow.getTableCells().size() < headerRow.getTableCells().size())
                        newRow.addNewTableCell();

                    for (int i = 0; i < newRow.getTableCells().size(); i++) {
                        String key = headerIdxToKey.get(i);
                        String val = (key == null) ? "" : fila.getOrDefault(key, "");
                        XWPFTableCell cell = newRow.getCell(i);
                        if (cell.getParagraphs().size() > 0) cell.removeParagraph(0);
                        cell.addParagraph().createRun().setText(val == null ? "" : val);
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                document.write(fos);
            }
        }
    }

    /**
     * Crea una responsiva desde cero (copiando la plantilla), aplica metadatos
     * (reemplazo de marcadores) y agrega un lote de filas a la primera tabla.
     * @return ruta absoluta del archivo creado.
     */
    public String crearResponsivaDesdeLote(String nombreArchivo,
                                           Map<String,String> metadatos,
                                           List<Map<String,String>> filas) throws IOException {
        String ruta = crearNuevaResponsiva(nombreArchivo);
        if (metadatos != null && !metadatos.isEmpty()) {
            reemplazarMarcadores(ruta, metadatos);
        }
        agregarLoteATabla(ruta, filas);
        return ruta;
    }

    // =========================================================
    //                 TABLA DE PERSONA (META)
    // =========================================================

    /** Busca el índice de una columna por nombre objetivo; tolera variaciones para "fecha". */
    private int findColumnIndex(Map<Integer, String> idxToHeader, String target) {
        String tgt = norm(target);
        for (Map.Entry<Integer, String> e : idxToHeader.entrySet()) {
            if (Objects.equals(e.getValue(), tgt)) return e.getKey();
        }
        // tolerancia especial para "fecha" (p.ej. "fecha de asignacion")
        if ("fecha".equals(tgt)) {
            for (Map.Entry<Integer, String> e : idxToHeader.entrySet()) {
                if (e.getValue().contains("fecha")) return e.getKey();
            }
        }
        return -1;
    }

    /**
     * Identifica una tabla de metadatos de persona: requiere al menos
     * columnas "nombre", "area" y "departamento" en el header.
     */
    private XWPFTable findPersonaTable(XWPFDocument document) {
        Set<String> minNeeded = Set.of("nombre", "area", "departamento");

        for (XWPFTable t : document.getTables()) {
            if (t.getRows().isEmpty()) continue;

            XWPFTableRow header = t.getRow(0);
            Set<String> cols = new HashSet<>();
            for (XWPFTableCell c : header.getTableCells()) {
                cols.add(norm(c.getText()));
            }
            if (cols.containsAll(minNeeded)) return t; // puede tener puesto/clave/fecha extra
        }
        return null;
    }

    /**
     * Llena/actualiza la tabla de persona (si existe). Si no existe, devuelve false.
     * Escribe valores en la segunda fila (crea si no existe).
     */
    public boolean actualizarTablaPersona(String rutaResponsiva,
                                          String persona, String area, String departamento,
                                          String puesto, String clave, String fecha) throws IOException {
        File archivo = new File(rutaResponsiva);
        if (!archivo.exists()) throw new FileNotFoundException("No se encontró la responsiva: " + rutaResponsiva);

        try (FileInputStream fis = new FileInputStream(archivo);
             XWPFDocument document = new XWPFDocument(fis)) {

            XWPFTable personaTable = findPersonaTable(document);
            if (personaTable == null) return false;

            // Asegura al menos dos filas: header + data
            while (personaTable.getRows().size() < 2) personaTable.createRow();

            XWPFTableRow header = personaTable.getRow(0);
            XWPFTableRow data   = personaTable.getRow(1);

            Map<Integer, String> idxToHeader = new HashMap<>();
            for (int i = 0; i < header.getTableCells().size(); i++) {
                idxToHeader.put(i, norm(header.getCell(i).getText()));
            }

            // Helper para setear por nombre de header
            BiConsumer<String, String> set = (hdr, val) -> {
                int idx = findColumnIndex(idxToHeader, hdr);
                if (idx >= 0) {
                    while (data.getTableCells().size() <= idx) data.addNewTableCell();
                    XWPFTableCell cell = data.getCell(idx);
                    if (cell.getParagraphs().size() > 0) cell.removeParagraph(0);
                    cell.addParagraph().createRun().setText(val != null ? val : "");
                }
            };

            set.accept("nombre", persona);
            set.accept("area", area);
            set.accept("puesto", puesto);
            set.accept("departamento", departamento);
            set.accept("clave", clave);
            set.accept("fecha", fecha);

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                document.write(fos);
            }
            return true;
        }
    }

    /**
     * Escribe metadatos de persona: intenta tabla; si no existe, usa marcadores
     * (${persona}, ${area}, ${departamento}, ${puesto}, ${clave}, ${fecha}).
     */
    public void actualizarPersonaEnDocumento(String rutaResponsiva,
                                             String persona, String area, String departamento,
                                             String puesto, String clave, String fecha) throws IOException {
        boolean okTabla = actualizarTablaPersona(rutaResponsiva, persona, area, departamento, puesto, clave, fecha);
        if (!okTabla) {
            Map<String,String> m = new HashMap<>();
            m.put("persona", persona != null ? persona : "");
            m.put("area", area != null ? area : "");
            m.put("departamento", departamento != null ? departamento : "");
            m.put("puesto", puesto != null ? puesto : "");
            m.put("clave", clave != null ? clave : "");
            m.put("fecha", fecha != null ? fecha : "");
            reemplazarMarcadores(rutaResponsiva, m);
        }
    }

    /** Versión corta (compatibilidad): sólo 3 campos. */
    public void actualizarPersonaEnDocumento(String rutaResponsiva,
                                             String persona, String area, String departamento) throws IOException {
        actualizarPersonaEnDocumento(rutaResponsiva, persona, area, departamento, null, null, null);
    }
}
