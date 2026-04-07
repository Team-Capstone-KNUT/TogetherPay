package com.devcrew.togetherpay.domain.settlement.repository;

import com.devcrew.togetherpay.domain.settlement.Settlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

  List<Settlement> findByExpense_Id(Long expenseId);

  Optional<Settlement> findByIdAndUser_Id(Long id, Long userId);

  List<Settlement> findByExpense_Team_Id(Long teamId);

  List<Settlement> findByUser_Id(Long userId);
}
