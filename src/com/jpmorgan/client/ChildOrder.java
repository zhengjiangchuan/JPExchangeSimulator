package com.jpmorgan.client;

import com.jpmorgan.orderbook.Side;

public class ChildOrder
{
    long orderId;
    double price;
    Side side;
    long quantity;

    public ChildOrder(long orderId, double price, Side side, long quantity) {
        this.orderId = orderId;
        this.price = price;
        this.side = side;
        this.quantity = quantity;
    }

    public long getOrderId() {
        return orderId;
    }

    public double getPrice() {
        return price;
    }

    public Side getSide() {
        return side;
    }

    public long getQuantity() {
        return quantity;
    }

    public String toString() {

        return "[orderId=" + orderId + ", price=" + price + ", side=" + side + ", quantity=" + quantity + "]";

    }
}
