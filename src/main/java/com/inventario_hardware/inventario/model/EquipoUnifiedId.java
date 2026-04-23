package com.inventario_hardware.inventario.model;
// com.inventario_hardware.inventario.model.EquipoUnifiedId

    import java.io.Serializable;
    import java.util.Objects;

    public class EquipoUnifiedId implements Serializable {
        private String tipo;
        private Long id;
        // equals/hashCode
        @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof EquipoUnifiedId)) return false;
            EquipoUnifiedId that=(EquipoUnifiedId)o; return Objects.equals(tipo,that.tipo)&&Objects.equals(id,that.id); }
        @Override public int hashCode(){ return Objects.hash(tipo,id); }
    }
