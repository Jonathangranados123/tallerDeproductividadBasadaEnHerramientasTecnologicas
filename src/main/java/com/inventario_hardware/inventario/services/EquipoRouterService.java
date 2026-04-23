package com.inventario_hardware.inventario.services;

import com.inventario_hardware.inventario.dto.CommonEquipo;
import com.inventario_hardware.inventario.repositorio.*;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.thymeleaf.util.StringUtils.contains;

@Service
public class EquipoRouterService {

    // Repositorios reales por categoría/tablas físicas
    private final PantallaRepository pantallas;
    private final CctvRepository cctv;
    private final AccesoriosRepository accesorios;
    private final RadioRepository radio;
    private final Switch_apsRepository switchAps;
    private final TabletsRepository tablets;
    private final TelefonosRepository telefonos;
    private final TerminalesRepository terminalesPv;
    private final Ups_regulador_nobreakRepository ups;
    private final Multifuncionales_impresorasRepository multifuncionales;
    private final Nas_server_pc_laptopsRepository nas;

    public EquipoRouterService(
            PantallaRepository pantallas,
            CctvRepository cctv,
            AccesoriosRepository accesorios,
            RadioRepository radio,
            Switch_apsRepository switchAps,
            TabletsRepository tablets,
            TelefonosRepository telefonos,
            TerminalesRepository terminalesPv,
            Ups_regulador_nobreakRepository ups,
            Multifuncionales_impresorasRepository multifuncionales,
            Nas_server_pc_laptopsRepository nas
    ) {
        this.pantallas = pantallas;
        this.cctv = cctv;
        this.accesorios = accesorios;
        this.radio = radio;
        this.switchAps = switchAps;
        this.tablets = tablets;
        this.telefonos = telefonos;
        this.terminalesPv = terminalesPv;
        this.ups = ups;
        this.multifuncionales = multifuncionales;
        this.nas = nas;
    }

    /* =========================================================
       ===============  MÉTODOS DE LECTURA  ====================
       ========================================================= */

    /**
     * Lista registros por tipo real de tabla con filtro opcional "q".
     * El filtro se aplica en memoria sobre campos comunes (marca/modelo/serie/estado/área/departamento).
     * @param tipo nombre lógico de la tabla (pantallas, cctv, nas_server_pc_laptops, etc.)
     * @param q    texto a buscar (opcional)
     */
    public List<?> buscarPorTipo(String tipo, String q) {
        String T = norm(tipo);
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        // Selecciona el repositorio físico
        List<?> base = switch (T) {
            case "pantallas" -> pantallas.findAll();
            case "cctv" -> cctv.findAll();
            case "accesorios" -> accesorios.findAll();
            case "radio" -> radio.findAll();
            case "switch_aps" -> switchAps.findAll();
            case "tablets" -> tablets.findAll();
            case "telefonos" -> telefonos.findAll();
            case "terminales_pv" -> terminalesPv.findAll();
            case "ups_regulador_nobreak" -> ups.findAll();
            case "multifuncionales_impresoras" -> multifuncionales.findAll();
            case "nas_server_pc_laptops" -> nas.findAll();
            default -> List.of();
        };

        if (query.isEmpty()) return base;

        // Filtro genérico en memoria (para volúmenes medios)
        return base.stream().filter(o -> matches(o, query)).collect(Collectors.toList());
    }

    /**
     * Busca un registro por tipo + id en la tabla real.
     */
    public Optional<Object> findByTipoAndId(String tipo, Long id) {
        String T = norm(tipo);
        return switch (T) {
            case "pantallas" -> pantallas.findById(id).map(e -> (Object) e);
            case "cctv" -> cctv.findById(id).map(e -> (Object) e);
            case "accesorios" -> accesorios.findById(id).map(e -> (Object) e);
            case "radio" -> radio.findById(id).map(e -> (Object) e);
            case "switch_aps" -> switchAps.findById(id).map(e -> (Object) e);
            case "tablets" -> tablets.findById(id).map(e -> (Object) e);
            case "telefonos" -> telefonos.findById(id).map(e -> (Object) e);
            case "terminales_pv" -> terminalesPv.findById(id).map(e -> (Object) e);
            case "ups_regulador_nobreak" -> ups.findById(id).map(e -> (Object) e);
            case "multifuncionales_impresoras" -> multifuncionales.findById(id).map(e -> (Object) e);
            case "nas_server_pc_laptops" -> nas.findById(id).map(e -> (Object) e);
            default -> Optional.empty();
        };
    }

