package com.shopsphere.inventory;
import com.shopsphere.common.events.Events.Item;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReservationTxTest {
  StockRepository stock = mock(StockRepository.class);
  ReservationRepository reservations = mock(ReservationRepository.class);
  ReservationTx tx = new ReservationTx(stock, reservations);

  @Test void reserveMovesAvailableToReserved() {
    Stock s = new Stock(1L, 10, 0, 0);
    when(stock.findById(1L)).thenReturn(Optional.of(s));
    tx.reserve(100L, List.of(new Item(1L, 3)));
    assertEquals(7, s.getAvailable()); assertEquals(3, s.getReserved());
    verify(reservations).save(any());
  }
  @Test void insufficientStockIsRejectedAndNothingSaved() {
    when(stock.findById(1L)).thenReturn(Optional.of(new Stock(1L, 2, 0, 0)));
    assertThrows(InsufficientStockException.class, () -> tx.reserve(100L, List.of(new Item(1L, 3))));
    verify(reservations, never()).save(any());
  }
  @Test void replayOfSameOrderIsIdempotent() {
    when(reservations.existsById(100L)).thenReturn(true);
    tx.reserve(100L, List.of(new Item(1L, 3)));
    verifyNoInteractions(stock);
  }
  @Test void releaseRestoresStockOnce() {
    Stock s = new Stock(1L, 7, 3, 0);
    Reservation r = new Reservation(); r.setOrderId(100L); r.getLines().add(new Reservation.Line(1L, 3));
    when(reservations.findById(100L)).thenReturn(Optional.of(r)); when(stock.findById(1L)).thenReturn(Optional.of(s));
    tx.release(100L); tx.release(100L);                        // second call is a no-op
    assertEquals(10, s.getAvailable()); assertEquals(0, s.getReserved()); assertEquals(Reservation.State.RELEASED, r.getState());
  }
  @Test void commitConsumesReservedStock() {
    Stock s = new Stock(1L, 7, 3, 0);
    Reservation r = new Reservation(); r.setOrderId(100L); r.getLines().add(new Reservation.Line(1L, 3));
    when(reservations.findById(100L)).thenReturn(Optional.of(r)); when(stock.findById(1L)).thenReturn(Optional.of(s));
    tx.commit(100L);
    assertEquals(7, s.getAvailable()); assertEquals(0, s.getReserved()); assertEquals(Reservation.State.COMMITTED, r.getState());
  }
}
