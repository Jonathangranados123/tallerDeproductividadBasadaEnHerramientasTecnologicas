package com.inventario_hardware.inventario.dto;

import java.util.List;
public class ResponsivaDTO {

    // DTO para procesar QR
    public static class ProcesarQRRequest {
        private Long equipoId;
        private String nombreResponsiva;
        private boolean crearNueva;
        private boolean agregarMasEquipos;

        // Getters y Setters
        public Long getEquipoId() { return equipoId; }
        public void setEquipoId(Long equipoId) { this.equipoId = equipoId; }

        public String getNombreResponsiva() { return nombreResponsiva; }
        public void setNombreResponsiva(String nombreResponsiva) { this.nombreResponsiva = nombreResponsiva; }

        public boolean isCrearNueva() { return crearNueva; }
        public void setCrearNueva(boolean crearNueva) { this.crearNueva = crearNueva; }

        public boolean isAgregarMasEquipos() { return agregarMasEquipos; }
        public void setAgregarMasEquipos(boolean agregarMasEquipos) { this.agregarMasEquipos = agregarMasEquipos; }
    }

    // DTO para respuesta de procesamiento
    public static class ProcesarQRResponse {
        private boolean success;
        private String mensaje;
        private String rutaResponsiva;
        private String nombreArchivo;
        private boolean continuarAgregando;

        // Constructor
        public ProcesarQRResponse() {}

        public ProcesarQRResponse(boolean success, String mensaje) {
            this.success = success;
            this.mensaje = mensaje;
        }

        // Getters y Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }

        public String getRutaResponsiva(  ) { return rutaResponsiva; }
        public void setRutaResponsiva(String rutaResponsiva) { this.rutaResponsiva = rutaResponsiva; }

        public String getNombreArchivo() { return nombreArchivo; }
        public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

        public boolean isContinuarAgregando() { return continuarAgregando; }
        public void setContinuarAgregando(boolean continuarAgregando) { this.continuarAgregando = continuarAgregando; }
    }

    // DTO para agregar múltiples equipos
    public static class AgregarMultiplesRequest {
        private String nombreResponsiva;
        private List<Long> equiposIds;
        private boolean crearNueva;

        // Getters y Setters
        public String getNombreResponsiva() { return nombreResponsiva; }
        public void setNombreResponsiva(String nombreResponsiva) { this.nombreResponsiva = nombreResponsiva; }

        public List<Long> getEquiposIds() { return equiposIds; }
        public void setEquiposIds(List<Long> equiposIds) { this.equiposIds = equiposIds; }

        public boolean isCrearNueva() { return crearNueva; }
        public void setCrearNueva(boolean crearNueva) { this.crearNueva = crearNueva; }
    }
}