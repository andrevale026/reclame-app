package br.com.reclame.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "denuncias", indexes = {
        @Index(name = "idx_categoria", columnList = "categoria"),
        @Index(name = "idx_criado_em", columnList = "criado_em")
})
public class Denuncia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 36)
    private String protocolo;

    @Column(nullable = false, columnDefinition = "ENUM('anonimo','identificado')")
    private String modo;

    @Column(nullable = false, length = 40)
    private String categoria;

    @Lob @Column(name = "dados_criptografados", nullable = false, columnDefinition = "TEXT")
    private String dadosCriptografados;

    @Lob @Column(name = "email_denunciante_criptografado", columnDefinition = "TEXT")
    private String emailDenuncianteCriptografado;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist void prePersist() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }

    public Integer getId() { return id; }
    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDadosCriptografados() { return dadosCriptografados; }
    public void setDadosCriptografados(String valor) { this.dadosCriptografados = valor; }
    public String getEmailDenuncianteCriptografado() { return emailDenuncianteCriptografado; }
    public void setEmailDenuncianteCriptografado(String valor) { this.emailDenuncianteCriptografado = valor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
