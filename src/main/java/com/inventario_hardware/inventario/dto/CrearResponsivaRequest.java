package com.inventario_hardware.inventario.dto;

import java.util.List;
import java.util.Map;

/**
 * Petición para crear/actualizar una Responsiva .docx y cargarle equipos.
 * Se usa en /api/responsivas/crear-con-items y flujos similares.
 *
 * Notas:
 * - Todos los campos de metadatos son opcionales; el servicio decide si escribe
 *   en tabla o reemplaza marcadores (${persona}, ${area}, etc.).
 * - Si nombreResponsiva es null/vacío, el servicio puede generar uno.
 * - items es la lista de equipos a agregar; cada item requiere {tipo, id}.
 */
public class CrearResponsivaRequest {

    /** Nombre del archivo .docx (ej. "JUAN_PEREZ.docx"). Si viene vacío, el servicio puede generar uno. */
    public String nombreResponsiva;

    /** Si la responsiva no existe, crearla automáticamente. Por defecto: true. */
    public boolean crearNueva = true;

    // ----------------- Metadatos de persona/área/departamento (opcionales) -----------------

    /** Nombre completo del responsable (mapea a columna/marker "Nombre" o ${persona}). */
    public String persona;

    /** Área o gerencia (mapea a "Area" o ${area}). */
    public String area;

    /** Departamento (mapea a "Departamento" o ${departamento}). */
    public String departamento;

    // ----------------- Campos nuevos (tabla extendida / marcadores) -----------------

    /** Puesto del responsable (mapea a "Puesto" o ${puesto}). */
    public String puesto;

    /** Clave interna/nomina/ID empleado (mapea a "Clave" o ${clave}). */
    public String clave;

    /**
     * Fecha a mostrar en la responsiva (ej. "2025-10-07" o texto libre).
     * El servicio no valida formato; se inserta como viene.
     * Mapea a "Fecha" o ${fecha}.
     */
    public String fecha;

    // ----------------- Ítems (equipos) a insertar en la tabla principal -----------------

    /**
     * Lote de equipos a agregar.
     * Cada item debe incluir al menos:
     *   { "tipo": "<categoria>", "id": <long> }
     * Ejemplo:
     *   { "tipo": "pantallas", "id": 12 }
     *
     * El servicio normaliza cada item a un mapa de columnas (convertirCommonAMapa)
     * y puede completar claves como "cantidad"=1 y "equipo"=tipo si faltan.
     */
    public List<Map<String, Object>> items;
}
