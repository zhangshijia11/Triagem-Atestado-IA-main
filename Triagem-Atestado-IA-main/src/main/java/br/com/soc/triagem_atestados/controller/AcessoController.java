package br.com.soc.triagem_atestados.controller;

import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.PerfilUsuario;
import br.com.soc.triagem_atestados.model.Atestado;
import br.com.soc.triagem_atestados.repository.UsuarioRepository;
import br.com.soc.triagem_atestados.repository.AtestadoRepository;
import jakarta.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller // 处理 Web 路由
public class AcessoController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AtestadoRepository atestadoRepository;

    // 1. 初始路由（打开登录页面）
    @GetMapping("/")
    public String telaLogin() {
        return "login";
    }

    // 2. Lógica de Login
    @PostMapping("/login")
    // RequestParam 获取表单输入 nas caixas de texto email e senha
    public String fazerLogin(@RequestParam String email, @RequestParam String senha, 
                             HttpSession session, RedirectAttributes redirectAttributes) {
       // 从 Optional 中提取用户 
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        
        // 用户存在且密码匹配
        if (usuario != null && BCrypt.checkpw(senha, usuario.getSenha())) {
            // 将登录用户信息存入 Session
            session.setAttribute("usuarioLogado", usuario.getNome());
            session.setAttribute("emailUsuario", usuario.getEmail());
            session.setAttribute("perfil", usuario.getPerfil());

            // 登录后重定向
            if (usuario.getPerfil() == PerfilUsuario.EMPRESA) {
                return "redirect:/portal-empresa/home";
            } else {
                return "redirect:/portal-colaborador";
            }
        }

        redirectAttributes.addFlashAttribute("erro", "邮箱或密码错误。");
        return "redirect:/";
    }

    // 3. Rota para carregar o Dashboard do Colaborador com os dados históricos
    @GetMapping("/portal-colaborador")
    public String exibirPortalColaborador(HttpSession session, Model model) {
        String emailLogado = (String) session.getAttribute("emailUsuario");
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailLogado).orElse(null);
        // 验证用户身份权限
       if (!PerfilUsuario.FUNCIONARIO.equals(session.getAttribute("perfil"))) {
            return "redirect:/";
        }

        // 从数据库查询该员工的病假条
        List<Atestado> meusAtestados = atestadoRepository.findByFuncionario(usuarioLogado);
        model.addAttribute("meusAtestados", meusAtestados);

        return "colaborador"; // Abre o arquivo colaborador.html
    }

    // 4. Lógica para sair do sistema
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 清除 Session
        return "redirect:/";
    }

}