# TP4 – Aplicação com threads: Sensores

**Formando:** NOME APELIDO

**java -version:**
```
openjdk version "21.0.10" 2026-01-20
OpenJDK Runtime Environment (build 21.0.10+7-Ubuntu-124.04)
OpenJDK 64-Bit Server VM (build 21.0.10+7-Ubuntu-124.04, mixed mode, sharing)
```

## Compilação (a partir da pasta monitorizacao/)

```
javac -d bin src/*.java src/central/*.java src/registo/*.java src/sensores/*.java
```

## Execução

```
java -cp bin Principal
```

## Arquitetura

- `Principal`: ponto de entrada; delega no `SistemaCentral`.
- `SistemaCentral`: configura (`configurarSensores()`), cria e arranca uma `Thread` por sensor, executa o menu, conduz o graceful shutdown (sinalizar, `join()`, verificar `isAlive()`) e emite o relatório final.
- `Sensor` (`Runnable`): tarefa periódica; gera valor e intervalo com `Random`, regista no `RegistoCentral`, ecoa na consola e pausa com `Thread.sleep()` fora de qualquer secção crítica.
- `Leitura`: objeto imutável (sensor, valor, unidade, instante).
- `RegistoCentral`: único recurso partilhado; histórico, contador, estatísticas por sensor e últimas leituras. O monitor (lock) que o protege é o próprio objeto `RegistoCentral` (`this`), através de métodos `synchronized`. As consultas devolvem cópias defensivas.
- `RegistoCentral.EstatisticaSensor` (classe auxiliar aninhada, imutável): cópia das estatísticas de um sensor devolvida pelas consultas, evitando expor o estado interno.

## Justificação técnica do mecanismo de interrupção

Foi usada a combinação de flag `volatile` (`ativo`, alterada por `Sensor.parar()`) com `interrupt()`. (a) A escrita num campo `volatile` tem relação happens-before com a leitura seguinte desse campo, pelo que o sensor vê sempre o pedido de paragem feito pela thread principal. (b) Se o sensor estiver bloqueado em `Thread.sleep()`, o `interrupt()` faz com que o `sleep()` lance `InterruptedException` de imediato; o `catch` restaura o estado de interrupção com `Thread.currentThread().interrupt()` e o ciclo termina, sem esperar o fim da pausa. A leitura em curso é sempre concluída porque a condição só é testada no início de cada iteração. (c) A latência de encerramento medida no relatório foi de 30 ms, muito abaixo do limite de 2 s e coerente com o mecanismo: sem o `interrupt()` poderia chegar a 1,2 s (pausa máxima do PRES-01), com ele depende apenas do desbloqueio do `sleep()` e do `join()`. (d) `Thread.stop()` não é válida porque liberta à força todos os monitores detidos pela thread, podendo deixar o `RegistoCentral` a meio de `registar()` (histórico, estatísticas e contador inconsistentes), além de estar removida das versões recentes do JDK.

## Evidência de teste (execução real de 10 s)

```
Encerramento coordenado iniciado...
[Sensor-HUM-01] terminado de forma controlada (13 leituras).
[Sensor-PRES-01] terminado de forma controlada (10 leituras).
[Sensor-TEMP-01] terminado de forma controlada (21 leituras).
================================================================
RELATÓRIO FINAL DE MONITORIZAÇÃO
================================================================
Duração da monitorização : 10,0 s
Duração do encerramento : 30 ms
----------------------------------------------------------------
Sensor     Leituras   Mínimo   Máximo    Média  Unidade
HUM-01           13     41,7     59,1     53,2  %
PRES-01          10   1000,5   1023,5   1013,2  hPa
TEMP-01          21     18,5     26,5     21,0  °C
----------------------------------------------------------------
Total de leituras (contador) : 44
Soma das leituras por sensor : 44
Registos no histórico : 44
Verificação de consistência : OK
Estado final das threads :
  Sensor-TEMP-01 TERMINATED
  Sensor-HUM-01 TERMINATED
  Sensor-PRES-01 TERMINATED
Todas as threads terminadas : SIM
================================================================
```
