# Changelog

Todas as mudanças relevantes deste projeto são registradas aqui. O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa [versionamento semântico](https://semver.org/lang/pt-BR/).

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
- Documentação do modelo do banco em `docs/banco-de-dados.md` e manual de uso em `docs/manual/Manual-Pyhoma-Versão 1.0.0.pdf`.
- Chamados (incidentes e requisições), problemas e mudanças com fluxo de status ITIL, matriz de prioridade, SLA de atendimento e solução em horário comercial, aprovações, soluções com aceite do requerente e pesquisa de satisfação.
- Quadro Kanban, Gantt de projetos com dependências e caminho crítico, portfólio e agenda dos técnicos.
- Inventário de ativos, software e licenças, contratos e fornecedores, documentos, importação e exportação CSV.
- Base de conhecimento com FAQ e interface de autoatendimento.
- Unidades organizacionais, perfis e permissões por módulo, grupos, listas, regras de negócio, chamados recorrentes, auditoria, lixeira, relatórios e notificações.
- Suporte a H2, PostgreSQL, MySQL, MariaDB, SQL Server e Oracle com migrações Flyway.
- Assistente de primeiro uso, temas claro e escuro, português e inglês.
- Instalador `.exe` para Windows gerado com `jpackage`; a desinstalação remove configurações, logs e o banco local.