    /** Alias por compatibilidad con controladores existentes. */
    public Optional<Object> obtenerPorTipoId(String tipo, Long id) {
        return findByTipoAndId(tipo, id);
    }

    /**
     * Convierte cualquier entidad concreta a un DTO común {@link CommonEquipo}
     * usando reflexión sobre getters convencionales (getMarca, getModelo, etc.).
     * Esto permite que el resto de capas (QR/Docx) trabajen con un contrato unificado.
     */
    public CommonEquipo toCommon(String tipo, Object entity) {
        CommonEquipo c = new CommonEquipo();
        c.tipo = norm(tipo);
        c.id = (Long) call(entity, "getId");
        c.marca = s(call(entity, "getMarca"));
        c.modelo = s(call(entity, "getModelo"));
        c.numeroDeSerie = s(call(entity, "getNumeroDeSerie"));
        c.estado = s(call(entity, "getEstado"));
        c.area = s(call(entity, "getArea"));
        c.departamento = s(call(entity, "getDepartamento"));
        c.responsable = s(call(entity, "getResponsable")); // tu entidad expone getResponsable()
        c.puesto = s(call(entity, "getPuesto"));
        return c;
    }

    /* =========================================================
       ===============  HELPERS INTERNOS  ======================
       ========================================================= */

    /** Normaliza el tipo a minúsculas sin espacios extremos. */
    private static String norm(String t) { return t == null ? "" : t.trim().toLowerCase(Locale.ROOT); }

