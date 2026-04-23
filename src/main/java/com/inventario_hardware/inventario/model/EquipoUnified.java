package com.inventario_hardware.inventario.model;
// com.inventario_hardware.inventario.model.EquipoUnified


import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@IdClass(EquipoUnifiedId.class)
@Table(name = "equipos_unificados")
public class EquipoUnified {

    @Id private String tipo;
    @Id private Long id;

    private String marca;
    private String modelo;
    @Column(name = "tipo_de_producto")   // <- si la columna es snake_case
    private String tipoDeProducto;
    @Column(name="numero_de_serie")
    private String numeroDeSerie;

    private String estado;
    private String area;
    private String departamento;
    private String responsable;
    private String puesto;

    // getters (y setters si quieres, pero no se usan al ser read-only)
    public String getTipo() { return tipo; }
    public Long getId() { return id; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getNumeroDeSerie() { return numeroDeSerie; }
    public String getEstado() { return estado; }
    public String getArea() { return area; }
    public String getDepartamento() { return departamento; }
    public String getResponsable() { return responsable; }
    public String getPuesto() { return puesto; }
    public String getTipoDeProducto() { return tipoDeProducto; }
    public void setTipoDeProducto(String v) { this.tipoDeProducto = v; }
}
