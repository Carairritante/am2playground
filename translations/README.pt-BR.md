# Compêndios AM2.5 e Playground em português

Pacote de recursos do cliente: ative o ZIP em Opções > Pacotes de Recursos e
escolha Português (Brasil) em Opções > Idioma. Não extraia para a pasta mods.
Use a [CR10 ou mais recente do AM2.5](https://github.com/Carairritante/ArsMagica2/tree/correcoes)
para traduzir também os rótulos fixos e trocar idioma sem reiniciar.
Reabra o livro após a troca. Inglês permanece disponível pelo idioma do jogo.

Inclui 535 entradas do AM2.5 e 136 do Playground, com os requisitos extras da
manutenção advanced-silver-progression. Não modifica o JAR do Playground.
Contém somente textos, XML e metadados de pacote, sem classes Java.
[Detalhes, instalação e validação](https://github.com/Carairritante/ArsMagica2/blob/correcoes/docs/TRADUCAO-COMPENDIUM-CR10.md).

Fontes PT-BR do addon: am2pg/docs/ArcaneCompendium_pt_BR.xml, am2pg/lang/pt_BR.lang
e arsmagica2/lang/pt_BR.lang. O mesclador dos requisitos aceita o marcador antigo
e o traduzido, sem duplicar o guia nem os requisitos.

As sete descobertas avançadas exigem AM2.5 com Fusão com o Mundo nativa. No AM2
original de Mithion, os demais recursos do Playground continuam disponíveis;
essa progressão não é substituída por nível 70 nem por uma cópia do ritual.
O pacote inclui a mensagem dessa limitação e a orientação de compras Silver
da manutenção, no próprio namespace do Playground.

Para atualizar o ZIP após alterar as fontes, execute na raiz do fork:

```text
python maintenance/update_translation_pack.py
python maintenance/update_translation_pack.py --check
```

O atualizador troca somente o XML e o idioma PT-BR do Playground, preservando
os recursos completos do AM2.5 e os demais arquivos do ZIP. Também mescla as
mensagens próprias em `am2pg/lang/pt_BR.lang` e `am2pg/lang/en_US.lang`, sem
duplicar chaves, e atualiza `translations/SHA256SUMS`. A verificação automatizada
rejeita um pacote publicado cujo XML ou idioma difira das fontes mantidas.

Créditos: Mithion, TCLProject, DrParadox7 e AirBurn0 pelas obras originais.
Tradução de Carairritante com participação substancial do OpenAI Codex.
As licenças e os avisos originais não foram substituídos.