    /** Convierte null → "" para comparaciones seguras. */
    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }

    /**
     * Verifica si alguno de los campos comunes contiene el texto buscado (case-insensitive).
     * Usa thymeleaf StringUtils.contains (ya importado) sobre strings pre-normalizados.
     */
    private static boolean matches(Object o, String q) {
        return contains(callStr(o, "getMarca"), q)
                || contains(callStr(o, "getModelo"), q)
                || contains(callStr(o, "getNumeroDeSerie"), q)
                || contains(callStr(o, "getEstado"), q)
                || contains(callStr(o, "getArea"), q)
                || contains(callStr(o, "getDepartamento"), q);
    }

    /** Llama un getter y devuelve String seguro ("" si null). */
    private static String callStr(Object o, String method) {
        Object v = call(o, method);
        return v == null ? "" : String.valueOf(v).toLowerCase(Locale.ROOT);
    }

    /**
     * Invoca un método sin parámetros por reflexión. Si falla, devuelve null (silencioso).
     * Está pensado para getters convencionales presentes en todas las entidades.
     */
    private static Object call(Object o, String method) {
        try {
            Method m = o.getClass().getMethod(method);
            return m.invoke(o);
        } catch (Exception ignored) {
            return null;
        }
    }

    // =========================================================
    //                CRUD (create/update/delete)
    // =========================================================

    /** Crea una entidad del tipo indicado mapeando desde CommonEquipo. */
    public Object crear(String tipo, CommonEquipo dto) {
        String t = norm(tipo);
        Object entity = newEntity(t);
        if (entity == null) throw new IllegalArgumentException("Tipo no soportado: " + tipo);
        mapCommonToEntity(dto, entity);
        return saveByTipo(t, entity);
    }

    /** Actualiza una entidad existente por tipo+id; si no existe, devuelve null. */
    public Object actualizar(String tipo, Long id, CommonEquipo dto) {
        String t = norm(tipo);
        Optional<Object> opt = findByTipoAndId(tipo, id);
        if (opt.isEmpty()) return null;
        Object entity = opt.get();
        mapCommonToEntity(dto, entity);
        return saveByTipo(t, entity);
    }

    /** Elimina una entidad por tipo+id.  true si fue eliminada, false si no existía. */
    public boolean eliminar(String tipo, Long id) {
        String t = norm(tipo);
        switch (t) {
            case "pantallas" -> { if (!pantallas.existsById(id)) return false; pantallas.deleteById(id); return true; }
            case "cctv" -> { if (!cctv.existsById(id)) return false; cctv.deleteById(id); return true; }
            case "accesorios" -> { if (!accesorios.existsById(id)) return false; accesorios.deleteById(id); return true; }
            case "radio" -> { if (!radio.existsById(id)) return false; radio.deleteById(id); return true; }
            case "switch_aps" -> { if (!switchAps.existsById(id)) return false; switchAps.deleteById(id); return true; }
            case "tablets" -> { if (!tablets.existsById(id)) return false; tablets.deleteById(id); return true; }
            case "telefonos" -> { if (!telefonos.existsById(id)) return false; telefonos.deleteById(id); return true; }
            case "terminales_pv" -> { if (!terminalesPv.existsById(id)) return false; terminalesPv.deleteById(id); return true; }
            case "ups_regulador_nobreak" -> { if (!ups.existsById(id)) return false; ups.deleteById(id); return true; }
            case "multifuncionales_impresoras" -> { if (!multifuncionales.existsById(id)) return false; multifuncionales.deleteById(id); return true; }
            case "nas_server_pc_laptops" -> { if (!nas.existsById(id)) return false; nas.deleteById(id); return true; }
            default -> { return false; }
        }
    }

    /* ===== helpers de guardado/instanciación/mapeo ===== */

    /** Persiste una entidad en su repositorio correspondiente según el tipo. */
    private Object saveByTipo(String t, Object entity) {
        return switch (t) {
            case "pantallas" -> pantallas.save((com.inventario_hardware.inventario.model.Pantallas) entity);
            case "cctv" -> cctv.save((com.inventario_hardware.inventario.model.Cctv) entity);
            case "accesorios" -> accesorios.save((com.inventario_hardware.inventario.model.Accesorios) entity);
            case "radio" -> radio.save((com.inventario_hardware.inventario.model.Radio) entity);
            case "switch_aps" -> switchAps.save((com.inventario_hardware.inventario.model.Switch_aps) entity);
            case "tablets" -> tablets.save((com.inventario_hardware.inventario.model.Tablets) entity);
            case "telefonos" -> telefonos.save((com.inventario_hardware.inventario.model.Telefonos) entity);
            case "terminales_pv" -> terminalesPv.save((com.inventario_hardware.inventario.model.Terminales_pv) entity);
            case "ups_regulador_nobreak" -> ups.save((com.inventario_hardware.inventario.model.Ups_regulador_nobreak) entity);
            case "multifuncionales_impresoras" -> multifuncionales.save((com.inventario_hardware.inventario.model.Multifuncionales_impresoras) entity);
            case "nas_server_pc_laptops" -> nas.save((com.inventario_hardware.inventario.model.Nas_server_pc_laptops) entity);
            default -> throw new IllegalArgumentException("Tipo no soportado: " + t);
        };
    }

    /** Instancia una entidad vacía del tipo indicado. */
    private Object newEntity(String t) {
        return switch (t) {
            case "pantallas" -> new com.inventario_hardware.inventario.model.Pantallas();
            case "cctv" -> new com.inventario_hardware.inventario.model.Cctv();
            case "accesorios" -> new com.inventario_hardware.inventario.model.Accesorios();
            case "radio" -> new com.inventario_hardware.inventario.model.Radio();
            case "switch_aps" -> new com.inventario_hardware.inventario.model.Switch_aps();
            case "tablets" -> new com.inventario_hardware.inventario.model.Tablets();
            case "telefonos" -> new com.inventario_hardware.inventario.model.Telefonos();
            case "terminales_pv" -> new com.inventario_hardware.inventario.model.Terminales_pv();
            case "ups_regulador_nobreak" -> new com.inventario_hardware.inventario.model.Ups_regulador_nobreak();
            case "multifuncionales_impresoras" -> new com.inventario_hardware.inventario.model.Multifuncionales_impresoras();
            case "nas_server_pc_laptops" -> new com.inventario_hardware.inventario.model.Nas_server_pc_laptops();
            default -> null;
        };
    }

    /**
     * Rellena una entidad concreta con valores del DTO común.
     * Usa reflexión "suave": si falta un setter, se ignora sin romper.
     */
    private void mapCommonToEntity(CommonEquipo d, Object e) {
        callSet(e, "setMarca", d.marca);
        callSet(e, "setModelo", d.modelo);
        callSet(e, "setNumeroDeSerie", d.numeroDeSerie);
        callSet(e, "setEstado", d.estado);
        callSet(e, "setArea", d.area);
        callSet(e, "setDepartamento", d.departamento);
        callSet(e, "setResponsable", d.responsable);
        callSet(e, "setPuesto", d.puesto);
    }

    /** Busca un setter (1 parámetro) y lo invoca; si no existe, no hace nada. */
    private static void callSet(Object target, String setter, Object val) {
        try {
            var m = Arrays.stream(target.getClass().getMethods())
                    .filter(mm -> mm.getName().equals(setter) && mm.getParameterCount()==1)
                    .findFirst().orElse(null);
            if (m != null) m.invoke(target, val);
        } catch (Exception ignored) {}
    }
}
