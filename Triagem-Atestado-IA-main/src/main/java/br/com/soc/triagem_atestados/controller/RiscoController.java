package br.com.soc.triagem_atestados.controller;

import br.com.soc.triagem_atestados.model.Atestado;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.PerfilUsuario;
import br.com.soc.triagem_atestados.model.enums.StatusAtestado;
import br.com.soc.triagem_atestados.repository.AtestadoRepository;
import br.com.soc.triagem_atestados.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class RiscoController {

    @Autowired
    private AtestadoRepository atestadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== 职业风险评估看板 ====================
    @GetMapping("/portal-empresa/riscos")
    public String telaRiscos(HttpSession session, Model model) {
        if (!PerfilUsuario.EMPRESA.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }

        List<Atestado> todosAtestados = atestadoRepository.findAll();
        List<Usuario> funcionarios = usuarioRepository.findByPerfil(PerfilUsuario.FUNCIONARIO);

        if (todosAtestados.isEmpty()) {
            model.addAttribute("semDados", true);
            return "riscos";
        }

        // ---- 风险等级评估 ----
        // 高危: INSS (>15天) 占比 > 20%
        // 中危: INSS 占比 5%-20%
        // 低危: INSS 占比 < 5%
        long totalAtestados = todosAtestados.size();
        long alertasINSS = todosAtestados.stream()
                .filter(a -> a.getDiasAfastamento() != null && a.getDiasAfastamento() > 15)
                .count();
        long aprovados = todosAtestados.stream().filter(a -> a.getStatus() == StatusAtestado.APROVADO).count();

        double percINSS = totalAtestados > 0 ? (double) alertasINSS / totalAtestados * 100 : 0;
        String nivelRisco;
        String corRisco;
        String iconeRisco;
        if (percINSS > 20) {
            nivelRisco = "高风险";
            corRisco = "danger";
            iconeRisco = "fa-triangle-exclamation";
        } else if (percINSS > 5) {
            nivelRisco = "中等风险";
            corRisco = "warning";
            iconeRisco = "fa-exclamation";
        } else {
            nivelRisco = "低风险";
            corRisco = "success";
            iconeRisco = "fa-check-circle";
        }

        // ---- 缺勤率 ----
        // 简化计算: (总批准请假天数 / (员工数 * 365)) * 100
        int totalDiasAfastados = todosAtestados.stream()
                .filter(a -> a.getDiasAfastamento() != null)
                .mapToInt(Atestado::getDiasAfastamento).sum();
        double taxaAbsenteismoGeral = funcionarios.size() > 0
                ? Math.round((double) totalDiasAfastados / (funcionarios.size() * 365) * 10000.0) / 100.0
                : 0.0;

        // ---- Top 10 CID 分布 ----
        Map<String, Long> cidFrequencia = todosAtestados.stream()
                .filter(a -> a.getCid() != null && !a.getCid().isBlank())
                .collect(Collectors.groupingBy(Atestado::getCid, Collectors.counting()));

        List<Map.Entry<String, Long>> topCids = cidFrequencia.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        model.addAttribute("cidLabels", topCids.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        model.addAttribute("cidData", topCids.stream().map(Map.Entry::getValue).collect(Collectors.toList()));

        // ---- CID 风险分类 ----
        // 按 CID 首字母分组: F=精神, M=肌肉骨骼, J=呼吸, S=损伤, Z=社会因素
        Map<String, Long> categoriaRisco = new LinkedHashMap<>();
        for (Atestado a : todosAtestados) {
            if (a.getCid() == null || a.getCid().isBlank()) continue;
            String cat = classificarCID(a.getCid());
            categoriaRisco.merge(cat, 1L, Long::sum);
        }
        model.addAttribute("catLabels", new ArrayList<>(categoriaRisco.keySet()));
        model.addAttribute("catData", new ArrayList<>(categoriaRisco.values()));

        // ---- INSS 预警列表 ----
        List<Map<String, Object>> alertasINSSLista = todosAtestados.stream()
                .filter(a -> a.getDiasAfastamento() != null && a.getDiasAfastamento() > 15)
                .sorted(Comparator.comparing(Atestado::getDiasAfastamento).reversed())
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("funcionario", a.getFuncionario().getNome());
                    m.put("cid", a.getCid());
                    m.put("dias", a.getDiasAfastamento());
                    m.put("dataEnvio", a.getDataEnvio());
                    m.put("status", a.getStatus().name());
                    return m;
                }).collect(Collectors.toList());

        // ---- 最近6个月趋势 (模拟按月分组) ----
        Map<String, Long> tendenciaMensal = new LinkedHashMap<>();
        for (Atestado a : todosAtestados) {
            if (a.getDataEnvio() == null) continue;
            String mes = a.getDataEnvio().getYear() + "-" + String.format("%02d", a.getDataEnvio().getMonthValue());
            tendenciaMensal.merge(mes, 1L, Long::sum);
        }
        // 只保留最近6个月
        List<String> mesesOrdenados = tendenciaMensal.keySet().stream().sorted().collect(Collectors.toList());
        if (mesesOrdenados.size() > 6) {
            mesesOrdenados = mesesOrdenados.subList(mesesOrdenados.size() - 6, mesesOrdenados.size());
        }
        List<Long> valoresMensais = mesesOrdenados.stream().map(tendenciaMensal::get).collect(Collectors.toList());

        // ---- 模型属性 ----
        model.addAttribute("semDados", false);
        model.addAttribute("totalAtestados", totalAtestados);
        model.addAttribute("totalFuncionarios", funcionarios.size());
        model.addAttribute("aprovados", aprovados);
        model.addAttribute("alertasINSS", alertasINSS);
        model.addAttribute("percINSS", Math.round(percINSS * 10.0) / 10.0);
        model.addAttribute("nivelRisco", nivelRisco);
        model.addAttribute("corRisco", corRisco);
        model.addAttribute("iconeRisco", iconeRisco);
        model.addAttribute("taxaAbsenteismoGeral", taxaAbsenteismoGeral);
        model.addAttribute("totalDiasAfastados", totalDiasAfastados);
        model.addAttribute("alertasINSSLista", alertasINSSLista);
        model.addAttribute("mesesLabels", mesesOrdenados);
        model.addAttribute("mesesData", valoresMensais);

        return "riscos";
    }

    /**
     * 按 CID 编码首字母分类到职业健康风险大类
     */
    private String classificarCID(String cid) {
        if (cid == null || cid.isBlank()) return "其他";
        char primeira = Character.toUpperCase(cid.charAt(0));
        switch (primeira) {
            case 'F': return "精神障碍 (F)";
            case 'M': return "肌肉骨骼 (M)";
            case 'J': return "呼吸系统 (J)";
            case 'S': return "损伤/外伤 (S)";
            case 'Z': return "社会/环境因素 (Z)";
            case 'I': return "循环系统 (I)";
            case 'K': return "消化系统 (K)";
            case 'G': return "神经系统 (G)";
            case 'A':
            case 'B': return "传染病 (A-B)";
            case 'C': return "肿瘤 (C)";
            case 'R': return "症状/体征 (R)";
            default: return "其他类别";
        }
    }
}
