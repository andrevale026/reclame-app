package br.com.reclame.repository;

import br.com.reclame.model.Denuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DenunciaRepository extends JpaRepository<Denuncia, Integer> {
    Optional<Denuncia> findByProtocolo(String protocolo);
}
