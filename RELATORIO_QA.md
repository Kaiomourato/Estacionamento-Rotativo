# Relatório de QA – Estacionamento Rotativo

**Data dos testes:** 11/06/2026
**Ambiente testado:** API de produção (Render) – `https://estacionamento-rotativo-l65y.onrender.com` + análise estática do front-end React (`painel-estacionamento`)
**Fluxos prioritários:** Reservar Estadia (motorista) e Registrar Entrada Manual (operador)
**Metodologia:** Testes funcionais via `curl` (HTTP), revisão de código backend (Spring Boot) e frontend (React/Vite), edge cases de payload/validação/autenticação.

## Ambiente de testes criado

Para executar os testes foram criadas as seguintes contas e dados (todos com prefixo `qa.*.1781206496@teste.com`, senha `senha123`):

- 1 estacionamento de teste ("QA Teste Park", `valorHora=5.0`)
- 1 conta OPERADOR e 1 conta USER (motorista)
- 2 vagas (uma exclusiva para CARRO, outra sem restrição de tipo)
- 2 veículos (1 CARRO, 1 MOTO)

> ⚠️ **Nota para a equipe de dev:** esses registros de teste permanecem no banco de produção e podem ser removidos com segurança (estacionamento id=12, vagas id=23/24, veículos id=13/14, usuários `qa.operador.1781206496@teste.com` e `qa.motorista.1781206496@teste.com`, além de 1-2 contas extras criadas durante testes de validação de cadastro com `role` inválida e credenciais vazias).

---

## Tabela Resumo

| ID | Severidade | Módulo | Resumo do Bug |
|---|---|---|---|
| BUG-01 | Crítico | Backend | `GET /veiculos` expõe para **qualquer usuário autenticado** (incluindo role `USER`) a lista de todos os veículos do sistema, junto com os dados completos do dono — incluindo **email e hash BCrypt da senha** de todos os usuários, inclusive contas reais de produção. |
| BUG-02 | Crítico | Backend | `POST /estadias/reservar` retorna **HTTP 500** e deixa a vaga marcada como `ocupada=true` permanentemente, sem nenhuma `Estadia` associada (corrupção de dados / "vaga fantasma"). |
| BUG-03 | Alta | Backend | "Registrar entrada manual" (`POST /estadias`) exige que o veículo já esteja cadastrado no banco (`Veículo não encontrado`), contrariando o fluxo esperado pelo negócio. |
| BUG-04 | Alta | Backend | Cadastrar veículo com placa maior que 10 caracteres retorna **HTTP 500** genérico em vez de erro de validação amigável. |
| BUG-05 | Alta | Backend | Login com senha incorreta ou usuário inexistente retorna **HTTP 400** (deveria ser 401), e as mensagens de erro diferem entre os dois casos, permitindo enumeração de usuários. |
| BUG-06 | Média | Backend | `POST /auth/register` aceita `email`/`senha` vazios e qualquer string como `role` (ex.: `"SUPERADMIN"`), retornando **HTTP 201** sem validação. |
| BUG-07 | Média | Backend | Recursos inexistentes (Estadia, Vaga, Estacionamento, Veículo) retornam **HTTP 400** em vez de **404**. |
| BUG-08 | Média | Backend | `POST /veiculos` (e outros POSTs de criação) retornam **HTTP 200** em vez de **201 Created**. |
| BUG-09 | Média | Backend/Frontend | Mensagens de erro técnicas internas (ex.: erro de conversão de tipo do Spring) vazam na resposta da API e não são totalmente filtradas pelo frontend antes de exibir ao usuário. |
| BUG-10 | Média | Frontend | Timeout do axios (15s) é muito menor que o tempo de "cold start" do backend no Render (~145s), causando erro de timeout no primeiro acesso após período de inatividade. |
| BUG-11 | Baixa | Frontend | Botões de ação (Confirmar reserva, Registrar entrada, Check-in, Adicionar veículo, Finalizar, Cancelar) não exibem estado de carregamento/disabled durante a chamada à API, permitindo duplo envio. |
| BUG-12 | Baixa | Frontend | Campo "Placa" no formulário "Adicionar veículo" do painel do motorista não possui limite de caracteres (`maxLength`), ao contrário do campo equivalente no painel do operador. |
| BUG-13 | Baixa | Backend | Anotação `@CrossOrigin(origins = "http://localhost:5173")` em `VeiculoController` é redundante/inconsistente com a configuração global de CORS (que já permite todas as origens). |

