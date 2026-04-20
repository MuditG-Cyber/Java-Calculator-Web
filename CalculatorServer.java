import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.net.InetSocketAddress;

public class CalculatorServer {
    public static void main(String[] args) throws IOException {
        // Read PORT from environment variable (required for Render/Railway hosting), fallback to 8080 for localhost
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null) ? Integer.parseInt(portEnv) : 8080;
        
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/calculate", new CalculateHandler());
        
        server.setExecutor(null); // creates a default executor
        System.out.println("=================================================");
        System.out.println("🚀 WEB CALCULATOR BACKEND STARTED!");
        System.out.println("👉 Open your browser and go to: http://localhost:" + port);
        System.out.println("=================================================");
        server.start();
    }
    
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String root = ".";
            String path = t.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            File file = new File(root + path).getCanonicalFile();
            if (!file.exists()) {
                String response = "404 (Not Found)\n";
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                // Add correct content types
                if (path.endsWith(".html")) {
                    t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                } else if (path.endsWith(".css")) {
                    t.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
                }
                
                t.sendResponseHeaders(200, file.length());
                OutputStream os = t.getResponseBody();
                Files.copy(file.toPath(), os);
                os.close();
            }
        }
    }

    static class CalculateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String requestBody = new String(is.readAllBytes());
                
                try {
                    // Extract values manually to avoid needing external JSON libraries (like Jackson/Gson)
                    // This makes the project extremely easy to run for a student
                    String num1Str = extractJsonValue(requestBody, "num1");
                    String num2Str = extractJsonValue(requestBody, "num2");
                    String op = extractJsonValue(requestBody, "operation");
                    
                    double num1 = Double.parseDouble(num1Str);
                    double num2 = Double.parseDouble(num2Str);
                    double result = 0;
                    String error = null;
                    
                    // The core logic (same as the terminal program)
                    switch (op) {
                        case "+": result = num1 + num2; break;
                        case "-": result = num1 - num2; break;
                        case "*": result = num1 * num2; break;
                        case "/": 
                            if (num2 == 0) error = "Cannot divide by zero!";
                            else result = num1 / num2; 
                            break;
                        default: error = "Invalid operation!";
                    }
                    
                    String jsonResponse;
                    if (error != null) {
                        jsonResponse = "{\"error\": \"" + error + "\"}";
                    } else {
                        jsonResponse = "{\"result\": " + result + "}";
                    }
                    
                    // Add slight artificial delay to show off the loading animation on frontend (Optional but looks premium)
                    Thread.sleep(300); 
                    
                    t.getResponseHeaders().set("Content-Type", "application/json");
                    t.sendResponseHeaders(200, jsonResponse.length());
                    OutputStream os = t.getResponseBody();
                    os.write(jsonResponse.getBytes());
                    os.close();
                } catch (Exception e) {
                    String response = "{\"error\": \"Invalid request\"}";
                    t.getResponseHeaders().set("Content-Type", "application/json");
                    t.sendResponseHeaders(400, response.length());
                    OutputStream os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            } else {
                t.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
        
        // Helper function to parse JSON simply
        private String extractJsonValue(String json, String key) {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return "";
            startIndex += searchKey.length();
            
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }
            
            if (json.charAt(startIndex) == '"') {
                startIndex++;
                int endIndex = json.indexOf('"', startIndex);
                return json.substring(startIndex, endIndex);
            } else {
                int endIndex = startIndex;
                while (endIndex < json.length() && (Character.isDigit(json.charAt(endIndex)) || json.charAt(endIndex) == '.' || json.charAt(endIndex) == '-')) {
                    endIndex++;
                }
                return json.substring(startIndex, endIndex);
            }
        }
    }
}
