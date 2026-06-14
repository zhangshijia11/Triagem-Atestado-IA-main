package br.com.soc.triagem_atestados.controller;

import br.com.soc.triagem_atestados.model.Equipamento;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.PerfilUsuario;
import br.com.soc.triagem_atestados.model.enums.StatusEquipamento;
import br.com.soc.triagem_atestados.model.enums.TipoEquipamento;
import br.com.soc.triagem_atestados.repository.EquipamentoRepository;
import br.com.soc.triagem_atestados.repository.UsuarioRepository;
import br.com.soc.triagem_atestados.service.EquipamentoService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
public class EquipamentoController {

    @Autowired
    private EquipamentoService equipamentoService;

    @Autowired
    private EquipamentoRepository equipamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== 员工端 ====================

    // 员工查看自己的劳保用品
    @GetMapping("/meus-equipamentos")
    public String telaMeusEquipamentos(HttpSession session, Model model) {
        if (!PerfilUsuario.FUNCIONARIO.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }
        String emailLogado = (String) session.getAttribute("emailUsuario");
        Usuario usuario = usuarioRepository.findByEmail(emailLogado).orElse(null);
        List<Equipamento> meusEquipamentos = (usuario != null)
                ? equipamentoService.listarPorFuncionario(usuario)
                : java.util.Collections.emptyList();
        model.addAttribute("lista", meusEquipamentos);
        return "equipamentos";
    }

    // 员工确认签收
    @PostMapping("/equipamento/confirmar/{id}")
    public String confirmarRecebimento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            equipamentoService.confirmarRecebimento(id);
            redirectAttributes.addFlashAttribute("sucesso", "劳保用品 #" + id + " 已确认签收！");
        } catch (Exception e) {
            log.error("签收失败", e);
            redirectAttributes.addFlashAttribute("erro", "签收失败：" + e.getMessage());
        }
        return "redirect:/meus-equipamentos";
    }

    // 员工申请更换
    @PostMapping("/equipamento/devolver/{id}")
    public String solicitarTroca(@PathVariable Long id,
                                  @RequestParam("observacao") String observacao,
                                  RedirectAttributes redirectAttributes) {
        try {
            equipamentoService.registrarDevolucao(id, observacao);
            redirectAttributes.addFlashAttribute("sucesso", "已提交更换申请，请等待管理员处理。");
        } catch (Exception e) {
            log.error("更换申请失败", e);
            redirectAttributes.addFlashAttribute("erro", "申请失败：" + e.getMessage());
        }
        return "redirect:/meus-equipamentos";
    }

    // ==================== 企业端 ====================

    // 企业劳保管理面板
    @GetMapping("/portal-equipamentos")
    public String telaEquipamentosEmpresa(HttpSession session, Model model) {
        if (!PerfilUsuario.EMPRESA.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }
        List<Equipamento> todosEquipamentos = equipamentoService.listarTodos();
        if (todosEquipamentos == null) {
            todosEquipamentos = java.util.Collections.emptyList();
        }

        long totalEquipamentos = todosEquipamentos.size();
        long entregues = todosEquipamentos.stream().filter(e -> e.getStatus() == StatusEquipamento.ENTREGUE || e.getStatus() == StatusEquipamento.EM_USO).count();
        long vencidos = todosEquipamentos.stream().filter(e -> e.getStatus() == StatusEquipamento.VENCIDO).count();
        long devolvidos = todosEquipamentos.stream().filter(e -> e.getStatus() == StatusEquipamento.DEVOLVIDO).count();

        model.addAttribute("lista", todosEquipamentos);
        model.addAttribute("totalEquipamentos", totalEquipamentos);
        model.addAttribute("entregues", entregues);
        model.addAttribute("vencidos", vencidos);
        model.addAttribute("devolvidos", devolvidos);
        model.addAttribute("tiposEquipamento", TipoEquipamento.values());

        return "equipamentos-empresa";
    }

    // 企业发放劳保用品
    @PostMapping("/equipamento/entregar")
    public String entregarEquipamento(
            @RequestParam("emailFuncionario") String emailFuncionario,
            @RequestParam("tipo") TipoEquipamento tipo,
            @RequestParam("quantidade") Integer quantidade,
            @RequestParam("especificacao") String especificacao,
            @RequestParam("dataValidade") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataValidade,
            RedirectAttributes redirectAttributes) {

        Usuario funcionario = usuarioRepository.findByEmail(emailFuncionario).orElse(null);
        if (funcionario == null) {
            redirectAttributes.addFlashAttribute("erroAction", "未找到该员工邮箱。");
            return "redirect:/portal-equipamentos";
        }

        try {
            equipamentoService.entregarEquipamento(funcionario, tipo, quantidade, especificacao, dataValidade);
            redirectAttributes.addFlashAttribute("sucessoAction", "劳保用品已成功发放给 " + funcionario.getNome() + "！");
        } catch (Exception e) {
            log.error("劳保用品发放失败", e);
            redirectAttributes.addFlashAttribute("erroAction", "发放失败：" + e.getMessage());
        }
        return "redirect:/portal-equipamentos";
    }

    // 企业更换劳保用品（自动创建新记录并关闭旧记录）
    @PostMapping("/equipamento/substituir/{id}")
    public String substituirEquipamento(
            @PathVariable Long id,
            @RequestParam("especificacao") String especificacao,
            @RequestParam("dataValidade") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataValidade,
            RedirectAttributes redirectAttributes) {

        try {
            Equipamento novo = equipamentoService.substituirEquipamento(id, especificacao, dataValidade);
            redirectAttributes.addFlashAttribute("sucessoAction", "劳保用品已更换！新用品 #" + novo.getId() + " 已发放，旧用品 #" + id + " 已处理。");
        } catch (Exception e) {
            log.error("劳保用品更换失败", e);
            redirectAttributes.addFlashAttribute("erroAction", "更换失败：" + e.getMessage());
        }
        return "redirect:/portal-equipamentos";
    }

    // 企业标记用品已过期
    @PostMapping("/equipamento/vencer/{id}")
    public String marcarVencido(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Equipamento eq = equipamentoRepository.findById(id).orElse(null);
        if (eq != null) {
            eq.setStatus(StatusEquipamento.VENCIDO);
            equipamentoRepository.save(eq);
            redirectAttributes.addFlashAttribute("sucessoAction", "劳保用品 #" + id + " 已标记为过期。");
        }
        return "redirect:/portal-equipamentos";
    }

    // 企业标记更换请求已处理
    @PostMapping("/equipamento/resolver/{id}")
    public String marcarResolvido(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Equipamento eq = equipamentoRepository.findById(id).orElse(null);
        if (eq != null) {
            eq.setStatus(StatusEquipamento.RESOLVIDO);
            equipamentoRepository.save(eq);
            redirectAttributes.addFlashAttribute("sucessoAction", "劳保用品 #" + id + " 更换请求已标记为已处理。");
        }
        return "redirect:/portal-equipamentos";
    }
}