---

## Detalhes do Bug BUG-01

**Título:** Vazamento de dados sensíveis (hash de senha) de todos os usuários via `GET /veiculos`

**Descrição:** O endpoint `GET /veiculos` chama `veiculoService.listarTodos()`, que retorna **todos** os veículos cadastrados no sistema, sem filtrar por usuário e sem restrição de papel (`role`). Como a entidade `Veiculo` possui um relacionamento `@ManyToOne` para `Usuario` sem `@JsonIgnore`, e a entidade `Usuario` possui `getPassword()` (herdado de `UserDetails`) **sem `@JsonIgnore`** (apenas o campo `senha` tem a anotação, mas o getter derivado `getPassword()` não), o JSON retornado inclui, para cada veículo, o objeto completo do dono — **incluindo o campo `"password"` com o hash BCrypt da senha e o email**. Qualquer usuário autenticado, mesmo com role `USER`, consegue acessar essa rota e obter os dados de **todos os usuários do sistema**, inclusive contas reais de produção (não apenas dados de teste).

**Passos para reproduzir:**
1. Fazer login com qualquer conta válida (role `USER` ou `OPERADOR`) via `POST /auth/login` e obter o token JWT.
2. Chamar `GET /veiculos` com o header `Authorization: Bearer <token>`.
3. Observar que a resposta é uma lista com todos os veículos do sistema, cada um contendo `"usuario": {"id":..., "email":"...", "role":"...", "password":"$2a$10$..."}`.

**Comportamento Esperado:**
- O endpoint deveria retornar apenas os veículos do usuário autenticado (ou exigir role `ADMIN`/`OPERADOR` para listagem geral).
- Em nenhum cenário o campo de senha (hash ou não) deveria ser serializado na resposta da API.

**Comportamento Atual:**
- Qualquer usuário autenticado recebe a lista completa de veículos de todos os usuários, incluindo email e hash da senha de cada dono.

**Sugestão de Correção:**
1. **Imediato (mitigação crítica):** adicionar `@JsonIgnore` no método `getPassword()` da classe `Usuario` (não apenas no campo `senha`), e/ou anotar a classe com `@JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})` para impedir que os getters da interface `UserDetails` sejam serializados.
2. Adicionar `@JsonIgnore` no campo `usuario` de `Veiculo`, ou criar um DTO de resposta (`VeiculoResponseDTO`) que não exponha o relacionamento completo com `Usuario`.
3. Restringir `GET /veiculos` (`listarTodos()`) com `@PreAuthorize("hasRole('ADMIN')")`, e expor para usuários comuns apenas `GET /veiculos/meus` (já existe `listarPorUsuario`).
4. **Ação recomendada imediata:** considerando que hashes de senha de contas reais foram expostos, recomenda-se invalidar/forçar troca de senha das contas afetadas após o deploy da correção.

---

## Detalhes do Bug BUG-02

**Título:** `POST /estadias/reservar` retorna erro 500 e corrompe o estado da vaga (vaga "fantasma" ocupada)

**Descrição:** Ao reservar uma vaga (`POST /estadias/reservar?vagaId=X&veiculoId=Y`), o método `EstadiaService.reservarVaga()`:
1. Marca a vaga como `ocupada = true` e salva (`vagaRepository.save(vaga)`).
2. Cria uma nova `Estadia` com `ativa=true`, `pendente=true`, `codigo=...`, `criadoEm=now()`, **mas nunca define o campo `entrada`**.
3. Tenta salvar a `Estadia` (`repository.save(estadia)`).

A coluna `estadias.entrada` no banco de produção parece possuir constraint `NOT NULL` (herdada do schema legado, em que `entrada` era sempre preenchida na criação). Como `entrada` fica `null`, o `INSERT` falha com violação de constraint, o `DataAccessException` é capturado pelo `GlobalExceptionHandler` e convertido em **HTTP 500** (`"Erro interno ao acessar os dados. Tente novamente mais tarde."`). Como o método **não é `@Transactional`**, o `UPDATE` da vaga (passo 1) já foi commitado antes da falha do passo 3 — a vaga fica permanentemente com `ocupada=true`, sem nenhuma `Estadia` vinculada, e não pode mais ser reservada nem usada para entrada manual até ser corrigida manualmente (ex.: via `PUT /vagas/{id}/liberar`).

