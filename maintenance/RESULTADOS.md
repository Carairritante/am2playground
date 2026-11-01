# Resultados registrados nos testes locais

Registro publicado em 31 de outubro de 2026. Os resultados abaixo são da sessão
local de manutenção; não são resultados de CI deste fork.

## Regressão adicional — AM2.5 CR6

Em 31/10/2026, o AM2.5 CR6 foi compilado com o novo Giz do Arquiteto Arcano.
O JAR advanced-silver-progression do Playground permaneceu idêntico ao já testado.
Quatro ensaios de servidor Forge em mundos descartáveis passaram novamente:

```text
ADVANCED_SILVER_PROGRESSION=PASS
PORTAL_PATCH=PASS
MANA_REGEN_FIX=PASS
PATCHED_INTEGRATION=PASS
```

Isso inclui os requisitos pessoais e as sete descobertas, World Fusion original,
consagração final, orb de um ponto Silver, regeneração de mana e Void Paradox.
O giz foi validado separadamente no AM2.5: 30 rituais em quatro orientações,
montagem exclusiva do criativo e conferência sem consumo. A aparência do cliente
continua dependendo de conferência visual dentro do jogo.

## Progressão avançada — AM2.5 CR5

Passaram no servidor Forge com as APIs reais de Botania e Witchery:

- World Fusion obrigatório para os sete alvos; Mana Link isento dessa exigência.
- Fronteiras de nível mágico de cada parte e guardiões pessoais corretos.
- Coven com duas bruxas recusado e com três aceito.
- 199.999 de mana recusados e 200.000 aceitos; soma de inventário e Baubles.
- Orb bloqueada preserva item e pontos; orb apta ensina e consome corretamente.
- Partes em estágios separados, mana insuficiente e evento cancelado não descobrem.
- Sete descobertas e evento real de conjuração; repetição não gera pontos livres.
- Descobertas não gastam a mana de Botania nem removem bruxas do coven.
- Consagração uma vez, consumo exato das duas oferendas extras e persistência.
- World Fusion original realizado normalmente; repetição incompleta preserva
  círculo e reagentes; repetição apta realiza a consagração e consome os materiais.

Testes de XML verificam preservação, idempotência, capítulo desbloqueado e ausência
das combinações no Compendium. O teste do pacote e os ensaios anteriores de portal,
orb de um ponto, regeneração e caldeirão foram executados novamente.

## Orb do portal: somente um ponto cinza

A recompensa mais recente substitui os extras descritos nas seções históricas.
Os testes executaram uma morte real de portal e o uso da orb obtida:

```text
RITUAL_ORB_PAYLOAD_SILVER_ONLY=PASS
ACTUAL_ORB_USE_SILVER_BEFORE=0; AFTER=1
ACTUAL_ORB_USE_NO_COLORED_POINTS=PASS
ACTUAL_ORB_USE_NO_ABILITIES=PASS
LEGACY_MARKED_ORB_SILVER_ONLY=PASS
BLOOD_BOSS_TEMPLATE_UNCHANGED=PASS
SILVER_REWARD_WITHOUT_BLOOD_MAGIC_FLAG=PASS
PORTAL_PATCH=PASS
```

Também passaram consumo por unidade, bloqueio de pontos para personagem não iniciado,
proteção contra duplicação, preservação da pérola, restrição de compras a talentos,
aprendizado por recompensas especiais e sincronização/salvamento dos quatro saldos.
O teste de Blood Magic ausente altera temporariamente a flag da integração, não
desinstala esse mod. Os patches de caldeirão e regeneração foram preservados byte a byte.

## Travamento de regeneração e compras — AM2.5 CR4

O ciclo foi reproduzido em processo descartável com a versão anterior:
`ORIGINAL_ARCHMAGE_INFINITE_LOOP_REPRODUCED=PASS`.
Com o reparo, a mesma chamada retorna e preserva a redução de duração do Archmage,
sem aplicar também a redução de Mana Regen III.

```text
ARCHMAGE_RETURNS_WITH_45_PERCENT_DURATION=PASS
CANCELED_ARMORMATERIAL_FORWARD_AND_DURATION=PASS
CANCELED_SKILLDATA_FORWARD_AND_DURATION=PASS
CANCELED_ARMORINFUSION_FORWARD_AND_DURATION=PASS
ZERO_DURATION_FINITE_AND_NO_LOOP=PASS
ATRONACH_REGEN_SUPPRESSION_PRESERVED=PASS
MANA_REGEN_FIX=PASS
SILVER_SPELLS_AND_MODIFIERS_BLOCKED_UI_AND_SERVER=PASS
SILVER_SPENT_ON_TALENT=PASS
DIRECT_DISCOVERY_AND_SPECIAL_REWARDS_PRESERVED=PASS
```

