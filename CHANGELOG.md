# Changelog

Todas as mudanças relevantes deste projeto são registradas aqui. O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa [versionamento semântico](https://semver.org/lang/pt-BR/).

## [Não publicado]

## [1.2.1] - 2026-11-08

### Alterado
- **Nova paleta**, a mesma do Bento e do Turno: no tema claro, fundo `#F5F4F4`, cartões brancos, bordas `#E8E7E6`, textos e botões pretos e barra lateral clara; no tema escuro, fundo `#020202`, cartões `#121111`, bordas `#333232` e textos e botões brancos. Verde, âmbar e vermelho ficam só nos status.
- Na barra lateral, o item aberto aparece num cartão com contorno, e a logo segue o tema (preta no claro, branca no escuro).
- Cores das categorias de novas instalações e do seletor de cor na mesma paleta.
- Prints do README, banners e manual em PDF (`Manual-Talos-Versão 1.2.1.pdf`) refeitos com a nova paleta.
- Instalador renomeado para `Talos-1.2.1.exe`. Nenhuma tabela nova: quem usa a 1.0.0, a 1.1.0 ou a 1.2.0 instala por cima.

## [1.2.0] - 2026-11-03

### Adicionado
- **Ações em lote** nas listas de chamados, problemas e mudanças: seleção múltipla (Ctrl ou Shift + clique, <kbd>Ctrl</kbd> + <kbd>A</kbd>) e o botão **Ações em lote**, também no menu do botão direito, para atribuir (a mim ou a um técnico e grupo), alterar status, urgência ou categoria, adicionar o mesmo acompanhamento, duplicar, mesclar e mover para a lixeira ou restaurar. Cada item passa pelas mesmas permissões da tela do item, e as falhas são listadas com o número e o motivo.
- **Mesclar itens duplicados**, pela lista ou por **Mesclar em outro item…** na tela do item: o item principal recebe a descrição, os acompanhamentos (com o número de origem), as tarefas e o tempo gasto (transferidos, sem contar duas vezes), o requerente, os seguidores, os grupos e os ativos; os duplicados ganham o vínculo **Duplica** e são fechados (mudanças são canceladas). Itens na lixeira não podem ser mesclados.
- **Duplicar** um chamado, problema ou mudança, com um vínculo para o original; no autoatendimento, a cópia fica em nome de quem a criou.
- **Itens parecidos** no formulário de novo item: enquanto o título é digitado, até quatro itens abertos com título parecido aparecem como links.
- Relatórios **Primeiro atendimento** (tempo até assumir e cumprimento do SLA de atendimento), **Idade dos chamados abertos** e **Quem mais abre chamados**.
- Atalhos <kbd>Ctrl</kbd> + <kbd>A</kbd> e <kbd>Shift</kbd> + <kbd>↑</kbd> / <kbd>↓</kbd> no guia de atalhos (<kbd>F1</kbd>).

### Corrigido
- A busca no banco de dados (H2, PostgreSQL, MySQL, MariaDB, SQL Server e Oracle) ignora acentos, como o modo demonstração.
- Buscar um número com mais de 18 dígitos não gera mais erro.
- O alerta de SLA em risco conta só o horário comercial quando o SLA usa horário comercial, sem avisos falsos no fim de semana.
- Uma mudança recusada pode ser aprovada depois de um novo pedido: vale a resposta mais recente de cada aprovador.
- Regras de negócio que não alteram urgência nem impacto não recalculam mais a prioridade; o operador **não contém** com grupos do requerente funciona.
- Um chamado recorrente com erro não impede mais os outros de rodar nem fica tentando a cada 5 minutos.
- O progresso e as datas das tarefas-mãe de projetos são atualizados em todos os níveis.
- Importação CSV de ativos só atualiza ativos visíveis para quem importa e com permissão de edição.
- Salvar um item não move mais o item para uma unidade que o usuário não vê nem o envia para a lixeira sem a permissão Excluir.
- Excluir uma tarefa respeita a visibilidade do item, e o tempo gasto não fica negativo.
- Seguidores do autoatendimento não alteram mais os responsáveis e seguidores do chamado de outra pessoa.
- O CSV aceita aspas no meio de um campo e recusa datas impossíveis como 31/02.

### Alterado
- Manual de uso atualizado para a versão 1.2.0 (`Manual-Talos-Versão 1.2.0.pdf`), com as ações em lote, a mescla, os itens parecidos, os novos relatórios e as correções.
- README no padrão dos demais repositórios do autor.
- Instalador renomeado para `Talos-1.2.0.exe`. Nenhuma tabela nova: quem usa a 1.0.0 ou a 1.1.0 instala por cima.
- Código-fonte, scripts e workflows sem comentários.

## [1.1.0] - 2026-10-27

### Adicionado
- **Respostas prontas**: textos padronizados cadastrados em Listas e cadastros e inseridos pelo menu **Resposta pronta** na linha do tempo, com os campos `{requester}`, `{reference}`, `{title}` e `{me}`. Novas instalações já vêm com quatro respostas de exemplo.
- **Central de integrações** em Administração › Integrações: webhooks para Slack, Microsoft Teams, Discord, Google Chat e JSON genérico, com assinatura HMAC-SHA256 opcional no cabeçalho `X-Talos-Signature`.
- Eventos selecionáveis por webhook: item criado, atribuído, com status alterado e solucionado (chamados, problemas e mudanças) e alertas de vencimento de garantias, contratos e licenças.
- Botão **Enviar teste**, pausa/retomada e registro das **últimas entregas** (status HTTP e horário). Envio assíncrono, com tempo limite de 10 segundos, sem travar a interface.
- Exportação da agenda em **iCalendar (`.ics`)** com os prazos dos chamados abertos, para Outlook, Google Agenda e Apple Calendário.
- **Paleta de comandos** (<kbd>Ctrl</kbd> + <kbd>K</kbd>): busca global e ações rápidas (novo chamado, ir para qualquer tela, alternar tema, abrir atalhos), navegável pelo teclado.
- **Guia de atalhos de teclado** (<kbd>F1</kbd>) e novos atalhos: <kbd>Ctrl</kbd> + <kbd>N</kbd> (novo chamado) e <kbd>F5</kbd> (atualizar a tela).
- Gerador do manual em PDF versionado no repositório (`scripts/manual/build_manual.py`, com o texto em `scripts/manual/content.py`).

### Alterado
- Manual de uso atualizado para a versão 1.1.0 (`Manual-Talos-Versão 1.1.0.pdf`), no mesmo visual do manual anterior, com sumário clicável, prints novos e seções sobre integrações, paleta de comandos, atalhos e respostas prontas; READMEs atualizados.
- A busca da barra superior virou a paleta de comandos, e a barra superior ganhou o botão **Atalhos de teclado**; o menu do usuário também abre o guia.
- O visual da 1.0.0 (paleta minimalista em cinzas, areia e preto) foi mantido; as telas novas seguem o mesmo estilo.
- Instalador renomeado para `Talos-1.1.0.exe`.
- O projeto passou a se chamar **Talos.** (antes Pyhoma): pacotes `io.talos`, módulos `talos-domain`, `talos-infrastructure` e `talos-desktop`, pasta de dados `%LOCALAPPDATA%\Talos`, configuração em `talos.properties` e manual em PDF.
- Nova logo: um rack de servidor com três gavetas no lugar do prompt `>_`, aplicada no aplicativo, nos ícones da janela e do instalador, nos banners do README, nos prints e no manual em PDF. Em tamanhos pequenos (barra lateral e ícones de até 48 px) a logo usa uma versão compacta, sem a fenda das gavetas, para continuar legível.

## [1.0.0] - 2026-10-14

### Segurança
- Senhas com PBKDF2-HMAC-SHA256 de 600 mil iterações, política de senha mais forte e atualização automática de hashes antigos.
- Login sem enumeração de usuários, atraso progressivo, limites seguros de bloqueio, sessão encerrada por inatividade e revalidada continuamente.
- Proteção contra escalada de privilégio em perfis e habilitações, perfil Super-Administrador protegido e aprovação por outra pessoa.
- Links de documentos restritos e confirmados, CSV sem fórmulas, regex com tempo limitado, SQL Server com validação de certificado, H2 sem porta de rede e JVM sem anexação externa.

### Adicionado
- Paleta minimalista (cinza-claro, areia, pedra, malva e preto) nos temas claro e escuro.
- Animações suaves na navegação, rolagem suave, cartões e indicadores com movimento; a janela abre maximizada.
- Zoom da interface com Ctrl + rolagem do mouse, Ctrl + / Ctrl - e Ctrl 0, com padrão de 75%.
- Gráficos do Painel com troca de visualização (barras, colunas, linha ou rosca), lembrada por computador.
- Criação de chamados, problemas e mudanças direto no quadro Kanban, na coluna escolhida.
- Kanban redesenhado: colunas com contadores (itens, tempo trabalhado e atrasados), ordenação e "Adicionar chamado"; cartões com categoria colorida, tipo, responsáveis, prazo, bandeira de prioridade, tempo trabalhado, tarefas e destaque de atraso.
- Novo item em formulário enxuto: título, quadro, tipo, categoria, responsáveis, seguidores e descrição.
- Vários responsáveis por item e seguidores, definidos por qualquer pessoa, na aba Pessoas ou no cartão do Kanban.
- Administrador cria e exclui categorias de chamados (com cor e subcategorias) direto no Kanban e no novo item.
- Modo demonstração sem banco de dados; a instalação real começa vazia, sem dados fictícios.
- Configuração inicial guiada depois do primeiro login do administrador (organização, unidades, localizações, equipe, horário e categorias), com etapas obrigatórias e gravação imediata no banco.
- Documentação do modelo do banco em `docs/banco-de-dados.md` e manual de uso em `docs/manual/Manual-Talos-Versão 1.0.0.pdf`.
- Chamados (incidentes e requisições), problemas e mudanças com fluxo de status ITIL, matriz de prioridade, SLA de atendimento e solução em horário comercial, aprovações, soluções com aceite do requerente e pesquisa de satisfação.
- Quadro Kanban, Gantt de projetos com dependências e caminho crítico, portfólio e agenda dos técnicos.
- Inventário de ativos, software e licenças, contratos e fornecedores, documentos, importação e exportação CSV.
- Base de conhecimento com FAQ e interface de autoatendimento.
- Unidades organizacionais, perfis e permissões por módulo, grupos, listas, regras de negócio, chamados recorrentes, auditoria, lixeira, relatórios e notificações.
- Suporte a H2, PostgreSQL, MySQL, MariaDB, SQL Server e Oracle com migrações Flyway.
- Assistente de primeiro uso, temas claro e escuro, português e inglês.
- Instalador `.exe` para Windows gerado com `jpackage`; a desinstalação remove configurações, logs e o banco local.

[Não publicado]: https://github.com/ravelroot/Information-Technology-Service-Management/compare/v1.2.1...HEAD
[1.2.1]: https://github.com/ravelroot/Information-Technology-Service-Management/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/ravelroot/Information-Technology-Service-Management/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ravelroot/Information-Technology-Service-Management/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/ravelroot/Information-Technology-Service-Management/releases/tag/v1.0.0
