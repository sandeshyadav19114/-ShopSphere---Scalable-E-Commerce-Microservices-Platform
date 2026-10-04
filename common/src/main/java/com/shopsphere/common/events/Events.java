package com.shopsphere.common.events;
import java.math.BigDecimal;
import java.util.List;
public final class Events {
  private Events() {}
  public record Item(Long productId, int quantity) {}
  public record ReserveInventoryCmd(Long orderId, List<Item> items) {}
  public record ReleaseInventoryCmd(Long orderId) {}
  public record InventoryReserved(Long orderId) {}
  public record InventoryFailed(Long orderId, String reason) {}
  public record PaymentRequest(Long orderId, Long userId, BigDecimal amount) {}
  public record RefundCmd(Long orderId) {}
  public record PaymentResult(Long orderId, boolean success, String paymentId, String reason) {}
  public record OrderEvent(Long orderId, Long userId, String email, String status, BigDecimal total) {}
}
