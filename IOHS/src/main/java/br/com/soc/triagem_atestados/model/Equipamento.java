package br.com.soc.triagem_atestados.model;

import br.com.soc.triagem_atestados.model.enums.StatusEquipamento;
import br.com.soc.triagem_atestados.model.enums.TipoEquipamento;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 领用员工
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario funcionario;

    // 劳保用品类型
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEquipamento tipo;

    // 发放日期
    @Column(nullable = false)
    private LocalDate dataEntrega;

    // 预计到期/更换日期
    private LocalDate dataValidade;

    // 归还日期（若已归还）
    private LocalDate dataDevolucao;

    // 当前状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEquipamento status;

    // 数量
    @Column(nullable = false)
    private Integer quantidade;

    // 规格/型号
    private String especificacao;

    // 备注（如损坏原因、更换说明）
    @Column(length = 500)
    private String observacao;
}
