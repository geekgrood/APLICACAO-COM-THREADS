package registo;

public final class Leitura {

    private final String sensor;
    private final double valor;
    private final String unidade;
    private final long instante;

    public Leitura(String sensor, double valor, String unidade, long instante) {
        this.sensor = sensor;
        this.valor = valor;
        this.unidade = unidade;
        this.instante = instante;
    }

    public String getSensor() {
        return sensor;
    }

    public double getValor() {
        return valor;
    }

    public String getUnidade() {
        return unidade;
    }

    public long getInstante() {
        return instante;
    }
}
