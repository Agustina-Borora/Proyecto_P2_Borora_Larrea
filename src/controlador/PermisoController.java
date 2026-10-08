package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.Map;
import modelo.Sesion;

/**
 * Punto único para consultar, en tiempo real, qué nivel de acceso tiene el usuario de la sesión
 * actual ({@link Sesion}) para cada examen -- lo usan {@code RegistrarResultadosScreen} (para no
 * listar órdenes de exámenes sin acceso) y {@code CargarResultadosScreen} (para bloquear del todo
 * o dejar en solo lectura la carga de un examen puntual). Un Administrador siempre tiene "Cargar"
 * en todos los exámenes (mismo bypass que ya usa {@link dao.Usuario#tienePermiso} para las
 * pantallas -- {@code AppShell} llama directo a {@code UsuarioController#obtenerPantallasPermitidas}
 * para ese caso, así que el bypass de pantallas no pasa por acá).
 */
public final class PermisoController {

    private PermisoController() {
    }

    /**
     * Trae, en una sola consulta, el nivel de acceso por examen del usuario de la sesión actual --
     * se pide una vez por pantalla (no una consulta por cada examen que se dibuja).
     */
    public static PermisosExamenSesion obtenerPermisosExamenSesion(Component padre) {
        boolean esAdministrador = "Admin".equalsIgnoreCase(Sesion.rol) || "Administrador".equalsIgnoreCase(Sesion.rol);
        Map<Integer, String> niveles = esAdministrador
                ? Collections.emptyMap()
                : ConexionUtil.ejecutar(padre, "Error al verificar permisos por examen",
                        con -> dao.PermisoExamenDAO.listarNivelesExamen(con, Sesion.idUsuario),
                        Collections.emptyMap());
        return new PermisosExamenSesion(esAdministrador, niveles);
    }

    /**
     * Nivel de acceso de la sesión actual para cada examen, ya resuelto (con el bypass de
     * Administrador incluido).
     */
    public static final class PermisosExamenSesion {
        private final boolean esAdministrador;
        private final Map<Integer, String> niveles;

        private PermisosExamenSesion(boolean esAdministrador, Map<Integer, String> niveles) {
            this.esAdministrador = esAdministrador;
            this.niveles = niveles;
        }

        /** "Sin acceso" / "Solo ver" / "Cargar" -- un examen ausente del mapa es "Sin acceso". */
        public String nivel(int idAnalisisTipo) {
            if (esAdministrador) {
                return "Cargar";
            }
            return niveles.getOrDefault(idAnalisisTipo, "Sin acceso");
        }

        public boolean puedeCargar(int idAnalisisTipo) {
            return "Cargar".equals(nivel(idAnalisisTipo));
        }

        /** true tanto para "Cargar" como para "Solo ver" -- "Sin acceso" es el único nivel que no puede ver nada. */
        public boolean puedeVer(int idAnalisisTipo) {
            String nivel = nivel(idAnalisisTipo);
            return "Cargar".equals(nivel) || "Solo ver".equals(nivel);
        }
    }
}
