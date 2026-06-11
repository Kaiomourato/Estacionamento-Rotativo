package br.com.estacionamento.api.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String senha;

    @Column(nullable = false)
    private String role;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Veiculo> veiculos;

    @ManyToOne
    @JoinColumn(name = "estacionamento_id")
    private Estacionamento estacionamento;

    public Usuario() {}

    public Usuario(String email, String senha, String role) {
        this.email = email;
        this.senha = senha;
        this.role = role;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role != null && this.role.equalsIgnoreCase("ADMIN")) {
            return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"), 
                new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public List<Veiculo> getVeiculos() { return veiculos; }
    public void setVeiculos(List<Veiculo> veiculos) { this.veiculos = veiculos; }
    
    public Estacionamento getEstacionamento() { return estacionamento; }
    public void setEstacionamento(Estacionamento estacionamento) { this.estacionamento = estacionamento; }
}