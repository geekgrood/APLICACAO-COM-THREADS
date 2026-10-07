package central;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import registo.Leitura;
import registo.RegistoCentral;
import registo.RegistoCentral.EstatisticaSensor;
import sensores.Sensor;

public class SistemaCentral {

    private static final Locale LOCALE = Locale.forLanguageTag("pt-PT");
    private static final String LINHA_DUPLA = "================================================================";
    private static final String LINHA_SIMPLES = "----------------------------------------------------------------";

    private final RegistoCentral registo = new RegistoCentral();
    private final List<Sensor> sensores = new ArrayList<>();
    private final List<Thread> threads = new ArrayList<>();
    private long inicio;

    private void configurarSensores() {
        sensores.add(new Sensor("TEMP-01", "°C", 18.0, 27.0, 500, registo));
        sensores.add(new Sensor("HUM-01", "%", 40.0, 60.0, 800, registo));
        sensores.add(new Sensor("PRES-01", "hPa", 1000.0, 1025.0, 1000, registo));
    }

    private void arrancarSensores() {
        for (Sensor sensor : sensores) {
            Thread thread = new Thread(sensor, "Sensor-" + sensor.getNome());
            threads.add(thread);
        }
        inicio = System.nanoTime();
        for (Thread thread : threads) {
            thread.start();
        }
    }

    public void executar() {
        configurarSensores();
        arrancarSensores();
        executarMenu();
        try {
            encerrar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Encerramento interrompido; relatório não emitido.");
        }
    }

    private void executarMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true;
        while (continuar) {
            System.out.println();
            System.out.println("[1] Últimas leituras  [2] Estatísticas parciais  [0] Encerrar");
            System.out.print("Opção: ");
            if (!scanner.hasNextLine()) {
                System.out.println();
                System.out.println("Fim do input: encerramento controlado.");
                continuar = false;
            } else {
                String opcao = scanner.nextLine().trim();
                switch (opcao) {
                    case "1":
                        mostrarUltimasLeituras();
                        break;
                    case "2":
                        mostrarEstatisticas();
                        break;
                    case "0":
                        continuar = false;
                        break;
                    default:
                        System.out.println("Opção inválida: '" + opcao + "'.");
                        break;
                }
            }
        }
    }

    private void mostrarUltimasLeituras() {
        List<Leitura> ultimas = registo.getUltimasLeituras();
        if (ultimas.isEmpty()) {
            System.out.println("Ainda não existem leituras.");
            return;
        }
        for (Leitura l : ultimas) {
            System.out.println(String.format(LOCALE, "%-10s %8.1f %s", l.getSensor(), l.getValor(), l.getUnidade()));
        }
    }

    private void mostrarEstatisticas() {
        List<EstatisticaSensor> lista = registo.getEstatisticas();
        if (lista.isEmpty()) {
            System.out.println("Ainda não existem leituras.");
            return;
        }
        System.out.println(String.format(LOCALE, "%-10s %8s %8s %8s %8s  %s",
                "Sensor", "Leituras", "Mínimo", "Máximo", "Média", "Unidade"));
        for (EstatisticaSensor e : lista) {
            System.out.println(formatarEstatistica(e));
        }
    }

    private String formatarEstatistica(EstatisticaSensor e) {
        return String.format(LOCALE, "%-10s %8d %8.1f %8.1f %8.1f  %s",
                e.getSensor(), e.getContagem(), e.getMinimo(), e.getMaximo(), e.getMedia(), e.getUnidade());
    }

    private void encerrar() throws InterruptedException {
        System.out.println("Encerramento coordenado iniciado...");
        long pedido = System.nanoTime();
        for (Sensor sensor : sensores) {
            sensor.parar();
        }
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long fim = System.nanoTime();
        boolean todasTerminadas = true;
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                todasTerminadas = false;
            }
        }
        emitirRelatorio(pedido, fim, todasTerminadas);
    }

    private void emitirRelatorio(long pedido, long fim, boolean todasTerminadas) {
        double duracaoMonitorizacao = (pedido - inicio) / 1_000_000_000.0;
        long duracaoEncerramento = (fim - pedido) / 1_000_000;
        System.out.println(LINHA_DUPLA);
        System.out.println("RELATÓRIO FINAL DE MONITORIZAÇÃO");
        System.out.println(LINHA_DUPLA);
        System.out.println(String.format(LOCALE, "Duração da monitorização : %.1f s", duracaoMonitorizacao));
        System.out.println("Duração do encerramento : " + duracaoEncerramento + " ms");
        System.out.println(LINHA_SIMPLES);
        System.out.println(String.format(LOCALE, "%-10s %8s %8s %8s %8s  %s",
                "Sensor", "Leituras", "Mínimo", "Máximo", "Média", "Unidade"));
        for (EstatisticaSensor e : registo.getEstatisticas()) {
            System.out.println(formatarEstatistica(e));
        }
        System.out.println(LINHA_SIMPLES);
        System.out.println("Total de leituras (contador) : " + registo.getContador());
        System.out.println("Soma das leituras por sensor : " + registo.getSomaContagens());
        System.out.println("Registos no histórico : " + registo.getTamanhoHistorico());
        System.out.println("Verificação de consistência : " + (registo.verificarConsistencia() ? "OK" : "FALHA"));
        System.out.println("Estado final das threads :");
        for (Thread thread : threads) {
            System.out.println("  " + thread.getName() + " " + thread.getState());
        }
        System.out.println("Todas as threads terminadas : " + (todasTerminadas ? "SIM" : "NÃO"));
        System.out.println(LINHA_DUPLA);
    }
}
