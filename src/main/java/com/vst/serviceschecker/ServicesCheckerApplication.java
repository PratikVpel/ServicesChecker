package com.vst.serviceschecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@SpringBootApplication
public class ServicesCheckerApplication {
    // Email configurations
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String EMAIL_USERNAME = "alerts.virtuoso@gmail.com";
    private static final String EMAIL_PASSWORD = "rgddqskfyqdnqcqp";
    private static final String EMAIL_FROM = "alerts@virtuososofttech.com";
    private static final String EMAIL_TO = "snehal.matke@vpel.in, pratik.pingale@virtuososofttech.com, anup.jadhav@virtuososofttech.com";
    private static final String JENKINS_ADMIN="admin";
    private static final String JENKINS_PASS="Qawzsx!3";
    private static final String JENKINS_HOST="192.168.0.243";
    private static final String TOKEN="VSTBuild";
    private static final String PORT="8080";
    
    public static void main(String[] args) throws MessagingException, InterruptedException, IOException {
    	
    	SpringApplication.run(ServicesCheckerApplication.class, args);
    	
    	
    	
        if (args.length < 2) {
            System.out.println("Please provide the API URL and Jenkins Service Name as an argument.");
          System.exit(0);
            return;
        }
         System.out.println("URL- "+args[0]);
         System.out.println("Service Name- "+args[1]);
        String apiUrl = args[0];
        String serviceName=args[1];
      
        try {
            int statusCode = checkApiStatus(apiUrl);
          
          
            if (statusCode == 200) {
            	  System.out.println("API is working fine.");
                  System.exit(0);
             
            } else {
            	 String response = getApiResponse(apiUrl);
            	 String jenkinsResponse=retryBuild(serviceName);
//                 sendEmail("API Status Alert", "The API returned status code " + statusCode + ".\n\nResponse:\n" + response+ "\n\n"+ "API is : "+apiUrl+"\n\n"+ "Retrying the build for: "+serviceName+"\n\n"+ "Response from jenkins is: "+jenkinsResponse);
                String msg="The API returned status code " + statusCode + ".\n\nResponse:\n" + response+ "\n\n"+ "API is : "+apiUrl+"\n\n"+ "Retrying the build for: "+serviceName+"\n\n"+ "Response from jenkins is: "+jenkinsResponse;
                 Thread.sleep(150000); // Adjust the delay time as needed

                 // Rechecks the API status
                 statusCode = checkApiStatus(apiUrl);

                 if (statusCode == 200) {
                     sendEmail(args[1]+"-"+"API Status Alert", msg+"\n\nThe API is working fine now.\n\n");
                 } else {
                     response = getApiResponse(apiUrl);
                     sendEmail(args[1]+"-"+"API Status Alert",msg+ "\n\nThe API is still not working properly after the build.\n\nResponse:\n" + response);
                 }

                 System.exit(0);
           
            }
        } catch (IOException e) {
        	 String jenkinsResponse=retryBuild(serviceName);
//            sendEmail("API Status Alert", "Failed to connect to the API:\n\n" + e.getMessage()+ "\n\n"+ "API is : "+apiUrl+"\n\n"+ "Retrying the build for: "+serviceName+"\n\n"+ "Response from jenkins is: "+jenkinsResponse);
            Thread.sleep(150000); // Adjust the delay time as needed
            String msg="Failed to connect to the API:\n\n" + e.getMessage()+ "\n\n"+ "API is : "+apiUrl+"\n\n"+ "Retrying the build for: "+serviceName+"\n\n"+ "Response from jenkins is: "+jenkinsResponse;
            // Rechecks the API status
        int    statusCode = checkApiStatus(apiUrl);

            if (statusCode == 200) {
                sendEmail(args[1]+"-"+"API Status Alert", msg+"\n\nThe API is working fine now.");
            } else {
            String response = getApiResponse(apiUrl);
                sendEmail(args[1]+"-"+"API Status Alert",msg+ "\n\nThe API is still not working properly after the build.\n\nResponse:\n" + response);
            }

            System.exit(0);
           
            
        }
    }
    
 
    
    private static int checkApiStatus(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        return connection.getResponseCode();
    }
    
    private static String getApiResponse(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        
//        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//        StringBuilder response = new StringBuilder();
//        String line;
//        
//        while ((line = reader.readLine()) != null) {
//            response.append(line);
//        }
        
//        reader.close();
//        System.out.println(response.toString());
        return connection.getResponseMessage();
    }
    
    private static void sendEmail(String subject, String body) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD);
            }
        });
        
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO));
        message.setSubject(subject);
        message.setText(body);
        
        Transport.send(message);
        
        System.out.println("Email sent successfully.");
    
    }
    
    private static String retryBuild(String serviceName) throws IOException, InterruptedException {
        String jenkinsUrl = "http://" + JENKINS_ADMIN + ":" + JENKINS_PASS + "@" + JENKINS_HOST + ":" + PORT + "/job/" + serviceName + "/build?token=" + TOKEN;
//        System.out.println("Calling the Jenkins build on " + jenkinsUrl);

        // Retrieve Jenkins crumb
        String crumbUrl = "http://" + JENKINS_ADMIN + ":" + JENKINS_PASS + "@" + JENKINS_HOST + ":" + PORT + "/crumbIssuer/api/json";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest crumbRequest = HttpRequest.newBuilder()
                .uri(URI.create(crumbUrl))
                .build();
        HttpResponse<String> crumbResponse = client.send(crumbRequest, BodyHandlers.ofString());
        String crumbHeader = crumbResponse.headers().firstValue("X-Jenkins-Crumb").orElse("");

        // Include crumb header in the Jenkins build request
        HttpClient jenkinsClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest jenkinsRequest = HttpRequest.newBuilder()
                .uri(URI.create(jenkinsUrl))
                .header("Authorization", "Basic " + getEncodedCredentials())
                .header("Jenkins-Crumb", crumbHeader)
                .build();
        HttpResponse<String> jenkinsResponse = jenkinsClient.send(jenkinsRequest, BodyHandlers.ofString());

        System.out.println(jenkinsResponse.statusCode());
        return "Response code: " + jenkinsResponse.statusCode() + " & Response message: " + jenkinsResponse.body();
    }

    private static String getEncodedCredentials() {
        String credentials = JENKINS_ADMIN + ":" + JENKINS_PASS;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }
    
   
}
