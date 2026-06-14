package br.com.soc.triagem_atestados.config;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        
        // 拦截未登录请求
        if (session.getAttribute("usuarioLogado") == null) {
            response.sendRedirect("/");
            return false; // 阻止请求
        }
        return true; // 放行
    }
}
