package com.hogaria.integration.cjprestamos;

import com.hogaria.integration.cjprestamos.remote.CjPrestamosCashControlRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosDashboardRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosInstallmentRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosLoanActiveRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosPaymentRemoteResponse;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpCjPrestamosClient implements CjPrestamosClient {
  private static final Logger log = LoggerFactory.getLogger(HttpCjPrestamosClient.class);

  private final RestClient restClient;
  private final String apiPrefix;

  public HttpCjPrestamosClient(CjPrestamosProperties properties, RestTemplateBuilder restTemplateBuilder) {
    this.apiPrefix = properties.resolvedApiPrefix();
    this.restClient = RestClient.builder(restTemplateBuilder
        .setConnectTimeout(java.time.Duration.ofMillis(properties.connectTimeoutMs()))
        .setReadTimeout(java.time.Duration.ofMillis(properties.readTimeoutMs()))
        .basicAuthentication(properties.username(), properties.password())
        .build()).baseUrl(properties.baseUrl()).build();
  }

  @Override public List<CjPrestamosLoanActiveRemoteResponse> getActiveLoans(UUID profileId, UUID userId) { return getList(apiPrefix + "/loans/active", "loans/active", CjPrestamosLoanActiveRemoteResponse[].class, profileId, userId); }
  @Override public CjPrestamosDashboardRemoteResponse getDashboardSummary(UUID profileId, UUID userId) { return getObject(apiPrefix + "/dashboard", "dashboard", CjPrestamosDashboardRemoteResponse.class, profileId, userId); }
  @Override public CjPrestamosCashControlRemoteResponse getCashControl(UUID profileId, UUID userId) { return getObject(apiPrefix + "/control-caja", "control-caja", CjPrestamosCashControlRemoteResponse.class, profileId, userId); }
  @Override public List<CjPrestamosInstallmentRemoteResponse> getLoanInstallments(UUID profileId, UUID userId, Long externalLoanId) { return getList(apiPrefix + "/loans/" + externalLoanId + "/installments", "loans/{id}/installments", CjPrestamosInstallmentRemoteResponse[].class, profileId, userId); }
  @Override public List<CjPrestamosPaymentRemoteResponse> getLoanPayments(UUID profileId, UUID userId, Long externalLoanId) { return getList(apiPrefix + "/loans/" + externalLoanId + "/payments", "loans/{id}/payments", CjPrestamosPaymentRemoteResponse[].class, profileId, userId); }

  private <T> T getObject(String path, String logicalEndpoint, Class<T> clazz, UUID profileId, UUID userId) {
    try {
      return restClient.get().uri(URI.create(path)).header("X-Profile-Id", profileId.toString()).header("X-User-Id", userId.toString())
          .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
            int status = res.getStatusCode().value();
            log.warn("cjprestamos remote error endpoint={} status={}", logicalEndpoint, status);
            throw new CjPrestamosIntegrationException(mapHttpError(status, logicalEndpoint));
          }).body(clazz);
    } catch (CjPrestamosIntegrationException e) { throw e;
    } catch (RestClientResponseException e) {
      log.warn("cjprestamos remote error endpoint={} status={}", logicalEndpoint, e.getRawStatusCode());
      throw new CjPrestamosIntegrationException(mapHttpError(e.getRawStatusCode(), logicalEndpoint), e);
    } catch (RestClientException e) { throw new CjPrestamosIntegrationException("No se pudo conectar con cjprestamos (endpoint=" + logicalEndpoint + ")", e);
    } catch (Exception e) { throw new CjPrestamosIntegrationException("Respuesta inválida de cjprestamos (endpoint=" + logicalEndpoint + ")", e); }
  }

  private <T> List<T> getList(String path, String logicalEndpoint, Class<T[]> clazz, UUID profileId, UUID userId) {
    T[] response = getObject(path, logicalEndpoint, clazz, profileId, userId);
    return response == null ? List.of() : List.of(response);
  }

  private String mapHttpError(int status, String logicalEndpoint) {
    if (status == 401 || status == 403) return "Autenticación/autorización rechazada por cjprestamos (endpoint=" + logicalEndpoint + ", status=" + status + ")";
    if (status >= 500) return "cjprestamos no disponible (endpoint=" + logicalEndpoint + ", status=" + status + ")";
    return "Error HTTP en cjprestamos (endpoint=" + logicalEndpoint + ", status=" + status + ")";
  }
}
