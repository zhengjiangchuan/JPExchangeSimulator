package com.jpmorgan;
import com.jpmorgan.client.Client;
import com.jpmorgan.client.ClientState;
import com.jpmorgan.instruction.Instruction;
import com.jpmorgan.instruction.InstructionProcessor;
import com.jpmorgan.message.ExchangeMessage;
import com.jpmorgan.message.OrderMessage;
import com.jpmorgan.message.RegisterMessage;
import com.jpmorgan.message.TradeMessage;
import com.jpmorgan.orderbook.OrderBook;
import com.jpmorgan.orderbook.Trade;

import java.util.*;
import java.io.*;

public class ExchangeSimulator {

    private long globalClientID = 0;

    private OrderBook orderBook;
    private Trade lastTrade;

    private Map<Long, Client> clientMap;

    private PrintWriter outWriter;

    private final InstructionProcessor placeInstructionProcessor;
    private final InstructionProcessor cancelInstructionProcessor;
    private final InstructionProcessor amendPriceInstructionProcessor;
    private final InstructionProcessor amendQuantityInstructionProcessor;

    public ExchangeSimulator()
    {
        orderBook = new OrderBook();

        clientMap = new HashMap<>();

        placeInstructionProcessor = InstructionProcessor.createPlaceInstructionProcessor(orderBook, this);
        cancelInstructionProcessor = InstructionProcessor.createCancelInstructionProcessor(orderBook, this);
        amendPriceInstructionProcessor = InstructionProcessor.createAmendPriceInstructionProcessor(orderBook, this);
        amendQuantityInstructionProcessor = InstructionProcessor.createAmendQuantityInstructionProcessor(orderBook, this);
    }

    public void setPrevClose(double prevClose) {
        orderBook.setPrevClose(prevClose);
    }

    public void setLotSize(long lotSize) {
        orderBook.setLotSize(lotSize);
    }

    public void setTickSize(double tickSize) { orderBook.setTickSize(tickSize); }

    public void setOutWriter(PrintWriter outWriter) {
        this.outWriter = outWriter;
    }

    public void setLastTrade(Trade trade) {
        this.lastTrade = trade;
    }

    public Trade getLastTrade() {
        return this.lastTrade;
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }

    public void registerClient(Client client)
    {
        globalClientID++;
        client.setClientID(globalClientID);
        clientMap.put(globalClientID, client);

        sendMessageToClient(new RegisterMessage(ClientState.REGISTER_ACKED, globalClientID, "Register successfully"), client);

    }

    public void unregisterClient(Client client)
    {
        client.setClientID(-1L);
        if (clientMap.containsKey(client.getClientID()))
        {
            clientMap.remove(client.getClientID());
            sendMessageToClient(new RegisterMessage(ClientState.UNREGISTER_ACKED, client.getClientID(), "Unregister successfully"), client);
        }
        else
        {
            sendMessageToClient(new RegisterMessage(ClientState.UNREGISTER_REJECTED, client.getClientID(), "Client does not exist"), client);
        }
    }


    public void receiveInstruction(Instruction instruction, long clientID)
    {
        if (!clientMap.containsKey(clientID))
        {
            String errorMessage = "Client " + clientID + " does not exist, discard this instruction " + instruction;

            System.err.println(errorMessage);

            this.outWriter.println(errorMessage);

            return;
        }

        this.outWriter.println("Exchange Received instruction from Client " + clientID);
        this.outWriter.println(instruction);
        this.outWriter.println();

        this.outWriter.flush();


        Client client = clientMap.get(clientID);

        List<ExchangeMessage> messages = null;

        switch (instruction.getAction())
        {
            case PLACE_ORDER: messages = placeInstructionProcessor.processInstruction(instruction); break;
            case CANCEL: messages = cancelInstructionProcessor.processInstruction(instruction); break;
            case AMEND_PRICE: messages = amendPriceInstructionProcessor.processInstruction(instruction); break;
            case AMEND_QUANTITY: messages = amendQuantityInstructionProcessor.processInstruction(instruction); break;
            default: messages = null;
        }


        for (ExchangeMessage message : messages)
        {
            if (message instanceof OrderMessage)
            {
                sendMessageToClient(message, client);
            }
            else if (message instanceof TradeMessage)
            {
                broadcast(message);
            }
        }


        if (this.lastTrade != null)
        {
            this.outWriter.println("Exchange last trade: ");
            this.outWriter.println(lastTrade);
            this.outWriter.println();
            this.outWriter.flush();

            //this.lastTrade = null;

        }

        this.outWriter.println("Exchange book update: ");
        this.outWriter.println(this.orderBook);
        this.outWriter.println();
        this.outWriter.flush();

    }

    public void sendMessageToClient(ExchangeMessage message, Client client)
    {
        client.receiveExchangeMessage(message);
    }

    public void broadcast(ExchangeMessage message)
    {
        for (Map.Entry<Long, Client> entry : clientMap.entrySet()) {

            Client client = entry.getValue();
            client.receiveExchangeMessage(message);
        }
    }



}
