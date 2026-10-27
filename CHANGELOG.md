# Changelog

Todas as mudanças relevantes deste projeto são registradas aqui. O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa [versionamento semântico](https://semver.org/lang/pt-BR/).

## [Não publicado]

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

[Não publicado]: https://github.com/ravelroot/Information-Technology-Service-Management/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/ravelroot/Information-Technology-Service-Management/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/ravelroot/Information-Technology-Service-Management/releases/tag/v1.0.0
