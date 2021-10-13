package com.jpmorgan.orderbook;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that represents a trade
 * @author Jiangchuan Zheng
 *
 */
public class Trade {
    private int totalQuantity; //Total quantity of the trade
    private List<Order> tradedOrders; //The list of limit orders that get traded with the given order that triggers the trade
    private Order tradingOrder;

    public Trade() {
        this.totalQuantity = 0;
        this.tradedOrders = new LinkedList<Order>();
    }

    public void addTradedOrder(Order tradedOrder) {
        this.tradedOrders.add(tradedOrder);
        this.totalQuantity += tradedOrder.getQuantity();
    }

    public void setTradingOrder(Order tradingOrder) {
        this.tradingOrder = tradingOrder;
    }

    public Order getTradingOrder() {
        return this.tradingOrder;
    }

    /**
     * Add all the traded orders of another trade object to the current trade object
     * @param trade
     */
    public void addTrade(Trade trade) {
        this.tradedOrders.addAll(trade.getTradedOrders());
        this.totalQuantity += trade.getTotalQuantity();
    }

    public int getTotalQuantity() {
        return this.totalQuantity;
    }

    public List<Order> getTradedOrders() {
        return this.tradedOrders;
    }

    public String toString() {

        int capacity = 8;

        StringBuilder b = new StringBuilder();

        b.append("Trades:\n");

        Iterator<Order> it = tradedOrders.iterator();
        while (it.hasNext()) {
            Order tradedOrder = it.next();
            String price = tradedOrder.getPrice().toString();
            String quantity = String.valueOf(tradedOrder.getQuantity());
            for (int i = 0; i < capacity - quantity.length(); i++) {
                b.append(" ");
            }
            b.append(quantity);
            b.append("@");
            b.append(price);
            b.append("\n");
        }
        //b.append("\n");

        return b.toString();
    }
}
