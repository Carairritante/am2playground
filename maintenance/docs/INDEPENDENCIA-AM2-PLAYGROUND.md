# Independência entre AM2 e Playground — validação local

Resultados de 2 de novembro de 2026. Trabalho de Carairritante com participação
substancial do OpenAI Codex; autores, histórico e licenças originais preservados.

## O que causou o crash

O crash `crash-2026-11-02_01.04.05-client.txt` ocorreu com AM2.5 CR2 e o antigo
Playground `advanced-silver-progression`. Um listener desse addon exigia a classe
`am2.api.events.WorldFusionEvent`, criada no nosso core a partir da CR5. A ausência
da classe derrubava o carregamento do Forge antes do menu.

O TEOF 4.7 fornecido contém CR11 e Playground antigo intencionalmente: era a
referência que funcionava. O SHA do CR11 nele é exatamente o SHA256SUMS publicado
na execução do GitHub Actions 41285874131:

`b34f6fe67b715ebb59806d685fdd53c3338fe67c171285e7eb8c1ea5315796ef`

Portanto esse arquivo do core corresponde ao CI; não era um download trocado.
A cópia do TEOF também iniciou com o antigo Playground mantido quando combinada
com esse CR11. Esse teste não reproduziu ali o crash observado no par CR2/addon
anterior; confirma que a combinação de versões era relevante.

## Limites entre os projetos

| Responsabilidade | Projeto |
| --- | --- |
| Solar/Lunar, Wave, AoE/Zone, Bless, giz e Compêndio nativos | AM2.5 |
| Leitura dos quatro saldos já transmitidos pelo protocolo nativo | AM2.5; o PG também corrige cores antigos quando necessário |
| Contador cinza, mensagem de descoberta e compras Silver somente de talentos | Playground |
| Requisitos e consagração das sete descobertas avançadas | Playground, usando as capacidades nativas do AM2.5 |
| Consulta opcional aos rituais PG pelo giz nativo | Compatibilidade opcional do item AM2.5, sem tornar PG obrigatório |

A CR13 retira os comportamentos Silver incondicionais do core. Seu Oculus volta
aos três contadores nativos quando o Playground mantido não está presente.
O addon aplica seu próprio transformer, protege a compra no cliente e no servidor
e não importa o evento customizado do core. A API genérica do evento é preservada
no AM2.5 para extensões antigas, mas o novo Playground não depende dela.

Conforme a escolha do usuário, o Ars Magica original de Mithion recebe o restante
do Playground e o contador Silver do addon; as sete descobertas avançadas ficam
indisponíveis. A mensagem e o Compêndio explicam a exigência de AM2.5. Tentar usar
uma orb de descoberta avançada nessa base não consome o item nem os pontos.
Não se substituiu World Fusion por nível 70 nem se criou uma progressão alternativa.

## Matriz gráfica mínima

Cada caso iniciou um cliente Forge real, criou um mundo descartável e abriu o
Oculus. O servidor integrado enviou 3/4/5 pontos coloridos e 17 Silver; o cliente
recebeu os quatro saldos. Sem os hooks novos, o contador cinza não é desenhado.

| Base | Playground | Resultado |
| --- | --- | --- |
| AM2.5 CR13 | ausente | PASS |
| AM2.5 CR13 | antigo do TEOF | PASS |
| AM2.5 CR13 | original baixado | PASS |
| AM2.5 LTS 1.6.11-LE original | independent-silver | PASS |
| AM2 original Mithion 1.4.0.009 | independent-silver | PASS |
| AM2.5 CR13 | independent-silver | PASS |
| AM2.5 CR2, sem evento customizado | independent-silver | PASS |

Nos casos com PG corrigido, o teste enviou pacotes reais ao método do servidor:
formas, componentes e modificadores Silver não podem ser comprados; talentos
exigem categoria correta, árvore, pré-requisitos e um ponto, que é descontado
uma única vez. Compras coloridas nativas e habilidades já aprendidas são preservadas.
Mithion registrou `advancedAvailable=false` e zero descobertas avançadas; as bases
LTS registraram todas as partes disponíveis naquela seleção de mods opcionais.

A base Mithion foi obtida da [distribuição oficial AM2 1.4.0.009](https://www.curseforge.com/minecraft/mc-mods/ars-magica-2/files/2280862).
Esta matriz não afirma compatibilidade universal com outras versões ou forks.

## Regressões e TEOF completo

- AM2.5 original, sem WorldFusionEvent customizado: sete descobertas, mana Botania,
  três bruxas no coven, requisitos pessoais e orbs passaram. World Fusion inicial,
  consagração repetida, falha sem consumir materiais e persistência passaram.
- CR13 com novo PG: portal e orb, Void Paradox/caldeirão e regeneração de mana
  passaram nos verificadores existentes, inclusive Archmage e cancelamentos.
- TEOF completo com CR13 e novo PG: menu Cinematic estável, sete descobertas
  registradas, saída zero e `TEOF_STARTUP=PASS`.
- Doze testes Python de recursos/assembly e verificadores ASM de pilha e
  idempotência passaram localmente. Hooks também verificados nas classes reais
  de Mithion, LTS original e CR13.

A variante do Playground fornecida no TEOF possui ajustes próprios para GTNH,
Aether e Lwjgl3ify. Foram preservadas 18 classes byte a byte; o dispatcher recebeu
somente o passe adicional de compatibilidade. O manifesto identifica os arquivos
originais e as alterações, sem atribuir esses ajustes anteriores a esta etapa.

No core, comparado à CR12, somente duas classes mudaram: GuiSkillTrees e SkillData;
as outras classes, incluindo as correções anteriores e APIs genéricas, permanecem
byte a byte iguais. Também foram removidas as duas mensagens Silver do namespace
nativo, substituídas pelas mensagens do próprio Playground.

## Artefatos locais validados

| Arquivo | SHA-256 |
| --- | --- |
| `AM2.5-LTS-1.6.11-LE-CR13.jar` | `ee2c49d0b2f4b95014bb07f496396cd9503813183e4c5e329e7c053ac17850b4` |
| `AM2PlayGround-Beta-0.12.1-independent-silver.jar` | `4087684f23661957bdf87b87ffc2c5e4670e32ba0097d736ba8cf814c0c21a4d` |
| `Compendium-AM2-Playground-PT-BR.zip` | `7295ca953a2876f5ccc62aaab4e16844f3f9148f082cb884b940d76a18a1b4fa` |

Os JARs completos do PG ficam apenas no arquivo privado. O fork público entrega
fontes autorais de manutenção, recursos, traduções e ferramentas. Seu Actions
produz AM2PG-maintenance-tools.jar, que não é um mod instalável. O Actions do AM2.5
produz o mod base e seu SHA256SUMS. Ambos mantêm arquivos separados, sem ZIP externo,
retenção de 14 dias e sem releases automáticas.

Os resultados acima são locais, associados aos hashes. O CI do commit publicado
deve ser conferido separadamente. O JAR refeito pelo CI pode ter outro hash de
empacotamento; vale o checksum daquela execução para o arquivo baixado.

Todos os mundos foram descartáveis. O teste do TEOF cobriu inicialização/menu;
não representa uma sessão prolongada multijogador ou teste de todas as magias.
As regras desta separação estão registradas no AGENTS.md dos dois forks.
