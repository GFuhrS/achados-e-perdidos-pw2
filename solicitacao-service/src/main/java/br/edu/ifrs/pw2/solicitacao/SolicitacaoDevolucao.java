package br.edu.ifrs.pw2.solicitacao;

import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "solicitacoes_devolucao")
public class SolicitacaoDevolucao extends PanacheEntity {
    @Column(nullable = false)
    public Long objetoId;

    @Column(nullable = false, length = 120)
    public String nomeSolicitante;

    @Column(nullable = false, length = 254)
    public String emailSolicitante;

    @Column(nullable = false, length = 500)
    public String comprovacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public SolicitacaoStatus status;

    @Column(nullable = false)
    public LocalDateTime criadaEm;

    public LocalDateTime analisadaEm;
}
