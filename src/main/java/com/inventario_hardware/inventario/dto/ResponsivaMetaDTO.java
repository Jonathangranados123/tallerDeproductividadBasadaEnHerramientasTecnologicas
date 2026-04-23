package com.inventario_hardware.inventario.dto;
public class ResponsivaMetaDTO {
    private String nombreResponsiva; // solo nombre: ej. "JUAN_PEREZ.docx"
    private String persona;
    private String area;
    private String departamento;


    // getters/setters
    public String getNombreResponsiva() { return nombreResponsiva; }
    public void setNombreResponsiva(String nombreResponsiva) { this.nombreResponsiva = nombreResponsiva; }
    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
}


