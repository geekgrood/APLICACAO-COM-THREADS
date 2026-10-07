package sensores;

import java.util.Locale;
import java.util.Random;
import registo.Leitura;
import registo.RegistoCentral;

public class Sensor implements Runnable {

    private final String nome;
    private final String unidade;
    private final double minimo;
    private final double maximo;
    private final long intervaloBase;
    private final RegistoCentral registo;
    private final Random random = new Random();
    private volatile boolean ativo = true;
    private volatile int leituras;

    public Sensor(String nome, String unidade, double minimo, double maximo, long intervaloBase, RegistoCentral registo) {
        this.nome = nome;
        this.unidade = unidade;
        this.minimo = minimo;
        this.maximo = maximo;
        this.intervaloBase = intervaloBase;
        this.registo = registo;
    }

    public String getNome() {
        return nome;
    }

    public int getLeituras() {
        return leituras;
    }

    public void parar() {
        ativo = false;
    }

    @Override
    public void run() {
        String nomeThread = Thread.currentThread().getName();
        while (ativo && !Thread.currentThread().isInterrupted()) {
            double valor = minimo + random.nextDouble() * (maximo - minimo);
            Leitura leitura = new Leitura(nome, valor, unidade, System.currentTimeMillis());
            registo.registar(leitura);
            leituras++;
            System.out.println(String.format(Locale.forLanguageTag("pt-PT"),
                    "[%s] %s = %.1f %s", nomeThread, nome, valor, unidade));
            try {
                Thread.sleep(calcularIntervalo());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("[" + nomeThread + "] terminado de forma controlada (" + leituras + " leituras).");
    }

    private long calcularIntervalo() {
        double fator = 0.8 + random.nextDouble() * 0.4;
        long intervalo = Math.round(intervaloBase * fator);
        return Math.max(1L, intervalo);
    }
}