**Passos para reproduzir:**
1. Login como motorista (role `USER`) com pelo menos um veículo cadastrado.
2. `GET /vagas/por-estacionamento/{id}` para obter uma vaga livre (`ocupada=false`).
3. `POST /estadias/reservar?vagaId={vagaId}&veiculoId={veiculoId}` com o token do motorista.
4. Observar resposta `HTTP 500`.
5. Consultar a vaga novamente (`GET /vagas/por-estacionamento/{id}` ou `GET /vagas` como operador) — ela aparece como `ocupada=true`, mas nenhuma estadia ativa/pendente existe para ela.

**Comportamento Esperado:**
- A reserva deveria ser criada com sucesso (`HTTP 201`), retornando o objeto `Estadia` com `status: "PENDENTE"` e o `codigo` de check-in.
- Em caso de erro, **nenhuma alteração parcial** deveria persistir (a vaga deveria continuar livre).

**Comportamento Atual:**
- `HTTP 500` com mensagem genérica.
- A vaga fica "travada" como ocupada indefinidamente, exigindo intervenção manual no banco/endpoint administrativo para liberar.
- No frontend, o motorista vê apenas a mensagem genérica "Não foi possível concluir a reserva. Tente novamente." (de `getErroMsg`), sem qualquer indicação do problema real, e pode tentar novamente repetidas vezes, "travando" múltiplas vagas.

**Sugestão de Correção:**
1. Definir `estadia.setEntrada(LocalDateTime.now())` (ou tornar a coluna `entrada` `NULL`-able no banco, já que para reservas pendentes a entrada real só ocorre no check-in) em `reservarVaga()`. A opção recomendada é **alterar a constraint do banco** (`ALTER TABLE estadias ALTER COLUMN entrada DROP NOT NULL`), pois semanticamente uma reserva pendente ainda não tem horário de entrada real — esse valor só deve ser definido em `confirmarCheckin()`.
2. Anotar `reservarVaga()` com `@Transactional`, garantindo que a alteração da vaga (`ocupada=true`) só seja persistida se a `Estadia` for criada com sucesso (rollback automático em caso de exceção).
3. Adicionar um teste de integração cobrindo `POST /estadias/reservar` de ponta a ponta para evitar regressão.
4. Como ação corretiva imediata no ambiente de produção, liberar manualmente (via `PUT /vagas/{id}/liberar`) quaisquer vagas que já tenham ficado "fantasma" por causa deste bug.

---

## Detalhes do Bug BUG-03

**Título:** "Registrar entrada manual" exige veículo pré-cadastrado, contrariando o fluxo de negócio esperado

**Descrição:** O endpoint `POST /estadias?placa=X&vagaId=Y` (usado pelo operador para registrar a entrada de um veículo no pátio) chama `veiculoRepository.findByPlaca(placa).orElseThrow(() -> new RuntimeException("Veículo não encontrado"))`. Ou seja, se a placa informada não existir previamente na tabela `veiculos`, a operação falha com `HTTP 400` e a mensagem `"Veículo não encontrado"`. Segundo o briefing do projeto, **o veículo não precisa estar cadastrado no banco de dados para esse fluxo** — um operador deve poder registrar a entrada de qualquer veículo que chegue ao pátio, mesmo que o motorista nunca tenha usado o app antes.

**Passos para reproduzir:**
1. Login como operador.
2. `POST /estadias?placa=ZZZ9999&vagaId={vagaId_livre}` (placa que nunca foi cadastrada via `/veiculos`).
3. Observar resposta `HTTP 400` com `{"message":"Veículo não encontrado"}`.

**Comportamento Esperado:**
- A entrada deveria ser registrada normalmente, criando (ou reaproveitando, se já existir) um registro de `Veiculo` "avulso" associado à placa informada — possivelmente sem `usuario` vinculado (ou vinculado a um usuário "anônimo"/genérico do estacionamento) — e criando a `Estadia` correspondente com `entrada=now()`.

**Comportamento Atual:**
- `HTTP 400` — operador não consegue registrar a entrada de um veículo que não esteja previamente cadastrado por um motorista no app.

