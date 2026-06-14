package br.com.soc.triagem_atestados.repository;

import br.com.soc.triagem_atestados.model.Exame;
import br.com.soc.triagem_atestados.model.Usuario;
import br.com.soc.triagem_atestados.model.enums.StatusExame;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExameRepository extends JpaRepository<Exame, Long> {
    List<Exame> findByFuncionario(Usuario funcionario);
    List<Exame> findByStatus(StatusExame status);
    long countByStatus(StatusExame status);
}
