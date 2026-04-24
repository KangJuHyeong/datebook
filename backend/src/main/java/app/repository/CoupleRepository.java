package app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domain.Couple;

public interface CoupleRepository extends JpaRepository<Couple, Long> {
}
