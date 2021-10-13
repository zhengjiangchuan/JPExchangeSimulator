package com.jpmorgan.instruction;
import java.util.*;

import com.jpmorgan.ExchangeSimulator;
import com.jpmorgan.message.ExchangeMessage;
import com.jpmorgan.message.OrderMessage;
import com.jpmorgan.message.TradeMessage;
import com.jpmorgan.orderbook.*;
import javafx.util.Pair;

public abstract class InstructionProcessor {

    protected final OrderBook orderBook;

    protected final ExchangeSimulator exchange;

    public InstructionProcessor(OrderBook orderBook, ExchangeSimulator exchange)
    {
        this.orderBook = orderBook;
        this.exchange = exchange;
    }

    public static InstructionProcessor createPlaceInstructionProcessor(OrderBook orderBook, ExchangeSimulator exchange) {
        return new PlaceInstructionProcessor(orderBook, exchange);
    }

    public static InstructionProcessor createCancelInstructionProcessor(OrderBook orderBook, ExchangeSimulator exchange) {
        return new CancelInstructionProcessor(orderBook, exchange);
    }

    public static InstructionProcessor createAmendPriceInstructionProcessor(OrderBook orderBook, ExchangeSimulator exchange) {
        return new AmendPriceInstructionProcessor(orderBook, exchange);
    }

    public static InstructionProcessor createAmendQuantityInstructionProcessor(OrderBook orderBook, ExchangeSimulator exchange) {
        return new AmendQuantityInstructionProcessor(orderBook, exchange);
    }


    public abstract List<ExchangeMessage> processInstruction(Instruction instruction);

}


class PlaceInstructionProcessor extends InstructionProcessor {

    public PlaceInstructionProcessor(OrderBook orderBook, ExchangeSimulator exchange)
    {
        super(orderBook, exchange);
    }

    public List<ExchangeMessage> processInstruction(Instruction instruction) {

        List<ExchangeMessage> messages = new ArrayList<>();


        Order order = null;

        if (instruction.getOrderType() == OrderType.LIMIT)
        {
            order = new Order(new Price(instruction.getPrice()), instruction.getSide(), instruction.getQuantity());
        }
        else if (instruction.getOrderType() == OrderType.MARKET)
        {
            order = new Order(null, instruction.getSide(), instruction.getQuantity());
        }

        if (order != null)
        {
            double price = 0.0;
            if (order.getPrice() != null)
            {
                price = order.getPrice().getRawPrice();
            }
            else {
                if (order.isBid())
                {
                    price = orderBook.getHighLimit().getRawPrice();
                }
                else
                {
                    price = orderBook.getLowLimit().getRawPrice();
                }
            }


            String rejectString = orderBook.checkPlaceOrder(order);
            if (rejectString == null)
            {
                messages.add(new OrderMessage(OrderState.PLACE_ACKED, order.getOrderId(),
                        order.getRawSide(), price, order.getQuantity(), "Order successfully placed"));


                Trade trade = orderBook.processInboundOrder(order);

                //if (order.getQuantity() > 0) //This is the quantity of the sent order after filling with opposite orders if any
                //{
                //}

                if (trade != null) {

                    this.exchange.setLastTrade(trade);

                    List<Order> tradedOrders = trade.getTradedOrders();
                    for (Order filledOrder : tradedOrders)
                    {
                        messages.add(new TradeMessage(filledOrder.getOrderId(), filledOrder.getPrice().getRawPrice(),
                                filledOrder.getQuantity(), filledOrder.getRawSide()));
                    }

                    Order tradingOrder = trade.getTradingOrder();
                    for (Order filledOrder : tradedOrders)
                    {
                        messages.add(new TradeMessage(tradingOrder.getOrderId(), filledOrder.getPrice().getRawPrice(),
                                filledOrder.getQuantity(), tradingOrder.getRawSide()));
                    }

                }
            }
            else {
                messages.add(new OrderMessage(OrderState.PLACE_REJECTED, order.getOrderId(),
                        order.getRawSide(), price, order.getQuantity(), rejectString));
            }



        }


        return messages;
    }

}



class CancelInstructionProcessor extends InstructionProcessor {

    public CancelInstructionProcessor(OrderBook orderBook, ExchangeSimulator exchange)
    {
        super(orderBook, exchange);
    }

