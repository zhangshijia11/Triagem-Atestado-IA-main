package br.com.soc.triagem_atestados.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

import br.com.soc.triagem_atestados.model.enums.StatusAtestado;

@Entity
@Data
public class Atestado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dados do Funcionário
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario funcionario;
    @Column(nullable = false)
    private LocalDate dataEnvio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAtestado status;// "PENDENTE", "APROVADO", "RECUSADO"

    // Dados Extraídos pela IA
    private String nomeMedico;

    @Column(nullable = false)
    private String crm;

    @Column(nullable = false)
    private Integer diasAfastamento;
    private String cid;
    private String nomeArquivo;
    
    @Column(length = 500)
    private String resumoDiagnostico;
}