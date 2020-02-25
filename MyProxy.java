package proxytest;
import java.net.*; 
import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MyProxy {
    final static int BUFFER_SIZE = 262144;
    final static String ERROR_PAGE = "HTTP/1.1 301 Moved Permanently\nContent-Length: 145\nLocation: http://zebroid.ida.liu.se/error1.html\nConnection: keep-alive\nContent-Type: text/html";
    final static String[] blackList = {}; 
    
    public static void main(String[] args) {
        start_client_proxy();
    }

    public static void start_client_proxy() {
        try {
                ServerSocket proxy = new ServerSocket(8080);

                while(true) {
                        Socket clientSocket = proxy.accept();
                        proxy_server(clientSocket);
                }
        } catch (IOException e) {
                System.err.println(e);
        }
    }

    public static void proxy_server(Socket clientSocket) {
        new Thread(new Runnable() {
                public void run() {
                        try {
                                byte[] requestBuffer = new byte[BUFFER_SIZE];
                                
                                InputStream clientInputStream = clientSocket.getInputStream();
                                clientInputStream.read(requestBuffer);
                                String clientRequest = new String(requestBuffer);
                                String firstLine = clientRequest.split("\n")[0];
                                System.out.println(clientRequest);

                                if (matchesBlacklist(firstLine)) {
                                        String redirect = getErrorPage();
                                        System.out.println(new String(redirect.getBytes()));

                                        OutputStream clientOutputStream = clientSocket.getOutputStream();
                                        clientOutputStream.write(redirect.getBytes()); 
                                        clientSocket.close();
                                        return;  
                                }

                                
                                String host = firstLine.split(" ")[1];
                                String port = "80";
                                
                                if (host.startsWith("http://")) {
                                        if (host.split(":").length > 2) {
                                                port = host.split(":")[2].split("/")[0];
                                        }
                                        host = host.split(":")[1].split("//")[1].split("/")[0];
                                } else {
                                        if (host.split(":").length > 1) {
                                                port = host.split(":")[1].split("/")[0];
                                        }
                                        host = host.split(":")[0];
                                }

                                Socket serverSocket = new Socket(host, Integer.parseInt(port));
                                OutputStream serverOutputStream = serverSocket.getOutputStream();
                                serverOutputStream.write(requestBuffer);

                                byte[] responseBuffer = new byte[BUFFER_SIZE];

                                InputStream serverInputStream = serverSocket.getInputStream();
                                serverInputStream.read(responseBuffer);
                                String serverResponse = new String(responseBuffer);

                                System.out.print(serverResponse);

                                OutputStream clientOutputStream = clientSocket.getOutputStream();
                                clientOutputStream.write(responseBuffer);

                                clientOutputStream.close();
                                serverOutputStream.close();
                                serverSocket.close();
                        } catch (Exception e) {
                                System.err.println(e);
                        }
                }
        }).start();
    }

    public static boolean matchesBlacklist(String request) {
        for (String host: blackList) {
                if (request.contains(host)) return true;
        }
        return false;
    }

    public static String getErrorPage() {
        DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        Date date = new Date();
        String dateText = dateFormat.format(date) + " GMT\n";
        dateText = dateText.replace(".", "");
        return ERROR_PAGE + "\nDate: " + dateText;
    }
}