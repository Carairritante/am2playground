---
name: github-publish-standards
description: >-
  Use this skill whenever the user asks to publish, push, sync, or send code/changes to GitHub
  ("enviar pro github", "subir no github", "coloca no github", "push pro github").
  Ensures all changes are delivered strictly following the friend's specified collaboration standards:
  true forks preserving history, public repositories, dedicated 'correcoes' branch for PRs,
  automated CI compiling unzipped artifacts with SHA256 checksums, and returning direct access links.
---

# GitHub Publishing & Collaboration Standards

Este guia define as regras obrigatórias para publicar qualquer código ou correção no GitHub deste projeto e ecossistema.

Sempre que o usuário pedir para **"enviar pro github"**, **"subir pro github"**, **"colocar no github"** ou qualquer comando equivalente, siga rigorosamente os 7 pilares estabelecidos:

---

## Os 7 Pilares Obrigatórios

### 1. Links Claros e Acessíveis
Após concluir o push, **sempre forneça os links diretos** para:
- O repositório no GitHub
- A branch exata com o commit enviado
- A aba de execuções do GitHub Actions (`/actions`)
- O hash do commit e o checksum SHA256 do artefato gerado

### 2. Forks Verdadeiros (Preservar Histórico)
- **Nunca crie repositórios vazios ou desvinculados do upstream.**
- Projetos base devem ser forks vinculados aos originais:
  - `ArsMagica2` (AM2.5): Fork oficial de `DrParadox7/ArsMagica2` (ex.: `Carairritante/ArsMagica2`)
  - `am2playground`: Fork oficial de `AirBurn0/am2playground` (ex.: `Carairritante/am2playground`)
- O histórico completo de commits original deve ser preservado.

### 3. Repositório 100% Público
- O repositório deve ser público para que o amigo e outros colaboradores consigam clonar, inspecionar e baixar os artefatos diretamente sem restrições de permissão.

### 4. Branch Dedicada para Colaboração via PR (`correcoes`)
- **Nunca comite correções diretamente na `master` ou em branch desconectada.**
- Mantenha todo o desenvolvimento e correções na branch:
  `correcoes`
- Isso permite que o amigo faça Pull Request (PR) na nossa branch, e nós nele, sem conflitar com o upstream original.

### 5. Compilação Automática no GitHub Actions sem ZIP Externo
- O workflow (`.github/workflows/ci.yml`) deve ser disparado em `push` e `pull_request` na branch `correcoes`.
- Os artefatos devem ser compilados automaticamente na esteira do CI.
- **Importante**: O mod e os checksums devem ser disponibilizados diretamente como artefatos da própria execução do Actions (`actions/upload-artifact` com `archive: false` quando suportado ou artefato avulso), **sem criar arquivos .zip externos/desnecessários**.

### 6. Integridade de Arquivos e Checksum (SHA256)
- Todo binário/JAR jogável gerado deve acompanhar seu respectivo arquivo `SHA256SUMS`.
- O histórico de commits deve ser cronológico, limpo e detalhar exatamente os patches e ferramentas alterados.

### 7. Verificação dos Novos Artefatos
- Novos commits enviados passam pela esteira de CI.
- Confirme que o job do GitHub Actions rodou com sucesso (`success`) e informe o resultado ao usuário.

---

## Procedimento Passo a Passo para Envio

Quando o usuário solicitar o envio para o GitHub:

1. **Validação Local Pré-Push**:
   - Rodar a suíte de testes e o build local para garantir que nada quebre no CI:
     - No `am2playground`: `python maintenance/ci.py` e `python maintenance/build_playable.py`.
     - No `ArsMagica2`: `./gradlew build` / verificação de integridade.
   - Conferir se o SHA256 local está atualizado.

2. **Commit Cronológico e Organizado**:
   - Adicionar os arquivos modificados de forma atômica.
   - Mensagem de commit clara descrevendo as correções (ex.: `fix: Ring of Recursion server crashes and Baubles synchronization`).

3. **Garantir a Branch `correcoes`**:
   - Confirmar que o branch atual é `correcoes`:
     ```powershell
     git branch --show-current
     ```
   - Caso não esteja, migrar para a branch `correcoes`.

4. **Push com Credenciais Seguras**:
   - Enviar para o remote configurado (ex.: `origin correcoes`).

5. **Acompanhar e Reportar**:
   - Fornecer imediatamente:
     - Link da branch: `https://github.com/<user>/<repo>/tree/correcoes`
     - Link do commit
     - Link da execução do Actions: `https://github.com/<user>/<repo>/actions`
     - Checksum SHA256 do artefato gerado
