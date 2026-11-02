# Verificador de independência AM2 / Playground

Mod de teste autoral para cliente Forge 1.7.10 com servidor integrado. Execute
somente em uma instância descartável: ele cria um mundo plano próprio, verifica
pacotes reais e encerra o cliente automaticamente. Não utiliza saves do jogador.

O CI compila esta fonte para Java 8 junto aos demais probes, usando apenas
assinaturas de compilação. O probe não entra no payload nem no JAR de ferramentas.
O CI público não executa Minecraft; compilar este arquivo não atesta a matriz.

Para o ensaio gráfico, empacote somente as classes de `independenceprobe` em um
JAR separado. Use Minecraft/Forge 1.7.10, AnimationAPI, as dependências da versão
de AM2 escolhida e os dois JARs da combinação em teste. Não inclua o Segundo
Cérebro nem outros addons neste ensaio mínimo. Acrescente os argumentos Java:

```text
-Dindependence.expectProgress=true
-Dindependence.advancedAvailable=true
```

| Combinação | expectProgress | advancedAvailable |
| --- | --- | --- |
| AM2.5 corrigido sem Playground | false | auto |
| AM2.5 corrigido com Playground original | false | auto |
| AM2.5 original ou corrigido com Playground corrigido | true | true |
| AM2 original de Mithion suportado com Playground corrigido | true | false |

`expectProgress` identifica o auxiliar novo do Playground, não o mod original.
No ambiente mínimo, cinco descobertas avançadas pertencem ao AM2.5. As outras
duas só são registradas com Botania e Witchery, e exigem ensaio separado no pack
completo. No original de Mithion, as sete avançadas ficam indisponíveis; o teste
exige zero desbloqueadores e recusa de orb avançada sem consumir item ou pontos.

O ensaio confere overlays condicionais, política Silver na UI e em pacotes
reais do servidor, pré-requisitos e custo de talentos, preservação de habilidades
aprendidas, compras Blue, sincronização real dos quatro saldos e renderização do
Oculus. A classificação Silver de uma forma pode ser uma fixture temporária,
restaurada após a verificação; ela não acrescenta uma habilidade ao mod.

O resultado é `independence-result.txt`, encerrado por `AM2_INDEPENDENCE=PASS`,
mais imagens do menu e do Oculus. Registre os hashes exatos dos JARs e confira
as imagens. O verificador não garante compatibilidade universal nem substitui
testes prolongados multijogador e de todos os rituais.

Desenvolvido com participação substancial do OpenAI Codex. Os autores, créditos
e licenças dos mods originais permanecem preservados.
