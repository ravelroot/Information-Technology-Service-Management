# Contribuindo com o Pyhoma.

Obrigado pelo interesse! Este guia mostra como preparar o ambiente e as poucas convenções do projeto.

## Ambiente

- JDK 21 ou mais novo e Maven 3.9+.
- Qualquer IDE com suporte a Maven (IntelliJ IDEA, Eclipse, VS Code).
- Docker, apenas se quiser rodar os testes contra PostgreSQL, MySQL, MariaDB, SQL Server e Oracle.

```bash
mvn clean verify                                 # compila e roda os testes
java -jar pyhoma-desktop/target/app/pyhoma.jar    # abre o programa
```

Para não misturar com a sua instalação, rode com uma pasta de dados separada:

```bash
java -Dpyhoma.home=/tmp/pyhoma-dev -jar pyhoma-desktop/target/app/pyhoma.jar
```

## Onde mexer

| Quero… | Onde |
|---|---|
| Mudar uma regra de negócio | `pyhoma-domain/src/main/java/io/pyhoma/domain/service` (com teste em `src/test`) |
| Criar uma tela | `pyhoma-desktop/src/main/java/io/pyhoma/desktop/views` e a rota em `shell/Route.java` |
| Ajustar cores ou espaçamentos | `pyhoma-desktop/src/main/resources/io/pyhoma/desktop/css` (tokens em `light.css` e `dark.css`) |
| Adicionar um texto | `messages_pt_BR.properties` **e** `messages.properties` (inglês) |
| Alterar o banco | Nova migração Flyway em **todas** as pastas de `pyhoma-infrastructure/src/main/resources/db/migration` |

## Convenções

- **Domínio sem framework**: `pyhoma-domain` só depende das anotações `jakarta.persistence`. Entidades referenciam outras por id, sem associações JPA.
- **Textos sempre traduzidos**: nada de texto fixo na interface; use `I18n.t("chave")`. O teste `TranslationsTest` falha se faltar uma chave em algum idioma.
- **Migrações**: nunca edite um `V*__*.sql` já publicado; crie `V2__descricao.sql`, `V3__…` para cada banco (`h2`, `postgresql`, `mysql`, `sqlserver`, `oracle`). O `SchemaGeneratorTest` compara a linha de base com as entidades.
- **Permissões**: verifique direitos no serviço (`services.security().require(Module.X, Right.Y)`), não só na tela.
- **Dados fictícios**: exemplos, testes e capturas de tela usam apenas nomes inventados e e-mails `@example.com`.
- Estilo: 4 espaços, UTF-8, linhas de até 130 colunas, `final` em classes utilitárias.

## Pull requests

1. Crie um branch a partir de `main`.
2. Escreva ou ajuste os testes.
3. Rode `mvn verify`.
4. Descreva o que mudou e como testou.

Nunca inclua senhas, strings de conexão reais, chaves, bancos de dados ou logs com informações pessoais em commits, issues ou pull requests.
