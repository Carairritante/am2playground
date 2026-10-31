# Resultados registrados nos testes locais

Registro publicado em 31 de outubro de 2026. Os resultados abaixo são da sessão
local de manutenção; não são resultados de CI deste fork.

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
