package tp_socket;

import java.net.*;
import java.io.*;
import java.util.*;

class ForumServer {

    ServerSocket sock;
    int port = 7777;
    final static String Id = "Chatterbox";		// Chat server name
    ArrayList clients;							// Users on line (each user is represented by a ChatManager object)
    Map<String, Integer> map;
    /*	***********	*/
    /*	CONSTRUCTOR	*/
    /*	***********	*/

    ForumServer() {
        clients = new ArrayList();
        map = new HashMap<String, Integer>();
        try {
            sock = new ServerSocket(port);
            System.out.println("Server is running on " + port);
        } catch (IOException e) {
            System.err.println("Launching error !!!");
            System.exit(0);
        }
    }

    /*	***************	*/
    /*	CLIENT MANAGER	*/
    /*	***************	*/
    class ChatManager extends Thread {

        Socket sockC;							// Client socket
        BufferedReader reader;					// Reader on client socket
        PrintWriter writer;						// Writer on client socket
        String clientIP;						// Client machine
        String clientName;

        ChatManager(Socket sk, String ip) {
            sockC = sk;
            clientIP = ip;
            try {
                reader = new BufferedReader(new InputStreamReader(sk.getInputStream()));
                writer = new PrintWriter(sk.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("IO error !!! on server");
            }
        }

        public void send(String mess) {			// Send the given message to the client
            writer.println(mess);
        }

        public void broadcast(String mess) {	// Send the given message to all the connected users
            synchronized (clients) {
                for (int i = 0; i < clients.size(); i++) {
                    ChatManager gct = (ChatManager) clients.get(i);
                    if (gct != null) {
                        gct.send(mess);
                    }
                }
            }
        }

        /// CHECKER SI LE MESSAGE EST VIDE OU PAS
        
        /**
         * Send a message to a specific user
         * @param mess 
         */
        public void sendTo(String mess) {	// Send the given message to all the connected users

            String arr[] = mess.trim().split(" ", 2);
            String recipient = arr[0];
            String message = arr[1];

            synchronized (clients) {
                for (int i = 0; i < clients.size(); i++) {
                    ChatManager gct = (ChatManager) clients.get(i);
                    if (gct != null && gct.clientName.equals(recipient)) {
                        gct.send(clientName + "> " + message);
                        send(clientName + "> " + message);
                        return;
                    }
                }
            }
            send(recipient + " n\'existe pas ...");
        }

        /**
         * This methods displays all connected users
         */
        public void displayAllConnected() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n----- Users currently connected -----\n\n");
            synchronized (clients) {
                for(int i = 0; i < clients.size(); i++) {
                    ChatManager gct = (ChatManager) clients.get(i);
                    if (gct != null) {
                        sb.append(gct.clientName).append("\n");
                    }
                }
                send(sb.toString());
            }
        }
        
        /**
         * This methods allows to disconnect a client 
         * @param name 
         */
        public void deleteChatManager(String name) {
            StringBuilder sb = new StringBuilder();
            synchronized (clients) {
                for(int i = 0; i < clients.size(); i++) {
                    ChatManager gct = (ChatManager) clients.get(i);
                    if(gct.clientName.equals(name)){
                        broadcast("All> " + clientName + " left the chat room");
                        clients.remove(i);
                    }
                }
            }
        }
        
        /**
         * Display help for user
         */
        public void displayHelp() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n----- HELP -----\n\n")
                    .append("! : broadcast message headed by sender's name\n"
                            + "@ : send message to the user identified by name\n"
                            + "? : rename the user with this new name and let all know\n"
                            + "& : display help on communication codes\n"
                            + "% : display the names of all the users that are currently connected");
            send(sb.toString());
        }

        /**
         * GETTER Client Name
         * @return 
         */
        public String getClientName() {
            return clientName;
        }

        /**
         * SETTER Client name
         * @param clientName 
         */
        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        /**
         * This method checks if login already exists
         * @param map : list of username + number of occurs
         * @param st : current login
         */
        public void checkIfLoginExists(Map<String, Integer> map, String st) {
            String newLogin;
            String currentLogin = st.substring(2).trim();
            if (currentLogin.length() < 2) {
                currentLogin = "unknown";
            }
            int count = map.containsKey(currentLogin) ? map.get(currentLogin) : 0;

            if (map.containsKey(currentLogin)) {
                newLogin = currentLogin.concat(Integer.toString(count + 1));
                map.put(newLogin, 1);
                send("> Welcome " + newLogin);
                this.setClientName(newLogin);
            } else {
                send("> Welcome " + currentLogin);
                this.setClientName(currentLogin);
            }
            map.put(currentLogin, count + 1);
        }

        public void deletePseudo(String name) {
            map.remove(name);
        }

        public void run() {						// Regular activity (as a thread): treat the command received from the client
            String st;
            try {
                while ((st = reader.readLine()) != null) {
                    switch (st.charAt(0)) {
                        case '?':
                            checkIfLoginExists(map, st);
                            send(clientName + " > te voilà parmi nous");
                            break;
                        case '!':
                            broadcast("All> " + st.substring(2));
                            break;
                        case '@':
                            sendTo(st.substring(2));
                            break;
                        case '&':
                            displayHelp();
                            break;
                        case '%':
                            displayAllConnected();
                            break;
                        case '+':
                            send("Goodbye ! Take care buddy");
                            deleteChatManager(clientName);
                            //send("Goodbye ! Take care buddy");
                            break;

                        default:
                            send("> I don't understand '" + st + "'");
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        public void close() {
            if (sockC == null) {
                return;
            }
            try {
                sockC.close();
                sockC = null;
            } catch (IOException e) {
                System.err.println("Connection closing error with " + clientIP);
            }
        }
    }

    /*	***********	*/
    /*	LAUNCHING	*/
    /*	***********	*/
    public static void main(String[] arg) {
        ForumServer me = new ForumServer();
        me.process();
    }

    public void process() {
        try {
            while (true) {
                Socket userSock = sock.accept();
                String userName = userSock.getInetAddress().getHostName();
                ChatManager user = new ChatManager(userSock, userName);
                synchronized (clients) {
                    clients.add(user);
                    user.start();
                    user.send(userName + ": client " + clients.size() + " on line");
                }
            }
        } catch (IOException e) {
            System.err.println("Server error !!!");
            System.exit(0);
        }
    }
}
