package com.jpmorgan.client;
import com.jpmorgan.ExchangeSimulator;
import com.jpmorgan.instruction.Instruction;
import com.jpmorgan.message.ExchangeMessage;
import com.jpmorgan.message.OrderMessage;
import com.jpmorgan.message.RegisterMessage;
import com.jpmorgan.message.TradeMessage;
import com.jpmorgan.orderbook.Side;

import java.util.*;

import java.io.*;



public class Client {

    private final String name;
    private long clientID;

    private Map<Long, ChildOrder> childOrderMap;

    private ExchangeSimulator exchangeSimulator;

    //private Queue<ExchangeMessage> messages = new LinkedList<>();

    private List<ExchangeMessage> messages = new ArrayList<>();

    private PrintWriter outWriter;

    private long instructionChildOrderId;

    public Client(String name)
    {
        this.name = name;
        childOrderMap = new HashMap<>();
    }

    public boolean hasChildOrder(long childOrderId) {
        return childOrderMap.containsKey(childOrderId);
    }

    public ChildOrder childOrder(long childOrderId) {
        ChildOrder result = null;
        if (hasChildOrder(childOrderId))
        {
            result = childOrderMap.get(childOrderId);
        }

        return result;
    }

    public int childOrderNumber() {
        return childOrderMap.size();
    }

    public List<ExchangeMessage> getMessages() {
        return messages;
    }

    public void setOutWriter(PrintWriter writer) {
        //outWriter = new PrintWriter(new PrintStream(new File("")));

        this.outWriter = writer;
    }

    public void printChildOrders() {

        outWriter.println(name + " Child orders:");

        if (childOrderMap.size() == 0) {
            outWriter.println("None");
            outWriter.println();
            outWriter.flush();
            return;
        }

        for (Map.Entry<Long, ChildOrder> entry : childOrderMap.entrySet()) {
            ChildOrder childOrder = entry.getValue();

            outWriter.println(childOrder);
        }

        outWriter.println();
        outWriter.flush();

    }

    public long getInstructionChildOrderId() {
        return this.instructionChildOrderId;
    }

    public long getClientID()
    {
        return this.clientID;
    }

    public void setClientID(long clientID)
    {
        this.clientID = clientID;
    }

    public void setExchangeSimulator(ExchangeSimulator exchangeSimulator) {
        this.exchangeSimulator = exchangeSimulator;
    }

    public void receiveExchangeMessage(ExchangeMessage message)
    {
        messages.add(message);
    }

    public void processExchangeMessage(ExchangeMessage message)
    {
        //TODO:

        if (message instanceof OrderMessage) {
            OrderMessage orderMessage = (OrderMessage)message;

            switch (orderMessage.getOrderState())
            {
                case CANCEL_ACKED: childOrderMap.remove(orderMessage.getOrderID()); break;
                case PLACE_ACKED:
                case AMEND_PRICE_ACKED:
                case AMEND_QUANTITY_ACKED:
                    ChildOrder childOrder = new ChildOrder(orderMessage.getOrderID(),
                            orderMessage.getPrice(), orderMessage.getSide(), orderMessage.getQuantity());
                    childOrderMap.put(childOrder.orderId, childOrder);
                    break;
                default: break;
            }

            instructionChildOrderId = orderMessage.getOrderID();

            outWriter.println(name + " Received message: " + orderMessage);
            outWriter.println();
            outWriter.flush();

        }
        else if (message instanceof TradeMessage) {

            TradeMessage tradeMessage = (TradeMessage)message;
            if (childOrderMap.containsKey(tradeMessage.getOrderId())) {

                ChildOrder childOrder = childOrderMap.get(tradeMessage.getOrderId());
                outWriter.println(name + " Received fill " + tradeMessage);
                outWriter.println();
                outWriter.flush();

                childOrder.quantity -= tradeMessage.getFillQuantity();
                if (childOrder.quantity <= 0) {
                    outWriter.println(name + " Order id " + childOrder.orderId + " got fully filled");
                    outWriter.println();
                    outWriter.flush();
                    childOrderMap.remove(childOrder.orderId);
                }

            }

        }
        else if (message instanceof RegisterMessage) {
            RegisterMessage registerMessage = (RegisterMessage)message;

            outWriter.println(name + " Received message: " + registerMessage);
            outWriter.println();
            outWriter.flush();
        }
    }

    public void processAllExchangeMessages()
    {
        messages.stream().forEach(o -> this.processExchangeMessage(o));
        messages.clear();
        printChildOrders();
    }

    public void sendInstructionToExchange(Instruction instruction)
    {
        outWriter.println();
        outWriter.println("------------------------------------------------------------------------------------------");
        outWriter.println(name + " sending instruction to exchange");
        outWriter.println(instruction);
        outWriter.println();

        //System.out.println("exchangeSimulator == null? " + (exchangeSimulator == null));
        exchangeSimulator.receiveInstruction(instruction, clientID);
    }


    private Side getSide(String side)
    {
        if (side == "Buy")
            return Side.BUY;
        else if (side == "Sell")
            return Side.SELL;
        else {
            System.err.println("Unrecognized side " + side);
            System.exit(1);
        }

        return null;

    }


    public void placeLimitOrder(double price, long quantity, String side)
    {
        Instruction instruction = Instruction.createPlaceLimitInstruction(price, quantity, getSide(side));
        sendInstructionToExchange(instruction);
    }

    public void placeMarketOrder(long quantity, String side)
    {
        Instruction instruction = Instruction.createPlaceMarketInstruction(quantity, getSide(side));
        sendInstructionToExchange(instruction);
    }

    public void cancelOrder(String side, long orderID)
    {
        Instruction instruction = Instruction.createCancelInstruction(getSide(side), orderID);
        sendInstructionToExchange(instruction);
    }

    public void amendOrderPrice(String side, double price, long orderID)
    {
        Instruction instruction = Instruction.createAmendPriceInstruction(getSide(side), price, orderID);
        sendInstructionToExchange(instruction);
    }

    public void amendOrderQuantity(String side, long quantity, long orderID)
    {
        Instruction instruction = Instruction.createAmendQuantityInstruction(getSide(side), quantity, orderID);
        sendInstructionToExchange(instruction);
    }



}
