package br.com.soc.triagem_atestados.model;

import br.com.soc.triagem_atestados.model.enums.StatusExame;
import br.com.soc.triagem_atestados.model.enums.TipoExame;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Exame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 体检员工
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario funcionario;

    // 体检类型：入职 / 定期 / 离职 / 返岗
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoExame tipoExame;

    // 预约日期
    @Column(nullable = false)
    private LocalDate dataAgendamento;

    // 体检完成日期
    private LocalDate dataRealizacao;

    // 体检状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusExame status;

    // 体检结果摘要
    @Column(length = 500)
    private String resultado;

    // 体检报告文件名（上传后存储）
    private String nomeArquivo;

    // 体检诊所/医院
    private String clinica;

    // 医生 CRM
    private String crmMedico;
}
