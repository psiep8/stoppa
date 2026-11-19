package it.stoppa.backend.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerTest {

	private final HttpClient client = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Test
	void healthReportsUpStatusAndGameName() throws Exception {
		HttpResponse<String> response = client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/health")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertTrue(response.headers().firstValue("content-type").orElse("").contains("application/json"),
				"content-type inatteso: " + response.headers().firstValue("content-type"));
		assertTrue(response.body().contains("\"status\":\"UP\""), response.body());
		assertTrue(response.body().contains("\"game\":\"stoppa\""), response.body());
	}

}
