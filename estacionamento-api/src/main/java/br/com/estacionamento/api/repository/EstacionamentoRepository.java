package br.com.estacionamento.api.repository;

import br.com.estacionamento.api.model.Estacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstacionamentoRepository extends JpaRepository<Estacionamento, Long> {

    @Query(value = "SELECT * FROM (SELECT *, (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(latitude)))) AS distancia FROM estacionamentos) AS subquery WHERE distancia < :raio ORDER BY distancia", nativeQuery = true)
    List<Estacionamento> findProximos(@Param("lat") Double lat, @Param("lng") Double lng, @Param("raio") Double raio);
}
