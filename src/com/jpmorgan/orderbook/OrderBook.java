package com.jpmorgan.orderbook;

import java.util.*;
import javafx.util.Pair;
import java.math.*;


/**
 * Class that represents the limit order book
 * @author Jiangchuan Zheng
 *
 */
public class OrderBook {
	
	/*
	 * A map that records the mapping from an order id to the corresponding order
	 */
    private Map<Long, Order> orderDict;
	
	private SortedMap<Price, BookLevel> bidBook; //Store all price levels of the bid side of the order book
	private SortedMap<Price, BookLevel> askBook; //Store all price levels of the ask side of the order book

	private double prevClosePrice;

	private final double limitPct = 0.5;

	private Price HIGH_LIMIT;
	private Price LOW_LIMIT;

	private long lotSize;

	private double tickSize;

	public void setPrevClose(double prevClosePrice) {
		this.prevClosePrice = prevClosePrice;
		this.HIGH_LIMIT = new Price(this.prevClosePrice * (1 + limitPct));
		this.LOW_LIMIT = new Price(this.prevClosePrice * (1 - limitPct));
	}

	public void setLotSize(long lotSize) {
		this.lotSize = lotSize;
	}

	public void setTickSize(double tickSize) {
		this.tickSize = tickSize;
	}

	public Price getHighLimit() {
		return HIGH_LIMIT;
	}

	public Price getLowLimit() {
		return LOW_LIMIT;
	}

	public boolean isLimitUp() {
		return bidBook != null && getBestBid().compareTo(HIGH_LIMIT) == 0;
	}

	public boolean isLimitDown() {
		return askBook != null && getBestAsk().compareTo(LOW_LIMIT) == 0;
	}

	/**
	 * Class that represents a price level of the order book
	 * @author Jiangchuan Zheng
	 *
	 */
	private class BookLevel {
		
		private Price price; //The price of this level
		private int totalQuantity; //The total quantity of all the orders placed at this evel
		private int orderNum; //The number of orders placed at this level
		
		private List<Order> orders; //The list storing all orders at this level in the order they are placed 
		
		private BookLevel(Price price) {
			this.price = price;
			this.totalQuantity = 0;
			this.orderNum = 0;
			this.orders = new LinkedList<Order>();
		}
		
		/**
		 * Add an order to this level
		 * @param order  the order to be added
		 */
	    private void addOrder(Order order) {
	    	this.orders.add(order);
	    	this.orderNum++;
	    	this.totalQuantity += order.getQuantity();
	    }
	    
	    /**
	     * Remove an order from this level
	     * @param orderId  the id of the target order to be removed
	     * @return bool value indicating if the removal is successful
	     */
	    private boolean removeOrder(long orderId) {
	    	ListIterator<Order> lit = this.orders.listIterator();
	    	while (lit.hasNext()) {
	    		Order order = lit.next();
	    		if (order.getOrderId() == orderId) {
	    			lit.remove();
	    			this.orderNum--;
	    			this.totalQuantity -= order.getQuantity();
	    			return true;
	    		}
	    	}
	    	
	    	return false; //Target order does not exist
	    }
	    
	    /**
	     * Update the quantity of an order at this level
	     * @param orderId  the id of the target order
	     * @param deltaQuantity  the increment to be added to the quantity of the target order
	     * @return bool value indicating if the update is successful
	     */
	    private boolean updateOrder(long orderId, long deltaQuantity) {
	    	ListIterator<Order> lit = this.orders.listIterator();
	    	while (lit.hasNext()) {
	    		Order order = lit.next();
	    		if (order.getOrderId() == orderId) {
	    			order.updateQuantity(deltaQuantity);
	    			this.totalQuantity += deltaQuantity;
	    			return true;
	    		}
	    	}
	    	
	    	return false; //Target order does not exist
	    }
	    
