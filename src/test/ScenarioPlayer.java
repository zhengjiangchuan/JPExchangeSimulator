package test;

import com.jpmorgan.*;
import com.jpmorgan.client.Client;

import java.io.*;
import java.util.*;

public class ScenarioPlayer {


    public static void main(String[] args) throws Exception {

        boolean printToConsole = true;


        String outDir = "C:/JPMorgan/out/";
        new File(outDir).mkdirs();

        String exchangeFile = outDir + "exchange.txt";


        PrintWriter stdOut = new PrintWriter(System.out);
        File file = new File(exchangeFile);
        file.createNewFile();


        PrintWriter exchangeWriter = new PrintWriter(new PrintStream(new File(exchangeFile)));
        ExchangeSimulator exchangeSimulator = new ExchangeSimulator();

        exchangeSimulator.setPrevClose(10.0);
        //exchangeSimulator.setLotSize(500);
        exchangeSimulator.setTickSize(0.5);

        if (printToConsole)
            exchangeSimulator.setOutWriter(stdOut);
        else
            exchangeSimulator.setOutWriter(exchangeWriter);


        Client client1 = new Client("Haitong");


        if (printToConsole)
            client1.setOutWriter(stdOut);
        else {
            File client1File = new File(outDir + "/Haitong.txt");
            client1File.createNewFile();
            client1.setOutWriter(new PrintWriter(new PrintStream(client1File)));
        }

        client1.setExchangeSimulator(exchangeSimulator);



        Client client2 = new Client("JPMorgan");

        if (printToConsole)
            client2.setOutWriter(stdOut);
        else {
            File client2File = new File(outDir + "/JPMorgan.txt");
            client2File.createNewFile();
            client1.setOutWriter(new PrintWriter(new PrintStream(client2File)));
        }


        client2.setExchangeSimulator(exchangeSimulator);



        Client client3 = new Client("Instinet");

        if (printToConsole)
            client3.setOutWriter(stdOut);
        else {
            File client3File = new File(outDir + "/Instinet.txt");
            client3File.createNewFile();
            client1.setOutWriter(new PrintWriter(new PrintStream(client3File)));
        }


        client3.setExchangeSimulator(exchangeSimulator);


        List<Client> clients = new ArrayList<>();
        clients.add(client1);
        clients.add(client2);
        clients.add(client3);


        exchangeSimulator.registerClient(client1);
        exchangeSimulator.registerClient(client2);
        exchangeSimulator.registerClient(client3);

        clients.stream().forEach(Client::processAllExchangeMessages);


        //public void placeLimitOrder(double price, long quantity, Side side)
        //public void placeMarketOrder(long quantity, Side side)
        //public void cancelOrder(Side side, long orderID)
        //public void amendOrderPrice(String side, double price, long orderID)
        //public void amendOrderQuantity(String side, long quantity, long orderID)

        client1.placeLimitOrder(10.0, 100, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order1 = client1.getInstructionChildOrderId();

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//        100@10      |



        client2.placeLimitOrder(9.5, 20, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order1 = client2.getInstructionChildOrderId();

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//        100@10      |
//         20@9.5



        client1.cancelOrder("Buy", client1Order1);
        clients.stream().forEach(Client::processAllExchangeMessages);

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         20@9.5





        client1.placeLimitOrder(9.0, 20, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        client1Order1 = client1.getInstructionChildOrderId();

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         20@9.5
//         20@9.0

        client2.amendOrderQuantity("Buy", 12, client2Order1);
        clients.stream().forEach(Client::processAllExchangeMessages);

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         12@9.5
//         20@9.0




        client1.placeLimitOrder(10.0, 50, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order2 = client1.getInstructionChildOrderId();

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         50@10.0
//         12@9.5
//         20@9.0


        client3.placeLimitOrder(11.0, 100, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order1 = client1.getInstructionChildOrderId();

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         50@10.0       100@11.0
//         12@9.5
//         20@9.0

        client3.placeLimitOrder(12.0, 100, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order2 = client1.getInstructionChildOrderId();

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         50@10.0       100@11.0
//         12@9.5        100@12.0
//         20@9.0

        client2.placeLimitOrder(10, 60, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order2 = client2.getInstructionChildOrderId();

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         110@10.0       100@11.0
//         12@9.5        100@12.0
//         20@9.0





        client3.placeLimitOrder(10.0, 80, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         30@10.0       100@11.0
//         12@9.5        100@12.0
//         20@9.0



        client1.placeLimitOrder(11.0, 110, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client1Order3 = client1.getInstructionChildOrderId();

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         10@11.0      100@12.0
//         30@10.0
//         12@9.5
//         20@9.0



        client1.cancelOrder("Buy", client1Order3);
        clients.stream().forEach(Client::processAllExchangeMessages);

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         30@10.0       100@12.0
//         12@9.5
//         20@9.0




        client3.placeLimitOrder(12.0, 100, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order3 = client3.getInstructionChildOrderId();

        System.out.println("client3Order3 = " + client3Order3);

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         30@10.0       200@12.0
//         12@9.5
//         20@9.0

        client3.amendOrderPrice("Sell", 11.0, client3Order3);
        clients.stream().forEach(Client::processAllExchangeMessages);

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         30@10.0       100@11.0
//         12@9.5        100@12.0
//         20@9.0





        client1.placeLimitOrder(12.0, 150, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         30@10.0       50@12.0
//         12@9.5
//         20@9.0



        client3.placeLimitOrder(9.5, 100, "Sell");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client3Order4 = client1.getInstructionChildOrderId();

//        Order: side=BUY quantity=50 price=10
//        OrderBook:
//        Buy         |    Sell
//
//         20@9.0       58@9.5
//                      50@12.0
//


        client2.placeLimitOrder(12.0, 40, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order3 = client2.getInstructionChildOrderId();



//        OrderBook:
//        Buy         |    Sell
//
//        20@9       |      18@9.5
//                  |      50@12


        client2.placeMarketOrder(12, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order4 = client2.getInstructionChildOrderId();

//        OrderBook:
//        Buy         |    Sell
//
//        20@9       |       6@9.5
//                |      50@12


        client2.placeMarketOrder(100, "Buy");
        clients.stream().forEach(Client::processAllExchangeMessages);
        long client2Order5 = client2.getInstructionChildOrderId();

        System.exit(0);









    }

}
