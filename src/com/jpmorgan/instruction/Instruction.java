package com.jpmorgan.instruction;

import com.jpmorgan.orderbook.Side;

public class Instruction {

    private final InstructionAction action;
    private final double price;
    private final long quantity;
    private final long orderID;
    private final OrderType orderType;
    private final Side side;

    private Instruction(InstructionAction action, double price, long quantity, long orderID,
                       OrderType orderType, Side side) {

        this.action = action;
        this.price = price;
        this.quantity = quantity;
        this.orderID = orderID;
        this.orderType = orderType;
        this.side = side;
    }

    public static Instruction createPlaceLimitInstruction(double price, long quantity, Side side)
    {
        return new Instruction(InstructionAction.PLACE_ORDER, price, quantity, -1L, OrderType.LIMIT, side);
    }

    public static Instruction createPlaceMarketInstruction(long quantity, Side side)
    {
        return new Instruction(InstructionAction.PLACE_ORDER, 0, quantity, -1L, OrderType.MARKET, side);
    }

    public static Instruction createCancelInstruction(Side side, long orderID)
    {
        return new Instruction(InstructionAction.CANCEL, 0, 0, orderID, OrderType.LIMIT, side);
    }

    public static Instruction createAmendPriceInstruction(Side side, double price, long orderID)
    {
        return new Instruction(InstructionAction.AMEND_PRICE, price, 0, orderID, OrderType.LIMIT, side);
    }

    public static Instruction createAmendQuantityInstruction(Side side, long quantity, long orderID)
    {
        return new Instruction(InstructionAction.AMEND_QUANTITY, 0, quantity, orderID, OrderType.LIMIT, side);
    }



    public InstructionAction getAction()
    {
        return action;
    }

    public double getPrice()
    {
        return price;
    }

    public long getQuantity()
    {
        return quantity;
    }

    public long getOrderID()
    {
        return orderID;
    }

    public OrderType getOrderType()
    {
        return orderType;
    }

    public Side getSide()
    {
        return side;
    }

    public String toString()
    {

//        public static Instruction createPlaceLimitInstruction(double price, long quantity, Side side)
//        {
//            return new Instruction(InstructionAction.PLACE_ORDER, price, quantity, -1L, OrderType.LIMIT, side);
//        }
//
//        public static Instruction createPlaceMarketInstruction(long quantity, Side side)
//        {
//            return new Instruction(InstructionAction.PLACE_ORDER, 0, quantity, -1L, OrderType.MARKET, side);
//        }
//
//        public static Instruction createCancelInstruction(Side side, long orderID)
//        {
//            return new Instruction(InstructionAction.CANCEL, 0, 0, orderID, OrderType.LIMIT, side);
//        }
//
//        public static Instruction createAmendPriceInstruction(Side side, double price, long orderID)
//        {
//            return new Instruction(InstructionAction.AMEND_PRICE, price, 0, orderID, OrderType.LIMIT, side);
//        }
//
//        public static Instruction createAmendQuantityInstruction(Side side, long quantity, long orderID)
//        {
//            return new Instruction(InstructionAction.AMEND_QUANTITY, 0, quantity, orderID, OrderType.LIMIT, side);
//        }

        String ins = null;

        switch (this.action)
        {
            case PLACE_ORDER:
                if (this.orderType == OrderType.LIMIT)
                {
                    ins = "Instruction [action=" + action +", side=" + side + ", price=" + price +
                            ", quantity=" + quantity +  ", orderType=" + orderType;
                }
                else if (this.orderType == OrderType.MARKET)
                {
                    ins = "Instruction [action=" + action + ", side=" + side  +
                            ", quantity=" + quantity + ", orderID=" + orderID + ", orderType=" + orderType;
                }
                break;
            case CANCEL:
                ins = "Instruction [action=" + action + ", side=" + side +
                         ", orderID=" + orderID + ", orderType=" + orderType;
                break;
            case AMEND_PRICE:
                ins = "Instruction [action=" + action + ", side=" + side + ", price=" + price +
                        ", orderID=" + orderID + ", orderType=" + orderType;
                break;
            case AMEND_QUANTITY:
                ins = "Instruction [action=" + action + ", side=" + side + ", price=" + price +
                        ", quantity=" + quantity + ", orderID=" + orderID + ", orderType=" + orderType;
                break;
        }


        return ins;
    }

}
