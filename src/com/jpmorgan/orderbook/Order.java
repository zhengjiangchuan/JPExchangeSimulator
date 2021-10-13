package com.jpmorgan.orderbook;

import java.util.*;

/**
 * Class that describes an order
 * @author Jiangchuan Zheng
 *
 */



public class Order {

	//Global id automatically generated to assign to an inbound order
	private static long GLOBAL_ID = 0;

	private long orderId; //The id of the order
	private Price price;  //The price of the order. If this is null, then it means market order
	private Side side;  //The side of the order (Bid or Ask)
	private long quantity; //The quantity (number of shares) of the order

	public Order(Price price, Side side, long quantity) {
		this(++GLOBAL_ID, price, side, quantity);
	}
	
	public Order(long orderId, Price price, Side side, long quantity) {
		initialize(price, side, quantity);
		this.orderId = orderId;
	}
	
	public Order(Price price, String side, int quantity) {
		this(++GLOBAL_ID, price, side, quantity);
	}
	
	public Order(long orderId, Price price, String side, int quantity) {
		if (side.equals("Buy") || side.equals("B")) {
			initialize(price, Side.BUY, quantity);
		}
		else if (side.equals("Sell") || side.equals("S")) {
			initialize(price, Side.SELL, quantity);
		}
		this.orderId = orderId;
	}

	public void setPrice(Price newPrice) {
		this.price = newPrice;
	}


	/**
	 * Split the current order to two child orders, one traded order with quantity equal to the argument
	 * tradeQuantity, and the other remaining limit order with quantity equal to the current quantity minus tradeQuantity 
	 * @param tradeQuantity  the quantity traded for this order
	 * @return an order representing the trade filled by this order
	 */
	public Order split(long tradeQuantity) {
		Order order = new Order(this.orderId, this.price, this.side, tradeQuantity);
		this.updateQuantity(-tradeQuantity);
		return order;
	}
	
	public void initialize(Price price, Side side, long quantity) {
		this.price = price;
		this.side = side;
		this.quantity = quantity;
	}
	
	/**
	 * Tell if the current order is a buy order or not
	 * @return bool value indicating if the order is a bid order
	 */
	public boolean isBid() {
		return this.side == Side.BUY;
	}
	
	/**
	 * Tell if the current order is a sell order or not
	 * @return bool value indicating if the order is a sell order
	 */
	public boolean isAsk() {
		return this.side == Side.SELL;
	}
	
	/**
	 * Get the id of the order
	 * @return the id of the order
	 */
	public long getOrderId() {
		return this.orderId;
	}
	
	/**
	 * Get the price of the order
	 * @return the price of the order
	 */
	public Price getPrice() {
		return this.price;
	}
	
	/**
	 * Get the side of the order
	 * @return a character indicating the side of the order
	 */
	public String getSide() {
		if (this.side == Side.BUY) {
			return "B";
		}
		else if (this.side == Side.SELL) {
			return "S";
		}
		
		return null;	
	}

	public Side getRawSide() {
		return this.side;
	}
	
	/**
	 * Get the quantity of the order
	 * @return the quantity of the order
	 */
	public long getQuantity() {
		return this.quantity;
	}
	
	/**
	 * Update the quantity of the order by adding a delta quantity
	 * @param deltaQuantity  the increment added to the current quantity
	 */
	public void updateQuantity(long deltaQuantity) {
		this.quantity += deltaQuantity;
	}
	
	public String toString() {
		return "Order: side=" + this.side + " quantity=" + this.quantity + " price=" + this.price;
	}
	
	
}