**Sugestão de Correção:**
1. Em `EstadiaService.registrarEntrada(placa, vagaId)`, substituir o `orElseThrow` por um `orElseGet` que cria e persiste um novo `Veiculo` com a placa informada (`tipo` podendo ser inferido do tipo da vaga, se a vaga for exclusiva, ou um valor padrão como `CARRO`), sem `usuario` vinculado (tornando o campo `usuario` da entidade `Veiculo` opcional/nullable) ou vinculado a um usuário "convidado" do estacionamento.
2. Validar formato básico da placa (padrão Mercosul/antigo) antes de criar o registro avulso, para evitar lixo de dados (relacionado ao BUG-04).
3. Garantir que, quando o motorista posteriormente se cadastrar/reivindicar essa placa, o sistema associe o veículo já existente ao seu usuário em vez de criar duplicidade (a constraint `unique` em `placa` já impediria duplicação, mas a UX de "reivindicar veículo" precisa ser pensada).

---

## Detalhes do Bug BUG-04

**Título:** Cadastro de veículo com placa acima do limite de 10 caracteres retorna HTTP 500

**Descrição:** O campo `Veiculo.placa` é mapeado com `@Column(nullable = false, unique = true, length = 10)`, mas não há nenhuma validação (`@Size`, `@Pattern`) no DTO/entidade antes da persistência. Ao enviar uma placa com mais de 10 caracteres, o Postgres rejeita o `INSERT`/`UPDATE` com erro de "value too long for type character varying(10)", que é capturado pelo `GlobalExceptionHandler` como `DataAccessException` e convertido em `HTTP 500` genérico.

**Passos para reproduzir:**
1. Login como motorista.
2. `POST /veiculos` com body `{"placa": "PLACA-MUITO-LONGA-12345", "modelo": "X", "cor": "Y", "tipo": "CARRO", "ativo": true}`.
3. Observar resposta `HTTP 500` com `{"message":"Erro interno ao acessar os dados. Tente novamente mais tarde."}`.

**Comportamento Esperado:**
- `HTTP 400` com mensagem amigável, ex.: `"A placa deve ter no máximo 10 caracteres."`

**Comportamento Atual:**
- `HTTP 500` genérico, indistinguível de uma falha real de infraestrutura/banco de dados.
- No frontend (painel do motorista, aba "Meus veículos"), o campo de placa **não possui `maxLength`** (ver BUG-12), então o usuário consegue digitar uma placa longa e disparar este erro, recebendo a mensagem enganosa "Placa pode já estar cadastrada." (fallback de `getErroMsg`).

**Sugestão de Correção:**
1. Adicionar validação `@Size(max = 10)` (e idealmente `@Pattern` para o formato de placa Mercosul/antigo) no DTO de criação de veículo, com `@Valid` no controller.
2. Adicionar `maxLength={10}` (ou conforme o padrão de placa adotado) no campo de placa do frontend (ambos os formulários).

---

## Detalhes do Bug BUG-05

**Título:** Falha de login retorna HTTP 400 em vez de 401, com mensagens que permitem enumeração de usuários

**Descrição:** Em `AuthService.login()`, tanto o caso "usuário não encontrado" quanto "senha inválida" lançam `RuntimeException` com mensagens diferentes (`"Usuário não encontrado"` e `"Senha inválida"`, respectivamente), e o `GlobalExceptionHandler` mapeia `RuntimeException` genericamente para `HTTP 400 Bad Request`. Para uma falha de autenticação, o código semanticamente correto é `HTTP 401 Unauthorized`. Além disso, como as duas mensagens são diferentes, um atacante pode usar o endpoint de login para **descobrir quais emails estão cadastrados** no sistema (testando emails e observando se a resposta é "Usuário não encontrado" ou "Senha inválida").

**Passos para reproduzir:**
1. `POST /auth/login` com `{"email": "naoexiste@teste.com", "senha": "qualquer"}` → `{"message":"Usuário não encontrado"}`, `HTTP 400`.
2. `POST /auth/login` com `{"email": "<email_valido_existente>", "senha": "senhaerrada"}` → `{"message":"Senha inválida"}`, `HTTP 400`.
3. Comparar as duas respostas — a diferença na mensagem revela se o email existe ou não.

**Comportamento Esperado:**
- Ambos os casos deveriam retornar `HTTP 401 Unauthorized` com a **mesma** mensagem genérica, ex.: `"Email ou senha inválidos"`.

**Comportamento Atual:**
- `HTTP 400` em ambos os casos, com mensagens distintas que permitem enumeração de contas.

**Sugestão de Correção:**
1. Criar uma exceção dedicada (ex.: `CredenciaisInvalidasException`) e mapeá-la no `GlobalExceptionHandler` para `HTTP 401`.
2. Unificar a mensagem de erro para "Email ou senha inválidos" em ambos os casos (usuário não encontrado e senha incorreta), evitando enumeração.

