# Ferramentas e testes de compatibilidade

Trabalho experimental de Carairritante, com participação substancial do OpenAI
Codex. O autor do AM2 Playground é AirBurn0; esta manutenção não é uma versão
oficial do mod. Veja a [declaração de participação](AI_DISCLOSURE.md).

## Escopo e arquivos

- `src/PatchCauldron.java`: acrescenta o registro das quatro runas exigidas pela
  receita de Void Paradox, reutilizando o registrador idempotente do addon.
- `src/PatchPortal.java`: marca portais criados pela fábrica do ritual e acrescenta
  um observador do evento de drops que delega ao novo auxiliar.
- `src/airburn/am2playground/compat/thaumcraft/PGRitualPortalRewards.java`: auxiliar
  novo para a recompensa solicitada, restrita aos portais do ritual.
- `tests/`: verificadores originais da sessão, que consultam as classes reais dos
  mods em um ambiente descartável.
- `compile-helper.py`: compila o auxiliar usando assinaturas mínimas de API apenas
  durante a compilação e exporta somente sua classe como payload.

Estes são arquivos escritos na manutenção. Não incluem fontes decompilados,
classes originais modificadas, dependências, o código completo do addon ou seu JAR.
As permissões para modificar e distribuir o mod original são uma questão separada;
criar um fork não concede autorização adicional. A licença e os avisos originais
dos recursos e do addon não foram substituídos.

## Compilar as ferramentas

Use JDK 17 com destino Java 8 e ASM All 5.0.3. A biblioteca não acompanha o fork.
Exemplos para Windows executados neste diretório:

```powershell
javac --release 8 -cp "C:\dependencias\asm-all-5.0.3.jar" -d build\patchers src\PatchCauldron.java src\PatchPortal.java
python compile-helper.py --javac "C:\Java\jdk-17\bin\javac.exe"
```

Os patchers recebem arquivos de classe, não um modpack inteiro:

```powershell
java -cp "build\patchers;C:\dependencias\asm-all-5.0.3.jar" PatchCauldron entrada.class saida.class
java -cp "build\patchers;C:\dependencias\asm-all-5.0.3.jar" PatchPortal entrada.class saida.class factory
java -cp "build\patchers;C:\dependencias\asm-all-5.0.3.jar" PatchPortal entrada.class saida.class events
```

Para o caldeirão, a classe alvo é `PGWitcheryBrews`. No portal, `factory` atua em
`PGThaumcraftCompat` e `events` em `PGThaumcraftEvents`. Essas ferramentas foram
preparadas para a versão Beta 0.12.1 examinada. Não reaplique uma alteração sobre
uma classe já modificada; o patcher de caldeirão é uma ferramenta de aplicação única.

`compile-helper.py` exporta a classe nova em `build/payload/`. As assinaturas de API
geradas em `build/` são apenas auxiliares de compilação e **não devem acompanhar um
mod ou uma instalação de Minecraft**. Somente a classe exportada é o payload novo.
A montagem de um JAR local é uma etapa separada, não executada por estes exemplos.

## Verificadores e resultados

Os verificadores exigem os mods reais e encerram a JVM após gravar seus resultados.
Execute-os separadamente em servidor Forge 1.7.10 e mundo descartável. O verificador
do portal provoca uma morte real e uma explosão no mundo de teste.

Os testes registrados usaram AM2.5 LE-CR2, Playground Beta 0.12.1, Witchery
0.24.1-evernife-V6 e, para o portal, Thaumcraft 4.2.3.5 e Blood Magic 1.9.12.
O registro está em [RESULTADOS.md](RESULTADOS.md). Não houve validação visual
completa no cliente nem teste de todas as combinações de mods.

A recompensa de sangue é uma adição solicitada, não a restauração de um drop
original confirmado para o Crimson Portal. Ela concede Blood Devastation,
Blood Vortex e 5 pontos azuis, verdes e vermelhos; não concede pontos Silver livres.

## Colaboração

Abra os PRs com destino a `Carairritante/am2playground:correcoes`. Seu fork ou sua
branch de trabalho pode conter as próprias mudanças. Os recursos originais devem
ser mantidos separados das ferramentas novas durante a revisão.

As ferramentas e testes novos deste diretório acompanham CC BY-NC 3.0, sem que essa
declaração altere a licença do material original do autor.

## CI e download dos artefatos

O workflow do GitHub Actions compila as ferramentas e os verificadores em cada
push e PR para `correcoes`; também pode ser iniciado manualmente. A saída fica na
própria execução, em **Artifacts**, com download direto de dois arquivos:

- `AM2PG-maintenance-tools.jar`
- `SHA256SUMS`, contendo o SHA-256 desse JAR

Não há ZIP externo nem release automática por build. O JAR contém apenas os
patchers e o novo auxiliar: não é o mod completo e não deve ser colocado em `mods`.
As dependências de compilação, assinaturas de API e verificadores temporários não
acompanham o JAR. O CI verifica compilação e empacotamento; os testes de integração
no jogo continuam exigindo o ambiente local descrito acima.
