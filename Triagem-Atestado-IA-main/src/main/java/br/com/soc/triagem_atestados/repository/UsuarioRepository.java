package br.com.soc.triagem_atestados.repository;

import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.PerfilUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // O Spring Data transforma isso em um: SELECT * FROM USUARIO WHERE EMAIL = ?
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByPerfil(PerfilUsuario perfil);
    long countByPerfil(PerfilUsuario perfil);
}