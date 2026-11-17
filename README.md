# Runeheart

Add-on para o **Block Clover** (Minecraft Forge 1.16.5) que traz as técnicas de mana do Reino Heart: a **mana
natural** do ambiente e o **Mana Method**, magias montadas com runas e arranjos que tiram a força dessa mana.

> Projeto em começo: já tem a **mana natural** (densidade, esgotamento, HUD e partículas). A absorção e o Mana
> Method chegam nas próximas versões.

## Mana natural (pronta)

Cada chunk tem uma densidade de mana natural de 0 a 100:

| Fator | Efeito |
|---|---|
| Bioma (base) | Selva, floresta de flores, floresta escura: 75 · Cogumelos: 60 · Floresta, taiga: 55 · Pântano: 50 · Rio: 45 · Montanhas: 40 · Planície: 38 · Savana: 35 · Praia, gelo: 25 · Oceano: 20 · Badlands: 15 · Deserto: 10 · Nether: 5 · End: 0 |
| Vegetação | até +20 (folhas, troncos, flores, plantas; grama pura vale pouco) |
| Água corrente ou cachoeira | +10 |
| Tempestade com raios | +15 enquanto durar (não vale no Nether nem no End) |
| Construções | até -30 (tábuas, tijolos, vidro, ferro, redstone, escadas, cercas...) |

- **Esgota e volta:** puxar mana baixa a densidade do chunk, que volta sozinha em 5 minutos. O terreno é recontado a
  cada 5 minutos, então derrubar uma floresta ou construir uma cidade muda a mana do lugar.
- **HUD:** com o Mana Sense do Block Clover ligado, aparece um medidor no canto da tela com a densidade (a barra
  cinza é o cheio do lugar; a verde, o que tem agora) e uma seta quando ela sobe ou desce.
- **Partículas:** onde a densidade passa de 70, um pólen brilhante aparece no ar.
- **Comando** `/runeheart mana`: mostra a densidade de onde você está e as partes da conta. Para testar, operadores
  têm `/runeheart mana drain <quanto>` e `/runeheart mana refill`.

## O que vai ter

- **Absorção:** parado e concentrado, o jogador recupera mana mais rápido em lugares ricos.
- **Mana Method:** runas núcleo (Disparo, Barreira, Selo, Cura, Impulso) e modificadoras (Ampliação, Duração,
  Potência, Gatilho, Múltiplo) montadas em arranjos numa tela com slots. A própria mana só acende o arranjo; a força
  vem da mana natural do lugar.
- **Pergaminhos de runa** achados pelo mundo para aprender runas novas.
- **Arranjos fixos** (até 3 por jogador), como barreiras e armadilhas que ficam ativas enquanto houver mana.

A mana natural só complementa a mana do jogador: as magias do Block Clover continuam gastando a mana dele.

## Instalação

1. Forge 1.16.5 (36.2.x) e o Block Clover.
2. Coloque `runeheart-1.16.5-<versão>.jar` na pasta `mods`.

Servidor e clientes precisam da mesma versão do add-on.

## Configuração

- `<mundo>/serverconfig/runeheart-server.toml`: regras de jogo (o servidor manda uma cópia para cada cliente).
- `config/runeheart-client.toml`: visual e HUD deste computador.

## Compilar

```bash
./gradlew build
```

O `.jar` fica em `build/libs/`. As opções de configuração são geradas a partir de `tools/config/opts.py`
(veja `tools/README.md`).

## Créditos

Inspirado nas técnicas de mana do Reino Heart de *Black Clover*. Não usa personagens, áudio nem arte da obra. O
Block Clover é acessado só por reflexão, sem mudar o mod base.

Licença MIT.
