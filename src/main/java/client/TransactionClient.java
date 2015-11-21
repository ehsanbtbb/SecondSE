package client;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by ehsan on 11/19/15.
 */
public class TransactionClient {

    private static HashMap<Integer, String> serverResponse = new HashMap<Integer, String>();

    public static void readFile(String InputFileName) {
        try {
            File inputFile = new File(InputFileName);
            DocumentBuilderFactory dbFactory
                    = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("terminal");
            Element eElement = (Element) nList.item(0);

            String terminalID = eElement.getAttribute("id");
            String terminalType = eElement.getAttribute("type");

            NodeList nServer = eElement.getElementsByTagName("server");
            Element eServer = (Element) nServer.item(0);

            String ipAddress = eServer.getAttribute("ip");
            Integer serverPort = Integer.parseInt(eServer.getAttribute("port"));

            NodeList nOutLog = eElement.getElementsByTagName("outLog");
            Element eOutLog = (Element) nOutLog.item(0);
            String logFilePath = eOutLog.getAttribute("path");
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(logFilePath, true)));

            Socket clientToServerSocket = new Socket(ipAddress, serverPort);
            requestTransactions(terminalID, terminalType, eElement, clientToServerSocket, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void requestTransactions(String terminalID, String terminalType, Element eElement, Socket s, PrintWriter writer) {
        DataOutputStream os = null;
        DataInputStream is = null;
        try {
            os = new DataOutputStream(s.getOutputStream());
            is = new DataInputStream(s.getInputStream());
            os.writeBytes(terminalID + "\n");
            os.writeBytes(terminalType + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        NodeList nTransactions = eElement.getElementsByTagName("transaction");
        for (int i = 0; i < nTransactions.getLength(); i++) {
            Element eTransaction = (Element) nTransactions.item(i);
            String transactionID = eTransaction.getAttribute("id");
            String transactionType = eTransaction.getAttribute("type");
            String transactionAmount = eTransaction.getAttribute("amount");
            transactionAmount = transactionAmount.replace(",", "");
            String transactionDeposit = eTransaction.getAttribute("deposit");
            try {
                os.writeBytes(transactionID + "\n");
                os.writeBytes(transactionType + "\n");
                os.writeBytes(transactionAmount + "\n");
                os.writeBytes(transactionDeposit + "\n");
                String response = is.readLine();
                serverResponse.put(Integer.parseInt(transactionID), response);
                serverResponseLog(writer, transactionID, response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        createXMLResponse();
        writer.close();
    }

    public static void createXMLResponse() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("serverResponse");
            doc.appendChild(rootElement);

            Set<Integer> keys = serverResponse.keySet();
            for (Integer k : keys) {
                Element transaction = doc.createElement("transaction");
                rootElement.appendChild(transaction);

                Element transactionID = doc.createElement("id");
                transactionID.appendChild(doc.createTextNode(k.toString()));
                transaction.appendChild(transactionID);

                Element transactionResponse = doc.createElement("serverResponse");
                transactionResponse.appendChild(doc.createTextNode(serverResponse.get(k)));
                transaction.appendChild(transactionResponse);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("response.xml"));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);

            System.out.println("File saved!");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    public static void serverResponseLog(PrintWriter writer, String transactionID, String response) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        writer.println(dateFormat.format(date));
        writer.println("Request for transaction " + transactionID + " sent...");
        writer.println("Here is server's response: ");
        writer.println(response);
        writer.println();
    }

    public static void main(String[] args) {
        readFile("terminal.xml");
    }
}
