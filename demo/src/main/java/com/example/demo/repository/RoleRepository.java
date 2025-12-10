package com.example.demo.repository;

import com.example.demo.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    // Способ 1: Используйте @Query
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) = LOWER(:name)")
    Optional<Role> findByName(@Param("name") String name);
    
    // Или Способ 2: Попробуйте точное совпадение
    @Query("SELECT r FROM Role r WHERE r.name = :name")
    Optional<Role> findByNameExact(@Param("name") String name);
    
    boolean existsByName(String name);
}