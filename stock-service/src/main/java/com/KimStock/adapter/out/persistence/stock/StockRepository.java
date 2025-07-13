package com.KimStock.adapter.out.persistence.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, String> {

    @Query("SELECT s FROM StockEntity s WHERE s.marketCode = :marketCode")
    List<StockEntity> findByMarketCode(@Param("marketCode") String marketCode);

    @Query("SELECT s FROM StockEntity s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<StockEntity> findByNameContaining(@Param("name") String name);

    @Query("SELECT s FROM StockEntity s WHERE s.code LIKE %:code%")
    List<StockEntity> findByCodeContaining(@Param("code") String code);

    @Query("SELECT s FROM StockEntity s WHERE LOWER(s.name) = LOWER(:name)")
    List<StockEntity> findByNameIgnoreCase(@Param("name") String name);
}
