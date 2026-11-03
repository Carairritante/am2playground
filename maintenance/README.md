# Ferramentas e testes de compatibilidade

Trabalho experimental de Carairritante, com participação substancial do OpenAI
Codex. O autor do AM2 Playground é AirBurn0; esta manutenção não é uma versão
oficial do mod. Veja a [declaração de participação](AI_DISCLOSURE.md).

## Escopo e arquivos

O JAR jogável atual chama-se `AM2PlayGround-Beta-0.12.1-CR2.jar` e é entregue
pelo Actions público, junto do SHA256SUMS. A CR2 recompila os auxiliares de
manutenção sobre a base verificada da CR1. Cada atualização posterior
publicada deve incrementar o número CR, mantendo o nome original do mod e
atualizando [VERSION.json](VERSION.json), manifestos, README e SHA256SUMS.
O JAR `AM2PG-maintenance-tools.jar` continua sendo somente ferramenta interna.

Execute `python maintenance/ci.py` e depois `python maintenance/build_playable.py`
na raiz do repositório, com JDK 17 e Python 3.9 ou superior. A saída jogável fica
em `maintenance/build/playable-dist`. Alterações nos auxiliares e nos recursos
são incorporadas na montagem; as outras entradas da base são preservadas.
Atualize a revisão antes de publicar uma nova versão com conteúdo diferente.

A versão atual acrescenta descobertas avançadas com iniciação pessoal World Fusion,
nível mágico e desafios por habilidade. Nullify exige o Ender Guardian; Flowering
Nature verifica 200.000 de mana carregada e Coven Connection verifica três bruxas
do coven real. As consultas não alteram Botania nem Witchery.

O Compendium mostra somente requisitos extras, em um Guia acessível desde o início
e nas entradas das sete partes. As combinações não são publicadas no livro.
Orbs com habilidades pendentes são bloqueadas antes de qualquer consumo ou ponto.
Requisitos de aquisição não são exigidos novamente a cada conjuração depois de aprender.
Mana Link e os desbloqueios originais necessários à iniciação continuam disponíveis.

