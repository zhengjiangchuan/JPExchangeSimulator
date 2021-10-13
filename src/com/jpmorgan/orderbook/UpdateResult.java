package com.jpmorgan.orderbook;

public class UpdateResult {

    Order order;
    String rejectString;
    Trade trade;

    public UpdateResult(Order order, String rejectString, Trade trade) {
        this.order = order;
        this.rejectString = rejectString;
        this.trade = trade;
    }

    public Order getOrder() {
        return order;
    }

    public String getRejectString() {
        return rejectString;
    }

    public Trade getTrade() {
        return trade;
    }
}