O portal, o consumo da orb, o ponto Silver extra e o caldeirão de Void Paradox
passaram novamente. A política CR4 substitui a compra livre de habilidades testada
na CR3 abaixo; habilidades já salvas não são apagadas.

## Recompensa Silver livre — AM2.5 CR3

Em servidor Forge descartável, o portal foi morto por um jogador simulado e a orb
real do drop foi usada. Foram conferidos o saldo, as habilidades, o consumo,
a compra de Level e o ciclo de serialização dos quatro pontos.

```text
ACTUAL_PLAYER_KILL_ORB=PASS
MAGIC_LEVEL_ZERO_REJECTED=PASS
ACTUAL_ORB_USE_SILVER_BEFORE=0; AFTER=1
ACTUAL_ORB_USE_FIVE_POINTS_EACH_COLOR=PASS
ACTUAL_ORB_USE_BLOOD_SKILLS=PASS
BLOOD_BOSS_TEMPLATE_UNCHANGED=PASS
BLANK_NEI_ORB_NO_REWARD=PASS
STACKED_ORBS_ONE_POINT_PER_ITEM=PASS
ALL_FOUR_POINTS_PACKET_AND_SAVE_ROUNDTRIP=PASS
SILVER_SPENT_ON_LEVEL=PASS
PORTAL_PATCH=PASS
```

Também passaram: portal natural sem orb extra, evento cancelado sem recompensa,
morte sem participação de jogador sem recompensa e proteção contra evento duplicado.
O caldeirão foi reexecutado com os dois JARs novos: receita de Void Paradox,
nota correta, rejeição de nota errada e idempotência passaram novamente.
Não foi feita validação visual completa do cliente. Os registros abaixo preservam
os testes anteriores à adição do ponto Silver livre.

## Caldeirão original

```text
RUNE 0 REGISTERED=false
RUNE 7 REGISTERED=false
RUNE 9 REGISTERED=false
RUNE 15 REGISTERED=false
NOTE0 CAN_FINALIZE=false
ORIGINAL_BUG_REPRODUCED=PASS
```

## Caldeirão com o registro das runas

```text
RUNE 0 REGISTERED=true
RUNE 7 REGISTERED=true
RUNE 9 REGISTERED=true
RUNE 15 REGISTERED=true
NOTE0 CAN_FINALIZE=true
VOID_PARADOX OUTPUT=PASS
XYLOPHONE_ZERO_COUNT_NOTE_SERIALIZATION=PASS
WRONG_NOTE_REJECTED=PASS
IDEMPOTENCY=PASS
PATCHED_INTEGRATION=PASS
```

## Portal com a nova recompensa

```text
NATURAL_PORTAL_UNCHANGED=PASS
RITUAL_FACTORY_MARKED=true
RITUAL_ORB_COUNT=1
DUPLICATE_EVENT_PROTECTION=PASS
NON_PLAYER_DEATH_NO_ORB=PASS
CANCELED_DROPS_NO_ORB=PASS
EXISTING_DROPS_PRESERVED=PASS
ACTUAL_PLAYER_KILL_ORB=PASS
ACTUAL_ORB_USE_SILVER_BEFORE=0; AFTER=0
ACTUAL_ORB_USE_BLOOD_SKILLS=PASS
ORB_POINTS_BLUE_GREEN_RED=5,5,5; SILVER=0
BLOOD_SKILL_COUNT=2
PORTAL_PATCH=PASS
```

No original examinado, o portal do ritual não recebeu esse orb. A alteração
acrescenta uma recompensa escolhida pelo mantenedor. O teste de uso confirmou
as duas habilidades de sangue e saldo Silver livre zero antes e depois.

O patch de runas preserva ações já registradas e o requisito de poder. O patch do
portal preserva os drops existentes e exige um portal novo criado pela fábrica do
ritual. A validação foi automatizada em servidor e mundo descartáveis; não foi
uma sessão visual completa de cliente nem um teste de todos os conteúdos dos mods.