As descobertas avançadas exigem AM2.5 com World Fusion e registros pessoais de
guardiões nativos. Nenhuma CR personalizada ou evento novo do core é exigido.
No AM2 original de Mithion, o restante do Playground continua funcionando;
essas descobertas avançadas ficam indisponíveis com aviso explícito, sem
consumir orbes ou materiais. A capacidade é detectada pela API, não pelo nome
do arquivo ou número de versão.
As quatro regressões também passaram com AM2.5 CR6, sem alteração do JAR do addon.
O Giz do Arquiteto Arcano pertence ao [fork do AM2.5](https://github.com/Carairritante/ArsMagica2/tree/correcoes),
onde estão suas fontes e instruções. A consagração
final repete a estrutura e os reagentes normais de World Fusion, com uma Nether
Star e um Void Paradox adicionais no inventário. A iniciação original não muda.

- `src/PatchProgression.java`: hooks de inicialização, conjuração, aprendizado e uso de orb.
- `src/airburn/am2playground/progression/`: regras e coordenação da progressão.
- `merge_compendium.py` e `resources/assets/am2pg/docs/`: mesclagem de requisitos
  próprios ao compêndio local, sem incluir o corpo original no fork público.
- `tests/progressionprobe/`: fronteiras de requisitos, sete descobertas, consumo
  de orbs e realização dos dois rituais em ambiente descartável.

A correção da regeneração impede um ciclo infinito nos eventos de mana,
reproduzido após aprender Mana Regen Archmage. Na manutenção atual, o próprio
Playground reserva os pontos Silver livres para talentos; magias e modificadores
continuam pelos seus métodos de descoberta ou recompensa especial. Essa regra
foi retirada do AM2.5 na CR13 e migrou para os hooks do addon.

O painel de pontos usa a moldura e as cores nativas, com azul/verde na primeira
linha e vermelho/Silver na segunda. O próprio addon observa o anúncio do canal
`am2pg` pelo servidor e limpa a informação ao desconectar. Sem esse anúncio,
retorna ao painel nativo de três saldos. Valores grandes são abreviados, com o
saldo completo na tooltip. Essa mudança não altera a mesa de inscrição nem exige
uma CR personalizada do mod base. Os testes e seus limites constam em
[RESULTADOS.md](RESULTADOS.md).

- `src/PatchManaRegen.java`: conecta o reparo à saída do transformador original.
- `src/airburn/am2playground/preloader/ManaRegenTransformFix.java`: encaminha as
  etapas canceladas para a próxima etapa, preservando a duração ajustada.
- `src/airburn/am2playground/utils/ManaRegenEvents.java`: transporta cancelamento
  e duração separadamente pelo sinal do retorno; evita duração zero/negativa.
- `tests/regenprobe/RegenProbe.java`: reproduz o ciclo anterior e verifica Archmage,
  cancelamento em cada etapa, duração zero e a supressão de regeneração do Atronach.

- `src/PatchCauldron.java`: acrescenta o registro das quatro runas exigidas pela
  receita de Void Paradox, reutilizando o registrador idempotente do addon.
- `src/PatchPortal.java`: marca portais criados pela fábrica do ritual e acrescenta
  um observador do evento de drops que delega ao novo auxiliar.
- `src/PatchSilverOrb.java`: acrescenta os hooks de uso e descrição da recompensa
  Silver em `ItemResource`, sem incluir a implementação original no repositório.
- `src/airburn/am2playground/compat/thaumcraft/PGRitualPortalRewards.java`: auxiliar
  novo para a recompensa solicitada, restrita aos portais do ritual.
- `src/airburn/am2playground/items/PGSilverOrbRewards.java`: concede um ponto
  Silver livre por orb marcada, no servidor, mantendo o consumo normal do item.
- `resources/`: somente as duas novas mensagens, em inglês e português.
- `tests/`: verificadores originais da sessão, que consultam as classes reais dos
  mods em um ambiente descartável.
- `compile-helper.py`: compila o auxiliar usando assinaturas mínimas de API apenas
  durante a compilação e exporta somente sua classe como payload.

Os fontes de manutenção são autorais e não incluem fontes decompilados do addon.
A base binária verificada fica em `base/` para a montagem automática do mod jogável.
As permissões para modificar e distribuir o mod original são uma questão separada;
criar um fork não concede autorização adicional. A licença e os avisos originais
dos recursos e do addon não foram substituídos.

## Compilar as ferramentas

Use JDK 17 com destino Java 8 e ASM All 5.0.3. A biblioteca não acompanha o fork.
Exemplos para Windows executados neste diretório:

```powershell
javac --release 8 -cp "C:\dependencias\asm-all-5.0.3.jar" -d build\patchers src\PatchCauldron.java src\PatchPortal.java src\PatchSilverOrb.java src\PatchManaRegen.java src\PatchProgression.java
python compile-helper.py --javac "C:\Java\jdk-17\bin\javac.exe" --asm "C:\dependencias\asm-all-5.0.3.jar" --forge "C:\dependencias\forge-1.7.10-1614.jar"
```

Os patchers recebem arquivos de classe, não um modpack inteiro:

```powershell
java -cp "build\patchers;C:\dependencias\asm-all-5.0.3.jar" PatchCauldron entrada.class saida.class
java -cp "build\patchers;C:\dependencias\asm-all-5.0.3.jar" PatchPortal entrada.class saida.class factory
java -cp "build\patchers;C:\dependencias\asm-all-5.0.3.jar" PatchPortal entrada.class saida.class events
java -cp "build\patchers;C:\dependencias\asm-all-5.0.3.jar" PatchSilverOrb ItemResource-original.class ItemResource-corrigida.class
java -cp "build\patchers;C:\dependencias\asm-all-5.0.3.jar" PatchManaRegen ExtendedPropertiesTransformer-original.class ExtendedPropertiesTransformer-corrigida.class
java -cp "build\patchers;C:\dependencias\asm-all-5.0.3.jar" PatchProgression entrada.class saida.class init
```

Para o caldeirão, a classe alvo é `PGWitcheryBrews`. No portal, `factory` atua em
`PGThaumcraftCompat` e `events` em `PGThaumcraftEvents`. Essas ferramentas foram
preparadas para a versão Beta 0.12.1 examinada. Não reaplique uma alteração sobre
uma classe já modificada; o patcher de caldeirão é uma ferramenta de aplicação única.

Para acrescentar o reparo da regeneração ao JAR local que já possui os rituais e
a orb Silver corrigidos, use `PatchManaRegen` no transformador
`airburn/am2playground/preloader/transformers/am2/ExtendedPropertiesTransformer.class`
e acrescente os dois novos payloads `ManaRegenTransformFix` e `ManaRegenEvents`.
Não substitua a classe de jogo ExtendedProperties por esse arquivo: o alvo é o
transformador do addon, e o reparo é aplicado na próxima inicialização do jogo.

Na progressão, `PatchProgression` tem quatro modos: `init` em AM2PG, `cast` em
PGCommonEventHandler, `learn` em PGLearnHelper e `orb` em ItemResource. Ele exige
classes sem esses hooks anteriores. Acrescente também as duas classes do payload
`progression/`, mescle as mensagens novas e execute `merge_compendium.py` sobre
uma cópia do JAR. Nunca inclua as assinaturas de API geradas para compilação.

`compile-helper.py` exporta as classes novas em `build/payload/`. As assinaturas de API
geradas em `build/` são apenas auxiliares de compilação e **não devem acompanhar um
mod ou uma instalação de Minecraft**. Somente as classes exportadas são payload novo.
A montagem de um JAR local é uma etapa separada, não executada por estes exemplos.

## Verificadores e resultados

Os verificadores exigem os mods reais e encerram a JVM após gravar seus resultados.
Execute-os separadamente em servidor Forge 1.7.10 e mundo descartável. O verificador
do portal provoca uma morte real e uma explosão no mundo de teste.

Os testes originais usaram AM2.5 LE-CR2; a recompensa Silver requer AM2.5 LE-CR3.
O ambiente usa Playground Beta 0.12.1, Witchery
0.24.1-evernife-V6 e, para o portal, Thaumcraft 4.2.3.5 e Blood Magic 1.9.12.
O registro está em [RESULTADOS.md](RESULTADOS.md). Não houve validação visual
completa no cliente nem teste de todas as combinações de mods.

A recompensa do Crimson Portal ritual é uma adição solicitada, não a restauração
de um drop original confirmado. A versão atual concede **somente 1 ponto Silver
livre**, sem pontos coloridos e sem ensinar habilidades. Orbs marcadas de versões
anteriores também seguem essa regra ao serem usadas. A variante vazia do NEI
continua sem recompensa. O modelo de orb do outro boss permanece inalterado.
A recompensa Silver não exige Blood Magic carregado.

Para montar o patch local Silver sobre o JAR que já tem os dois rituais corrigidos,
aplique `PatchSilverOrb` em `ItemResource`, substitua o auxiliar do portal e acrescente
o auxiliar do item, ambos de `build/payload/`. Mescle as duas novas mensagens dos
arquivos em `resources/` com as traduções existentes, preservando as outras chaves.
Não inclua os stubs. Na manutenção atual, o cliente e o servidor precisam do AM2.5
CR5 ou mais recente para a progressão atual e o evento de extensão da consagração.

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
patchers, os novos auxiliares e suas mensagens: não é o mod completo e não deve ser colocado em `mods`.
As dependências de compilação, assinaturas de API e verificadores temporários não
acompanham o JAR. O CI verifica compilação e empacotamento; os testes de integração
no jogo continuam exigindo o ambiente local descrito acima.


## Fronteira entre os mods

A extensão da consagração é aplicada pelo transformer do Playground ao Mana Link.
O início original de World Fusion continua no AM2.5, e somente a cerimônia
adicional pertence ao addon. O Playground não importa nem registra um listener
para `am2.api.events.WorldFusionEvent`.

O contador Silver, a dica de descoberta e a regra de comprar somente talentos
são sobreposições do Playground. Ele confere árvore, pré-requisitos, pontos e
categoria no servidor antes de qualquer gasto. Habilidades já aprendidas não
são removidas. Sem o addon, o AM2.5 mantém sua interface e regras nativas.

O reparo genérico da quarta leitura do saldo de pontos permanece no AM2.5,
pois o protocolo original já transmite quatro valores. O transformer também
corrige essa leitura nos cores originais que ainda a omitem e detecta a correção
existente para não ler o valor duas vezes.

`PatchIndependence` acrescenta o novo passe ao dispatcher original do Playground,
preservando os outros transforms e variantes de compatibilidade. O passe é
idempotente e atua apenas nas três classes necessárias. Os testes sintéticos
`IndependentTransformChecks` verificam pilha, hooks e idempotência sem incluir
implementações de outros mods. A matriz com clientes reais é validada à parte.

## Montagem privada com uma variante do modpack

`assemble_independent.py` combina um JAR local já corrigido, a versão original
correspondente e, opcionalmente, uma variante do colaborador. O script compara
as classes antes de compor: se a variante e nossa manutenção mudarem a mesma
classe de maneiras diferentes, a montagem para e exige revisão. Assim os
ajustes de Aether, GTNH e Lwjgl3ify não se perdem numa troca de JAR.

Depois de compilar as ferramentas com `ci.py`, execute com caminhos locais:

```text
python assemble_independent.py --patched /privado/advanced-silver-progression.jar --original /privado/Playground-original.jar --compat-source /privado/Playground-do-modpack.jar --output /privado/Playground-independent-silver.jar
```

`--java`, `--asm`, `--payload` e `--patchers` permitem informar as ferramentas
explicitamente. `--compat-source` é opcional. Os arquivos de entrada não são
alterados, e a saída deve ficar fora deste repositório público. Para substituir
uma montagem anterior, informe `--overwrite` expressamente.

A montagem preserva os patches anteriores, aplica o hook ao dispatcher escolhido,
inclui somente os oito payloads autorais previstos e atualiza os requisitos do
Compendium. A linguagem é mesclada por chave, sem acumular traduções duplicadas.
Esse fluxo legado gera uma montagem local, manifesto e checksum. O fluxo atual
do Actions usa `build_playable.py` e entrega o JAR jogável com checksum separado.