---

## Detalhes do Bug BUG-06

**Título:** `POST /auth/register` aceita campos vazios e valores arbitrários de `role` sem validação

**Descrição:** `RegisterRequestDTO` não possui nenhuma anotação de Bean Validation (`@NotBlank`, `@Email`, `@Size`, etc.), apesar de o controller usar `@Valid`. Isso permite registrar usuários com `email` e/ou `senha` em branco (`""`), e com `role` igual a qualquer string arbitrária (ex.: `"SUPERADMIN"`), retornando `HTTP 201` em todos os casos. O método `getAuthorities()` em `Usuario` trata qualquer valor de `role` diferente de `"ADMIN"` (case-insensitive) como `ROLE_USER`, então o impacto prático de uma `role` inválida é limitado — mas dados inconsistentes (`role="SUPERADMIN"`) ficam persistidos no banco, e contas com `email`/`senha` vazios são criadas.

**Passos para reproduzir:**
1. `POST /auth/register` com `{"email": "", "senha": "", "nome": "Teste", "role": "USER"}` → `HTTP 201`.
2. `POST /auth/register` com `{"email": "qa.role.invalido...@teste.com", "senha": "senha123", "nome": "Teste", "role": "SUPERADMIN"}` → `HTTP 201`, usuário criado com `role="SUPERADMIN"` no banco.

**Comportamento Esperado:**
- `email` e `senha` deveriam ser obrigatórios (`@NotBlank`), `email` deveria ter formato válido (`@Email`), e `role` deveria ser restrito a um conjunto fechado de valores (`USER`, `OPERADOR`, `ADMIN`), retornando `HTTP 400` para valores fora desse conjunto.

**Comportamento Atual:**
- Todos os casos acima retornam `HTTP 201`, criando contas com dados inválidos/inconsistentes.

**Sugestão de Correção:**
1. Adicionar anotações de validação no `RegisterRequestDTO`: `@NotBlank @Email private String email;`, `@NotBlank @Size(min = 6) private String senha;`.
2. Validar `role` contra um `enum` (`TipoUsuario { USER, OPERADOR, ADMIN }`) em vez de `String` livre, ou validar manualmente em `AuthService.register()` lançando `RuntimeException`/exceção de validação para valores fora da lista permitida.

---

## Detalhes do Bug BUG-07

**Título:** Recursos não encontrados retornam HTTP 400 em vez de HTTP 404

**Descrição:** Diversos métodos de serviço (`EstadiaService.finalizarEstadia`, `VagaService.buscarPorId`, `EstacionamentoService.buscarPorId`, `VeiculoService.buscarPorId`, etc.) lançam `new RuntimeException("... não encontrada/não encontrado")` quando um recurso não existe. O `GlobalExceptionHandler` mapeia `RuntimeException` genericamente para `HTTP 400`. Já existe uma exceção dedicada `RecursoNaoEncontradoException` mapeada para `HTTP 404`, mas ela é usada apenas em alguns pontos (ex.: `VagaService.deletar`), não de forma consistente.

**Passos para reproduzir:**
1. Login como operador.
2. `PUT /estadias/999999/finalizar` (ID inexistente) → `{"message":"Estadia não encontrada"}`, `HTTP 400` (esperado: `404`).
3. `GET /estacionamentos/999999` (ID inexistente, se aplicável) → mesmo padrão.

**Comportamento Esperado:**
- `HTTP 404 Not Found` para qualquer operação referenciando um ID/recurso inexistente.

**Comportamento Atual:**
- `HTTP 400 Bad Request`, misturando semanticamente "requisição malformada" com "recurso inexistente".

**Sugestão de Correção:**
- Substituir os `throw new RuntimeException("... não encontrad[a|o]")` por `throw new RecursoNaoEncontradoException(...)` nos métodos `buscarPorId` de todos os serviços (Estadia, Vaga, Estacionamento, Veículo, Usuário), aproveitando o mapeamento já existente para `HTTP 404`.

---

## Detalhes do Bug BUG-08

**Título:** Endpoints de criação de recursos retornam HTTP 200 em vez de HTTP 201 Created

**Descrição:** Endpoints como `POST /veiculos`, `POST /vagas` e `POST /estadias` (registrar entrada) retornam `HTTP 200 OK` ao criar um novo recurso, em vez do `HTTP 201 Created` esperado pela convenção REST (e explicitamente solicitado no checklist de testes).

