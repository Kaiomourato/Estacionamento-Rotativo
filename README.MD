 # Estacionamento Rotativo — API

 Projeto backend em Java + Spring Boot para gerenciamento de vagas e estadias em um estacionamento rotativo.

 O repositório contém uma API REST simples, preparada para evolução e deploy.

 ---

## 🏗️ Arquitetura

### Backend
- **Linguagem:** Java 21
- **Framework:** Spring Boot
- **Banco de Dados:** PostgreSQL
- **Persistência:** Spring Data JPA
- **Arquitetura:** REST

### Frontend (em desenvolvimento)
- **Tipo:** Aplicação Mobile
- **Comunicação:** HTTP + JSON
- **Consome:** API REST do backend

---

## 📦 Estrutura do Projeto

src/main/java/br/com/estacionamento/api
│
├── controller
│ ├── VeiculoController.java
│ ├── VagaController.java
│ └── EstadiaController.java
│
├── model
│ ├── Veiculo.java
│ ├── Vaga.java
│ ├── Estadia.java
│ └── TipoVeiculo.java
│
├── repository
│ ├── VeiculoRepository.java
│ ├── VagaRepository.java
│ └── EstadiaRepository.java
│
├── service
│ ├── VeiculoService.java
│ ├── VagaService.java
│ └── EstadiaService.java
│
└── EstacionamentoApiApplication.java


---

## 🧩 Modelo de Dados

### 🚗 Veículo
- `id`
- `placa` (única)
- `modelo`
- `cor`
- `tipo` (CARRO, MOTO, CAMINHONETE)
- `ativo`

---

### 🅿️ Vaga
- `id`
- `codigo` (ex: A1, B2)
- `ocupada`

---

### ⏱️ Estadia
- `id`
- `veiculo`
- `vaga`
- `entrada`
- `saida`
- `ativa`
- `valor`

---

## 📜 Regras de Negócio

- ❌ Um veículo **não pode ter mais de uma estadia ativa**
- ❌ Uma vaga **não pode estar ocupada por mais de um veículo**
- ✅ A vaga é escolhida **manualmente**
- ✅ Vagas de carro e moto são diferentes
- ✅ O valor é calculado **por fração**
- ✅ O valor é atualizado **durante a estadia**
- ✅ O cliente pode consultar o valor a qualquer momento

---

## 🔗 Endpoints Disponíveis

### 🚗 Veículos

**Criar veículo**

POST /veiculos


```json
{
  "placa": "ABC1234",
  "modelo": "Civic",
  "cor": "Preto",
  "tipo": "CARRO"
}

**Listar veículos**

GET /veiculos

**Buscar veículo por ID**

GET /veiculos/{id}

Desativar veículo

DELETE /veiculos/{id}

🅿️ Vagas

Criar vaga

POST /vagas

{
  "codigo": "A1",
  "ocupada": false
}

Listar vagas

GET /vagas

Listar vagas disponíveis

GET /vagas/disponiveis

⏱️ Estadias

Iniciar estadia

POST /estadias/entrada

{
  "veiculoId": 1,
  "vagaId": 2
}

Consultar valor atual

GET /estadias/{id}/valor

Finalizar estadia

POST /estadias/{id}/saida

Listar estadias ativas

GET /estadias/ativas

🔌 Integração com Frontend

    O frontend não acessa o banco de dados diretamente

    Toda comunicação ocorre via API REST

    Formato: JSON

    URL base local:

http://localhost:8080

▶️ Como Executar o Projeto
Pré-requisitos

    Java 21

    PostgreSQL

    Maven (ou Maven Wrapper)

Passos

./mvnw clean spring-boot:run

📊 Status do Projeto
✅ Concluído

    Backend funcional

    Banco de dados configurado

    CRUD de veículos, vagas e estadias

    Regras de negócio aplicadas

    Endpoints testados

🚧 Em andamento

    Aplicação mobile (frontend)

    Deploy em nuvem

    Autenticação de usuários

🚀 Próximas Evoluções

    Autenticação (funcionário)

    Histórico completo de estadias

    Dashboard administrativo

    Deploy em nuvem (Railway / Render / AWS)

    Integração com leitura de placas (câmera)

👨‍💻 Observações para Desenvolvedores

    O backend está preparado para crescimento

    O frontend pode ser desenvolvido em paralelo

    Nenhuma refatoração estrutural será necessária