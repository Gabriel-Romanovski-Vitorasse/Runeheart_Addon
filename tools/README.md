# Ferramentas

## Configuração (`tools/config`)

`RuneConfig.java` e as chaves `config.runeheart.*` dos arquivos de idioma são **gerados**. Para criar ou mudar uma
opção, edite `tools/config/opts.py` (nome, padrão, limites, textos pt_br/en_us) e rode, na raiz do repositório:

```bash
python3 tools/config/genconfig.py src/main/java/com/gabriel/runeheart/RuneConfig.java src/main/resources/assets/runeheart/lang
```

Não edite `RuneConfig.java` à mão: ele é sobrescrito.
