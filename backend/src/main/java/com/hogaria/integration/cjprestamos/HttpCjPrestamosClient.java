package com.hogaria.integration.cjprestamos;

import com.hogaria.integration.cjprestamos.remote.CjPrestamosCashControlRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosDashboardRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosInstallmentRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosLoanActiveRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosPaymentRemoteResponse;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpCjPrestamosClient implements CjPrestamosClient {
  private final RestClient restClient;

  public HttpCjPrestamosClient(CjPrestamosProperties properties, RestTemplateBuilder restTemplateBuilder) {
    this.restClient = RestClient.builder(restTemplateBuilder
        .setConnectTimeout(java.time.Duration.ofMillis(properties.connectTimeoutMs()))
        .setReadTimeout(java.time.Duration.ofMillis(properties.readTimeoutMs()))
        .basicAuthentication(properties.username(), properties.password())
        .build()).baseUrl(properties.baseUrl()).build();
  }

  @Override public List<CjPrestamosLoanActiveRemoteResponse> getActiveLoans(UUID profileId, UUID userId) { return getList("/api/integration/hogaria/loans/active", CjPrestamosLoanActiveRemoteResponse[].class, profileId, userId); }
  @Override public CjPrestamosDashboardRemoteResponse getDashboardSummary(UUID profileId, UUID userId) { return getObject("/api/integration/hogaria/dashboard", CjPrestamosDashboardRemoteResponse.class, profileId, userId); }
  @Override public CjPrestamosCashControlRemoteResponse getCashControl(UUID profileId, UUID userId) { return getObject("/api/integration/hogaria/control-caja", CjPrestamosCashControlRemoteResponse.class, profileId, userId); }
  @Override public List<CjPrestamosInstallmentRemoteResponse> getLoanInstallments(UUID profileId, UUID userId, Long externalLoanId) { return getList("/api/integration/hogaria/loans/" + externalLoanId + "/installments", CjPrestamosInstallmentRemoteResponse[].class, profileId, userId); }
  @Override public List<CjPrestamosPaymentRemoteResponse> getLoanPayments(UUID profileId, UUID userId, Long externalLoanId) { return getList("/api/integration/hogaria/loans/" + externalLoanId + "/payments", CjPrestamosPaymentRemoteResponse[].class, profileId, userId); }

  private <T> T getObject(String path, Class<T> clazz, UUID profileId, UUID userId) {
    try {
      return restClient.get().uri(URI.create(path)).header("X-Profile-Id", profileId.toString()).header("X-User-Id", userId.toString())
          .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> { throw new CjPrestamosIntegrationException("Error remoto cjprestamos: " + res.getStatusCode().value()); }).body(clazz);
    } catch (RestClientResponseException e) { throw new CjPrestamosIntegrationException(mapHttpError(e.getRawStatusCode()), e);
    } catch (RestClientException e) { throw new CjPrestamosIntegrationException("No se pudo conectar con cjprestamos", e);
    } catch (Exception e) { throw new CjPrestamosIntegrationException("Respuesta inválida de cjprestamos", e); }
  }

  private <T> List<T> getList(String path, Class<T[]> clazz, UUID profileId, UUID userId) {
    T[] response = getObject(path, clazz, profileId, userId);
    return response == null ? List.of() : List.of(response);
  }

  private String mapHttpError(int status) {
    if (status == 401 || status == 403) return "Autenticación/autorización rechazada por cjprestamos";
    if (status >= 500) return "cjprestamos no disponible (5xx)";
    return "Error HTTP en cjprestamos: " + status;
  }
}
