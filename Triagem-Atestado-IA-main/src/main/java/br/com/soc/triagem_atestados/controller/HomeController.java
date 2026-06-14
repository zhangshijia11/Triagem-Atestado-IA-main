package br.com.soc.triagem_atestados.controller;

import br.com.soc.triagem_atestados.model.Atestado;
import br.com.soc.triagem_atestados.model.Exame;
import br.com.soc.triagem_atestados.model.Equipamento;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.PerfilUsuario;
import br.com.soc.triagem_atestados.model.enums.StatusAtestado;
import br.com.soc.triagem_atestados.model.enums.StatusExame;
import br.com.soc.triagem_atestados.model.enums.StatusEquipamento;
import br.com.soc.triagem_atestados.repository.AtestadoRepository;
import br.com.soc.triagem_atestados.repository.ExameRepository;
import br.com.soc.triagem_atestados.repository.EquipamentoRepository;
import br.com.soc.triagem_atestados.repository.UsuarioRepository;
import br.com.soc.triagem_atestados.service.EquipamentoService;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired
    private AtestadoRepository atestadoRepository;

    @Autowired
    private ExameRepository exameRepository;

    @Autowired
    private EquipamentoRepository equipamentoRepository;

    @Autowired
    private EquipamentoService equipamentoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== 企业综合首页 ====================
    @GetMapping("/portal-empresa/home")
    public String dashboard(HttpSession session, Model model) {
        if (!PerfilUsuario.EMPRESA.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }

        // ---- 员工统计 ----
        long totalFuncionarios = usuarioRepository.countByPerfil(PerfilUsuario.FUNCIONARIO);

        // ---- 病假条统计 ----
        List<Atestado> todosAtestados = atestadoRepository.findAll();
        long totalAtestados = todosAtestados.size();
        long pendentesAtestados = todosAtestados.stream().filter(a -> a.getStatus() == StatusAtestado.PENDENTE).count();
        long aprovados = todosAtestados.stream().filter(a -> a.getStatus() == StatusAtestado.APROVADO).count();
        long recusados = todosAtestados.stream().filter(a -> a.getStatus() == StatusAtestado.RECUSADO).count();

        // 缺勤率: (aprovados / total) * 100 se houver registros
        double taxaAbsenteismo = totalAtestados > 0
                ? Math.round(((double) aprovados / totalAtestados) * 1000.0) / 10.0
                : 0.0;

        // ---- 体检统计 ----
        List<Exame> todosExames = exameRepository.findAll();
        long totalExames = todosExames.size();
        long examesAgendados = todosExames.stream().filter(e -> e.getStatus() == StatusExame.AGENDADO).count();
        long examesAptos = todosExames.stream().filter(e -> e.getStatus() == StatusExame.APTO).count();
        long examesInaptos = todosExames.stream().filter(e -> e.getStatus() == StatusExame.INAPTO).count();

        // ---- 劳保统计（通过 Service 获取，自动触发过期检测）----
        List<Equipamento> todosEquipamentos = equipamentoService.listarTodos();
        long totalEquipamentos = todosEquipamentos.size();
        long equipamentosEmUso = todosEquipamentos.stream().filter(e -> e.getStatus() == StatusEquipamento.ENTREGUE || e.getStatus() == StatusEquipamento.EM_USO).count();
        long equipamentosVencidos = todosEquipamentos.stream().filter(e -> e.getStatus() == StatusEquipamento.VENCIDO).count();
        long equipamentosDevolvidos = todosEquipamentos.stream().filter(e -> e.getStatus() == StatusEquipamento.DEVOLVIDO).count();
        long equipamentosEntregues = todosEquipamentos.stream().filter(e -> e.getStatus() == StatusEquipamento.ENTREGUE).count(); // 待签收

        // ---- CID 分布 (Top 8) ----
        Map<String, Long> cidFrequencia = todosAtestados.stream()
                .filter(a -> a.getCid() != null && !a.getCid().isBlank())
                .collect(Collectors.groupingBy(Atestado::getCid, Collectors.counting()));

        // Top 8 CIDs
        List<Map.Entry<String, Long>> topCids = cidFrequencia.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .collect(Collectors.toList());

        model.addAttribute("cidLabels", topCids.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        model.addAttribute("cidData", topCids.stream().map(Map.Entry::getValue).collect(Collectors.toList()));

        // 饼图: 待审核 / 已通过 / 已拒绝
        model.addAttribute("piePending", pendentesAtestados);
        model.addAttribute("pieApproved", aprovados);
        model.addAttribute("pieRejected", recusados);

        // ---- INSS 预警 (请假 > 15天) ----
        long alertasINSS = todosAtestados.stream()
                .filter(a -> a.getDiasAfastamento() != null && a.getDiasAfastamento() > 15)
                .count();

        // ---- 最近活动 ----
        List<Atestado> recentes = todosAtestados.stream()
                .sorted(Comparator.comparing(Atestado::getDataEnvio).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // ---- KPI 卡片 ----
        model.addAttribute("totalFuncionarios", totalFuncionarios);
        model.addAttribute("totalAtestados", totalAtestados);
        model.addAttribute("pendentesAtestados", pendentesAtestados);
        model.addAttribute("totalExames", totalExames);
        model.addAttribute("examesAgendados", examesAgendados);
        model.addAttribute("totalEquipamentos", totalEquipamentos);
        model.addAttribute("equipamentosVencidos", equipamentosVencidos);
        model.addAttribute("taxaAbsenteismo", taxaAbsenteismo);
        model.addAttribute("alertasINSS", alertasINSS);
        model.addAttribute("recentes", recentes);

        // 待处理汇总
        long examesPendentesTotal = examesAgendados;
        long episPendentesTotal = equipamentosEntregues + equipamentosDevolvidos + equipamentosVencidos;

        // 摘要: 用于下方卡片
        model.addAttribute("aprovados", aprovados);
        model.addAttribute("recusados", recusados);
        model.addAttribute("examesAptos", examesAptos);
        model.addAttribute("examesInaptos", examesInaptos);
        model.addAttribute("examesPendentesTotal", examesPendentesTotal);
        model.addAttribute("episPendentesTotal", episPendentesTotal);
        model.addAttribute("equipamentosEmUso", equipamentosEmUso);
        model.addAttribute("equipamentosDevolvidos", equipamentosDevolvidos);
        model.addAttribute("equipamentosEntregues", equipamentosEntregues); // 待签收

        return "dashboard";
    }
}
