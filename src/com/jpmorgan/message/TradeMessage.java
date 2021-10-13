package com.jpmorgan.message;

import com.jpmorgan.orderbook.Side;

public class TradeMessage extends ExchangeMessage {

    long orderId;
    double price;
    long fillQuantity;
    Side side;

    public TradeMessage(long orderId, double price, long fillQuantity, Side side)
    {
        this.orderId = orderId;
        this.price = price;
        this.fillQuantity = fillQuantity;
        this.side = side;
    }

    public long getOrderId() {
        return orderId;
    }

    public double getPrice() {
        return price;
    }

    public long getFillQuantity() {
        return fillQuantity;
    }

    public Side getSide() {
        return side;
    }

    public String toString()
    {
        return "[orderId=" + orderId + ", price=" + price + ", fillQuantity=" + fillQuantity + ", side=" + side + "]";
    }

}
