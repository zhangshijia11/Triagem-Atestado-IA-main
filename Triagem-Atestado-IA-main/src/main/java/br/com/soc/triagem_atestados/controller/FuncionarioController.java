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
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class FuncionarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AtestadoRepository atestadoRepository;

    @Autowired
    private ExameRepository exameRepository;

    @Autowired
    private EquipamentoRepository equipamentoRepository;

    // ==================== 员工列表 ====================
    @GetMapping("/portal-empresa/funcionarios")
    public String listarFuncionarios(HttpSession session, Model model) {
        if (!PerfilUsuario.EMPRESA.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }

        List<Usuario> funcionarios = usuarioRepository.findByPerfil(PerfilUsuario.FUNCIONARIO);

        // 为每个员工计算统计数据
        List<Map<String, Object>> lista = funcionarios.stream().map(func -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("usuario", func);

            List<Atestado> atestados = atestadoRepository.findByFuncionario(func);
            item.put("totalAtestados", atestados.size());
            item.put("atestadosPendentes", atestados.stream().filter(a -> a.getStatus() == StatusAtestado.PENDENTE).count());
            item.put("diasTotaisAfastamento", atestados.stream()
                    .filter(a -> a.getDiasAfastamento() != null)
                    .mapToInt(Atestado::getDiasAfastamento).sum());

            List<Exame> exames = exameRepository.findByFuncionario(func);
            item.put("totalExames", exames.size());
            item.put("examesInaptos", exames.stream().filter(e -> e.getStatus() == StatusExame.INAPTO).count());

            List<Equipamento> equipamentos = equipamentoRepository.findByFuncionario(func);
            item.put("totalEquipamentos", equipamentos.size());
            item.put("equipamentosVencidos", equipamentos.stream().filter(e -> e.getStatus() == StatusEquipamento.VENCIDO).count());

            return item;
        }).collect(Collectors.toList());

        // 汇总 KPI
        long comAtestados = lista.stream().filter(f -> ((Number) f.get("totalAtestados")).intValue() > 0).count();
        long comExamesInaptos = lista.stream().filter(f -> ((Number) f.get("examesInaptos")).intValue() > 0).count();
        long comEquipVencidos = lista.stream().filter(f -> ((Number) f.get("equipamentosVencidos")).intValue() > 0).count();

        model.addAttribute("funcionarios", lista);
        model.addAttribute("totalFuncionarios", funcionarios.size());
        model.addAttribute("comAtestados", comAtestados);
        model.addAttribute("comExamesInaptos", comExamesInaptos);
        model.addAttribute("comEquipVencidos", comEquipVencidos);

        return "funcionarios";
    }

    // ==================== 员工详情 ====================
    @GetMapping("/portal-empresa/funcionarios/{id}")
    public String detalheFuncionario(@PathVariable Long id, HttpSession session, Model model) {
        if (!PerfilUsuario.EMPRESA.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }

        Usuario funcionario = usuarioRepository.findById(id).orElse(null);
        if (funcionario == null || funcionario.getPerfil() != PerfilUsuario.FUNCIONARIO) {
            return "redirect:/portal-empresa/funcionarios";
        }

        // 病假条
        List<Atestado> atestados = atestadoRepository.findByFuncionario(funcionario);
        atestados.sort(Comparator.comparing(Atestado::getDataEnvio).reversed());
        long aprovados = atestados.stream().filter(a -> a.getStatus() == StatusAtestado.APROVADO).count();
        int totalDias = atestados.stream().filter(a -> a.getDiasAfastamento() != null)
                .mapToInt(Atestado::getDiasAfastamento).sum();

        // 体检
        List<Exame> exames = exameRepository.findByFuncionario(funcionario);
        exames.sort(Comparator.comparing(Exame::getDataAgendamento).reversed());

        // 劳保
        List<Equipamento> equipamentos = equipamentoRepository.findByFuncionario(funcionario);
        equipamentos.sort(Comparator.comparing(Equipamento::getDataEntrega).reversed());

        model.addAttribute("funcionario", funcionario);
        model.addAttribute("atestados", atestados);
        model.addAttribute("totalAtestados", atestados.size());
        model.addAttribute("atestadosAprovados", aprovados);
        model.addAttribute("totalDiasAfastamento", totalDias);
        model.addAttribute("exames", exames);
        model.addAttribute("totalExames", exames.size());
        model.addAttribute("equipamentos", equipamentos);
        model.addAttribute("totalEquipamentos", equipamentos.size());

        return "funcionario-detalhe";
    }
}
