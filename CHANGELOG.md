# Changelog

## 0.1.0 (não lançado)

- **Inscrição de runas nas magias do Block Clover** (todas menos Anti-Magia e Tempo): na tela de runas, escolha uma
  magia equipada e inscreva runas nela. Potência aumenta o dano, Ampliação espalha parte dele, Múltiplo cria cópias
  dos projéteis, Duração aumenta o alcance, Gatilho deixa uma armadilha onde o projétil bate, e Selo, Impulso, Cura e
  Barreira somam efeitos ao acerto. A força vem da mana natural (recarga a cada 2 s, 3 de mana natural por runa).
  O Block Clover não é modificado: o addon reage ao dano de magia (que diz de qual magia veio) e aos projéteis.
- Protocolo de rede 4.
- **Mana Method.** 10 runas (núcleo: Disparo, Barreira, Selo, Cura, Impulso; modificadoras: Ampliação, Duração,
  Potência, Gatilho, Múltiplo). Arranjos montados na tela de arranjos (J), trocados com N e conjurados com Z, com
  canalização de 0,5 s por runa que quebra com dano. A mana do jogador só acende (4% do máximo); a força vem da
  densidade da mana natural, e o arranjo se desfaz abaixo de 10. O Gatilho vira armadilha (até 3 por jogador).
- **Pergaminhos de runa** em baús de estruturas (chance de 25%, lida da config na hora).
- **Maestria:** 2 runas e 3 slots no começo; 3 runas com 50 conjurações; 5 runas e 5 slots com 200.
- Comando `/runeheart runes` (e `grant`, `reset`, `casts` para operadores).
- Teclas padrão sem colidir com o ChronoBubble: a concentração passou de V para R.
- Protocolo de rede 3.
- **Absorção.** Segurando a tecla de concentração (R), parado, o jogador puxa a mana natural para a própria mana:
  3% da mana máxima por segundo com densidade 100, proporcional à densidade, gastando 2 de mana natural por segundo.
  Mexer-se ou levar dano quebra a concentração; abaixo de densidade 10 vem a sobrecarga (náusea e dano).
- **Sintonia:** antes de absorver, é preciso ficar 2 minutos (somados) parado com o Mana Sense ligado num lugar com
  densidade 60 ou mais. Comando `/runeheart attune` (e `grant`/`reset` para operadores).
- Protocolo de rede 2.
- **Mana natural.** Cada chunk tem uma densidade de 0 a 100: base do bioma, mais vegetação (até +20) e água
  corrente (+10), menos construções (até -30), mais +15 em tempestades. Puxar mana esgota o chunk, que volta em 5
  minutos; o que foi puxado é salvo com o mundo.
- Medidor no HUD com o Mana Sense ligado, com seta de tendência; pólen brilhante onde a densidade passa de 70.
- Comando `/runeheart mana` (e `drain`/`refill` para operadores).
- Base do projeto: Forge 1.16.5, mod id `runeheart`, canal de rede (protocolo 1).
- Ponte com o Block Clover por reflexão: ler e mudar a mana, saber se o Mana Sense está ligado e se alguma Mana Zone
  foi aprendida, sincronizar a mana com o cliente. O log de inicialização mostra a versão e o que foi encontrado.
- Configuração gerada a partir de `tools/config/opts.py`, com textos em português e inglês.
- Testes JUnit e build no GitHub Actions.
