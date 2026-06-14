package br.com.soc.triagem_atestados.repository;

import br.com.soc.triagem_atestados.model.DominioCid;
import org.springframework.data.jpa.repository.JpaRepository;

// O tipo do ID é String porque tem letras
public interface CidRepository extends JpaRepository<DominioCid, String> {
}