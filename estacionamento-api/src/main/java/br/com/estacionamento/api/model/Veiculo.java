package br.com.estacionamento.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "veiculos")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "A placa é obrigatória.")
    @Size(max = 10, message = "A placa deve ter no máximo 10 caracteres.")
    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Column(length = 50)
    private String modelo;

    @Column(length = 30)
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVeiculo tipo;

    @Column(nullable = false)
    private boolean ativo = true;

    // NOVO: Relacionamento com Usuário (oculto no JSON: nunca deve expor dados de outro usuário)
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Usuario usuario;

    public Veiculo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }

    public TipoVeiculo getTipo() { return tipo; }
    public void setTipo(TipoVeiculo tipo) { this.tipo = tipo; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}