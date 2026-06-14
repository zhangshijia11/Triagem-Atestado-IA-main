package br.com.soc.triagem_atestados.service;

import br.com.soc.triagem_atestados.model.Equipamento;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.StatusEquipamento;
import br.com.soc.triagem_atestados.model.enums.TipoEquipamento;
import br.com.soc.triagem_atestados.repository.EquipamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EquipamentoService {

    @Autowired
    private EquipamentoRepository equipamentoRepository;

    // 企业发放劳保用品给员工
    public Equipamento entregarEquipamento(Usuario funcionario, TipoEquipamento tipo,
                                           Integer quantidade, String especificacao,
                                           LocalDate dataValidade) {
        Equipamento eq = new Equipamento();
        eq.setFuncionario(funcionario);
        eq.setTipo(tipo);
        eq.setQuantidade(quantidade);
        eq.setEspecificacao(especificacao);
        eq.setDataEntrega(LocalDate.now());
        eq.setDataValidade(dataValidade);
        eq.setStatus(StatusEquipamento.ENTREGUE);
        return equipamentoRepository.save(eq);
    }

    // 更换劳保用品：创建新记录并关闭旧记录
    public Equipamento substituirEquipamento(Long equipamentoAntigoId, String especificacao, LocalDate dataValidade) {
        Equipamento antigo = equipamentoRepository.findById(equipamentoAntigoId)
                .orElseThrow(() -> new IllegalArgumentException("未找到编号为 " + equipamentoAntigoId + " 的劳保用品记录。"));

        // 创建新用品（继承旧用品的员工、类型、数量）
        Equipamento novo = new Equipamento();
        novo.setFuncionario(antigo.getFuncionario());
        novo.setTipo(antigo.getTipo());
        novo.setQuantidade(antigo.getQuantidade());
        novo.setEspecificacao(especificacao);
        novo.setDataEntrega(LocalDate.now());
        novo.setDataValidade(dataValidade);
        novo.setStatus(StatusEquipamento.ENTREGUE);
        Equipamento salvo = equipamentoRepository.save(novo);

        // 关闭旧记录
        antigo.setStatus(StatusEquipamento.RESOLVIDO);
        equipamentoRepository.save(antigo);

        return salvo;
    }

    // 员工确认签收 → 状态变为 EM_USO
    public Equipamento confirmarRecebimento(Long equipamentoId) {
        Equipamento eq = equipamentoRepository.findById(equipamentoId)
                .orElseThrow(() -> new IllegalArgumentException("未找到编号为 " + equipamentoId + " 的劳保用品记录。"));
        eq.setStatus(StatusEquipamento.EM_USO);
        return equipamentoRepository.save(eq);
    }

    // 员工申请更换 / 企业登记归还
    public Equipamento registrarDevolucao(Long equipamentoId, String observacao) {
        Equipamento eq = equipamentoRepository.findById(equipamentoId)
                .orElseThrow(() -> new IllegalArgumentException("未找到编号为 " + equipamentoId + " 的劳保用品记录。"));
        eq.setStatus(StatusEquipamento.DEVOLVIDO);
        eq.setDataDevolucao(LocalDate.now());
        eq.setObservacao(observacao);
        return equipamentoRepository.save(eq);
    }

    // 自动将过期用品标记为已过期
    private void atualizarVencidos(List<Equipamento> lista) {
        LocalDate hoje = LocalDate.now();
        for (Equipamento eq : lista) {
            if (eq.getDataValidade() != null && eq.getDataValidade().isBefore(hoje)
                    && eq.getStatus() != StatusEquipamento.VENCIDO
                    && eq.getStatus() != StatusEquipamento.RESOLVIDO
                    && eq.getStatus() != StatusEquipamento.DEVOLVIDO) {
                eq.setStatus(StatusEquipamento.VENCIDO);
                equipamentoRepository.save(eq);
            }
        }
    }

    // 员工查看自己的劳保用品
    public List<Equipamento> listarPorFuncionario(Usuario funcionario) {
        if (funcionario == null) return java.util.Collections.emptyList();
        List<Equipamento> result = equipamentoRepository.findByFuncionario(funcionario);
        if (result != null) {
            atualizarVencidos(result);
        }
        return result != null ? result : java.util.Collections.emptyList();
    }

    // 公司查看全部劳保用品
    public List<Equipamento> listarTodos() {
        List<Equipamento> result = equipamentoRepository.findAll();
        if (result != null) {
            atualizarVencidos(result);
        }
        return result != null ? result : java.util.Collections.emptyList();
    }
}