**Passos para reproduzir:**
1. Login como motorista.
2. `POST /veiculos` com payload válido → resposta contém o veículo criado (com `id`), mas o status HTTP retornado é `200`.

**Comportamento Esperado:**
- `HTTP 201 Created` (idealmente com header `Location` apontando para o novo recurso).

**Comportamento Atual:**
- `HTTP 200 OK`.

**Sugestão de Correção:**
- Nos métodos de controller responsáveis pela criação (`@PostMapping`), retornar `ResponseEntity.status(HttpStatus.CREATED).body(...)` em vez do retorno implícito (`200`).

---

## Detalhes do Bug BUG-09

**Título:** Mensagens de erro técnicas internas vazam na resposta da API e não são totalmente filtradas pelo frontend

**Descrição:** O `GlobalExceptionHandler` mapeia `RuntimeException` para `HTTP 400` retornando `ex.getMessage()` diretamente ao cliente. Para exceções geradas internamente pelo Spring (ex.: erro de conversão de tipo de `@RequestParam`), essa mensagem contém detalhes técnicos da implementação. Por exemplo, ao enviar um valor não numérico para `vagaId`:

```
POST /estadias?placa=ABC1234&vagaId=abc
→ HTTP 400
{"message":"Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"abc\""}
```

No frontend, a função `getErroMsg` (em `src/utils/erro.js`) tenta filtrar mensagens técnicas usando regex (`/jdbc/i`, `/\bsql\b/i`, `/exception/i`, `/org\.(springframework|hibernate)/i`), mas essa mensagem específica **não é capturada por nenhum desses padrões** e seria exibida ao usuário final tal como recebida da API.

**Passos para reproduzir:**
1. Login como operador.
2. `POST /estadias?placa=ABC1234&vagaId=abc` (vagaId não numérico).
3. Observar a mensagem de erro técnica na resposta `HTTP 400`.

**Comportamento Esperado:**
- `HTTP 400` com mensagem amigável, ex.: `"Parâmetro 'vagaId' inválido."`, sem detalhes de implementação (tipos Java, nomes de classes, etc.).

**Comportamento Atual:**
- A mensagem bruta do Spring (`"Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'..."`) é repassada ao cliente e não é filtrada pelo frontend.

**Sugestão de Correção:**
1. No backend, adicionar um `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` no `GlobalExceptionHandler`, retornando `HTTP 400` com mensagem genérica (ex.: `"Parâmetro inválido: " + ex.getName()`).
2. No frontend, expandir a regex de `getErroMsg` para também filtrar padrões como `/failed to convert/i` e `/required type/i`, como camada extra de defesa.

---

## Detalhes do Bug BUG-10

**Título:** Timeout do frontend (15s) é incompatível com o tempo de "cold start" do backend no Render (~145s)

**Descrição:** O cliente axios em `src/services/api.js` é configurado com `timeout: 15000` (15 segundos). No entanto, a API hospedada no plano gratuito do Render entra em "modo de suspensão" após período de inatividade, e a primeira requisição após esse período pode levar até ~145 segundos para responder ("cold start"). Nesse cenário, **toda** requisição feita pelo usuário nos primeiros ~2 minutos falhará por timeout do axios antes mesmo de a API conseguir responder.

**Passos para reproduzir:**
1. Aguardar a API de produção entrar em modo de suspensão (sem requisições por ~15 minutos).
2. Acessar o frontend e realizar login ou qualquer ação que chame a API.
3. Observar erro de timeout/conexão após 15 segundos, mesmo que a API eventualmente responda com sucesso ~145s depois.

**Comportamento Esperado:**
- O usuário deveria ver uma mensagem indicando que o servidor está "acordando" (cold start) e aguardar automaticamente, ou o timeout deveria ser configurado para um valor compatível (ex.: 60-180s) ao menos na primeira requisição.

**Comportamento Atual:**
- Erro de timeout genérico após 15s, sem indicação do motivo real, levando o usuário a achar que o sistema está fora do ar ou com erro.

**Sugestão de Correção:**
1. Aumentar o `timeout` do axios para um valor compatível com o cold start do Render (ex.: 60-120s), ou
2. Implementar um "ping" de aquecimento (`GET /actuator/health` ou endpoint leve) ao carregar a aplicação, exibindo uma tela de "Conectando ao servidor, isso pode levar até 2 minutos na primeira vez..." enquanto aguarda.
3. Avaliar migração para um plano pago do Render (ou similar) que não sofra cold start, caso o orçamento permita, já que isso afeta a primeira impressão de qualquer usuário.