	    /**
	     * Trade a certain quantity on orders placed at this level
	     * @param tradeQuantity  the maximum quantity to trade with orders at this level
	     * @return a Trade object storing all those orders at this level that get traded
	     */
	    private Trade trade(long tradeQuantity) {
	    	Trade trade = new Trade();
	    	
	    	ListIterator<Order> lit = this.orders.listIterator();
	    	Order order = null;
	    	long quantity = 0;
	    	
	    	/*
	    	 * Scan the list of orders to trade them one by one until all the order have been traded or
	    	 * the given quantity (tradeQuantity) gets traded completely
	    	 */
	    	while (lit.hasNext() && tradeQuantity > 0) {
	    		order = lit.next();
	    		quantity = order.getQuantity();
	    		if (tradeQuantity >= quantity) {
	    			trade.addTradedOrder(order);
	    			lit.remove();

	    			orderDict.remove(order.getOrderId());

	    			this.orderNum--;
	    			this.totalQuantity -= quantity;
	    			
	    			tradeQuantity -= quantity;
	    			
	    		}
	    		else {
	    			trade.addTradedOrder(order.split(tradeQuantity));
	    			this.totalQuantity -= tradeQuantity;
	    			break;
	    		}
	    	}
	    	
	    	return trade;
	    }
	}
	
	
	
	public OrderBook() {
		
		this.orderDict = new HashMap<Long, Order>();
		
		this.bidBook = new TreeMap<Price, BookLevel>(Collections.reverseOrder()); //Bid book stores the price levels from the highest price to the lowest
		this.askBook = new TreeMap<Price, BookLevel>(); //Ask book stores the price levels from the lowest price to the highest price
	}
	
	/**
	 * Return a text representation of the current order book for printing
	 */
	public String toString() {
		
		int capacity = 8;
		
		StringBuilder b = new StringBuilder();
		b.append("OrderBook:\n");
		
		Iterator<BookLevel> bidIt = this.bidBook.values().iterator();
		Iterator<BookLevel> askIt = this.askBook.values().iterator();
		
		String headBuy = "Buy";
		String headSell = "Sell";
		
		for (int i = 0; i < capacity - headBuy.length(); i++) {
			b.append(" ");
		}
		b.append(headBuy);
		b.append(" ");
		for (int i = 0; i < capacity; i++) {
			b.append(" ");
		}
		
		b.append("|");
		
		for (int i = 0; i < capacity - headSell.length(); i++) {
			b.append(" ");
		}
		b.append(headSell);
		b.append("\n\n");
		
		BookLevel bidLevel = null;
		BookLevel askLevel = null;
		while (bidIt.hasNext() || askIt.hasNext()) {
			if (bidIt.hasNext()) {
				bidLevel = bidIt.next();
				String bidPrice = bidLevel.price.toString();
				String quantity = String.valueOf(bidLevel.totalQuantity);
				for (int i = 0; i < capacity - quantity.length(); i++) {
					b.append(" ");
				}
				b.append(quantity);
				b.append("@");
				b.append(bidPrice);
				for (int i = 0; i < capacity - bidPrice.length(); i++) {
					b.append(" ");
				}
			}
			else {
				for (int i = 0; i < 2*capacity+1; i++) {
					b.append(" ");
				}
			}
			
			b.append("|");
			
			if (askIt.hasNext()) {
				askLevel = askIt.next();
				String askPrice = askLevel.price.toString();
				String quantity = String.valueOf(askLevel.totalQuantity);
				for (int i = 0; i < capacity - quantity.length(); i++) {
					b.append(" ");
				}
				b.append(quantity);
				b.append("@");
				b.append(askPrice);
				b.append("\n");
			}
			else {
				b.append("\n");
			}
		}
		
		
		return b.toString();
	}
	

	public Price getBestBid() {
		if (this.bidBook.isEmpty()) {
			return null;
		}
		return this.bidBook.firstKey();
	}

	public long getBestBidQuantity() {
		if (this.bidBook.isEmpty())
		{
			return 0;
		}
		else {
			BookLevel bookLevel = this.bidBook.get(getBestBid());
			if (bookLevel != null) {
				return bookLevel.totalQuantity;
			}
			else {
				return 0;
			}
		}
	}



	public Price getBestAsk() {
		if (this.askBook.isEmpty()) {
			return null;
		}
		return this.askBook.firstKey();
	}