    public List<ExchangeMessage> processInstruction(Instruction instruction) {

        List<ExchangeMessage> messages = new ArrayList<>();

        Pair<Order, String> result = orderBook.removeOrder(instruction.getOrderID(), instruction.getSide());

        Order removedOrder = result.getKey();
        String rejectString = result.getValue();

        if (rejectString == null)
        {
            //OrderMessage(OrderState state, long orderID, Side side, double price, long quantity, String reason)
            messages.add(new OrderMessage(OrderState.CANCEL_ACKED, removedOrder.getOrderId(),
                    removedOrder.getRawSide(), removedOrder.getPrice().getRawPrice(),
                    removedOrder.getQuantity(),  rejectString));
        }
        else {
            messages.add(new OrderMessage(OrderState.CANCEL_REJECTED, instruction.getOrderID(), null, Double.NaN, -1L,
                    rejectString));
        }

        return messages;
    }
}

class AmendPriceInstructionProcessor extends InstructionProcessor {

    public AmendPriceInstructionProcessor(OrderBook orderBook, ExchangeSimulator exchange)
    {
        super(orderBook, exchange);
    }

    public List<ExchangeMessage> processInstruction(Instruction instruction) {

        List<ExchangeMessage> messages = new ArrayList<>();

        String rejectString = orderBook.checkPlaceOrderPrice(new Price(instruction.getPrice()));

        if (rejectString == null)
        {
            UpdateResult result = orderBook.updateOrderPrice(instruction.getOrderID(), instruction.getSide(), new Price(instruction.getPrice()));


            Order updatedOrder = result.getOrder();
            rejectString = result.getRejectString();
            Trade trade = result.getTrade();

            if (rejectString == null)
            {

                //OrderMessage(OrderState state, long orderID, Side side, double price, long quantity, String reason)
                messages.add(new OrderMessage(OrderState.AMEND_PRICE_ACKED, updatedOrder.getOrderId(),
                        updatedOrder.getRawSide(), updatedOrder.getPrice().getRawPrice(),
                        updatedOrder.getQuantity(),  rejectString));

                if (trade != null) {

                    this.exchange.setLastTrade(trade);

                    List<Order> tradedOrders = trade.getTradedOrders();
                    for (Order filledOrder : tradedOrders)
                    {
                        messages.add(new TradeMessage(filledOrder.getOrderId(), filledOrder.getPrice().getRawPrice(),
                                filledOrder.getQuantity(), filledOrder.getRawSide()));
                    }

                    Order tradingOrder = trade.getTradingOrder();
                    for (Order filledOrder : tradedOrders)
                    {
                        messages.add(new TradeMessage(tradingOrder.getOrderId(), filledOrder.getPrice().getRawPrice(),
                                filledOrder.getQuantity(), tradingOrder.getRawSide()));
                    }

                }

            }
        }

        if (rejectString != null)
        {
            messages.add(new OrderMessage(OrderState.AMEND_PRICE_REJECTED, instruction.getOrderID(),
                    null, Double.NaN, -1L,
                    rejectString));
        }

        return messages;
    }
}

class AmendQuantityInstructionProcessor extends InstructionProcessor {

    public AmendQuantityInstructionProcessor(OrderBook orderBook, ExchangeSimulator exchange)
    {
        super(orderBook, exchange);
    }

    public List<ExchangeMessage> processInstruction(Instruction instruction) {

        List<ExchangeMessage> messages = new ArrayList<>();

        String rejectString = orderBook.checkPlaceOrderSize(instruction.getSide(), instruction.getQuantity());


        if (rejectString == null)
        {
            Pair<Order, String> result = orderBook.updateOrderQuantity(instruction.getOrderID(), instruction.getSide(), instruction.getQuantity());

            Order updatedOrder = result.getKey();
            rejectString = result.getValue();

            if (rejectString == null)
            {
                messages.add(new OrderMessage(OrderState.AMEND_QUANTITY_ACKED, updatedOrder.getOrderId(),
                        updatedOrder.getRawSide(), updatedOrder.getPrice().getRawPrice(),
                        updatedOrder.getQuantity(),  rejectString));
            }
        }

        if (rejectString != null)
        {
            messages.add(new OrderMessage(OrderState.AMEND_QUANTITY_REJECTED, instruction.getOrderID(),
                    null, Double.NaN, -1L,
                    rejectString));
        }

        return messages;
    }
}