---

## Detalhes do Bug BUG-11

**Título:** Botões de ação não exibem estado de carregamento, permitindo duplo envio

**Descrição:** Em ambos os painéis (Motorista e Operador), os formulários de ações críticas — `confirmarReserva` (Reservar Estadia), `handleEntrada` (Registrar Entrada Manual), `handleCheckin`, `handleAddVeiculo`, `handleFinalizar`, `handleCancelarReserva` — não desabilitam o botão de submit nem exibem um spinner enquanto a requisição `await api.xxx(...)` está em andamento. A única exceção é `handleSalvarPrecos` (`salvandoPrecos`), que implementa esse padrão corretamente. Combinado com o BUG-10 (latência alta da API em cold start), o usuário pode clicar várias vezes no botão "Confirmar reserva" ou "Registrar entrada" enquanto aguarda, disparando múltiplas requisições idênticas.

**Passos para reproduzir:**
1. Abrir o painel do operador, preencher o formulário "Registrar entrada manual".
2. Clicar em "Registrar" múltiplas vezes rapidamente (especialmente perceptível durante cold start da API).
3. Observar que o botão permanece clicável e nenhuma indicação visual de carregamento aparece.
4. (Resultado possível) Múltiplas estadias/registros são criados para a mesma placa/vaga, ou múltiplos erros de "vaga já ocupada" aparecem em sequência.

**Comportamento Esperado:**
- Ao submeter, o botão deveria ficar `disabled` e exibir um spinner/texto "Registrando..." até a resposta da API chegar (sucesso ou erro), seguindo o mesmo padrão já usado em `salvandoPrecos`.

**Comportamento Atual:**
- Botão permanece habilitado durante toda a requisição, sem feedback visual de progresso.

**Sugestão de Correção:**
- Replicar o padrão `useState` + `disabled={salvando}` + spinner já existente em `handleSalvarPrecos` (PainelOperador.jsx) para todos os handlers de submissão mencionados, em ambos os painéis.

---

## Detalhes do Bug BUG-12

**Título:** Campo "Placa" no formulário "Adicionar veículo" (painel do motorista) sem limite de caracteres

**Descrição:** No painel do operador, o campo de placa para "Registrar entrada manual" possui `maxLength={8}`. Já no painel do motorista, no formulário "Adicionar veículo" (aba "Meus veículos"), o campo `placa` (`formV.placa`) **não possui nenhum atributo `maxLength`**, permitindo ao usuário digitar placas arbitrariamente longas. Isso é o principal vetor pelo qual um usuário comum (sem usar `curl`/Postman) pode acionar o BUG-04 (HTTP 500 ao salvar placa > 10 caracteres).

**Passos para reproduzir:**
1. Login como motorista, ir para a aba "Meus veículos" → "Adicionar veículo".
2. No campo "Placa", digitar uma string com mais de 10 caracteres (ex.: `PLACAMUITOLONGA123`).
3. Submeter o formulário.
4. Observar erro (relacionado ao BUG-04) com mensagem enganosa "Placa pode já estar cadastrada."

**Comportamento Esperado:**
- O campo deveria limitar a entrada a, no máximo, o tamanho de placa válido (ex.: `maxLength={8}` ou `7`, conforme padrão Mercosul/antigo adotado), assim como no painel do operador.

**Comportamento Atual:**
- Sem limite de caracteres no campo, permitindo input inválido que causa erro 500 no backend.

**Sugestão de Correção:**
- Adicionar `maxLength={8}` (ou valor consistente com a validação de backend sugerida no BUG-04) ao `<input>` de placa em `PainelMotorista.jsx`, igualando ao comportamento já existente em `PainelOperador.jsx`.

---

## Detalhes do Bug BUG-13

**Título:** Configuração `@CrossOrigin` redundante/inconsistente em `VeiculoController`

**Descrição:** A classe `VeiculoController` possui a anotação `@CrossOrigin(origins = "http://localhost:5173")`, restringindo CORS a essa origem específica. Porém, a configuração global em `SecurityConfig` (`CorsConfigurationSource`) já permite **todas** as origens (`*`) com credenciais para toda a API. Ter as duas configurações simultaneamente é inconsistente: dependendo de como o Spring resolve a precedência entre `@CrossOrigin` no controller e o `CorsFilter` global, requisições de origens diferentes de `localhost:5173` podem ser bloqueadas especificamente neste controller, ou a anotação pode simplesmente ser ignorada (comportamento não-óbvio e dependente de versão do Spring).

