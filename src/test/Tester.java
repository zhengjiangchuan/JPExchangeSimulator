package test;

import com.jpmorgan.*;
import com.jpmorgan.client.*;
import com.jpmorgan.message.*;
import com.jpmorgan.orderbook.*;
import org.junit.*;
import javafx.util.Pair;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class Tester {

    //Set up for testing
    private Pair<ExchangeSimulator, List<Client>> setUp() {
        PrintWriter stdOut = new PrintWriter(System.out);

        ExchangeSimulator exchangeSimulator = new ExchangeSimulator();

        exchangeSimulator.setPrevClose(10.0);
        exchangeSimulator.setLotSize(500);
        exchangeSimulator.setTickSize(0.5);

        exchangeSimulator.setOutWriter(stdOut);

        Client client1 = new Client("Haitong");
        client1.setOutWriter(stdOut);

        Client client2 = new Client("JPMorgan");
        client2.setOutWriter(stdOut);

        Client client3 = new Client("Instinet");
        client3.setOutWriter(stdOut);

        List<Client> clients = new ArrayList<>();
        clients.add(client1);
        clients.add(client2);
        clients.add(client3);

        clients.stream().forEach(o -> {
            o.setExchangeSimulator(exchangeSimulator);
            exchangeSimulator.registerClient(o);
        });

        clients.stream().forEach(Client::processAllExchangeMessages);

        return new Pair<>(exchangeSimulator, clients);
    }

    @Test
    public void testPostLimitOrderAcked()
    {
        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        //Client1 places a limit buy order 10000 shares at price 10.0
        client1.placeLimitOrder(10.0, 10000, "Buy");
        List<ExchangeMessage> messages = client1.getMessages();
        assertEquals(1, messages.size());

        ExchangeMessage message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        OrderMessage orderMessage = (OrderMessage)message;

        //This limit order is acked by the exchange
        assertEquals(OrderState.PLACE_ACKED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();
        ChildOrder childOrder1 = client1.childOrder(client1Order1);

        //Client1 has got a child order in the market
        assertEquals(10.0, childOrder1.getPrice(), 1e-5);
        assertEquals(Side.BUY, childOrder1.getSide());
        assertEquals(10000, childOrder1.getQuantity());


        OrderBook orderBook = exchangeSimulator.getOrderBook();

        System.out.println("orderBook1:");
        System.out.println(orderBook);

//        OrderBook:
//        Buy                     |    Sell
//
//        10000(client1)@10      |

        //Exchange book updated confirmed
        assertEquals(10.0, orderBook.getBestBid().getRawPrice(), 1e-5);
        assertEquals(10000, orderBook.getBestBidQuantity());
        assertEquals(0, orderBook.getBestAskQuantity());


        //Client2 places a limit buy order 2000 shares at price 9.5
        client2.placeLimitOrder(9.5, 2000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        //Client2 places a limit buy order 3000 shares at price 10
        client2.placeLimitOrder(10, 3000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order2 = client2.getInstructionChildOrderId();

        //Confirm Client2's child orders in the market
        ChildOrder childOrder2 = client2.childOrder(client2Order1);
        assertEquals(9.5, childOrder2.getPrice(), 1e-5);
        assertEquals(Side.BUY, childOrder2.getSide());
        assertEquals(2000, childOrder2.getQuantity());


        orderBook = exchangeSimulator.getOrderBook();

        System.out.println("orderBook2:");
        System.out.println(orderBook);

//        OrderBook:
//        Buy                                   |    Sell
//
//        10000(client1),3000(client2)@10      |
//                     2000@(client2)@9.5     |


        assertEquals(13000, orderBook.getBestBidQuantity());
        assertEquals(2, orderBook.getOrderNumAtPrice(orderBook.getBestBid().getRawPrice()));
        assertEquals(2000, orderBook.getQuantityAtPrice(9.5));


        //Client3 places a limit sell order 1000 shares at price 11.0
        client3.placeLimitOrder(11.0, 1000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        //Client3 places a limit sell order 4000 shares at price 11.5
        client3.placeLimitOrder(11.5, 4000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        //Confirm Client3's child orders in the market
        ChildOrder childOrder3 = client3.childOrder(client3Order2);
        assertEquals(11.5, childOrder3.getPrice(), 1e-5);
        assertEquals(Side.SELL, childOrder3.getSide());

        //Client3 has 2 child orders in the market
        assertEquals(2, client3.childOrderNumber());

        orderBook = exchangeSimulator.getOrderBook();

        System.out.println("orderBook3:");
        System.out.println(orderBook);

//        orderBook3:
//        OrderBook:
//        Buy                                   |    Sell
//
//        10000(client1),3000(client2)@10      |    1000(client3)@11
//                      2000@(client2)@9.5     |    4000(client3)@11.5

        assertEquals(1000, orderBook.getBestAskQuantity());
        assertEquals(4000, orderBook.getQuantityAtPrice(11.5));


    }


    @Test
    public void testCancelLimitOrderAcked() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        //Client1 places a limit buy order 10000 shares at price 10.0
        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        //Client1 as got a child order in the market
        assertTrue(client1.hasChildOrder(client1Order1));

        //Client2 places a limit buy order 20000 shares at price 10.0
        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client3 places a limit sell order 30000 shares at price 12.0
        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client3 places a limit sell order 20000 shares at price 11.0
        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        //Client1 places a limit buy order 30000 shares at price 9.5
        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client1 now has 2 child orders in the market
        assertEquals(2, client1.childOrderNumber());

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

        //Client1 cancels his first child order 10000@10
        client1.cancelOrder("Buy", client1Order1);
        List<ExchangeMessage> messages = client1.getMessages();
        assertEquals(1, messages.size());

        ExchangeMessage message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        OrderMessage orderMessage = (OrderMessage)message;

        //Client1's cancel child order request is acked by the exchange
        assertEquals(OrderState.CANCEL_ACKED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client1 now has only 1 child orders in the market
        assertEquals(1, client1.childOrderNumber());

        OrderBook orderBook = exchangeSimulator.getOrderBook();

        System.out.println("orderBook1:");
        System.out.println(orderBook);

//        OrderBook:
//        Buy                     |    Sell
//
//        20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5     |   30000(client3)@12

        //Best bid has only 20000 shares as client 1 has canceled one child order
        assertEquals(20000, orderBook.getBestBidQuantity());

        //Client 1's first child order is absent now
        assertTrue(!client1.hasChildOrder(client1Order1));


        //Client 3 has got 2 child orders
        assertEquals(2, client3.childOrderNumber());

        //Client 3 cancels his first child order
        client3.cancelOrder("Sell", client3Order1);

        messages = client3.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        orderMessage = (OrderMessage)message;

        //Client 3's cancel request is acked by the exchange
        assertEquals(OrderState.CANCEL_ACKED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client 3 now has only 1 child order in the market
        assertEquals(1, client3.childOrderNumber());

        orderBook = exchangeSimulator.getOrderBook();

        System.out.println("orderBook3:");
        System.out.println(orderBook);

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   30000(client3)@12
//        30000(client1)@9.5                     |

        //Because client 3 has canceled his child order at price 11, the best offer now becomes 12
        assertEquals(12.0, orderBook.getBestAsk().getRawPrice(), 1e-5);

    }


    @Test
    public void testCancelLimitOrderRejectedDueToFill() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12


        //Client 3 places a limit sell order of 10000 shares at best bid, triggering a fill
        client3.placeLimitOrder(10.0, 10000, "Sell");


        //A trade of 10000 shares happens
        Trade lastTrade = exchangeSimulator.getLastTrade();
        assertTrue(lastTrade != null);
        assertEquals(10000, lastTrade.getTotalQuantity());

        //Client 1's first child order of 10000@10 gets filled
        List<ExchangeMessage> messages = client1.getMessages();
        TradeMessage tradeMessage = (TradeMessage)messages.stream().filter(o -> ((TradeMessage)o).getSide() == Side.BUY).findFirst().orElse(null);
        assertTrue(tradeMessage != null);
        assertEquals(10000, tradeMessage.getFillQuantity());

        clients.stream().forEach(Client::processAllExchangeMessages);


//        OrderBook:
//        Buy                        |    Sell
//
//        20000(client2)@10          |   20000(client3)@11
//        30000(client1)@9.5         |   30000(client3)@12

        //Client 1 requests to cancel its child order of 10000@10
        client1.cancelOrder("Buy", client1Order1);
        messages = client1.getMessages();

        //Because this child order has got filled recently, it is no longer in the market and hence cannot be canceled and received a cancel reject message
        OrderMessage orderMessage = (OrderMessage)messages.stream().filter(o -> o instanceof OrderMessage).findFirst().orElse(null);
        assertTrue(orderMessage != null);
        assertEquals(OrderState.CANCEL_REJECTED, orderMessage.getOrderState());


        assertEquals(20000, orderBook.getBestBidQuantity());



    }



    @Test
    public void testAmendLimitOrderPriceAcked() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12


        //Confirm Client1's first child order is at price 10.0
        ChildOrder childOrder1 = client1.childOrder(client1Order1);
        assertEquals(10.0, childOrder1.getPrice(), 1e-5);


        //Client1 amends the price of its first child roder to 9.5
        client1.amendOrderPrice("Buy", 9.5, client1Order1);
        List<ExchangeMessage> messages = client1.getMessages();

        assertEquals(1, messages.size());

        ExchangeMessage message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        OrderMessage orderMessage = (OrderMessage)message;

        //This amend is acked by the exchange
        assertEquals(OrderState.AMEND_PRICE_ACKED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);


        System.out.println("Book 2:");
        System.out.println(orderBook);

//        OrderBook:
//        Buy                                    |    Sell
//
//                       20000(client2)@10      |   20000(client3)@11
//        30000(client1),10000(client1)@9.5     |   30000(client3)@12

        //Confirm Client1' first child order is not amended to price 9.5
        childOrder1 = client1.childOrder(client1Order1);
        assertEquals(9.5, childOrder1.getPrice(), 1e-5);

        orderBook = exchangeSimulator.getOrderBook();

        //Confirm best bid quantity reduces to 20000 due to Client1's amending price operation
        assertEquals(20000, orderBook.getBestBidQuantity());

        //Client2 amends its first child order to price 9.0
        client2.amendOrderPrice("Buy", 9, client2Order1);
        clients.stream().forEach(Client::processAllExchangeMessages);

//        OrderBook:
//        Buy                                    |    Sell
//
//        30000(client1),10000(client1)@9.5      |   20000(client3)@11
//                        20000(client2)@10      |   30000(client3)@12


        //As Client2 amends its child order from best bid 10 to 9, the best bid now becomes 9.5
        assertEquals(9.5, orderBook.getBestBid().getRawPrice(), 1e-5);

    }


    @Test
    public void testAmendLimitOrderPriceRejectedDueToFill() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12


        ChildOrder childOrder1 = client1.childOrder(client1Order1);
        assertEquals(10.0, childOrder1.getPrice(), 1e-5);


        //Client2 amends its child order at 10 to price 11.0, which is far_touch price, hence triggering a trade
        client2.amendOrderPrice("Buy", 11.0, client2Order1);
        clients.stream().forEach(Client::processAllExchangeMessages);


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1)@10                      |   30000(client3)@12
//        30000(client1)@9.5                     |

        //As Client3's child order at best ask price has got filled by Client2's cross order,
        //the best ask price now becomes 12, i.e., moving up
        assertEquals(12, orderBook.getBestAsk().getRawPrice(), 1e-5);


        //Client3 requests to amend its child order at 11 to price 11.5
        client3.amendOrderPrice("Sell", 11.5, client3Order1);

        List<ExchangeMessage> messages = client3.getMessages();

        assertEquals(1, messages.size());

        ExchangeMessage message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This amend request is rejected because that child order has already got filled and no longer in the market
        OrderMessage orderMessage = (OrderMessage)message;
        assertEquals(OrderState.AMEND_PRICE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);

    }


    @Test
    public void testAmendLimitOrderPriceTriggerTrade() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();


        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();

        System.out.println("Book here1");
        System.out.println(orderBook);

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12


        client1.amendOrderPrice("Buy", 9.5, client1Order1);
        clients.stream().forEach(Client::processAllExchangeMessages);


        client2.amendOrderPrice("Buy", 9, client2Order1);
        clients.stream().forEach(Client::processAllExchangeMessages);

//        OrderBook:
//        Buy                                    |    Sell
//
//        30000(client1), 10000(client1)@9.5      |   20000(client3)@11
//                         20000(client2)@19      |   30000(client3)@12


        //Client3 amends his child order at best ask 11 to best bid price, hence triggering a trade
        client3.amendOrderPrice("Sell", 9.5, client3Order1);

        //A trade of 20000 shares happens
        Trade lastTrade = exchangeSimulator.getLastTrade();
        assertTrue(lastTrade != null);
        assertEquals(20000, lastTrade.getTotalQuantity());

        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client1's child order of 30000 shares at best bid price 9.5 gets partial fill with Client3's sell order 20000 shares,
        //leaving 10000 shares in the book.
        ChildOrder childOrder = client1.childOrder(client1Order2);
        assertEquals(10000, childOrder.getQuantity());

        //As Client3 has amended its child order at best ask 11 to 9.5 getting a full fill,
        //the best ask now becomes 12.0.
        assertEquals(12.0, orderBook.getBestAsk().getRawPrice(), 1e-5);

    }



    @Test
    public void testAmendLimitOrderQuantityAcked() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        System.out.println("#################################");

        OrderBook orderBook = exchangeSimulator.getOrderBook();

        System.out.println("Book here1");
        System.out.println(orderBook);

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

        //Client3 amends its child order at price 12 down to 15000 shares
        client3.amendOrderQuantity("Sell", 15000, client3Order1);

        List<ExchangeMessage> messages = client3.getMessages();
        assertEquals(1, messages.size());

        ExchangeMessage message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //Amending quantity down is allowed, so Client3 receives an amend_quantity_acked message from exchange
        OrderMessage orderMessage = (OrderMessage)message;
        assertEquals(OrderState.AMEND_QUANTITY_ACKED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);

        //Confirm Client3's child order 1 now has quantity 15000 in the market
        ChildOrder childOrder = client3.childOrder(client3Order1);
        assertEquals(15000, childOrder.getQuantity());

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   15000(client3)@12

    }


    @Test
    public void testAmendLimitOrderQuantityRejected() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12


        //Client1 requests to amend its child order of 10000 shares at price 10 up to 20000
        client1.amendOrderQuantity("Buy", 20000, client1Order1);

        List<ExchangeMessage> messages = client1.getMessages();
        assertEquals(1, messages.size());

        ExchangeMessage message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //Amending quantity up is not allowed as newly joined quantity should have lowest queue priority,
        //Hence this amend request is rejected from the exchange
        OrderMessage orderMessage = (OrderMessage)message;
        assertEquals(OrderState.AMEND_QUANTITY_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

    }



    @Test
    public void testLimitOrderTradePartialFarTouch() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12


        //Client1 sends a limit buy order of 5000 shares at best_ask price 11.0, trigger a partial trade
        client1.placeLimitOrder(11.0, 5000, "Buy");

        //A trade of 5000 happens
        Trade lastTrade = exchangeSimulator.getLastTrade();
        assertTrue(lastTrade != null);
        assertEquals(5000, lastTrade.getTotalQuantity());


        List<TradeMessage> client1TradeMsg = client1.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY).collect(Collectors.toList());

        //Client1 receives a fill of its active buy order of 5000 shares at price 11.0
        assertEquals(1, client1TradeMsg.size());
        assertEquals(11.0, client1TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(5000, client1TradeMsg.get(0).getFillQuantity());


        List<TradeMessage> client3TradeMsg = client3.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.SELL).collect(Collectors.toList());

        //Client3 receives a fill of its resting(passive) sell order of 5000 shares at price 11.0
        assertEquals(1, client3TradeMsg.size());
        assertEquals(client3Order2, client3TradeMsg.get(0).getOrderId());
        assertEquals(11.0, client3TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(5000, client3TradeMsg.get(0).getFillQuantity());


        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client3's child order at 11 gets a partial of 5000 shares, leaving 15000 shares in the market
        assertTrue(client3.hasChildOrder(client3Order2));
        ChildOrder childOrder = client3.childOrder(client3Order2);
        assertEquals(15000, childOrder.getQuantity());

        //Best ask quantity now becomes 15000 shares due to the partial fill
        assertEquals(11.0, orderBook.getBestAsk().getRawPrice(), 1e-5);
        assertEquals(15000, orderBook.getBestAskQuantity());

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   15000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

    }



    @Test
    public void testLimitOrderTradeEntireFarTouch() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

        //Client1 places a limit buy order of 20000 shares at best ask, trigger a trade, filling the entire best ask
        client1.placeLimitOrder(11.0, 20000, "Buy");

        //A trade of 20000 shares happens in the market
        Trade lastTrade = exchangeSimulator.getLastTrade();
        assertTrue(lastTrade != null);
        assertEquals(20000, lastTrade.getTotalQuantity());


        List<TradeMessage> client1TradeMsg = client1.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY).collect(Collectors.toList());

        //Client1 receives a fill of its buy active order of 20000 shares at 11.0
        assertEquals(1, client1TradeMsg.size());
        assertEquals(11.0, client1TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(20000, client1TradeMsg.get(0).getFillQuantity());


        List<TradeMessage> client3TradeMsg = client3.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.SELL).collect(Collectors.toList());

        //Client3 receives a fill of its sell resting order of 20000 shares at 11.0
        assertEquals(1, client3TradeMsg.size());
        assertEquals(client3Order2, client3TradeMsg.get(0).getOrderId());
        assertEquals(11.0, client3TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(20000, client3TradeMsg.get(0).getFillQuantity());


        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client3's child order at 11.0 is gone due to the fill
        assertTrue(!client3.hasChildOrder(client3Order2));


        //The entire best ask layer is filled, so the new best ask becomes 12.0
        assertEquals(12.0, orderBook.getBestAsk().getRawPrice(), 1e-5);
        assertEquals(30000, orderBook.getBestAskQuantity());

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12
    }


    @Test
    public void testLimitOrderTradePartialFarTouchFillMultipleOrders() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12


        //Client3 amends its sell child order of 20000 shares at 11.0 to best bid price 10.0, trigger a trade
        client3.amendOrderPrice("Sell", 10.0, client3Order2);

        //A trade of 20000 shares happenes
        Trade lastTrade = exchangeSimulator.getLastTrade();
        assertTrue(lastTrade != null);
        assertEquals(20000, lastTrade.getTotalQuantity());


        List<TradeMessage> client3TradeMsg = client1.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.SELL).collect(Collectors.toList());

        //Client3's active order fills with 2 child orders at the best bid, 10000 shares from client1, and 10000 shares from client2,
        //So Client3 receives 2 fill messages, each indicates a fill quantity of 10000 shares
        assertEquals(2, client3TradeMsg.size());
        assertEquals(10.0, client3TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(10000, client3TradeMsg.get(0).getFillQuantity());
        assertEquals(10.0, client3TradeMsg.get(1).getPrice(), 1e-5);
        assertEquals(10000, client3TradeMsg.get(1).getFillQuantity());


        List<TradeMessage> client1TradeMsg = client1.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY && client1.hasChildOrder(o.getOrderId())).collect(Collectors.toList());


        //Client1's 10000 shares order at price 10 gets fully filled, receiving this fill message
        assertEquals(1, client1TradeMsg.size());
        assertEquals(client1Order1, client1TradeMsg.get(0).getOrderId());
        assertEquals(10.0, client1TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(10000, client1TradeMsg.get(0).getFillQuantity());


        //Client2's 20000 shares order at price 10 gets partial fill of 10000 shares, receiving this fill message
        List<TradeMessage> client2TradeMsg = client2.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY && client2.hasChildOrder(o.getOrderId())).collect(Collectors.toList());

        assertEquals(1, client2TradeMsg.size());
        assertEquals(client2Order1, client2TradeMsg.get(0).getOrderId());
        assertEquals(10.0, client2TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(10000, client2TradeMsg.get(0).getFillQuantity());



        clients.stream().forEach(Client::processAllExchangeMessages);


        //Client3's this order at 11 is gone as he amends its price to best bid getting fully filled
        assertTrue(!client3.hasChildOrder(client3Order2));

        //Client1's order at 10 of 10000 shares gets fully filled, hence gone
        assertTrue(!client1.hasChildOrder(client1Order1));

        //Client2's order of 20000 shares at 10 is still there, but only has 10000 shares left as 10000 shares gets filled
        assertTrue(client2.hasChildOrder(client2Order1));
        ChildOrder childOrder = client2.childOrder(client2Order1);
        assertEquals(10000, childOrder.getQuantity());

        //Confirm the best bid/ask price and quantity have changed correctly due to this trade
        assertEquals(12.0, orderBook.getBestAsk().getRawPrice(), 1e-5);
        assertEquals(30000, orderBook.getBestAskQuantity());

        assertEquals(10.0, orderBook.getBestBid().getRawPrice(), 1e-5);
        assertEquals(10000, orderBook.getBestBidQuantity());

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client2)@10                      |   30000(client3)@12
//        30000(client1)@9.5                     |

    }




    @Test
    public void testLimitOrderTradeEntireFarTouchCreateNewNearTouch() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

        //Client 3 places a sell limit order of 40000 shares at best bid price, trigger a trade
        client3.placeLimitOrder(10.0, 40000, "Sell");

        //As best bid only as 30000 shares in total, so only 30000 shares of trade happens
        Trade lastTrade = exchangeSimulator.getLastTrade();
        assertTrue(lastTrade != null);
        assertEquals(30000, lastTrade.getTotalQuantity());


        List<TradeMessage> client3TradeMsg = client1.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.SELL).collect(Collectors.toList());

        //Client3's active sell order gets filled with 2 child orders at best bid, one of 10000 shares, the other of 20000 shares
        assertEquals(2, client3TradeMsg.size());
        assertEquals(10.0, client3TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(10000, client3TradeMsg.get(0).getFillQuantity());
        assertEquals(10.0, client3TradeMsg.get(1).getPrice(), 1e-5);
        assertEquals(20000, client3TradeMsg.get(1).getFillQuantity());


        List<TradeMessage> client1TradeMsg = client1.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY && client1.hasChildOrder(o.getOrderId())).collect(Collectors.toList());

        //Client1's resting order of 10000 shares at 10 gets fully filled
        assertEquals(1, client1TradeMsg.size());
        assertEquals(client1Order1, client1TradeMsg.get(0).getOrderId());
        assertEquals(10.0, client1TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(10000, client1TradeMsg.get(0).getFillQuantity());



        List<TradeMessage> client2TradeMsg = client2.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY && client2.hasChildOrder(o.getOrderId())).collect(Collectors.toList());

        //Client2's resting order of 20000 shares at 10 gets fully filled
        assertEquals(1, client2TradeMsg.size());
        assertEquals(client2Order1, client2TradeMsg.get(0).getOrderId());
        assertEquals(10.0, client2TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(20000, client2TradeMsg.get(0).getFillQuantity());



        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order3 = client3.getInstructionChildOrderId();

        //Client3's active sell order only gets partial fill as it is 10000 shares more than the entire best bid size, so the remaining
        //10000 shares resides at price 10.0 creating a new best offer of 10.0
        assertTrue(client3.hasChildOrder(client3Order3));
        ChildOrder childOrder = client3.childOrder(client3Order3);
        assertEquals(10000, childOrder.getQuantity());

        //Client1's order at 10 is gone due to fill
        assertTrue(!client1.hasChildOrder(client1Order1));

        //Client2's order at 10 is gone due to fill
        assertTrue(!client2.hasChildOrder(client2Order1));

        //The best offer becomes 10.0 due to Client3's active sell order not filled completely hence creating a lower best offer
        assertEquals(10.0, orderBook.getBestAsk().getRawPrice(), 1e-5);
        assertEquals(10000, orderBook.getBestAskQuantity());

        assertEquals(9.5, orderBook.getBestBid().getRawPrice(), 1e-5);
        assertEquals(30000, orderBook.getBestBidQuantity());

//        OrderBook:
//        Buy                    |    Sell
//
//        30000(client1)@9.5     |   10000(client3)@10
//                               |   20000(client3)@11
//                               |   30000(client3)@12

    }


    @Test
    public void testLimitOrderTradeMultipleLayersFillMultipleOrders() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

        //Client 3 places a limit sell order of 50000 shares at price 9.5, triggering a trade
        client3.placeLimitOrder(9.5, 50000, "Sell");

        //A trade of 50000 shares happens as total shares at price no lower than 9.5 in bid book has more than 50000 shares
        //to fill with this active sell order
        Trade lastTrade = exchangeSimulator.getLastTrade();
        assertTrue(lastTrade != null);
        assertEquals(50000, lastTrade.getTotalQuantity());



        List<TradeMessage> client3TradeMsg = client3.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.SELL).collect(Collectors.toList());

        //Client3's active sell order of 50000 shares targets at limit price 9.5, which is lower than the best bid,
        //so it will first trade with the 2 orders at best bid 10, one of 10000 shares and the other of 20000 shares,
        //then the remaining 20000 shares trades with the order of 30000 shares at 9.5, partially filling this passive order,
        //leaving 10000 shares at price 9.5.
        assertEquals(3, client3TradeMsg.size());
        assertEquals(10.0, client3TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(10000, client3TradeMsg.get(0).getFillQuantity());
        assertEquals(10.0, client3TradeMsg.get(1).getPrice(), 1e-5);
        assertEquals(20000, client3TradeMsg.get(1).getFillQuantity());
        assertEquals(9.5, client3TradeMsg.get(2).getPrice(), 1e-5);
        assertEquals(20000, client3TradeMsg.get(2).getFillQuantity());


        List<TradeMessage> client1TradeMsg = client1.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY && client1.hasChildOrder(o.getOrderId())).collect(Collectors.toList());

        //Client1's order of 10000 shares at price 10 gets fully filled with this active sell order,
        //and its order of 30000 shares at price 9.5 gets partial fill of 20000 shares.
        assertEquals(2, client1TradeMsg.size());
        assertEquals(client1Order1, client1TradeMsg.get(0).getOrderId());
        assertEquals(10.0, client1TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(10000, client1TradeMsg.get(0).getFillQuantity());
        assertEquals(client1Order2, client1TradeMsg.get(1).getOrderId());
        assertEquals(9.5, client1TradeMsg.get(1).getPrice(), 1e-5);
        assertEquals(20000, client1TradeMsg.get(1).getFillQuantity());



        List<TradeMessage> client2TradeMsg = client2.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY && client2.hasChildOrder(o.getOrderId())).collect(Collectors.toList());

        //Client2's order of 20000 shares at price 10 gets fully filled with this active sell order
        assertEquals(1, client2TradeMsg.size());
        assertEquals(client2Order1, client2TradeMsg.get(0).getOrderId());
        assertEquals(10.0, client2TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(20000, client2TradeMsg.get(0).getFillQuantity());



        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client1's order at price 10 is gone due to full fill
        assertTrue(!client1.hasChildOrder(client1Order1));

        //Client2's order at price 10 is gone due to full fill
        assertTrue(!client2.hasChildOrder(client2Order1));

        //Client1's order at price 9.5 is still there due to partial fill, and only 10000 shares is left
        assertTrue(client1.hasChildOrder(client1Order2));
        ChildOrder childOrder = client1.childOrder(client1Order2);
        assertEquals(10000, childOrder.getQuantity());


        //Confirm the best bid/ask price and quantity and updated to the correct values
        assertEquals(11.0, orderBook.getBestAsk().getRawPrice(), 1e-5);
        assertEquals(20000, orderBook.getBestAskQuantity());

        assertEquals(9.5, orderBook.getBestBid().getRawPrice(), 1e-5);
        assertEquals(10000, orderBook.getBestBidQuantity());

//        OrderBook:
//        Buy                     |    Sell
//
//        10000(client1)@9.5      |   20000(client3)@11
//                                |   30000(client3)@12

    }

    @Test
    public void testMarketOrderTradeMultipleLayersFillMultipleOrders() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

        //Client 3 places market order of 50000 shares, triggering a trade
        client3.placeMarketOrder(50000, "Sell");

        //A trade of 50000 shares happens as total shares in bid book has more than 50000 shares
        //to fill with this active sell order
        Trade lastTrade = exchangeSimulator.getLastTrade();
        assertTrue(lastTrade != null);
        assertEquals(50000, lastTrade.getTotalQuantity());



        List<TradeMessage> client3TradeMsg = client3.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.SELL).collect(Collectors.toList());

        //Client3's active sell order of 50000 shares targets at limit price 9.5, which is lower than the best bid,
        //so it will first trade with the 2 orders at best bid 10, one of 10000 shares and the other of 20000 shares,
        //then the remaining 20000 shares trades with the order of 30000 shares at 9.5, partially filling this passive order,
        //leaving 10000 shares at price 9.5.
        assertEquals(3, client3TradeMsg.size());
        assertEquals(10.0, client3TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(10000, client3TradeMsg.get(0).getFillQuantity());
        assertEquals(10.0, client3TradeMsg.get(1).getPrice(), 1e-5);
        assertEquals(20000, client3TradeMsg.get(1).getFillQuantity());
        assertEquals(9.5, client3TradeMsg.get(2).getPrice(), 1e-5);
        assertEquals(20000, client3TradeMsg.get(2).getFillQuantity());


        List<TradeMessage> client1TradeMsg = client1.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY && client1.hasChildOrder(o.getOrderId())).collect(Collectors.toList());

        //Client1's order of 10000 shares at price 10 gets fully filled with this active sell order,
        //and its order of 30000 shares at price 9.5 gets partial fill of 20000 shares.
        assertEquals(2, client1TradeMsg.size());
        assertEquals(client1Order1, client1TradeMsg.get(0).getOrderId());
        assertEquals(10.0, client1TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(10000, client1TradeMsg.get(0).getFillQuantity());
        assertEquals(client1Order2, client1TradeMsg.get(1).getOrderId());
        assertEquals(9.5, client1TradeMsg.get(1).getPrice(), 1e-5);
        assertEquals(20000, client1TradeMsg.get(1).getFillQuantity());



        List<TradeMessage> client2TradeMsg = client2.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY && client2.hasChildOrder(o.getOrderId())).collect(Collectors.toList());

        //Client2's order of 20000 shares at price 10 gets fully filled with this active sell order
        assertEquals(1, client2TradeMsg.size());
        assertEquals(client2Order1, client2TradeMsg.get(0).getOrderId());
        assertEquals(10.0, client2TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(20000, client2TradeMsg.get(0).getFillQuantity());



        clients.stream().forEach(Client::processAllExchangeMessages);

        //Client1's order at price 10 is gone due to full fill
        assertTrue(!client1.hasChildOrder(client1Order1));

        //Client2's order at price 10 is gone due to full fill
        assertTrue(!client2.hasChildOrder(client2Order1));

        //Client1's order at price 9.5 is still there due to partial fill, and only 10000 shares is left
        assertTrue(client1.hasChildOrder(client1Order2));
        ChildOrder childOrder = client1.childOrder(client1Order2);
        assertEquals(10000, childOrder.getQuantity());


        //Confirm the best bid/ask price and quantity and updated to the correct values
        assertEquals(11.0, orderBook.getBestAsk().getRawPrice(), 1e-5);
        assertEquals(20000, orderBook.getBestAskQuantity());

        assertEquals(9.5, orderBook.getBestBid().getRawPrice(), 1e-5);
        assertEquals(10000, orderBook.getBestBidQuantity());

//        OrderBook:
//        Buy                     |    Sell
//
//        10000(client1)@9.5      |   20000(client3)@11
//                                |   30000(client3)@12

    }




    @Test
    public void testMarketOrderTradeEntireOppostieBookCreateLimitUp() { //Almost the same as previous one

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();

//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

        //Client2 places a market buy order of 60000 shares
        client2.placeMarketOrder(60000, "Buy");


        //Because this active buy order of 60000 shares exceeds the total quantity 50000 placed in the sell order book,
        //only 50000 shares of trade happens.
        Trade lastTrade = exchangeSimulator.getLastTrade();
        assertTrue(lastTrade != null);
        assertEquals(50000, lastTrade.getTotalQuantity());


        List<TradeMessage> client2TradeMsg = client2.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.BUY).collect(Collectors.toList());

        //Client2's active buy order trades all orders placed in the sell order book, receiving 2 fills at different prices
        assertEquals(2, client2TradeMsg.size());
        assertEquals(11.0, client2TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(20000, client2TradeMsg.get(0).getFillQuantity());
        assertEquals(12.0, client2TradeMsg.get(1).getPrice(), 1e-5);
        assertEquals(30000, client2TradeMsg.get(1).getFillQuantity());



        List<TradeMessage> client3TradeMsg = client3.getMessages().stream().filter(o -> o instanceof TradeMessage)
                .map(o -> (TradeMessage)o).filter(o -> o.getSide() == Side.SELL && client3.hasChildOrder(o.getOrderId())).collect(Collectors.toList());

        //Client3's resting sell orders in the book all get filled with this active buy order, receiving several fills
        assertEquals(2, client3TradeMsg.size());
        assertEquals(client3Order2, client3TradeMsg.get(0).getOrderId());
        assertEquals(11.0, client3TradeMsg.get(0).getPrice(), 1e-5);
        assertEquals(20000, client3TradeMsg.get(0).getFillQuantity());
        assertEquals(client3Order1, client3TradeMsg.get(1).getOrderId());
        assertEquals(12.0, client3TradeMsg.get(1).getPrice(), 1e-5);
        assertEquals(30000, client3TradeMsg.get(1).getFillQuantity());



        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order2 = client2.getInstructionChildOrderId();

        //Client3's child orders are all gone due to the fill with this active buy order
        assertTrue(!client3.hasChildOrder(client3Order1));
        assertTrue(!client3.hasChildOrder(client3Order2));

        //Client2's active buy order gets partial fill of 50000 shares, leavining 10000 shares.
        //Because this is a market buy order which does not have a limit price, the remaining 10000 shares is placed
        //at high limit price defined by the exchange, which is usually some percentage above the previous close price.
        assertTrue(client2.hasChildOrder(client2Order2));
        ChildOrder childOrder = client2.childOrder(client2Order2);
        assertEquals(10000, childOrder.getQuantity());
        assertEquals(orderBook.getHighLimit().getRawPrice(), childOrder.getPrice(), 1e-5);

        //As Client2's huge active buy order eats the entire sell book and has remaining shares,
        //it pushes the market to the high limit of the exchange hance now it becomes limit up.
        assertTrue(orderBook.getBestAsk() == null);
        assertEquals(0, orderBook.getBestAskQuantity());

        assertEquals(orderBook.getHighLimit().getRawPrice(), orderBook.getBestBid().getRawPrice(), 1e-5);
        assertEquals(10000, orderBook.getBestBidQuantity());

        assertTrue(orderBook.isLimitUp());

//        OrderBook:
//        Buy                                    |    Sell (empty book)
//
//        10000(client2)@15(exchange high limit price)
//        10000(client1), 20000(client2)@10      |
//        30000(client1)@9.5                     |

    }





    @Test
    public void testPlaceOrAmendOrderPriceRejectedDueToOutOfLimit() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

        //Client 3 places a limit sell order of 50000 shares at price 15.5
        client3.placeLimitOrder(15.5, 50000, "Sell");

        List<ExchangeMessage> messages = client3.getMessages();
        assertEquals(1, messages.size());

        ExchangeMessage message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because 15.5 is higher than exchange defined high limit of the day
        OrderMessage orderMessage = (OrderMessage)message;
        assertEquals(OrderState.PLACE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);


        //Client 3 amends one of its sell orders to price 15.5
        client3.amendOrderPrice("Sell", 15.5, client3Order2);

        messages = client3.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because 15.5 is higher than exchange defined high limit of the day
        orderMessage = (OrderMessage)message;
        assertEquals(OrderState.AMEND_PRICE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);




        //Client 1 places a limit buy order of 50000 shares at price 4.5
        client1.placeLimitOrder(4.5, 50000, "Buy");

        messages = client1.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because 4.5 is lower than exchange defined low limit of the day
        orderMessage = (OrderMessage)message;
        assertEquals(OrderState.PLACE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);


        //Client 1 amends one of its buy orders to price 4.5
        client1.amendOrderPrice("Buy", 4.5, client1Order1);

        messages = client1.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because 4.5 is lower than exchange defined low limit of the day
        orderMessage = (OrderMessage)message;
        assertEquals(OrderState.AMEND_PRICE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);





    }


    @Test
    public void testPlaceOrAmendOrderPriceRejectedDueToPriceInvalid() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12

        //Client 3 places a limit sell order of 50000 shares at price 15.5
        client3.placeLimitOrder(12.4, 50000, "Sell");

        List<ExchangeMessage> messages = client3.getMessages();
        assertEquals(1, messages.size());

        ExchangeMessage message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because 15.5 is higher than exchange defined high limit of the day
        OrderMessage orderMessage = (OrderMessage)message;
        assertEquals(OrderState.PLACE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);


        //Client 3 amends one of its sell orders to price 15.5
        client3.amendOrderPrice("Sell", 12.4, client3Order2);

        messages = client3.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because 15.5 is higher than exchange defined high limit of the day
        orderMessage = (OrderMessage)message;
        assertEquals(OrderState.AMEND_PRICE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);




        //Client 1 places a limit buy order of 50000 shares at price 4.5
        client1.placeLimitOrder(8.3, 50000, "Buy");

        messages = client1.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because 4.5 is lower than exchange defined low limit of the day
        orderMessage = (OrderMessage)message;
        assertEquals(OrderState.PLACE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);


        //Client 1 amends one of its buy orders to price 4.5
        client1.amendOrderPrice("Buy", 8.3, client1Order1);

        messages = client1.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because 4.5 is lower than exchange defined low limit of the day
        orderMessage = (OrderMessage)message;
        assertEquals(OrderState.AMEND_PRICE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);





    }




    @Test
    public void testPlaceOrAmendOrderQuantityRejectedDueToOddLotConstraint() {

        Pair<ExchangeSimulator, List<Client>> init = setUp();
        ExchangeSimulator exchangeSimulator = init.getKey();
        List<Client> clients = init.getValue();

        Client client1 = clients.get(0);
        Client client2 = clients.get(1);
        Client client3 = clients.get(2);

        client1.placeLimitOrder(10.0, 10000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

        assertTrue(client1.hasChildOrder(client1Order1));

        client2.placeLimitOrder(10.0, 20000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

        client3.placeLimitOrder(12.0, 30000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client3.getInstructionChildOrderId();

        client3.placeLimitOrder(11.0, 20000, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client3.getInstructionChildOrderId();

        client1.placeLimitOrder(9.5, 30000, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();


        OrderBook orderBook = exchangeSimulator.getOrderBook();


//        OrderBook:
//        Buy                                    |    Sell
//
//        10000(client1), 20000(client2)@10      |   20000(client3)@11
//        30000(client1)@9.5                     |   30000(client3)@12



        //Client 1 places a limit buy order of 5600 shares at price 10.5
        client1.placeLimitOrder(10.5, 5600, "Buy");

        List<ExchangeMessage> messages = client1.getMessages();
        assertEquals(1, messages.size());

        ExchangeMessage message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because cannot buy shares with odd lot (lot size is 500)
        OrderMessage orderMessage = (OrderMessage)message;
        assertEquals(OrderState.PLACE_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);


        //Client 1 amends one of its buy orders to quantity 5600
        client1.amendOrderQuantity("Buy", 5600, client1Order1);

        messages = client1.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //This is rejected because cannot buy shares with odd lot (lot size is 500)
        orderMessage = (OrderMessage)message;
        assertEquals(OrderState.AMEND_QUANTITY_REJECTED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);







        //Client 3 places a limit sell order of 5600 shares at price 13
        client3.placeLimitOrder(13, 5600, "Sell");

        messages = client3.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //Although this quantity includes odd lot, it is accepted because no-oddlot restriction only applies to buy order
        orderMessage = (OrderMessage)message;
        assertEquals(OrderState.PLACE_ACKED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);


        //Client 3 amends one of its sell orders to quantity 5700
        client3.amendOrderQuantity("Sell", 5700, client3Order2);

        messages = client3.getMessages();
        assertEquals(1, messages.size());

        message = messages.get(0);
        assertTrue(message instanceof  OrderMessage);

        //Although this quantity includes odd lot, it is accepted because no-oddlot restriction only applies to buy order
        orderMessage = (OrderMessage)message;
        assertEquals(OrderState.AMEND_QUANTITY_ACKED, orderMessage.getOrderState());

        clients.stream().forEach(Client::processAllExchangeMessages);


        //Quantity amending accepted and hence the best ask quantity reduces to 5700
        assertEquals(5700, orderBook.getBestAskQuantity());



    }






}
