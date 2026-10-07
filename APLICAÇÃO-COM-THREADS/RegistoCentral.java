package registo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RegistoCentral {

    public static final class EstatisticaSensor {

        private final String sensor;
        private final String unidade;
        private final int contagem;
        private final double minimo;
        private final double maximo;
        private final double soma;

        public EstatisticaSensor(String sensor, String unidade, int contagem, double minimo, double maximo, double soma) {
            this.sensor = sensor;
            this.unidade = unidade;
            this.contagem = contagem;
            this.minimo = minimo;
            this.maximo = maximo;
            this.soma = soma;
        }

        public String getSensor() {
            return sensor;
        }

        public String getUnidade() {
            return unidade;
        }

        public int getContagem() {
            return contagem;
        }

        public double getMinimo() {
            return minimo;
        }

        public double getMaximo() {
            return maximo;
        }

        public double getMedia() {
            return contagem == 0 ? 0.0 : soma / contagem;
        }
    }

    private static final class Acumulador {
        private final String unidade;
        private int contagem;
        private double minimo;
        private double maximo;
        private double soma;

        private Acumulador(String unidade) {
            this.unidade = unidade;
        }

        private void acumular(double valor) {
            if (contagem == 0) {
                minimo = valor;
                maximo = valor;
            } else {
                if (valor < minimo) {
                    minimo = valor;
                }
                if (valor > maximo) {
                    maximo = valor;
                }
            }
            soma += valor;
            contagem++;
        }
    }

    private final List<Leitura> historico = new ArrayList<>();
    private final Map<String, Acumulador> estatisticas = new TreeMap<>();
    private final Map<String, Leitura> ultimas = new TreeMap<>();
    private int contador;

    public synchronized void registar(Leitura leitura) {
        historico.add(leitura);
        Acumulador acumulador = estatisticas.get(leitura.getSensor());
        if (acumulador == null) {
            acumulador = new Acumulador(leitura.getUnidade());
            estatisticas.put(leitura.getSensor(), acumulador);
        }
        acumulador.acumular(leitura.getValor());
        ultimas.put(leitura.getSensor(), leitura);
        contador++;
    }

    public synchronized List<Leitura> getUltimasLeituras() {
        return new ArrayList<>(ultimas.values());
    }

    public synchronized List<EstatisticaSensor> getEstatisticas() {
        List<EstatisticaSensor> copia = new ArrayList<>();
        for (Map.Entry<String, Acumulador> e : estatisticas.entrySet()) {
            Acumulador a = e.getValue();
            copia.add(new EstatisticaSensor(e.getKey(), a.unidade, a.contagem, a.minimo, a.maximo, a.soma));
        }
        return copia;
    }

    public synchronized List<Leitura> getHistorico() {
        return new ArrayList<>(historico);
    }

    public synchronized int getContador() {
        return contador;
    }

    public synchronized int getSomaContagens() {
        int total = 0;
        for (Acumulador a : estatisticas.values()) {
            total += a.contagem;
        }
        return total;
    }

    public synchronized int getTamanhoHistorico() {
        return historico.size();
    }

    public synchronized boolean verificarConsistencia() {
        int soma = 0;
        for (Acumulador a : estatisticas.values()) {
            soma += a.contagem;
        }
        return contador == soma && soma == historico.size();
    }
}