**Passos para reproduzir:**
1. Inspecionar o código-fonte de `VeiculoController.java` e `SecurityConfig.java`.
2. Notar a presença de `@CrossOrigin(origins = "http://localhost:5173")` no controller vs. `allowedOrigins("*")` (ou equivalente) na configuração global.

**Comportamento Esperado:**
- Uma única fonte de verdade para a configuração de CORS (preferencialmente a configuração global em `SecurityConfig`), evitando comportamento inconsistente entre ambientes (dev vs. produção).

**Comportamento Atual:**
- Duas configurações de CORS potencialmente conflitantes coexistem; não foi observado bloqueio durante os testes (a API de produção respondeu normalmente a partir do ambiente de teste), mas a inconsistência representa risco de manutenção/depuração futura.

**Sugestão de Correção:**
- Remover a anotação `@CrossOrigin` de `VeiculoController` e confiar exclusivamente na configuração global de CORS em `SecurityConfig`, garantindo consistência entre todos os controllers.

---

## Pontos Positivos Confirmados

Durante os testes, os seguintes comportamentos funcionaram **corretamente** e merecem destaque:

- ✅ **Exclusividade de vaga por tipo de veículo:** tentar registrar a entrada de uma MOTO em uma vaga exclusiva para CARRO retorna corretamente `HTTP 400` com a mensagem `"Esta vaga é exclusiva para veículos do tipo CARRO"`.
- ✅ **Proteção contra SQL Injection:** placas contendo payloads como `ABC' OR '1'='1` são tratadas com segurança graças ao uso de Spring Data JPA com queries parametrizadas — retornam `"Veículo não encontrado"` (400), sem qualquer comportamento anômalo.
- ✅ **Resiliência a payloads com HTML/script:** uma placa contendo `<script>alert(1)</script>` não causa erro no backend, sendo tratada como placa inválida/não encontrada (400).
- ✅ **Cálculo de cobrança com mínimo de 1 hora:** `finalizarEstadia` calcula corretamente `valor = horas * valorHora`, aplicando o mínimo de 1 hora mesmo para estadias com menos de 1 hora de duração.
- ✅ **Tratamento de check-in/cancelamento com dados inválidos:** `PUT /estadias/checkin?codigo=INVALIDO` retorna `400` com `"Reserva não encontrada ou já utilizada"`; cancelar uma reserva que já está ativa/finalizada retorna `400` com `"Esta reserva não pode ser cancelada pois já está em uso ou finalizada."` — ambas mensagens amigáveis e claras.
- ✅ **Mensagens amigáveis para regras de negócio do operador:** tentar acessar funcionalidades de operador sem um estacionamento vinculado retorna mensagem clara (`"Este operador não possui um estacionamento vinculado."`).
- ✅ **Telas de carregamento inicial:** ambos os painéis (Motorista e Operador) exibem uma tela de "Carregando..." com spinner enquanto os dados iniciais são buscados, evitando telas em branco.
- ✅ **Filtro de mensagens técnicas (`getErroMsg`):** o frontend já implementa um filtro que esconde mensagens contendo termos como `jdbc`, `sql`, `exception`, `org.springframework`/`org.hibernate`, substituindo por mensagens de fallback amigáveis — funciona corretamente para a maioria dos erros 500 testados (apenas o caso específico do BUG-09 escapa do filtro).

---

## Resumo Executivo

Dos 13 problemas identificados, **2 são críticos** e devem ser tratados com prioridade máxima antes de qualquer divulgação mais ampla do sistema:

1. **BUG-01** expõe hashes de senha de usuários reais para qualquer pessoa com uma conta no sistema — risco de comprometimento de contas via quebra offline do hash BCrypt.
2. **BUG-02** quebra completamente o fluxo de "Reservar Estadia" (uma das duas funcionalidades-alvo deste teste), além de corromper o estado das vagas do estacionamento.

Os bugs **BUG-03** e **BUG-04** afetam diretamente os dois fluxos solicitados (Registrar Entrada Manual e cadastro de veículo, respectivamente) e devem ser tratados na sequência. Os demais itens (BUG-05 a BUG-13) são melhorias de robustez, consistência de API e UX que devem ser priorizadas conforme a capacidade da equipe.
