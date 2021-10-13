package com.jpmorgan.orderbook;

import java.util.*;

/**
 * Class that represents a price
 * @author Jiangchuan Zheng
 *
 */
public class Price implements Comparable<Price> {

	/*
	 * To save space and to be more efficient, store all prices as integers.
	 * So any raw price with no more than 3 decimal places will multiply by this multiplier to become an integer.
	 */
	private static final int DECIMAL_PLACE = 3;
	private static final int MULTIPLIER = (int)Math.pow(10, DECIMAL_PLACE);
	
	static enum Validity {
		VALID,
		NON_POSITIVE,
		EXTRA_DECIMAL
	}
	
	private static final Map<Validity, String> DIAGONOSE_MAP;
	static {
		DIAGONOSE_MAP = new HashMap<Validity, String>();
		DIAGONOSE_MAP.put(Validity.VALID, "");
		DIAGONOSE_MAP.put(Validity.NON_POSITIVE, "The input price should be a positive value");
		DIAGONOSE_MAP.put(Validity.EXTRA_DECIMAL, "The input price can have at most " + (DECIMAL_PLACE) + " decimal places");
		
	}

	private double rawPrice;
	public int price;
	private Validity validity; 
	
	public Price(double rawPrice) {
		initialize(rawPrice);
	}
	
	public Price(String rawPrice) {
		this(Double.valueOf(rawPrice));
	}
	
	private void initialize(double rawPrice) {
		this.rawPrice = rawPrice;
		double adjustedPrice = rawPrice * MULTIPLIER;
		this.price = (int)Math.floor(adjustedPrice);
		
		if (rawPrice <= 0) {
			this.validity = Validity.NON_POSITIVE;
		}
		else if (this.price != adjustedPrice) {
			this.validity = Validity.EXTRA_DECIMAL;
		}
		else {
			this.validity = Validity.VALID;
		}
	}

	public double getRawPrice() {
		return rawPrice;
	}
	
	public boolean isValid() {
		return this.validity == Validity.VALID;
	}
	
	public String getErrorMsg() {
		return DIAGONOSE_MAP.get(this.validity);
	}
	
	public String toString() {
		
		if (this.price%MULTIPLIER == 0) {
			return String.valueOf(this.price/MULTIPLIER);
		}
		else {
			return String.valueOf((double)this.price/MULTIPLIER);
		}
	}
	
	public int compareTo(Price other) {
		if (this.price < other.price)
			return -1;
		else if (this.price > other.price)
			return 1;
		else
			return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		else if (other == null) {
			return false;
		} else if (other instanceof Price) {
			Price otherPrice = (Price)other;
			return this.compareTo(otherPrice) == 0;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {

		return new Double(rawPrice).hashCode();
	}
}
