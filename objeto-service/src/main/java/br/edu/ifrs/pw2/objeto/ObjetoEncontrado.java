package br.edu.ifrs.pw2.objeto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "objetos_encontrados")
public class ObjetoEncontrado extends PanacheEntity {
    @Column(nullable = false, length = 120)
    public String titulo;

    @Column(nullable = false, length = 500)
    public String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public Categoria categoria;

    @Column(nullable = false, length = 150)
    public String localEncontrado;

    @Column(nullable = false)
    public LocalDate dataEncontrado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ObjetoStatus status;

    @Column(nullable = false)
    public LocalDateTime criadoEm;

    public Long solicitacaoAprovadaId;
}
