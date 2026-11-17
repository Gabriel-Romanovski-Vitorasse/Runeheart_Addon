# Changelog

## 0.1.0 (não lançado)

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