	public long getBestAskQuantity() {
		if (this.askBook.isEmpty())
		{
			return 0;
		}
		else {
			BookLevel bookLevel = this.askBook.get(getBestAsk());
			if (bookLevel != null) {
				return bookLevel.totalQuantity;
			}
			else {
				return 0;
			}
		}
	}



	public BookLevel getBookLevel(double rawPrice) {
		Price price = new Price(rawPrice);
		if (!this.bidBook.isEmpty() && price.compareTo(getBestBid()) <= 0) {
			BookLevel bookLevel = this.bidBook.get(price);
			if (bookLevel != null)
			{
				return bookLevel;
			}
			else {
				return null;
			}
		}
		else if (!this.askBook.isEmpty() && price.compareTo(getBestAsk()) >= 0) {
			BookLevel bookLevel = this.askBook.get(price);
			if (bookLevel != null)
			{
				return bookLevel;
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	public long getQuantityAtPrice(double rawPrice)
	{
        BookLevel bookLevel = getBookLevel(rawPrice);
        if (bookLevel == null) {
        	return 0;
		}
        else {
        	return bookLevel.totalQuantity;
		}
	}

	public int getOrderNumAtPrice(double rawPrice)
	{
		BookLevel bookLevel = getBookLevel(rawPrice);
		if (bookLevel == null) {
			return 0;
		}
		else {
			return bookLevel.orderNum;
		}
	}




	/**
	 * Execute the trade triggered by the given limit order according to the order matching rules
	 * @param order  the given order that triggers a trade
	 * @return a Trade object storing the list of all limit orders that get traded with this given order
	 */
	private Trade executeTrade(Order order) {
		Price price = order.getPrice();
		
		SortedMap<Price, BookLevel> book = null;
		if (order.isBid()) {
			book = this.askBook; //A cross book buy order trades with the orders in the sell limit book
		}
		else if (order.isAsk()) {
			book = this.bidBook; //A cross book sell order trades with the orders in the buy limit book
		}
		
		long quantity = order.getQuantity();

        Trade trade = new Trade();
		
        //All price levels better than the price of the given order are candidate levels that will trade with the order
		SortedMap<Price, BookLevel> betterLevels = null;

		if (price == null)
		{
			if (order.isBid())
			{
				price = HIGH_LIMIT;
				order.setPrice(price);
			}
			else {
				price = LOW_LIMIT;
				order.setPrice(price);
			}

			betterLevels = book; //This is market trade
		}
		else {
			betterLevels = book.headMap(price);
		}

		List<Price> levelsToRemove = new ArrayList<Price>();
		
		/*
		 * Trade the given order with all candidate levels from the best level all the way to the order's price level,
		 * until the given order gets traded completely, or all orders in all candidate levels get traded completely.
		 * In the former case, the remaining part of the given order remains in the order book as a limit order
		 */
		for (Map.Entry<Price, BookLevel> level : betterLevels.entrySet()) {
			Price betterPrice = level.getKey();
			BookLevel betterLevel = level.getValue();
			
			Trade levelTrade = betterLevel.trade(quantity);
			
			if (betterLevel.totalQuantity == 0) {
				levelsToRemove.add(betterPrice); //If all orders in this level get traded, then remove this level from the book later
			}
			
			quantity -= levelTrade.getTotalQuantity();
			trade.addTrade(levelTrade); //Add all the orders traded at this level to the Trade object
			
			if (quantity == 0) { // The given order gets traded completely, stop
				break;
			}
		}
		
		/*
		 * Remove all levels for which all orders have got traded with the given order
		 */
		for (Iterator<Price> it = levelsToRemove.iterator(); it.hasNext(); ) {
			Price priceToRemove = it.next();
			book.remove(priceToRemove);
		}
		
		/*
		 * Trade the remaining part of the given order with the orders at 
		 * its price level if all better levels are not sufficient to trade it completely
		 */
		if (quantity > 0) { 

			BookLevel currentLevel = book.get(price);
			if (currentLevel != null) {
				Trade levelTrade = currentLevel.trade(quantity);
				if (currentLevel.totalQuantity == 0) {
					book.remove(price);
				}
				trade.addTrade(levelTrade);
			}


		}
		
		return trade;
	}
	
	/**
	 * Judge if the given order will trigger a trade to happen
	 * @param order
	 * @return bool value indicating whether the given order will trigger a trade
	 */
	private boolean triggerTrade(Order order) {
		
		Price price = order.getPrice();

		if (price == null) {
			return true; //This is a market order, must trigger a trade
		}

		if (order.isBid()) {
			Price bestAsk = this.getBestAsk();
			if (bestAsk != null && price.compareTo(bestAsk) >= 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else if (order.isAsk()) {
			Price bestBid = this.getBestBid();
			if (bestBid != null && price.compareTo(bestBid) <= 0) {
				return true;
			}
			else {
				return false;
			}
		}
		
		return false;
	}
	

	public Trade processInboundOrder(Order order) {
		
		System.out.println(order);
		
		if (triggerTrade(order)) { //Judge if the given order will trigger a trade
			Trade trade = this.executeTrade(order); //If it triggers a trade, then executes the trade, get all limit orders that trade with the given order
			int tradeQuantity = trade.getTotalQuantity();
			if (order.getQuantity() > tradeQuantity) {//If the given order does not trade completely, the remaining part will form a limit order and added to the book

				Order filledOrder = order.split(tradeQuantity);
				trade.setTradingOrder(filledOrder);

				//order.updateQuantity(-tradeQuantity);
				this.addOrder(order); //Remaining part of the given order not filled is added to the book as a child order
				//this.orderDict.put(order.getOrderId(), order);
			}
			else if (order.getQuantity() == tradeQuantity) {
			    Order filledOrder = order;
			    trade.setTradingOrder(filledOrder);
			}

			return trade;
		}
		else {
			//If the given order does not trigger a trade, just add this order as a limit order to the book
			this.addOrder(order);
			//this.orderDict.put(order.getOrderId(), order);
			return null;
		}
		
		
	}

	public String checkPlaceOrder(Order order) {
		String rejectString = null;

		rejectString = checkPlaceOrderPrice(order.getPrice());
		if (rejectString != null)
		{
			return rejectString;
		}

		rejectString = checkPlaceOrderSize(order.getRawSide(), order.getQuantity());


		return rejectString;

	}

	public String checkPlaceOrderPrice(Price price) {

		String rejectString = null;

		if (price != null && price.compareTo(HIGH_LIMIT) > 0) {
			rejectString = "Cannot place order above exchange high limit " + HIGH_LIMIT.getRawPrice();
		}
		else if (price != null && price.compareTo(LOW_LIMIT) < 0) {
			rejectString = "Cannot place order below exchange low limit " + LOW_LIMIT.getRawPrice();
		}
		else if (price != null && Math.abs(Math.round(price.getRawPrice()/tickSize) - price.getRawPrice()/tickSize)>1e-5) {
			rejectString = "Price to be placed must be a multiple of tick size " + tickSize;
		}

		return rejectString;
	}

	public String checkPlaceOrderSize(Side side, long quantity) {

		String rejectString = null;

		if (side == Side.BUY && lotSize != 0 && quantity%lotSize != 0) {
			rejectString = "Cannot buy shares with odd lot";
		}

		return rejectString;

	}


	private void addOrder(Order order) {
		
		SortedMap<Price, BookLevel> book = null;
		if (order.isBid()) {
			book = this.bidBook;
		}
		else if (order.isAsk()) {
			book = this.askBook;
		}
		
		Price price = order.getPrice();

		if (price == null) { //This is a market order that trades the entire opposite side book and remains some quantity
            if (order.isBid()) {
            	price = this.HIGH_LIMIT;
			}
            else if (order.isAsk()) {
            	price = this.LOW_LIMIT;
			}
		}


		BookLevel bookLevel = book.get(price);
		if (bookLevel == null) {
			bookLevel = new BookLevel(price);
			book.put(price, bookLevel);
		}

		bookLevel.addOrder(order);

		this.orderDict.put(order.getOrderId(), order);
	}

	public UpdateResult updateOrderPrice(long orderId, Side side, Price newPrice) {

		String rejectString = null;

		if (this.orderDict.containsKey(orderId)) {
			Order order = this.orderDict.get(orderId);
            if (order.getPrice().equals(newPrice)) {

            	rejectString = "Order " + orderId + " cannot be amended to the same price";
                //return new Pair(null, rejectString);
                return new UpdateResult(null, rejectString, null);
			}
		}

		Pair<Order, String> result = removeOrder(orderId, side);
		rejectString = result.getValue();

		if (rejectString != null)
		{
			return new UpdateResult(result.getKey(), result.getValue(), null);
		}
		else {
			Order order = result.getKey();
            order.setPrice(newPrice);

            Trade trade = processInboundOrder(order);

            //addOrder(order);
            //return new Pair<Order, String>(order, null);
            return new UpdateResult(order, null, trade);
		}

	}


	/**
	 * Update the quantity of an order
	 * @param orderId  the id of the target order to be updated
	 * @return removed order and reject string if rejected
	 */
	public Pair<Order, String> removeOrder(long orderId, Side side) {

        String rejectString = null;

	    if (!this.orderDict.containsKey(orderId)) {

	    	rejectString = "Order " + orderId + " to be canceled does not exist";
	    	return new Pair(null, rejectString);
	    }
	    else {

			Order order = this.orderDict.get(orderId);

			if (side != order.getRawSide()) {
				rejectString = "Order " + orderId + " to be canceled has wrong side";
				return new Pair(null, rejectString);
			}

	    	Price price = order.getPrice();
	    	BookLevel level = null;
	    	SortedMap<Price, BookLevel> book = null;
	    	if (order.isBid()) {
	    		book = this.bidBook;
	    	}
	    	else if (order.isAsk()) {
	    		book = this.askBook;
	    	}
	    	
	    	level = book.get(price);
	    	if (level.removeOrder(orderId)) { //Remove the target order from the price level where it resides
	    		if (level.totalQuantity == 0) {  
	    			book.remove(price); //If after removing this order no orders are remaining in this level, remove this level from the book
	    		}

	    		this.orderDict.remove(orderId);

	    		return new Pair(order, rejectString);
	    	}
	    	else {
	    		return new Pair(order, rejectString);
	    	}
	    }
	}
	
	/**
	 * Update the quantity of an order 
	 * @param orderId  the id of the target order to be updated
	 * @param newQuantity  the quantity to be updated to
	 * @return updated order and reject string if rejected
	 */
	public Pair<Order, String> updateOrderQuantity(long orderId, Side side, long newQuantity) {

		String rejectString = null;

		if (newQuantity == 0) {
			rejectString = "Order " + orderId + " quantity cannot be amended to 0";
			return new Pair(null, rejectString);
		}

		if (!this.orderDict.containsKey(orderId)) {

			rejectString = "Order " + orderId + " to be amend quantity does not exist";
			return new Pair(null, rejectString);
		}
	    else {

			Order order = this.orderDict.get(orderId);

			if (side != order.getRawSide()) {
				rejectString = "Order " + orderId + " to be canceled has wrong side";
				return new Pair(null, rejectString);
			}

			long deltaQuantity = newQuantity - order.getQuantity();

			if (deltaQuantity < 0)
			{
				if (order.isBid()) {
					this.bidBook.get(order.getPrice()).updateOrder(orderId, deltaQuantity);
				}
				else if (order.isAsk()) {
					this.askBook.get(order.getPrice()).updateOrder(orderId, deltaQuantity);
				}

				return new Pair(order, rejectString);

			}
			else if (deltaQuantity > 0) {
				rejectString = "Order " + orderId + " quantity cannot be amended up";
				return new Pair(null, rejectString);
			}
			else {
				rejectString = "Order " + orderId + " quantity cannot be amended to the same value";
				return new Pair(null, rejectString);
			}


	    }
		

	}
	
}
