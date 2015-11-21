package transaction;

import deposit.Deposit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Created by ehsan on 11/18/15.
 *
 */
public class TransactionServer {

    private static ArrayList<Deposit> allDeposits = new ArrayList<Deposit>();
    private static Integer runningPort = null;
    private static String logFileName = null;
    private static final String filePath = "core.json";

    public void DataParser() {
        try {
            FileReader reader = new FileReader(filePath);
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            runningPort = ((Long) jsonObject.get("port")).intValue();
            logFileName = (String) jsonObject.get("outLog");
            JSONArray deposits = (JSONArray) jsonObject.get("deposits");
            Iterator i = deposits.iterator();
            while (i.hasNext()) {
                JSONObject innerObj = (JSONObject) i.next();
                Deposit currentDeposit = new Deposit((String) innerObj.get("customer"), (String) innerObj.get("id"),
                        Long.parseLong(((String) innerObj.get("initialBalance")).replace(",", "")),
                        Long.parseLong(((String) innerObj.get("upperBound")).replace(",", "")));
                allDeposits.add(currentDeposit);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Integer getRunningPort() {
        return runningPort;
    }

    public ArrayList<Deposit> getAllDeposits() {
        return allDeposits;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public static String transact(Integer transactionID, String type, Long amount, String depositID) {
        if (type.equals("deposit")) {
            for (Deposit d : allDeposits) {
                if (d.getID().equals(depositID)) {
                    if (d.deposit(amount)) {
                        return "Transaction " + transactionID.toString() + ": successfully done...";
                    }
                    return "Transaction " + transactionID.toString() + ": Error occurred... (Exceeds upper bound)";
                }
            }
        }
        else if (type.equals("withdraw")) {
            for (Deposit d : allDeposits) {
                if (d.getID().equals(depositID)) {
                    if (d.withdraw(amount)) {
                        return "Transaction " + transactionID.toString() + ": Successfully done...";
                    }
                    return "Transaction " + transactionID.toString() + ": Error occurred... (Not enough money)";
                }
            }
        }
        return "Transaction " + transactionID.toString() + ": Error occurred... (depositID not available)";
    }

    public static void main(String[] args) {
        TransactionServer ts = new TransactionServer();
        ts.DataParser();

        CommandProcessor cp = new CommandProcessor(ts);
        cp.start();
        try {
            ServerSocket listener = new ServerSocket(ts.getRunningPort());
            try {
                while (true) {
                    new Server(listener.accept()).start();
                }
            } finally {
                listener.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Deposit d : allDeposits) {
            System.out.println(d);
        }
    }

    private static class Server extends Thread {
        private Socket socket;

        public Server(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                String currentLine, type, depositID;
                Integer transactionID;
                Long amount;
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, true)));
                String terminalID, terminalType;
                terminalID = in.readLine();
                terminalType = in.readLine();
                while (true) {
                    currentLine = in.readLine();
                    if (currentLine != null) {
                        String log;
                        transactionID = Integer.parseInt(currentLine);
                        type = in.readLine();
                        amount = Long.parseLong(in.readLine());
                        depositID = in.readLine();
                        out.println(log = transact(transactionID, type, amount, depositID));
                        logTransaction(writer, terminalID, terminalType, transactionID, type, amount, depositID, log);
                    }
                    else {
                        writer.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void logTransaction(PrintWriter writer, String terminalID, String terminalType, Integer transactionID,
                                   String type, Long amount, String depositID, String log) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            writer.println(dateFormat.format(date));
            writer.println("Terminal id: " + terminalID);
            writer.println("Terminal type: " + terminalType);
            writer.println("Request received...");
            writer.println("Transaction " + transactionID + ": ");
            writer.println("Type: " + type);
            writer.println("Amount: " + amount);
            writer.println("DepositID: " + depositID);
            writer.println("Request processed...");
            writer.println(log);
            writer.println();
        }
    }

    private static class CommandProcessor extends Thread {
        private static TransactionServer ts;

        public CommandProcessor(TransactionServer ts) {
            this.ts = ts;
        }

        public void run() {
            while (true) {
                Scanner reader = new Scanner(System.in);
                String command = reader.next();
                if (command != null) {
                    if (command.equals("sync")) {
                        updateJSONFile();
                    } else {
                        System.out.println("Unknown command...");
                    }
                }
            }
        }
        public static void updateJSONFile() {
            JSONObject obj = new JSONObject();
            obj.put("port", ts.getRunningPort());
            ArrayList<Deposit> allDeposits = ts.getAllDeposits();
            JSONArray list = new JSONArray();
            for (Deposit d: allDeposits) {
                String customer = d.getOwnersName();
                String id = d.getID();
                String initialBalance = d.getInitialBalance().toString();
                String upperBound = d.getUpperBound().toString();
                JSONObject currentObj = new JSONObject();
                currentObj.put("customer", customer);
                currentObj.put("id", id);
                currentObj.put("initialBalance", initialBalance);
                currentObj.put("upperBound", upperBound);
                list.add(currentObj);
            }
            obj.put("deposits", list);
            obj.put("outLog", ts.getLogFileName());

            try {

                FileWriter file = new FileWriter("core.json");
                file.write(obj.toJSONString());
                file.flush();
                file.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
