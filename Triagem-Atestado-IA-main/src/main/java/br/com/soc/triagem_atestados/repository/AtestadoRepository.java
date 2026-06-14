package br.com.soc.triagem_atestados.repository;

import br.com.soc.triagem_atestados.model.Atestado;
import br.com.soc.triagem_atestados.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AtestadoRepository extends JpaRepository<Atestado, Long> {
    List<Atestado> findByStatus(String status); 
    List<Atestado> findByFuncionario(Usuario funcionario);
}