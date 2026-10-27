"""Conteúdo do manual do Talos. (pt-BR).

Blocos aceitos: ("p", texto), ("h2", título), ("h3", título), ("bullets", [itens]), ("steps", [itens]),
("table", cabeçalhos, linhas, larguras-relativas), ("fig", [arquivos candidatos], legenda, largura 0-1),
("note", rótulo, texto, "tip"|"warn"), ("code", texto), ("break",).
Marcação: **negrito**, `código`, [[Ctrl + K]] para teclas.
"""

VERSION = "1.1.0"


def chapters():
    return [
        {
            "number": "01", "eyebrow": "visão geral", "title": "Apresentação",
            "lead": "O **Talos.** é um aplicativo para Windows que reúne o trabalho diário de uma equipe de TI: "
                    "chamados com prazos de SLA, problemas e mudanças no modelo ITIL, quadros Kanban, cronogramas "
                    "de Gantt, agenda dos técnicos, inventário de equipamentos, software e contratos, base de "
                    "conhecimento, relatórios, integrações e administração completa de acessos.",
            "blocks": [
                ("p", "A arquitetura segue os conceitos do GLPI (unidades organizacionais, perfis por módulo, regras "
                      "de negócio, listas configuráveis, lixeira e auditoria). O programa roda na própria máquina, "
                      "sem site nem servidor web. Os dados ficam no banco que você escolher: um arquivo local H2 para "
                      "uso individual ou um servidor PostgreSQL, MySQL, MariaDB, SQL Server ou Oracle compartilhado "
                      "por toda a equipe."),
                ("h2", "Para quem é este manual"),
                ("bullets", [
                    "**Usuários e técnicos:** capítulos 3 a 7 explicam cada tela, aba e botão do dia a dia.",
                    "**Administradores:** capítulos 2, 8 e 9 cobrem instalação, configuração inicial, permissões, "
                    "regras, SLA, integrações e rotinas automáticas.",
                    "**Equipe de TI e desenvolvedores:** capítulos 10 e 11 trazem segurança, arquitetura, banco de "
                    "dados, compilação e testes.",
                ]),
                ("h2", "Novidades da versão 1.1.0"),
                ("table", ["Novidade", "O que muda no dia a dia"], [
                    ["Central de integrações", "Webhooks para Slack, Microsoft Teams, Discord, Google Chat e JSON "
                     "genérico (com assinatura HMAC-SHA256). Escolha os eventos, envie um teste e acompanhe as "
                     "últimas entregas."],
                    ["Agenda em iCalendar", "Exporte os prazos dos chamados abertos em `.ics` para o Outlook, "
                     "Google Agenda ou Apple Calendário."],
                    ["Paleta de comandos", "[[Ctrl + K]] busca em todo o sistema e executa ações: novo chamado, ir "
                     "para qualquer tela, alternar o tema, abrir os atalhos."],
                    ["Guia de atalhos", "[[F1]] mostra todos os atalhos de teclado em uma única janela."],
                    ["Respostas prontas", "Textos padronizados para os acompanhamentos, cadastrados em Listas e "
                     "cadastros, com os campos {requester}, {reference}, {title} e {me} preenchidos na hora."],
                ], [1.1, 3]),
                ("h2", "Principais recursos"),
                ("table", ["Área", "O que oferece"], [
                    ["Atendimento", "Chamados (incidentes e requisições), problemas, mudanças, linha do tempo, "
                     "tarefas, soluções, aprovações, vínculos, vários responsáveis e seguidores por item, SLA de "
                     "atendimento e de solução, pesquisa de satisfação, chamados recorrentes."],
                    ["Visões", "Painel com indicadores, Kanban de chamados, problemas, mudanças e tarefas de projeto, "
                     "Gantt, portfólio e agenda semanal com exportação `.ics`."],
                    ["Inventário", "Ativos com patrimônio, rede e garantia, conexões, software e licenças com "
                     "conformidade, contratos, fornecedores e documentos."],
                    ["Conhecimento", "Base de conhecimento em Markdown com FAQ, relatórios com gráficos e exportação "
                     "CSV, autoatendimento para usuários finais."],
                    ["Integrações", "Webhooks para Slack, Microsoft Teams, Discord, Google Chat e sistemas próprios "
                     "(JSON assinado), importação e exportação CSV, calendário iCalendar."],
                    ["Administração", "Unidades organizacionais, usuários, grupos, perfis e permissões, listas, "
                     "regras de negócio, SLAs, calendário, feriados, integrações, auditoria e configurações."],
                    ["Interface", "Temas claro e escuro, português e inglês, paleta de comandos, guia de "
                     "atalhos, zoom, animações suaves, gráficos em barras, colunas, linha ou rosca, modo "
                     "demonstração."],
                ], [1, 4]),
                ("h2", "Requisitos"),
                ("table", ["Item", "Mínimo"], [
                    ["Sistema", "Windows 10 ou 11, 64 bits"],
                    ["Memória", "4 GB (o programa usa até 1 GB)"],
                    ["Disco", "400 MB para o programa, mais o espaço do banco"],
                    ["Banco", "H2 embutido, ou PostgreSQL 12+, MySQL 8.0+, MariaDB 10.6+, SQL Server 2017+, "
                              "Oracle 19c+"],
                    ["Java", "Não precisa instalar: o instalador já traz o Java 21 e todas as bibliotecas"],
                    ["Rede (opcional)", "Acesso HTTPS de saída para os webhooks das integrações"],
                ], [1, 4]),
            ],
        },
        {
            "number": "02", "eyebrow": "começando", "title": "Instalação e primeiro uso",
            "lead": "Do instalador ao primeiro chamado em quatro passos guiados.",
            "blocks": [
                ("h2", "Instalar"),
                ("steps", [
                    "Execute **Talos-1.1.0.exe**. Se o Windows SmartScreen avisar que o editor é desconhecido, clique "
                    "em **Mais informações** e depois em **Executar assim mesmo** (o instalador não é assinado "
                    "digitalmente).",
                    "Aceite a licença MIT, escolha a pasta de instalação (padrão: `Arquivos de Programas\\Talos`) e "
                    "conclua.",
                    "Abra o Talos pelo menu Iniciar (grupo **Talos**) ou pelo atalho criado na área de trabalho.",
                ]),
                ("note", "Atualizando da 1.0.0?", "Instale a 1.1.0 por cima. O banco de dados, as configurações e "
                         "a conexão salva são mantidos; nenhuma tabela nova é criada."),
                ("h2", "Onde o Talos. guarda arquivos"),
                ("table", ["Pasta", "Conteúdo"], [
                    ["`%APPDATA%\\Talos`", "Configuração deste computador (`talos.properties`): conexão com o banco, "
                     "idioma, tema, zoom e tamanho da janela. A senha do banco é criptografada com o DPAPI do "
                     "Windows."],
                    ["`%LOCALAPPDATA%\\Talos`", "Dados locais: banco H2 (`database`) e registros de erro (`logs`)."],
                    ["`Arquivos de Programas\\Talos`", "O programa em si, com o Java embutido."],
                ], [1.3, 3]),
                ("h2", "Desinstalar"),
                ("p", "Use **Configurações › Aplicativos › Talos › Desinstalar**. A desinstalação remove o programa e "
                      "apaga também as duas pastas de dados acima (configurações, logs e banco H2 local). Na próxima "
                      "instalação o Talos. começa do zero, pela tela de boas-vindas. Bancos em servidor (PostgreSQL, "
                      "MySQL, SQL Server, Oracle) nunca são tocados."),
                ("note", "Atenção.", "Se você usa o banco H2 local e quer guardar os dados, copie a pasta "
                         "`%LOCALAPPDATA%\\Talos\\database` antes de desinstalar.", "warn"),
                ("h2", "Tela de boas-vindas"),
                ("fig", ["01-welcome", "setup-welcome"], "Tela de boas-vindas: idioma, os quatro passos e o modo "
                                                          "demonstração."),
                ("p", "Na primeira abertura aparece a tela de boas-vindas. Escolha o idioma (português ou inglês) e "
                      "clique em **Começar**; ao final do passo 1 o botão **Conectar** testa e grava a conexão. Os "
                      "quatro passos são: conectar o banco, criar a organização e o administrador, entrar e preencher "
                      "a configuração inicial."),
                ("note", "Dica.", "Se o programa for fechado antes de terminar o passo 2, nada fica salvo e o "
                                  "assistente recomeça do início na próxima abertura."),
                ("h3", "Modo demonstração"),
                ("p", "O botão **Explorar sem banco de dados** abre o Talos. com dados fictícios gerados na memória. "
                      "Todas as telas funcionam e é possível criar, editar e mover itens, mas nada é gravado: ao sair "
                      "ou fechar, tudo é descartado. Uma faixa no topo lembra que é uma demonstração e oferece "
                      "**Configurar banco de dados** (vai direto ao assistente) e **Sair da demonstração**. Na "
                      "demonstração a sessão não expira por inatividade."),
                ("h2", "Passo 1: banco de dados"),
                ("fig", ["02-database", "setup-database"], "Escolha do banco e dados de conexão."),
                ("p", "Escolha o banco em um dos cartões e preencha os campos. Use **Testar conexão** antes de "
                      "avançar."),
                ("table", ["Campo", "Uso"], [
                    ["Banco", "H2 (arquivo local), PostgreSQL, MySQL, MariaDB, SQL Server ou Oracle."],
                    ["Pasta dos dados", "Só para H2. Padrão: `%LOCALAPPDATA%\\Talos\\database`."],
                    ["Servidor e porta", "Endereço do servidor. As portas padrão são preenchidas sozinhas (5432, "
                                         "3306, 1433, 1521)."],
                    ["Nome do banco", "Banco (ou serviço, no Oracle) criado vazio para o Talos."],
                    ["Usuário e senha", "Conta com permissão para criar tabelas. A senha é guardada criptografada "
                                        "neste computador."],
                    ["Parâmetros extras", "Opções do driver JDBC. No SQL Server com certificado autoassinado use "
                                          "`trustServerCertificate=true`."],
                ], [1.1, 3]),
                ("p", "Ao conectar, as tabelas são criadas automaticamente (Flyway) seguindo o modelo descrito no "
                      "capítulo 11 e em `docs/banco-de-dados.md`. Se o banco já tiver uma instalação do Talos., o "
                      "programa vai direto para o login."),
                ("h2", "Passo 2: organização e administrador"),
                ("p", "Informe o nome da organização, o nome completo, o login e o e-mail do administrador e crie a "
                      "senha. Não existe usuário ou senha padrão. A senha precisa seguir a política abaixo, que vale "
                      "para todas as contas."),
                ("table", ["Regra", "Detalhe"], [
                    ["Tamanho", "De 10 a 128 caracteres, sem espaços no início ou no fim."],
                    ["Composição", "Letras e números, com pelo menos 5 caracteres diferentes."],
                    ["Bloqueios", "Senhas comuns (senha123, admin, talos, qwerty...) e variações com números no lugar "
                                  "de letras."],
                    ["Dados pessoais", "Não pode conter o login nem partes do nome com 4 letras ou mais."],
                ], [1, 3.4]),
                ("p", "Ao concluir, o Talos. cria a unidade organizacional principal, os cinco perfis padrão, os SLAs "
                      "por prioridade, os feriados nacionais e as listas comuns (origens, estados de ativo, "
                      "categorias de chamado, tipos de contrato...). Chamados, ativos, contratos e projetos começam "
                      "vazios."),
                ("h2", "Passo 3: entrar"),
                ("fig", ["03-login"], "Tela de login. O link **Usar outro banco de dados** permite trocar a conexão.",
                 0.85),
                ("p", "Entre com o login e a senha. Por segurança, qualquer erro mostra a mesma mensagem genérica. "
                      "Depois de algumas tentativas erradas (padrão: 5) a conta fica bloqueada por alguns minutos "
                      "(padrão: 15). O link **Usar outro banco de dados** volta à escolha do banco."),
                ("h2", "Passo 4: configuração inicial"),
                ("p", "No primeiro login de um administrador, o Talos. abre uma configuração guiada. Cada item é "
                      "gravado no banco assim que você clica em salvar ou adicionar. As etapas obrigatórias precisam "
                      "ser concluídas para liberar o programa; as opcionais podem ser preenchidas depois em "
                      "Administração. Se o programa for fechado no meio, o que já foi salvo permanece e a "
                      "configuração reabre no próximo login."),
                ("fig", ["04-setup", "setup-initial"], "Configuração inicial: etapas à esquerda (as concluídas ficam "
                                                       "marcadas) e cadastro de localizações."),
                ("table", ["Etapa", "Obrigatória", "O que cadastrar"], [
                    ["1. Organização", "Sim", "Nome e e-mail de contato; telefone e endereço são opcionais."],
                    ["2. Unidades", "Não", "Filiais e departamentos abaixo da organização principal."],
                    ["3. Localizações", "Sim", "Pelo menos um prédio, andar ou sala (pode ser hierárquico)."],
                    ["4. Equipe", "Sim", "Pelo menos um grupo técnico. Técnicos podem ser criados aqui, já com o "
                                         "perfil Técnico e dentro do grupo."],
                    ["5. Horário", "Sim", "Dias úteis e horário de atendimento usados pelos prazos de SLA em horário "
                                          "comercial."],
                    ["6. Categorias", "Não", "Categorias de chamado além das que já vêm prontas."],
                    ["7. Concluir", "—", "Resumo do que foi cadastrado e o botão **Começar a usar**."],
                ], [1.1, 0.8, 3.2]),
            ],
        },
        {
            "number": "03", "eyebrow": "navegação", "title": "Conhecendo a interface",
            "lead": "Barra lateral para os módulos, barra superior para busca e contexto, e a paleta de comandos "
                    "para chegar a qualquer lugar sem tirar as mãos do teclado.",
            "blocks": [
                ("fig", ["10-dashboard", "dashboard"], "Janela principal: barra lateral, barra superior e o Painel."),
                ("h2", "Barra lateral"),
                ("p", "Organiza os módulos por seção. Só aparecem os itens que o seu perfil pode ver. O item "
                      "selecionado fica em destaque, com fundo areia. No rodapé da barra ficam o banco em uso e a "
                      "versão do programa."),
                ("table", ["Seção", "Itens"], [
                    ["Início", "Painel (técnicos) ou Início (autoatendimento)"],
                    ["Atendimento", "Chamados, Kanban, Problemas, Mudanças, Agenda, Chamados recorrentes"],
                    ["Projetos", "Projetos, Portfólio (Gantt)"],
                    ["Inventário", "Ativos, Software e licenças, Contratos, Fornecedores, Documentos"],
                    ["Conhecimento", "Base de conhecimento, Relatórios"],
                    ["Administração", "Usuários, Grupos, Perfis e permissões, Unidades organizacionais, Listas e "
                                      "cadastros, Regras de negócio, SLAs e calendário, **Integrações**, Auditoria, "
                                      "Configurações"],
                ], [1, 3.6]),
                ("h2", "Barra superior"),
                ("table", ["Elemento", "Função"], [
                    ["Buscar", "Abre a paleta de comandos ([[Ctrl + K]]): busca global e ações rápidas."],
                    ["Unidade ativa", "Escolhe a unidade organizacional em que você está trabalhando. **Incluir "
                                      "subunidades** mostra também os dados das unidades abaixo dela."],
                    ["Tema", "Alterna entre os temas claro e escuro (vale só para este computador)."],
                    ["Sino", "Notificações: atribuições, acompanhamentos, aprovações, alertas de SLA, garantias, "
                             "contratos e licenças. Clique em uma para abrir o item; **Marcar todas como lidas** "
                             "limpa o contador."],
                    ["Menu do usuário", "Minha conta, Alterar senha, Atalhos de teclado, Sobre o Talos. e Sair."],
                ], [1.1, 3.6]),
                ("h2", "Paleta de comandos"),
                ("fig", ["46-command-palette", "command-palette"], "Paleta de comandos: resultados da busca e ações "
                                                                   "rápidas na mesma lista."),
                ("p", "Pressione [[Ctrl + K]] (ou clique em **Buscar**) em qualquer tela. Digite parte de um título, "
                      "número, nome ou patrimônio: a lista mistura resultados da **busca global** (chamados, "
                      "problemas, mudanças, ativos, pessoas, artigos, projetos, software, contratos e fornecedores, a "
                      "partir de 2 caracteres) com **ações**."),
                ("table", ["Ação", "O que faz"], [
                    ["Novo chamado", "Abre o formulário de novo item."],
                    ["Ir para…", "Abre qualquer tela que o seu perfil pode ver (Kanban, Ativos, Integrações...)."],
                    ["Alternar tema", "Troca entre claro e escuro."],
                    ["Atalhos de teclado", "Abre o guia de atalhos."],
                ], [1.2, 3.4]),
                ("p", "Use [[↑]] e [[↓]] para escolher, [[Enter]] para executar e [[Esc]] para fechar."),
                ("h2", "Atalhos de teclado"),
                ("fig", ["47-shortcuts"], "Guia de atalhos, aberto com F1.", 0.8),
                ("table", ["Atalho", "Ação"], [
                    ["[[Ctrl + K]]", "Abrir a paleta de comandos (busca e ações)"],
                    ["[[Ctrl + N]]", "Novo chamado"],
                    ["[[Ctrl + Shift + N]]", "Ir para Chamados"],
                    ["[[F1]]", "Abrir o guia de atalhos"],
                    ["[[F5]]", "Atualizar a tela atual"],
                    ["[[Esc]]", "Fechar janelas, menus e a paleta"],
                    ["[[Ctrl + rolagem do mouse]]", "Ampliar ou reduzir a interface"],
                    ["[[Ctrl + +]] / [[Ctrl + -]]", "Ampliar / reduzir"],
                    ["[[Ctrl + 0]]", "Voltar o zoom para o padrão (75%)"],
                    ["[[Enter]] em uma tabela", "Abrir o item selecionado (o mesmo que clique duplo)"],
                ], [1.6, 3]),
                ("h2", "Zoom, temas e movimento"),
                ("bullets", [
                    "**Zoom** padrão de 75%, como em um navegador. Os níveis vão de 50% a 175% (50, 67, 75, 80, 90, "
                    "100, 110, 125, 150 e 175%). O nível aparece por um instante no topo e fica salvo para as próximas "
                    "aberturas.",
                    "**Tema claro:** fundo cinza-claro, textos e botões pretos, barra lateral preta com destaque areia. "
                    "**Tema escuro:** fundo quase preto, textos claros e destaques em areia. Troque pelo botão da lua "
                    "na barra superior ou pela paleta de comandos.",
                    "**Idioma:** português ou inglês, em Configurações › Aparência e idioma ou na conta do usuário.",
                    "Transições suaves entre telas, rolagem suave e indicadores animados. A janela abre maximizada.",
                ]),
                ("h2", "Sessão"),
                ("p", "Sem uso por algum tempo (padrão: 15 minutos, ajustável em Configurações) a sessão é encerrada "
                      "e o login é pedido de novo. Se um administrador desativar ou bloquear a sua conta, a sessão "
                      "termina em até 30 segundos."),
                ("h2", "Padrões das telas"),
                ("bullets", [
                    "**Listas:** busca por texto, filtros no topo, ordenação por coluna, paginação, exportação para "
                    "CSV (botão de download) e clique duplo ou [[Enter]] para abrir.",
                    "**Detalhes:** cabeçalho com título e etiquetas de status, botões de ação à direita e abas. A aba "
                    "**Documentos** anexa arquivos ou links e a aba **Histórico** mostra a auditoria campo a campo.",
                    "**Lixeira:** itens excluídos vão para a lixeira e podem ser restaurados. Excluir "
                    "permanentemente exige o direito Expurgar.",
                    "Campos obrigatórios são marcados com asterisco; erros aparecem abaixo de cada campo.",
                ]),
            ],
        },
        {
            "number": "04", "eyebrow": "service desk", "title": "Atendimento",
            "lead": "Painel, chamados, Kanban, problemas, mudanças, agenda e chamados recorrentes.",
            "blocks": [
                ("h2", "Painel"),
                ("p", "Visão geral do atendimento da unidade ativa, atualizada a cada abertura."),
                ("table", ["Bloco", "Conteúdo"], [
                    ["Indicadores", "Em aberto, Sem técnico, Atrasados, SLA em risco, Solucionados hoje, Solução "
                                    "média (h) e Satisfação (média de 1 a 5), cada um com um marcador de cor. "
                                    "Clicar em um indicador abre a lista de chamados."],
                    ["Gráfico de 30 dias", "Chamados abertos e solucionados por dia, em linha ou colunas."],
                    ["Por status", "Quantidade de chamados em cada status, nas cores dos status."],
                    ["Minha fila", "Chamados atribuídos a você, por prazo."],
                    ["Carga por técnico", "Chamados abertos de cada técnico."],
                    ["Abertos por prioridade", "Distribuição de Muito baixa a Crítica."],
                    ["Principais categorias, Ativos por tipo", "Volume por categoria e composição do inventário."],
                    ["Precisa de atenção", "Aprovações aguardando você, mudanças em andamento, problemas abertos, "
                                           "tarefas de projeto atrasadas, garantias e contratos a vencer e licenças "
                                           "excedidas."],
                ], [1.3, 3.3]),
                ("h3", "Formas de visualizar os gráficos"),
                ("p", "Cada gráfico do Painel tem, no canto superior direito, botões para trocar a forma de "
                      "visualização. A escolha fica salva neste computador. Na rosca, passe o mouse sobre uma fatia ou "
                      "sobre a legenda para ver o valor no centro."),
                ("table", ["Gráfico", "Formas disponíveis", "Padrão"], [
                    ["Abertos e solucionados nos últimos 30 dias", "Linha, Colunas", "Linha"],
                    ["Por status", "Barras, Colunas, Rosca", "Barras"],
                    ["Carga por técnico", "Barras, Colunas, Rosca", "Barras"],
                    ["Abertos por prioridade", "Barras, Colunas, Rosca", "Barras"],
                    ["Principais categorias", "Barras, Colunas, Rosca", "Barras"],
                    ["Ativos por tipo", "Barras, Colunas, Rosca", "Rosca"],
                ], [2.4, 1.6, 0.8]),
                ("fig", ["dashboard-charts"], "Painel com outras visualizações: colunas no gráfico de 30 dias e rosca "
                                              "em Por status e prioridade."),
                ("h2", "Chamados"),
                ("fig", ["11-tickets", "tickets"], "Lista de chamados com visões, filtros, prioridade e prazo de SLA."),
                ("p", "Chamados são **incidentes** (algo parou de funcionar) ou **requisições** (pedido de serviço). "
                      "A lista tem visões prontas:"),
                ("table", ["Visão", "Mostra"], [
                    ["Todos / Abertos", "Todos os chamados visíveis / só os que não foram solucionados nem fechados"],
                    ["Atribuídos a mim / Abertos por mim", "Sua fila como técnico / o que você pediu"],
                    ["Meus grupos / Sem técnico", "Fila dos seus grupos / chamados ainda sem responsável"],
                    ["Atrasados", "Chamados com prazo de solução estourado"],
                    ["Solucionados / Fechados / Lixeira", "Histórico e itens excluídos"],
                ], [1.6, 3]),
                ("h3", "Abrir um chamado"),
                ("steps", [
                    "Pressione [[Ctrl + N]] ou clique em **Novo chamado** (na lista, no Painel, na tela Início ou no "
                    "Kanban) ou em **Adicionar chamado** no rodapé de uma coluna do Kanban.",
                    "Preencha o formulário enxuto descrito abaixo. Qualquer pessoa, seja qual for o perfil, pode "
                    "escolher um ou mais responsáveis e seguidores.",
                    "Clique em **Criar**. As regras de negócio e os SLAs são aplicados automaticamente, o item já "
                    "nasce com as pessoas escolhidas e as integrações configuradas recebem o evento.",
                    "Precisa de urgência, impacto, localização ou origem? Use o link **Formulário completo** no rodapé "
                    "da janela.",
                ]),
                ("table", ["Campo", "Uso"], [
                    ["Título", "Obrigatório. Aparece no cartão do Kanban ao lado do número."],
                    ["Quadro", "Chamados, Problemas ou Mudanças, conforme o seu perfil. Define em qual quadro do "
                               "Kanban o item entra."],
                    ["Tipo", "Incidente ou Requisição (chamados); Padrão, Normal ou Emergencial (mudanças). "
                             "Problemas não têm tipo."],
                    ["Categoria", "Lista de categorias de chamado. O administrador vê ao lado o botão de etiquetas "
                                  "para criar ou excluir categorias."],
                    ["Responsáveis", "Uma ou mais pessoas que vão trabalhar no item. A primeira é o responsável "
                                     "principal. Com responsável, o chamado já entra em Em atendimento."],
                    ["Seguidores", "Pessoas que só acompanham: veem o item e recebem as notificações."],
                    ["Descrição", "Caixa de texto para explicar o pedido."],
                ], [1, 3.6]),
                ("p", "A urgência começa em Média e a prioridade é calculada pela matriz abaixo; técnicos podem "
                      "ajustá-las depois na tela do chamado."),
                ("h3", "Matriz de prioridade (urgência × impacto)"),
                ("table", ["Impacto \\ Urgência", "1 Muito baixa", "2 Baixa", "3 Média", "4 Alta", "5 Muito alta"], [
                    ["1 Muito baixo", "Muito baixa", "Muito baixa", "Baixa", "Baixa", "Baixa"],
                    ["2 Baixo", "Muito baixa", "Baixa", "Baixa", "Média", "Média"],
                    ["3 Médio", "Baixa", "Baixa", "Média", "Alta", "Alta"],
                    ["4 Alto", "Baixa", "Média", "Alta", "Alta", "Muito alta"],
                    ["5 Muito alto", "Baixa", "Média", "Alta", "Muito alta", "Muito alta"],
                ], [1.3, 1, 1, 1, 1, 1]),
                ("p", "A prioridade 6 (**Crítica**) só pode ser definida manualmente por quem tem permissão de "
                      "edição."),
                ("h3", "Tela do chamado"),
                ("fig", ["40-ticket", "ticket"], "Detalhe do chamado: linha do tempo, ações e propriedades."),
                ("table", ["Ação", "Efeito"], [
                    ["Assumir", "Atribui o chamado a você e marca o início do atendimento (TTO)."],
                    ["Solucionar", "Registra a solução e o tipo de solução. O requerente aceita (fecha) ou recusa "
                                   "(reabre)."],
                    ["Alterar status", "Move para outro status permitido do fluxo."],
                    ["Salvar", "Grava as propriedades alteradas."],
                    ["Menu …", "Atualizar, Mover para a lixeira / Restaurar, Excluir permanentemente."],
                ], [1, 3.6]),
                ("table", ["Aba", "Conteúdo"], [
                    ["Linha do tempo", "Abertura, acompanhamentos (públicos ou privados, só para técnicos), tarefas, "
                                       "soluções e aprovações em ordem. A caixa no rodapé envia acompanhamentos e cria "
                                       "tarefas; o menu **Resposta pronta** insere um texto cadastrado, já com o nome "
                                       "do requerente e o número do item."],
                    ["Descrição", "Título e texto completo do chamado (e, em problemas e mudanças, os campos de "
                                  "análise)."],
                    ["Tarefas", "Ações técnicas com status (A fazer, Informativa, Concluída), técnico, categoria, "
                                "duração, início e término planejados. O tempo gasto é somado no chamado."],
                    ["Aprovações", "Pede aprovação a outra pessoa (nunca a si mesmo), com mensagem; o aprovador "
                                   "aceita ou recusa com resposta."],
                    ["Vínculos", "Relaciona com outros chamados, problemas ou mudanças (Relacionado a, Duplica, "
                                 "Causado por, Depende de) e com ativos afetados."],
                    ["Pessoas", "Responsáveis (um ou mais) e seguidores do item, editáveis por qualquer pessoa que "
                                "veja o item, e os grupos que acompanham."],
                    ["Documentos / Histórico", "Anexos e auditoria campo a campo."],
                ], [1.2, 3.4]),
                ("fig", ["41-ticket-timeline"], "Aba Descrição do chamado."),
                ("h3", "Responsáveis e seguidores"),
                ("bullets", [
                    "**Adicionar responsável** e **Adicionar seguidor** abrem uma busca por nome; o x de cada pessoa "
                    "remove. Clique em **Salvar pessoas** para gravar.",
                    "Qualquer pessoa que veja o item pode alterar responsáveis e seguidores, independentemente do "
                    "perfil. Itens fechados ou cancelados não mudam mais.",
                    "O primeiro responsável é o principal (campo Técnico das propriedades). Os demais aparecem em "
                    "Meus chamados e no filtro Somente meus do Kanban.",
                    "Responsáveis novos recebem notificação. Seguidores recebem as atualizações de status.",
                ]),
                ("h3", "Status do chamado"),
                ("table", ["Status", "Significado"], [
                    ["Novo", "Aberto e ainda sem técnico."],
                    ["Em atendimento", "Atribuído a um técnico ou grupo (muda sozinho ao atribuir)."],
                    ["Planejado", "Atendimento agendado."],
                    ["Pendente", "Aguardando terceiros ou o requerente. O tempo pendente não conta no SLA."],
                    ["Solucionado", "Solução registrada, aguardando o aceite do requerente. Fecha sozinho depois de "
                                    "alguns dias (padrão: 7)."],
                    ["Fechado", "Concluído. O requerente pode responder à pesquisa de satisfação (1 a 5 estrelas e "
                                "comentário)."],
                ], [1.1, 4]),
                ("h3", "SLA"),
                ("p", "Cada chamado recebe um prazo de atendimento (TTO) e um de solução (TTR) conforme a prioridade, "
                      "contados em horário comercial, sem feriados e sem o tempo pendente. Quando resta pouco tempo "
                      "(padrão: 25%) o técnico é avisado; quando o prazo estoura, o chamado aparece em **Atrasados** e "
                      "uma notificação é enviada. No Kanban e nas listas o prazo aparece em âmbar (em risco) ou "
                      "vermelho (vencido)."),
                ("h2", "Kanban"),
                ("fig", ["12-kanban", "kanban"], "Kanban de chamados: contadores por coluna, cartões com categoria, "
                                                 "responsáveis, prazo e tempo trabalhado."),
                ("bullets", [
                    "**Quadros:** Chamados, Problemas, Mudanças e Tarefas de projeto (com filtro por projeto). "
                    "**Somente meus** mostra os itens em que você é responsável, principal ou não.",
                    "**Arrastar e soltar** muda o status respeitando o fluxo. Soltar em Solucionado abre o chamado "
                    "para registrar a solução.",
                    "**Criar no quadro:** o botão **Novo chamado** ou **Adicionar chamado** no rodapé de cada coluna "
                    "aberta abre o formulário de novo item. O cartão aparece destacado na coluna escolhida.",
                    "Clique duplo no cartão abre o item.",
                ]),
                ("table", ["Cabeçalho da coluna", "Significado"], [
                    ["Nome e ponto colorido", "Status da coluna, na cor do status."],
                    ["Pilha", "Quantidade de itens na coluna."],
                    ["Relógio", "Tempo trabalhado somado dos itens (min, h ou d)."],
                    ["Sino", "Itens atrasados (prazo vencido). Fica destacado quando há atrasos."],
                    ["Menu …", "Adicionar item na coluna e ordenar por Prioridade, Data de criação, Prazo ou Título. "
                               "Uma faixa mostra a ordenação ativa; o x volta à ordem padrão."],
                    ["Adicionar chamado", "Rodapé da coluna: cria o item já nessa coluna."],
                ], [1.3, 3.3]),
                ("table", ["Cartão", "Significado"], [
                    ["Etiquetas", "Categoria (quadrado com a cor da categoria) e tipo do item (Incidente, Requisição, "
                                  "Padrão...)."],
                    ["#número e título", "Identificação do item."],
                    ["Avatares", "Responsáveis (até quatro iniciais e +N). Sem responsável, um ícone de pessoa abre "
                                 "a escolha."],
                    ["Data", "Prazo de solução (ex.: 30 set). Âmbar quando em risco, vermelho quando vencido; sem "
                             "prazo aparece um calendário."],
                    ["Bandeira", "Prioridade: cinza (até média), âmbar (alta) e vermelho (muito alta ou crítica)."],
                    ["Tempo 00:00:00", "Tempo trabalhado no item (soma das tarefas)."],
                    ["Tarefas 0/0", "Tarefas concluídas / tarefas do item, com barra de progresso."],
                    ["Fundo destacado", "Cartão atrasado: prazo vencido e item ainda aberto."],
                    ["Menu …", "Abrir, Responsáveis e seguidores, Mover para (técnicos)."],
                ], [1.3, 3.3]),
                ("h3", "Categorias de chamados"),
                ("bullets", [
                    "Disponível para o administrador (permissão de criar ou excluir em Listas e cadastros): botão de "
                    "etiquetas no topo do Kanban e ao lado do campo Categoria do novo item.",
                    "Para criar, digite o nome, escolha opcionalmente a categoria de cima (subcategoria) e a cor, e "
                    "clique em **Adicionar**.",
                    "Para excluir, clique na lixeira. As subcategorias também são excluídas e os itens dessas "
                    "categorias ficam sem categoria; a janela avisa quantos itens serão afetados.",
                ]),
                ("h2", "Problemas"),
                ("fig", ["13-problems"], "Lista de problemas."),
                ("p", "Um problema reúne a causa raiz de incidentes recorrentes. Além dos campos do chamado, tem "
                      "**Sintomas**, **Causa raiz** e **Análise de impacto**. Fluxo: Novo › Aceito › Em atendimento › "
                      "Planejado › Pendente › Solucionado › Fechado. Use a aba **Vínculos** para ligar os chamados "
                      "causados pelo problema."),
                ("h2", "Mudanças"),
                ("fig", ["14-changes"], "Lista de mudanças."),
                ("p", "Mudanças planejam alterações no ambiente com controle de risco e aprovação."),
                ("table", ["Campo", "Uso"], [
                    ["Tipo de mudança", "Padrão (pré-aprovada), Normal ou Emergencial."],
                    ["Risco", "Baixo, Médio ou Alto."],
                    ["Análise de impacto, Plano de implantação, Plano de retorno, Checklist",
                     "Textos que documentam o que será feito e como voltar atrás."],
                    ["Início e término planejados", "Janela de execução."],
                    ["Aprovação", "Não solicitada, Aguardando aprovação, Aprovada ou Recusada."],
                ], [1.8, 2.8]),
                ("p", "Fluxo: Novo › Em avaliação › Em aprovação › Aceito › Pendente › Em teste › Qualificação › "
                      "Aplicado › Em revisão › Fechado (ou Cancelado)."),
                ("h2", "Agenda"),
                ("fig", ["15-planning"], "Agenda semanal com as tarefas planejadas de cada técnico."),
                ("p", "Mostra, por técnico e por dia, as tarefas com início e término planejados. Use **Semana "
                      "anterior**, **Esta semana** e **Próxima semana** para navegar; clique em um bloco para abrir o "
                      "chamado."),
                ("h3", "Exportar para o calendário (.ics)"),
                ("steps", [
                    "Na Agenda, clique em **Exportar .ics** e escolha onde salvar o arquivo.",
                    "O arquivo traz um evento para cada chamado aberto com prazo de solução, com número, título, "
                    "prioridade e status na descrição.",
                    "Importe no **Outlook** (Arquivo › Abrir e Exportar › Importar), no **Google Agenda** "
                    "(Configurações › Importar e exportar) ou no **Apple Calendário** (Arquivo › Importar).",
                ]),
                ("note", "Dica.", "O arquivo é uma fotografia do momento. Exporte de novo quando quiser atualizar os "
                                  "prazos no seu calendário."),
                ("h2", "Chamados recorrentes"),
                ("fig", ["25-recurring"], "Modelos de chamados recorrentes."),
                ("p", "Modelos que abrem chamados automaticamente em intervalos (a cada N dias, semanas, meses ou "
                      "anos), para rotinas como backup, troca de senhas ou verificações mensais. Cada modelo tem "
                      "título, descrição, tipo, categoria, urgência, impacto, técnico, grupo, próxima execução e o "
                      "botão **Gerar agora**. Os chamados gerados guardam a recorrência de origem."),
            ],
        },
        {
            "number": "05", "eyebrow": "gestão", "title": "Projetos",
            "lead": "Projetos com tarefas, Kanban, Gantt com dependências e caminho crítico, e o portfólio.",
            "blocks": [
                ("fig", ["16-projects"], "Lista de projetos com estado, prioridade, datas e progresso."),
                ("p", "Cada projeto tem código, nome, descrição, estado (Planejado, Em andamento, Em pausa, Concluído, "
                      "Cancelado), prioridade, responsável, grupo, datas planejadas e reais, progresso, projeto pai e "
                      "cor."),
                ("table", ["Aba", "Conteúdo"], [
                    ["Visão geral", "Dados do projeto e indicadores: tarefas, concluídas, em andamento, atrasadas e "
                                    "no caminho crítico."],
                    ["Tarefas", "Lista de tarefas com status (A fazer, Em andamento, Em revisão, Bloqueada, "
                                "Concluída), datas, progresso, técnico, esforço em horas, marco e predecessoras."],
                    ["Kanban", "Tarefas do projeto em colunas por status."],
                    ["Gantt", "Cronograma com dependências e caminho crítico."],
                    ["Equipe", "Membros e papéis."],
                    ["Documentos / Histórico", "Anexos e auditoria."],
                ], [1.2, 3.4]),
                ("fig", ["42-project"], "Visão geral do projeto."),
                ("fig", ["43-project-kanban"], "Kanban das tarefas do projeto."),
                ("h2", "Gantt"),
                ("fig", ["44-project-gantt", "gantt"], "Gantt do projeto: barras com progresso, dependências e a linha "
                                                       "de hoje."),
                ("bullets", [
                    "Escalas **Dia**, **Semana** e **Mês**; o botão **Hoje** centraliza a data atual.",
                    "Arraste a barra para mover a tarefa e a borda direita para mudar a duração.",
                    "**Nova dependência** liga uma tarefa à predecessora, com intervalo em dias. **Reprogramar "
                    "dependências** empurra as datas das tarefas seguintes para respeitar as ligações.",
                    "**Destacar caminho crítico** marca as tarefas que atrasam o projeto inteiro se atrasarem. Marcos "
                    "aparecem como losangos.",
                ]),
                ("h2", "Portfólio"),
                ("fig", ["17-portfolio"], "Portfólio: todos os projetos ativos em uma única linha do tempo."),
                ("p", "Mostra todos os projetos ativos em uma linha do tempo, com a cor de cada projeto e o progresso. "
                      "Clique duplo em um projeto para abri-lo."),
            ],
        },
        {
            "number": "06", "eyebrow": "ativos", "title": "Inventário",
            "lead": "Ativos, software e licenças, contratos, fornecedores e documentos.",
            "blocks": [
                ("h2", "Ativos"),
                ("fig", ["18-assets", "assets"], "Inventário de ativos por tipo, com patrimônio, local, responsável e "
                                                 "garantia."),
                ("p", "Tipos: Computador, Notebook, Servidor, Monitor, Impressora, Equipamento de rede, Telefone, "
                      "Tablet, Periférico e Outro. O número de patrimônio é gerado sozinho com o prefixo configurado "
                      "(padrão `PYH-`) quando fica em branco."),
                ("table", ["Seção", "Campos"], [
                    ["Identificação", "Nome, tipo, patrimônio, número de série, estado (Em uso, Em estoque, Em "
                                      "manutenção, Emprestado, Descartado), fabricante, modelo."],
                    ["Uso e responsáveis", "Unidade, localização, usuário, grupo, técnico responsável, grupo "
                                           "responsável."],
                    ["Dados técnicos", "Hostname, IP, MAC, sistema operacional e versão, processador, memória (MB), "
                                       "armazenamento (GB), último inventário."],
                    ["Compra e garantia", "Fornecedor, data e valor de compra, fim da garantia, observações."],
                ], [1.2, 3.4]),
                ("fig", ["45-asset"], "Ficha do ativo com as abas Geral, Software, Conexões, Contratos, "
                                      "Atendimentos, Documentos e Histórico."),
                ("table", ["Aba", "Conteúdo"], [
                    ["Geral", "Todos os campos acima. Alerta de garantia terminando ou vencida."],
                    ["Software", "Programas instalados, versão e licença usada."],
                    ["Conexões", "A que switch, roteador ou outro ativo o equipamento está ligado."],
                    ["Contratos", "Contratos que cobrem o ativo."],
                    ["Atendimentos", "Chamados, problemas e mudanças que envolvem o ativo; **Abrir chamado** já cria o "
                                     "chamado vinculado."],
                ], [1.2, 3.4]),
                ("h3", "Importar e exportar CSV"),
                ("p", "**Importar CSV** lê uma planilha separada por ponto e vírgula. Linhas com patrimônio existente "
                      "atualizam o ativo; as demais criam ativos novos. O botão **Salvar modelo** gera a planilha com "
                      "as colunas certas. Qualquer lista exporta para CSV, com proteção contra fórmulas maliciosas ao "
                      "abrir no Excel."),
                ("h2", "Software e licenças"),
                ("fig", ["19-software"], "Catálogo de software com a situação das licenças."),
                ("p", "O catálogo registra cada software (nome, editora, categoria). A aba **Licenças** guarda nome, "
                      "chave, quantidade de instalações, validade, fornecedor e valor; a aba **Instalações** liga o "
                      "software aos ativos, com versão, licença e data. A coluna **Situação** compara licenças "
                      "adquiridas com instalações e avisa quando há excesso."),
                ("h2", "Contratos"),
                ("fig", ["20-contracts"], "Contratos com período, renovação e aviso prévio."),
                ("p", "Contratos de manutenção, suporte, locação, licenciamento ou garantia estendida, com número, "
                      "tipo, fornecedor, início, duração em meses, aviso prévio em meses, renovação (Sem renovação, "
                      "Renovação automática, Renovação expressa), valor e observações. O período mostra a data limite "
                      "do aviso prévio. A aba **Ativos cobertos** liga o contrato aos equipamentos."),
                ("h2", "Fornecedores"),
                ("fig", ["21-suppliers"], "Fornecedores."),
                ("p", "Empresas que fornecem equipamentos, software e serviços: nome, tipo (Fabricante, Revenda, "
                      "Prestador de serviços, Operadora), e-mail, telefone, site, endereço, cidade e se está ativo."),
                ("h2", "Documentos"),
                ("fig", ["22-documents"], "Documentos ligados a chamados e ativos."),
                ("p", "O Talos. guarda apenas o caminho ou o link do documento, nunca o arquivo. São aceitos endereços "
                      "http(s), e-mail (mailto) e arquivos de rede não executáveis. Arquivos como .exe, .bat, .cmd, "
                      ".ps1, .vbs, .js, .msi, .lnk, .jar e similares são recusados, e o endereço é mostrado para "
                      "confirmação antes de abrir. Cada documento pode ser anexado a vários itens pela aba "
                      "Documentos."),
            ],
        },
        {
            "number": "07", "eyebrow": "conhecimento", "title": "Conhecimento e autoatendimento",
            "lead": "Base de conhecimento, relatórios e a interface simplificada para usuários finais.",
            "blocks": [
                ("h2", "Base de conhecimento"),
                ("fig", ["23-knowledge"], "Base de conhecimento com categorias, rascunhos e FAQ."),
                ("bullets", [
                    "Artigos em Markdown: `#` para títulos, `**negrito**`, `*itálico*`, código entre crases e `-` "
                    "para listas, com pré-visualização ao lado.",
                    "Campos: título, categoria, conteúdo, **Exibir no FAQ do autoatendimento** e **Publicado** "
                    "(desmarcado = rascunho).",
                    "O leitor mostra autor, data de atualização e número de visualizações. O filtro **Somente FAQ** "
                    "lista as respostas públicas.",
                ]),
                ("h2", "Relatórios"),
                ("fig", ["24-reports", "reports"], "Relatórios com gráfico, tabela e exportação CSV."),
                ("table", ["Relatório", "O que mostra"], [
                    ["Chamados por mês", "Abertos e solucionados, incidentes e requisições."],
                    ["Chamados por categoria", "Volume e tempo médio de solução."],
                    ["Desempenho dos técnicos", "Atribuídos, solucionados, tempo gasto e satisfação."],
                    ["Cumprimento de SLA", "Chamados com SLA cumprido e estourado por mês."],
                    ["Satisfação", "Distribuição das notas dadas pelos requerentes."],
                    ["Ativos por tipo", "Composição do parque de equipamentos."],
                    ["Ativos por localização", "Locais com mais equipamentos."],
                    ["Garantias a vencer", "Ativos com garantia terminando no período."],
                    ["Contratos a vencer", "Término e data limite de aviso prévio."],
                    ["Conformidade de licenças", "Licenças adquiridas versus instalações."],
                ], [1.6, 3]),
                ("h2", "Autoatendimento"),
                ("fig", ["36-home"], "Tela Início do autoatendimento."),
                ("p", "Usuários com o perfil **Autoatendimento** veem uma interface simplificada: **Abrir um "
                      "chamado**, **Meus chamados** (acompanhar, responder, aceitar ou recusar soluções e avaliar o "
                      "atendimento) e as **Perguntas frequentes** da base de conhecimento."),
            ],
        },
        {
            "number": "08", "eyebrow": "configuração", "title": "Administração",
            "lead": "Pessoas, permissões, listas, regras, SLAs, integrações, auditoria e configurações.",
            "blocks": [
                ("h2", "Usuários"),
                ("fig", ["26-users"], "Usuários com perfil, último acesso e situação."),
                ("p", "Cadastro de quem abre e atende chamados: nome completo, login, e-mail, telefone, cargo, idioma, "
                      "unidade padrão, localização, ativo e **Trocar senha no próximo login**."),
                ("table", ["Aba", "Conteúdo"], [
                    ["Conta", "Dados pessoais, último acesso, tentativas falhas e bloqueio. Ações: **Redefinir "
                              "senha** (obriga a troca no próximo login) e **Desbloquear**."],
                    ["Habilitações", "Perfil + unidade organizacional (com ou sem subunidades). Sem habilitação a "
                                     "pessoa não consegue entrar."],
                    ["Grupos", "Grupos de que a pessoa participa."],
                    ["Ativos / Chamados", "Equipamentos em uso e chamados da pessoa."],
                ], [1.2, 3.4]),
                ("h2", "Grupos"),
                ("fig", ["27-groups"], "Grupos e membros."),
                ("p", "Equipes técnicas e grupos de requerentes, com unidade, grupo pai, e-mail, **Atribuível** (pode "
                      "receber chamados) e **Requerente** (pode abrir chamados como grupo). Na lista de membros, "
                      "**Gestor** marca quem coordena o grupo."),
                ("h2", "Perfis e permissões"),
                ("fig", ["28-profiles", "profiles"], "Perfis com a grade de permissões por módulo."),
                ("table", ["Direito", "Permite"], [
                    ["Ler", "Ver os itens do módulo"],
                    ["Criar / Editar / Excluir", "Incluir, alterar e mover para a lixeira"],
                    ["Expurgar", "Excluir permanentemente"],
                    ["Atribuir", "Escolher técnico e grupo"],
                    ["Ver todos", "Ver itens de todos, não só os seus"],
                    ["Aprovar", "Responder pedidos de aprovação"],
                ], [1.4, 3.2]),
                ("table", ["Perfil padrão", "Para quem"], [
                    ["Super-Administrador", "Configura tudo, inclusive as integrações. Perfil protegido: não pode ser "
                                            "editado nem retirado do último administrador ativo."],
                    ["Supervisor", "Coordena a equipe: vê e atribui todos os chamados, aprova mudanças, vê "
                                   "relatórios; administra grupos, regras e SLAs."],
                    ["Técnico", "Atende chamados e problemas, cuida do inventário, documentos, base de conhecimento e "
                                "tarefas de projeto."],
                    ["Observador", "Apenas consulta."],
                    ["Autoatendimento", "Usuários finais: abrem e acompanham os próprios chamados e leem o FAQ. Perfil "
                                        "padrão para novos usuários."],
                ], [1.4, 3.2]),
                ("p", "Perfis podem ser duplicados e ajustados. Ninguém consegue conceder a si mesmo ou a outra pessoa "
                      "mais direitos do que já tem."),
                ("h2", "Unidades organizacionais"),
                ("fig", ["29-org_units"], "Árvore de unidades organizacionais."),
                ("p", "Hierarquia de empresa, filiais e departamentos (as \"entidades\" do GLPI). Quase todo registro "
                      "pertence a uma unidade, e as habilitações definem quais unidades cada pessoa enxerga. Use "
                      "**Nova subunidade** para criar níveis abaixo."),
                ("h2", "Listas e cadastros"),
                ("fig", ["30-dropdowns"], "Listas usadas nos formulários."),
                ("table", ["Lista", "Observação"], [
                    ["Localizações, Categorias de chamado, Categorias da base de conhecimento",
                     "Hierárquicas (item superior). As categorias de chamado também podem ser criadas e excluídas no "
                     "Kanban."],
                    ["Estados de ativo, Estados de projeto", "Com cor e marcação de estado final."],
                    ["Fabricantes, Modelos de ativo, Sistemas operacionais, Origens de requisição, Tipos de solução, "
                     "Categorias de tarefa, Categorias de software, Tipos de fornecedor, Tipos de contrato, Categorias "
                     "de documento", "Listas simples, com ordem e ativo/inativo."],
                    ["Respostas prontas", "Nome da resposta e texto em **Observações**. Campos aceitos: {requester} "
                     "(primeiro nome do requerente), {reference} (número do item), {title} (título) e {me} (técnico "
                     "que responde). Aparecem no menu **Resposta pronta** da linha do tempo."],
                ], [2.6, 2]),
                ("h2", "Regras de negócio"),
                ("fig", ["31-rules"], "Regras com critérios, ações e o testador."),
                ("p", "Regras classificam e atribuem chamados automaticamente. Cada regra tem gatilho (**Ao abrir** "
                      "ou **Ao atualizar**), ordem, ativo, combinação (todos os critérios ou qualquer um) e a opção de "
                      "parar as regras seguintes."),
                ("table", ["Parte", "Opções"], [
                    ["Campos (SE)", "Título, Descrição, Categoria, Tipo, Urgência, Impacto, Prioridade, Localização, "
                                    "Origem, Requerente, Grupo do requerente, Unidade organizacional"],
                    ["Operadores", "é, não é, contém, não contém, começa com, termina com, maior que, menor que, está "
                                   "vazio, corresponde à expressão (regex, até 300 caracteres)"],
                    ["Ações (ENTÃO)", "atribuir ao grupo, atribuir ao técnico, definir categoria, tipo, urgência, "
                                      "impacto, prioridade ou localização, aplicar SLA de atendimento ou de solução, "
                                      "adicionar observador, solicitar aprovação"],
                ], [1.1, 3.5]),
                ("p", "O **testador** simula um chamado de exemplo e mostra quais regras seriam aplicadas."),
                ("h2", "SLAs e calendário"),
                ("fig", ["32-slas"], "SLAs por prioridade, horário comercial e feriados."),
                ("table", ["SLA criado na instalação", "Tipo", "Prioridade", "Prazo"], [
                    ["Atendimento padrão", "TTO", "qualquer", "2 h"],
                    ["Atendimento crítico", "TTO", "5 Muito alta", "30 min"],
                    ["Atendimento grave", "TTO", "6 Crítica", "15 min"],
                    ["Resolução P1 a P6", "TTR", "1 a 6", "40 h, 24 h, 16 h, 8 h, 4 h, 2 h"],
                ], [1.6, 0.6, 1.1, 1.6]),
                ("p", "Todos contam em horário comercial (padrão: segunda a sexta, 08:00 às 18:00). A aba "
                      "**Feriados** traz os feriados nacionais, que se repetem todo ano, e aceita feriados locais."),
                ("h2", "Integrações"),
                ("fig", ["33-integrations", "integrations"], "Central de integrações: webhooks, eventos e registro das "
                                                             "últimas entregas."),
                ("p", "Em **Administração › Integrações** você conecta o Talos. às ferramentas que a equipe já usa. "
                      "Cada webhook recebe os eventos escolhidos assim que eles acontecem. O envio é **assíncrono** "
                      "(a tela nunca trava) e cada tentativa tem **tempo limite de 10 segundos**."),
                ("table", ["Destino", "Formato da mensagem"], [
                    ["Slack", "Mensagem com o número, o título e o status do item (Incoming Webhook)."],
                    ["Microsoft Teams", "Cartão de mensagem no canal (webhook de entrada / Workflows)."],
                    ["Discord", "Mensagem no canal com o resumo do evento (webhook do canal)."],
                    ["Google Chat", "Mensagem no espaço (webhook do espaço)."],
                    ["JSON genérico", "POST com o evento completo em JSON, para scripts, automações (n8n, Power "
                                      "Automate, Zapier) e sistemas próprios. Pode ser assinado com HMAC-SHA256."],
                ], [1.2, 3.4]),
                ("table", ["Evento", "Quando é enviado"], [
                    ["Item criado", "Chamado, problema ou mudança aberto."],
                    ["Item atribuído", "Responsável ou grupo técnico definido ou alterado."],
                    ["Status alterado", "Qualquer mudança de status do fluxo."],
                    ["Item solucionado", "Solução registrada."],
                    ["Alertas de vencimento", "Garantias, contratos (aviso prévio) e licenças excedidas, junto com a "
                                              "rotina diária de alertas."],
                ], [1.4, 3.2]),
                ("h3", "Conectar o Slack ou o Teams"),
                ("steps", [
                    "No Slack, crie um **Incoming Webhook** para o canal desejado; no Teams, crie um webhook de entrada "
                    "(ou um fluxo do Workflows que recebe solicitações HTTP) e copie a URL.",
                    "No Talos., abra **Administração › Integrações** e clique em **Novo webhook**.",
                    "Dê um nome, escolha o destino (Slack, Microsoft Teams, Discord, Google Chat ou JSON genérico), "
                    "cole a URL e marque os eventos.",
                    "Clique em **Enviar teste**. A mensagem de teste chega no canal e o resultado (status HTTP e "
                    "horário) aparece em **Últimas entregas**.",
                    "Salve. Use o botão de pausa (ou a opção **Ativo**) para suspender o webhook sem apagar a configuração.",
                ]),
                ("h3", "Webhook JSON genérico"),
                ("p", "O corpo da requisição é um JSON com o evento, o momento, o item e quem fez a ação:"),
                ("code", """{
  "event": "item.solved",
  "occurredAt": "2026-10-27T14:32:05-03:00",
  "organization": "Acme Ltda.",
  "item": {
    "type": "Ticket",
    "id": 125,
    "reference": "#125",
    "title": "Outlook não sincroniza e-mails",
    "status": "SOLVED",
    "priority": 4
  },
  "actor": "Ana Souza",
  "detail": null,
  "text": "Chamado #125 — solucionado — Outlook não sincroniza e-mails"
}"""),
                ("p", "Códigos de evento: `item.created`, `item.assigned`, `item.status_changed`, `item.solved`, "
                      "`inventory.expiration_alert` e `integration.test` (botão **Enviar teste**). O código também "
                      "vai no cabeçalho `X-Talos-Event`."),
                ("p", "Se você informar um **segredo**, cada envio leva o cabeçalho `X-Talos-Signature` com o "
                      "HMAC-SHA256 do corpo em hexadecimal (`sha256=<assinatura>`). Calcule o mesmo HMAC com o segredo "
                      "no sistema que recebe e compare antes de confiar na mensagem."),
                ("note", "Boas práticas.", "Use sempre URLs `https://`. Trate a URL do webhook como senha: quem a "
                                           "tiver pode publicar no canal. Troque-a no Slack ou no Teams se ela "
                                           "vazar.", "warn"),
                ("h2", "Auditoria"),
                ("fig", ["34-audit"], "Auditoria: quem alterou o quê, e quando."),
                ("p", "Registra criação, alteração, lixeira, restauração e exclusão de cada item, com usuário, data, "
                      "campo, valor anterior e valor novo."),
                ("h2", "Configurações"),
                ("fig", ["35-settings"], "Configurações da organização, aparência e banco de dados."),
                ("table", ["Configuração", "Padrão", "Limites"], [
                    ["Nome da organização", "definido na instalação", "—"],
                    ["Fechar chamados solucionados após (dias)", "7", "—"],
                    ["Pesquisa de satisfação ao fechar", "ligada", "—"],
                    ["Alerta de SLA (% restante)", "25", "—"],
                    ["Alertar vencimentos com antecedência (dias)", "30", "—"],
                    ["Guardar notificações por (dias)", "90", "—"],
                    ["Prefixo do patrimônio", "PYH-", "—"],
                    ["Tentativas antes do bloqueio", "5", "3 a 10"],
                    ["Tempo de bloqueio (min)", "15", "5 a 1440"],
                    ["Encerrar sessão sem uso após (min)", "15", "1 a 240"],
                ], [2.8, 1.3, 0.9]),
                ("p", "**Aparência e idioma** valem só para este computador. Em **Banco de dados** você vê a conexão, "
                      "o servidor e o arquivo de configuração, e pode trocar a conexão (o programa reinicia)."),
            ],
        },
        {
            "number": "09", "eyebrow": "automação", "title": "Notificações e rotinas automáticas",
            "lead": "O que o Talos. faz sozinho, em segundo plano.",
            "blocks": [
                ("p", "As rotinas rodam em segundo plano em cada computador conectado. Uma trava no banco "
                      "(`pyh_job_lock`) garante que só um computador execute cada rotina por vez."),
                ("table", ["Rotina", "Frequência", "O que faz"], [
                    ["Monitor de SLA", "a cada minuto", "Avisa quando o prazo entra em risco e quando estoura."],
                    ["Chamados recorrentes", "a cada minuto", "Abre os chamados cujos modelos estão na hora."],
                    ["Fechamento automático", "a cada hora", "Fecha chamados solucionados há mais dias que o "
                                                             "configurado."],
                    ["Alertas de vencimento", "uma vez por dia", "Garantias e contratos (aviso prévio) a vencer e "
                                                                 "licenças excedidas; também enviados às integrações."],
                    ["Limpeza", "uma vez por dia", "Apaga notificações mais antigas que o período de retenção."],
                ], [1.4, 1.1, 2.6]),
                ("p", "Também geram notificações: chamado criado, atribuído ou com status alterado, novo "
                      "acompanhamento, tarefa atribuída, pedido de aprovação e resposta, e tarefa de projeto "
                      "atribuída."),
                ("h2", "Notificações fora do Talos."),
                ("p", "Os eventos de chamados, problemas e mudanças (criado, atribuído, status alterado e "
                      "solucionado) e os alertas de vencimento também podem ser publicados em Slack, Microsoft Teams, "
                      "Discord, Google Chat ou em qualquer sistema que receba JSON. Veja **Integrações** no capítulo "
                      "8."),
            ],
        },
        {
            "number": "10", "eyebrow": "segurança", "title": "Segurança e privacidade",
            "lead": "Proteções aplicadas por padrão, sem configuração extra.",
            "blocks": [
                ("table", ["Área", "Proteção"], [
                    ["Senhas", "PBKDF2-HMAC-SHA256 com 600 mil iterações e sal aleatório; hashes antigos são "
                               "atualizados no login. Política forte de senha."],
                    ["Login", "Mensagem de erro única e tempo constante (não revela se o usuário existe), bloqueio "
                              "por tentativas e atraso progressivo."],
                    ["Sessão", "Encerra por inatividade e é revalidada a cada 30 segundos (usuário desativado ou "
                               "bloqueado sai na hora)."],
                    ["Permissões", "Sem escalada de privilégio, perfil Super-Administrador protegido, último "
                                   "administrador não pode ser removido, ninguém aprova o próprio pedido."],
                    ["Dados", "Senha do banco criptografada com DPAPI do Windows; arquivos de configuração com "
                              "acesso restrito; SQL Server com conexão criptografada e validação de certificado; H2 "
                              "sem porta de rede."],
                    ["Integrações", "Só administradores configuram webhooks; envio assíncrono com tempo limite; "
                                    "assinatura HMAC-SHA256 opcional no JSON genérico; nenhuma senha ou dado pessoal "
                                    "além do nome de quem fez a ação."],
                    ["Arquivos", "Links de documentos restritos e confirmados; CSV exportado sem fórmulas; "
                                 "expressões regulares com limite de tempo."],
                    ["Banco", "Flyway com limpeza desativada e validação das migrações; logs sem comandos SQL nem "
                              "valores."],
                    ["Programa", "Java sem anexação externa e sem despejo de memória em caso de erro."],
                ], [1.1, 3.5]),
            ],
        },
        {
            "number": "11", "eyebrow": "referência", "title": "Informações técnicas",
            "lead": "Tecnologias, arquitetura, banco de dados, compilação e testes.",
            "blocks": [
                ("h2", "Tecnologias"),
                ("table", ["Componente", "Versão / uso"], [
                    ["Java", "21 (embutido no instalador)"],
                    ["Spring Boot", "4.0.8, sem servidor web (apenas injeção, JPA, transações e Flyway)"],
                    ["Hibernate ORM / JPA", "7, mapeamento das entidades"],
                    ["Flyway", "11, criação e evolução do esquema nos cinco bancos"],
                    ["HikariCP", "Pool de conexões"],
                    ["JavaFX", "21.0.8, interface gráfica (CSS próprio, ícones Lucide, fontes IBM Plex)"],
                    ["java.net.http", "Cliente HTTP nativo do Java, usado pelos webhooks (sem dependências extras)"],
                    ["JNA", "5.16.0, acesso ao DPAPI do Windows"],
                    ["Drivers JDBC", "H2, PostgreSQL, MySQL Connector/J, MariaDB, Microsoft SQL Server, Oracle "
                                     "ojdbc11"],
                    ["Empacotamento", "jpackage + WiX Toolset 3 (instalador .exe)"],
                    ["Testes", "JUnit 5 e Testcontainers (testes de compatibilidade com bancos reais)"],
                ], [1.3, 3.3]),
                ("h2", "Arquitetura"),
                ("p", "Arquitetura hexagonal em três módulos Maven. O domínio não depende de framework; a "
                      "infraestrutura é carregada pelo `ServiceLoader` (`BackendProvider`), o que permite rodar a "
                      "interface com um banco em memória (modo demonstração e testes)."),
                ("table", ["Módulo", "Responsabilidade"], [
                    ["`talos-domain`", "Entidades, regras de negócio, serviços (chamados, SLA, regras, permissões, "
                                       "inventário, projetos, integrações...), portas (`DataStore`, `Repository`) e "
                                       "armazenamento em memória."],
                    ["`talos-infrastructure`", "Spring Boot, JPA/Hibernate, Flyway, drivers, migrações dos cinco "
                                               "bancos e rotinas agendadas."],
                    ["`talos-desktop`", "Interface JavaFX, assistente de primeiro uso, configuração local, "
                                        "criptografia da senha do banco, temas, traduções, animações, zoom, paleta "
                                        "de comandos e atalhos."],
                ], [1.4, 3.2]),
                ("h2", "Banco de dados"),
                ("p", "São 43 tabelas com o prefixo `pyh_`. Toda tabela tem `id`, `version` (trava otimista), "
                      "`created_at` e `updated_at`. Os relacionamentos usam colunas `*_id` (integridade garantida pela "
                      "aplicação), itens excluíveis têm `deleted` (lixeira) e listas fixas são gravadas como texto. O "
                      "histórico das migrações fica em `pyh_schema_history`. A versão 1.1.0 não cria tabelas novas: a "
                      "configuração das integrações fica na tabela de configurações (`setting`). O modelo completo "
                      "está em `docs/banco-de-dados.md`."),
                ("table", ["Grupo", "Tabelas"], [
                    ["Organização e acesso", "org_unit, user, group, group_member, profile, profile_right, "
                                             "profile_assignment"],
                    ["Atendimento", "ticket, problem, change, itil_followup, itil_task, itil_solution, itil_approval, "
                                    "itil_observer, itil_relation, itil_asset, recurring_ticket"],
                    ["SLA e regras", "sla, holiday, rule, rule_criterion, rule_action"],
                    ["Inventário", "asset, asset_connection, software, software_license, software_install, supplier, "
                                   "contract, contract_item, document, document_link"],
                    ["Projetos", "project, project_task, project_dependency, project_member"],
                    ["Sistema", "kb_article, notification, dropdown, audit_log, setting, job_lock"],
                ], [1.3, 3.3]),
                ("h3", "Preparar um servidor"),
                ("p", "Crie um banco vazio e um usuário dono dele; as tabelas são criadas pelo Talos. Exemplo para "
                      "PostgreSQL:"),
                ("code", """CREATE USER talos WITH PASSWORD 'troque-esta-senha';
CREATE DATABASE talos OWNER talos ENCODING 'UTF8';"""),
                ("p", "Os comandos para MySQL/MariaDB, SQL Server e Oracle estão no README e em "
                      "`docs/banco-de-dados.md`."),
                ("h2", "Estrutura do repositório"),
                ("code", """Information-Technology-Service-Management/
+-- talos-domain/            regras de negócio (sem framework)
+-- talos-infrastructure/    Spring Boot, JPA, Flyway, drivers
|   `-- src/main/resources/db/migration/{h2,postgresql,mysql,sqlserver,oracle}
+-- talos-desktop/           interface JavaFX (css, i18n, ícones, fontes)
+-- packaging/windows/       ícone, imagens e limpeza do instalador
+-- scripts/                 build-windows.cmd, build-windows.ps1
|   `-- manual/              gerador deste manual (ReportLab)
+-- docs/
|   +-- banco-de-dados.md    modelo do banco
|   +-- manual/              este manual em PDF
|   `-- images/              imagens do README
`-- .github/workflows/       CI, publicação de versões e commits diários"""),
                ("h2", "Compilar o instalador"),
                ("steps", [
                    "No Windows, dê dois cliques em `scripts\\build-windows.cmd`.",
                    "O script procura (ou baixa para `.tools`) o JDK 21, o Maven e o WiX Toolset 3, sem instalar nada "
                    "no sistema.",
                    "Roda `mvn clean verify` com todos os testes e gera `Talos-1.1.0.exe` na raiz do projeto.",
                    "O log completo fica em `build-logs\\`. Use `-SkipTests` para compilar mais rápido.",
                ]),
                ("p", "Manualmente: JDK 21+ e Maven 3.9+, `mvn verify` e depois `jpackage`. Publicar uma tag `v*` no "
                      "GitHub dispara o workflow `release.yml`, que gera o instalador e o anexa à versão."),
                ("h3", "Gerar este manual"),
                ("code", """pip install reportlab
python3 scripts/manual/build_manual.py --images <pasta-dos-prints> \\
        --out "Manual-Talos-Versão 1.1.0.pdf\""""),
                ("p", "Os prints são gerados pela classe de teste `ScreenshotTour` do módulo `talos-desktop`."),
                ("h2", "Testes"),
                ("table", ["Módulo", "Cobertura"], [
                    ["`talos-domain`", "Ciclo de vida do chamado, SLA e calendário, matriz de prioridade, regras, "
                                       "segurança, permissões, CSV, documentos, configuração inicial, criação pelo "
                                       "Kanban e integrações (formato das mensagens e assinatura)."],
                    ["`talos-infrastructure`", "JPA com H2, esquema gerado igual às migrações dos cinco bancos e "
                                               "compatibilidade com bancos reais (ativada com `-Dtalos.databases`)."],
                    ["`talos-desktop`", "Paridade das traduções português/inglês."],
                ], [1.4, 3.2]),
                ("h2", "Traduções e licenças"),
                ("p", "Os textos ficam em `messages_pt_BR.properties` e `messages.properties` (inglês); o teste de "
                      "traduções garante que as duas versões tenham as mesmas chaves. O Talos. é distribuído sob a "
                      "licença MIT; ícones Lucide (ISC), fontes IBM Plex (SIL OFL 1.1) e as bibliotecas embutidas "
                      "estão listados em `THIRD-PARTY-NOTICES.md`."),
            ],
        },
        {
            "number": "12", "eyebrow": "ajuda", "title": "Solução de problemas",
            "lead": "As situações mais comuns e o que fazer em cada uma.",
            "blocks": [
                ("table", ["Situação", "O que fazer"], [
                    ["Não conecta ao banco", "Confira servidor, porta, nome do banco, usuário e senha; teste com "
                                             "**Testar conexão**; verifique firewall e se o usuário pode criar "
                                             "tabelas. Os detalhes do erro ficam em `%LOCALAPPDATA%\\Talos\\logs`."],
                    ["SQL Server recusa o certificado", "Em Parâmetros extras, use `trustServerCertificate=true` "
                                                        "(servidor com certificado autoassinado)."],
                    ["Conta bloqueada", "Espere o tempo de bloqueio ou peça a um administrador para **Desbloquear** "
                                        "em Usuários › Conta."],
                    ["Esqueci a senha", "Um administrador usa **Redefinir senha**; a nova senha precisa ser trocada no "
                                        "primeiro login."],
                    ["Sessão encerrada sozinha", "Inatividade além do limite de Configurações, ou a conta foi "
                                                 "desativada ou bloqueada."],
                    ["Webhook não chega", "Use **Enviar teste** e veja o status em **Últimas entregas**: 4xx indica "
                                          "URL errada ou revogada; tempo esgotado indica firewall ou proxy bloqueando "
                                          "a saída HTTPS. Confira se o webhook está **Ativo** e se o evento está "
                                          "marcado."],
                    ["Calendário não importa o .ics", "Importe o arquivo pelo menu de importação do calendário (não "
                                                      "por arrastar para um evento). Só chamados abertos com prazo "
                                                      "entram no arquivo."],
                    ["Interface lenta", "Atualize o driver de vídeo; o Talos. usa a placa de vídeo para desenhar a "
                                        "tela. Reduza o zoom se a tela for pequena."],
                    ["Abriu direto em \"Conectando...\"", "Há uma conexão salva neste computador. Use **Usar outro "
                                                         "banco de dados** no login ou desinstale e instale de novo "
                                                         "para começar do zero."],
                    ["Não aparece no menu Iniciar", "Reinstale: os atalhos do menu Iniciar e da área de trabalho são "
                                                    "sempre criados."],
                ], [1.4, 3.2]),
                ("h2", "Contribuir"),
                ("p", "O Talos. é um projeto de código aberto. Sugestões e correções podem ser enviadas pelo "
                      "repositório no GitHub, seguindo o `CONTRIBUTING.md`."),
            ],
        },
    ]
