package com.shopsphere.common.events;
public final class Topics {
  private Topics() {}
  public static final String INVENTORY_RESERVE_CMD = "inventory.reserve.cmd";
  public static final String INVENTORY_RELEASE_CMD = "inventory.release.cmd";
  public static final String INVENTORY_RESERVED = "inventory.reserved";
  public static final String INVENTORY_FAILED = "inventory.failed";
  public static final String PAYMENT_REQUEST_CMD = "payment.request.cmd";
  public static final String PAYMENT_REFUND_CMD = "payment.refund.cmd";
  public static final String PAYMENT_RESULT = "payment.result";
  public static final String ORDER_EVENTS = "order.events";
}
