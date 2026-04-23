package com.inventario_hardware.inventario.dto;

public class ResponsivaDeleteEquipoDTO {
    private String nombreResponsiva; // solo nombre .docx
    private Long equipoId; // id de Pantallas


    public String getNombreResponsiva() { return nombreResponsiva; }
    public void setNombreResponsiva(String nombreResponsiva) { this.nombreResponsiva = nombreResponsiva; }
    public Long getEquipoId() { return equipoId; }
    public void setEquipoId(Long equipoId) { this.equipoId = equipoId; }
}