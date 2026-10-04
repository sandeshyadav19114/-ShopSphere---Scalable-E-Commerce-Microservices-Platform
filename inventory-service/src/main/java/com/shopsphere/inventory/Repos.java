package com.shopsphere.inventory;
import org.springframework.data.jpa.repository.JpaRepository;
interface StockRepository extends JpaRepository<Stock, Long> {}
interface ReservationRepository extends JpaRepository<Reservation, Long> {}
