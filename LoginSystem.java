package ConsoleApp;

import User.User;
import javax.swing.*;
import java.io.*;
import java.util.*;

public class LoginSystem {
    private static final String FILE_PATH = System.getProperty("user.home") + "/users.txt";
    private static final String MESSAGES_FILE = System.getProperty("user.home") + "/messages.txt";
    private final Map<String, User> users = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private User currentUser;

    public LoginSystem() {
        loadUsers();
    }

    public void registerUser() {
        System.out.println("\nYapYap Registration");
        
        System.out.print("First Name: ");
        String firstName = scanner.nextLine();
        
        System.out.print("Last Name: ");
        String lastName = scanner.nextLine();
        
        System.out.print("Username (must contain '_' and be ≤5 chars): ");
        String username = scanner.nextLine();
        
        if (!username.contains("_") || username.length() > 5) {
            showError("Username must contain '_' and be ≤5 characters");
            return;
        }
        
        System.out.print("Password (min 8 chars, with capital, number, special char): ");
        String password = scanner.nextLine();
        
        if (!password.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$")) {
            showError("Password must contain 8+ chars, a capital, number, and special char");
            return;
        }
        
        System.out.print("South African Cellphone (+27xxxxxxxxx): ");
        String cellphone = scanner.nextLine();
        
        if (!cellphone.matches("^\\+27\\d{9}$")) {
            showError("Cellphone must be +27 followed by 9 digits");
            return;
        }
        
        User user = new User(firstName, lastName, username, password, cellphone);
        users.put(username, user);
        saveUsers();
        showSuccess("Registration successful!");
    }

    public void loginUser() {
        System.out.println("\nYapYap Login");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        
        System.out.print("Password: ");
        String password = scanner.nextLine();
        
        if (authenticate(username, password)) {
            currentUser = users.get(username);
            showSuccess("Welcome back, " + currentUser.getFirstName() + "!");
            showMessagingMenu();
        } else {
            showError("Invalid username or password");
        }
    }

    private boolean authenticate(String username, String password) {
        return users.containsKey(username) && 
               users.get(username).getPassword().equals(password);
    }

    private void showMessagingMenu() {
        while (true) {
            System.out.println("\n=== QuickChat ===");
            System.out.println("1. Send Messages");
            System.out.println("2. View Message History");
            System.out.println("3. Logout");
            System.out.print("Select: ");
            
            switch (scanner.nextLine()) {
                case "1" -> sendMessages();
                case "2" -> viewMessageHistory();
                case "3" -> { return; }
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private void sendMessages() {
        try {
            System.out.print("Number of messages to send: ");
            int count = Integer.parseInt(scanner.nextLine());
            
            for (int i = 0; i < count; i++) {
                System.out.println("\nMessage #" + (i+1));
                processMessage(new Message(
                    prompt("Recipient phone (+27xxx or 0xxx): ", this::validatePhone),
                    prompt("Message (max 250 chars): ", s -> s.length() <= 250)
                ));
            }
            
            System.out.println("Total sent: " + Message.getTotalSent());
        } catch (NumberFormatException e) {
            showError("Please enter a valid number");
        }
    }

    private String prompt(String message, java.util.function.Predicate<String> validator) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();
            if (validator.test(input)) return input;
            showError("Invalid input");
        }
    }

    private boolean validatePhone(String phone) {
        return phone.replaceAll("[^0-9]", "").matches("(27|0)\\d{9}");
    }

    private void processMessage(Message msg) {
        String result = switch (JOptionPane.showOptionDialog(
            null, "Message options", "Choose", 
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, 
            null, new String[]{"Send", "Store", "Discard"}, "Send"
        )) {
            case 0 -> { saveMessage(msg); yield "Sent: " + msg.getDetails(); }
            case 1 -> "Stored: " + msg.getDetails();
            case 2 -> "Discarded";
            default -> "Cancelled";
        };
        
        System.out.println(result);
    }

    private void saveMessage(Message msg) {
        try (PrintWriter out = new PrintWriter(new FileWriter(MESSAGES_FILE, true))) {
            out.printf("From: %s%nTime: %s%n%s%n-----%n",
                currentUser.getUsername(),
                new Date(),
                msg.getDetails()
            );
        } catch (IOException e) {
            showError("Error saving message");
        }
    }

    private void viewMessageHistory() {
        if (!new File(MESSAGES_FILE).exists()) {
            System.out.println("No messages yet");
            return;
        }
        
        try (Scanner fileScanner = new Scanner(new File(MESSAGES_FILE))) {
            fileScanner.useDelimiter("-----").tokens()
                .forEach(System.out::println);
        } catch (IOException e) {
            showError("Error reading messages");
        }
    }

    private void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            reader.lines()
                .map(User::fromString)
                .filter(Objects::nonNull)
                .forEach(user -> users.put(user.getUsername(), user));
        } catch (IOException e) {
            // Ignore if file doesn't exist
        }
    }

    private void saveUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            users.values().forEach(user -> {
                try { writer.write(user + "\n"); } 
                catch (IOException e) { showError("Error saving user"); }
            });
        } catch (IOException e) {
            showError("Error saving users");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(null, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        new LoginSystem();
    }

    private static class Message {
        private static int totalSent = 0;
        private final String id;
        private final String recipient;
        private final String content;
        private final String hash;

        public Message(String recipient, String content) {
            this.id = String.format("%010d", new Random().nextInt(1_000_000_000));
            this.recipient = recipient;
            this.content = content;
            this.hash = generateHash();
            totalSent++;
        }

        private String generateHash() {
            String[] words = content.split("\\s+");
            String first = words.length > 0 ? words[0] : "";
            String last = words.length > 1 ? words[words.length-1] : first;
            return id.substring(0, 2) + ":" + totalSent + ":" + first.toUpperCase() + last.toUpperCase();
        }

        public String getDetails() {
            return String.format(
                "ID: %s%nHash: %s%nTo: %s%nContent: %s",
                id, hash, recipient, content
            );
        }

        public static int getTotalSent() {
            return totalSent;
        }
    }
    
}