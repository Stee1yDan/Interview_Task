package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class CrptApi {

    public static class TestThread extends Thread {
        private CrptApi crptApi;
        public TestThread(CrptApi crptApi) {
            this.crptApi = crptApi;
        }

        @Override
        public void run() {
            for (int i = 0; i < 100; i++)
            {
                System.out.println(crptApi.makeEmptyRequest());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Document {

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Description {
            private String participantInn;
        }

        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private String certificate_document_number;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
    private final AtomicInteger requests = new AtomicInteger(0);
    private final static Timer timer = new Timer();
    private final String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    ObjectMapper objectMapper = new ObjectMapper();
    private final int apiRequestLimit;

    private CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.apiRequestLimit = requestLimit;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                requests.set(0);
            }
        }, timeUnit.toMillis(1));
    }

    public String makeRequest(Document document)  {
        requests.incrementAndGet();

        if (requests.get() > apiRequestLimit) {
            return "WARNING! The maximum limit for requests has been reached";
        }

        HttpClient httpclient = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(document)))
                    .build();
            CompletableFuture<HttpResponse<String>> futureResponse = httpclient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            HttpResponse<String> response = futureResponse.get();

            if (response.statusCode() == 200) {
                return "Request was sent successfully";
            }
            else {
                return  "Request was finished with status code: " + response.statusCode();
            }
        }
        catch (JsonProcessingException exception) {
            return "Couldn't send request. Error while converting to json.";
        }
        catch (ExecutionException | InterruptedException e) {
            return "Couldn't finish the execution";
        }
    }

    public String makeEmptyRequest()  {
        requests.incrementAndGet();

        System.out.println("Current request number: " + requests.get());

        if (requests.get() > apiRequestLimit) {
            return "WARNING! The maximum limit for requests has been reached";
        }

        HttpClient httpclient = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            CompletableFuture<HttpResponse<String>> futureResponse = httpclient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            HttpResponse<String> response = futureResponse.get();

            if (response.statusCode() == 200) {
                return  "Request was sent successfully";
            }
            else {
                return  "Request was finished with status code: " + response.statusCode();
            }
        }
        catch (ExecutionException | InterruptedException e) {
            return  "Couldn't finish the execution";
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 6);
        TestThread thread1 = new TestThread(crptApi);
        TestThread thread2 = new TestThread(crptApi);

        thread1.start();
        thread2.start();
    }
}