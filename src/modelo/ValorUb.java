package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo (POJO) para una fila de `valor_ub` -- el historial de precios de la Unidad Bioquímica.
 * Se usa para mostrar el valor vigente (y desde cuándo) en el Catálogo de Exámenes; para el
 * cálculo del precio en sí, el resto del sistema sigue usando {@link
 * dao.ValorUbDAO#obtenerVigente} (que sólo trae el número).
 */
public class ValorUb {

    private int idValorUb;
    private BigDecimal valor;
    private LocalDate vigenteDesde;

    public int getIdValorUb() {
        return idValorUb;
    }

    public void setIdValorUb(int idValorUb) {
        this.idValorUb = idValorUb;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public void setVigenteDesde(LocalDate vigenteDesde) {
        this.vigenteDesde = vigenteDesde;
    }
}
