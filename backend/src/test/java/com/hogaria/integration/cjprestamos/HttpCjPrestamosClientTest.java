package com.hogaria.integration.cjprestamos;

import static org.junit.jupiter.api.Assertions.*;

import com.hogaria.integration.cjprestamos.remote.CjPrestamosInstallmentRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosLoanActiveRemoteResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class HttpCjPrestamosClientTest {
  private HttpServer server;
  private String baseUrl;
  private final Queue<StubResponse> responses = new ArrayDeque<>();
  private final AtomicInteger receivedCalls = new AtomicInteger(0);
  private final AtomicReference<String> lastPath = new AtomicReference<>();
  private final List<String> receivedPaths = new ArrayList<>();
  private final AtomicReference<String> lastAuthorization = new AtomicReference<>();
  private final AtomicReference<String> lastProfileHeader = new AtomicReference<>();
  private final AtomicReference<String> lastUserHeader = new AtomicReference<>();

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/", this::handle);
    server.start();
    baseUrl = "http://localhost:" + server.getAddress().getPort();
    receivedPaths.clear();
  }

  @AfterEach
  void tearDown() {
    if (server != null) server.stop(0);
  }

  @Test
  void mapsValidLoansJsonResponse() throws Exception {
    responses.add(StubResponse.ok(readFixture("fixtures/cjprestamos/active-loans.json")));
    var client = buildClient(1000, 1000);

    List<CjPrestamosLoanActiveRemoteResponse> loans = client.getActiveLoans(UUID.randomUUID(), UUID.randomUUID());

    assertEquals(1, loans.size());
    assertEquals(101L, loans.getFirst().id());
    assertEquals("María Gómez", loans.getFirst().personaNombre());
    assertEquals(12, loans.getFirst().cantidadCuotas());
    assertEquals(1, receivedCalls.get());
    assertEquals("/api/integration/hogaria/loans/active", lastPath.get());
  }

  @Test
  void supportsV1PrefixAndRequiredHeaders() {
    responses.add(StubResponse.ok("[]"));
    responses.add(StubResponse.ok("{\"montoInvertido\":1,\"montoGanado\":1,\"montoPorGanar\":1,\"deudaTotal\":1,\"prestamosActivos\":1}"));
    responses.add(StubResponse.ok("{\"cajaDisponible\":1,\"inversionActiva\":1,\"capitalRecuperado\":1,\"capitalPendiente\":1,\"gananciaRealizada\":1,\"gananciaProyectada\":1,\"ingresosMesActual\":1,\"egresosMesActual\":1,\"balanceMesActual\":1,\"proyeccionCobro30Dias\":1,\"proyeccionCobro60Dias\":1,\"proyeccionCobro90Dias\":1,\"carteraEnMora\":1,\"cuotasPendientes\":1,\"cuotasVencenProximos7Dias\":1,\"recuperoCapitalPorcentaje\":1,\"rendimientoEsperadoPorcentaje\":1}"));
    responses.add(StubResponse.ok("[]"));
    responses.add(StubResponse.ok("[]"));
    var profileId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var client = buildClient(1000, 1000, "/api/v1/integration/hogaria");

    client.getActiveLoans(profileId, userId);
    client.getDashboardSummary(profileId, userId);
    client.getCashControl(profileId, userId);
    client.getLoanInstallments(profileId, userId, 7L);
    client.getLoanPayments(profileId, userId, 7L);

    assertEquals(List.of(
        "/api/v1/integration/hogaria/loans/active",
        "/api/v1/integration/hogaria/dashboard",
        "/api/v1/integration/hogaria/control-caja",
        "/api/v1/integration/hogaria/loans/7/installments",
        "/api/v1/integration/hogaria/loans/7/payments"), receivedPaths);
    assertEquals("Basic dXNlcjpwYXNz", lastAuthorization.get());
    assertEquals(profileId.toString(), lastProfileHeader.get());
    assertEquals(userId.toString(), lastUserHeader.get());
  }

  @Test
  void toleratesEmptyLists() {
    responses.add(StubResponse.ok("[]"));
    var client = buildClient(1000, 1000);

    var installments = client.getLoanInstallments(UUID.randomUUID(), UUID.randomUUID(), 11L);

    assertNotNull(installments);
    assertTrue(installments.isEmpty());
  }

  @Test
  void supportsNullableOptionalFields() throws Exception {
    responses.add(StubResponse.ok(readFixture("fixtures/cjprestamos/installments-null-fields.json")));
    var client = buildClient(1000, 1000);

    List<CjPrestamosInstallmentRemoteResponse> installments = client.getLoanInstallments(UUID.randomUUID(), UUID.randomUUID(), 101L);

    assertEquals(1, installments.size());
    assertNull(installments.getFirst().montoPagado());
    assertNull(installments.getFirst().saldoPendiente());
  }

  @Test
  void maps401And403ToControlledException() {
    responses.add(new StubResponse(401, "{}", 0));
    responses.add(new StubResponse(403, "{}", 0));
    var client = buildClient(1000, 1000);

    var ex401 = assertThrows(CjPrestamosIntegrationException.class,
        () -> client.getDashboardSummary(UUID.randomUUID(), UUID.randomUUID()));
    assertTrue(ex401.getMessage().contains("401"));

    var ex403 = assertThrows(CjPrestamosIntegrationException.class,
        () -> client.getCashControl(UUID.randomUUID(), UUID.randomUUID()));
    assertTrue(ex403.getMessage().contains("403"));
  }

  @Test
  void maps404ForUnknownLoanId() {
    responses.add(new StubResponse(404, "{}", 0));
    var client = buildClient(1000, 1000);

    var ex = assertThrows(CjPrestamosIntegrationException.class,
        () -> client.getLoanInstallments(UUID.randomUUID(), UUID.randomUUID(), 99999L));

    assertTrue(ex.getMessage().contains("404"));
  }

  @Test
  void mapsRemote500AsUnavailable() {
    responses.add(new StubResponse(500, "{}", 0));
    var client = buildClient(1000, 1000);

    var ex = assertThrows(CjPrestamosIntegrationException.class,
        () -> client.getLoanPayments(UUID.randomUUID(), UUID.randomUUID(), 15L));

    assertTrue(ex.getMessage().contains("500"));
  }

  @Test
  void mapsTimeoutOrConnectionFailure() {
    responses.add(new StubResponse(200, "[]", 300));
    var client = buildClient(200, 100);

    var ex = assertThrows(CjPrestamosIntegrationException.class,
        () -> client.getActiveLoans(UUID.randomUUID(), UUID.randomUUID()));

    assertTrue(ex.getMessage().contains("No se pudo conectar con cjprestamos")
        || hasCause(ex, SocketTimeoutException.class));
  }

  private HttpCjPrestamosClient buildClient(int connectTimeoutMs, int readTimeoutMs) {
    return buildClient(connectTimeoutMs, readTimeoutMs, null);
  }

  private HttpCjPrestamosClient buildClient(int connectTimeoutMs, int readTimeoutMs, String apiPrefix) {
    var properties = new CjPrestamosProperties(true, false, baseUrl, apiPrefix, "user", "pass", connectTimeoutMs, readTimeoutMs);
    return new HttpCjPrestamosClient(properties, new RestTemplateBuilder().setConnectTimeout(Duration.ofMillis(connectTimeoutMs)).setReadTimeout(Duration.ofMillis(readTimeoutMs)));
  }

  private void handle(HttpExchange exchange) throws IOException {
    receivedCalls.incrementAndGet();
    lastPath.set(exchange.getRequestURI().getPath());
    receivedPaths.add(exchange.getRequestURI().getPath());
    lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
    lastProfileHeader.set(exchange.getRequestHeaders().getFirst("X-Profile-Id"));
    lastUserHeader.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
    StubResponse response = responses.isEmpty() ? StubResponse.ok("[]") : responses.poll();
    if (response.delayMs > 0) {
      try { Thread.sleep(response.delayMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
    byte[] body = response.body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(response.status, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }

  private String readFixture(String path) throws IOException {
    return Files.readString(Path.of("src/test/resources", path), StandardCharsets.UTF_8);
  }

  private static boolean hasCause(Throwable error, Class<? extends Throwable> causeType) {
    Throwable current = error;
    while (current != null) {
      if (causeType.isInstance(current)) return true;
      current = current.getCause();
    }
    return false;
  }

  private record StubResponse(int status, String body, long delayMs) {
    static StubResponse ok(String body) { return new StubResponse(200, body, 0); }
  }
}
