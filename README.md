# Runeheart

Add-on para o **Block Clover** (Minecraft Forge 1.16.5) que traz as técnicas de mana do Reino Heart: a **mana
natural** do ambiente e o **Mana Method**, magias montadas com runas e arranjos que tiram a força dessa mana.

> Projeto em começo: esta versão tem só a base (projeto, configuração e ponte com o Block Clover). As técnicas
> chegam nas próximas versões.

## O que vai ter

- **Mana natural:** cada região tem uma densidade de mana de 0 a 100, conforme o bioma, a vegetação, a água, o clima
  e as construções em volta. Usar a mana de um lugar esgota esse lugar por alguns minutos.
- **Medidor no HUD:** com o Mana Sense do Block Clover ligado, mostra a densidade do lugar.
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
