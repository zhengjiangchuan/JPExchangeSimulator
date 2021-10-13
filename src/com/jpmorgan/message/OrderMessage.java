package com.jpmorgan.message;

import com.jpmorgan.orderbook.OrderState;
import com.jpmorgan.orderbook.Side;

public class OrderMessage extends ExchangeMessage {

    final OrderState state;
    final long orderID;
    final Side side;
    final double price;
    final long quantity;
    final String reason;

    public OrderMessage(OrderState state, long orderID, Side side, double price, long quantity, String reason)
    {
        this.state = state;
        this.orderID = orderID;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.reason = reason;
    }

    public OrderState getOrderState()
    {
        return state;
    }

    public long getOrderID() {
        return orderID;
    }

    public Side getSide() {
        return side;
    }

    public double getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public String toString()
    {
        return "[orderState=" + state + ", orderId=" + orderID + ", side=" + side + ", price=" + price + ", quantity=" + quantity + ", reason=" + reason + "]";
    }
}

