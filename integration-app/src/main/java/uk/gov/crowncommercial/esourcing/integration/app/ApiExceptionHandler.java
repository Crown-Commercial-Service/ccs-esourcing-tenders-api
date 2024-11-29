package uk.gov.crowncommercial.esourcing.integration.app;

import java.time.Clock;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.crowncommercial.esourcing.integration.exception.SalesforceUpdateException;
import uk.gov.crowncommercial.esourcing.integration.server.api.ApiUtil;
import uk.gov.crowncommercial.esourcing.integration.service.EmailService;

/*
 * Catches any errors raised by the API layer and returns a suitable error response.
 */
@ControllerAdvice(basePackageClasses = ApiUtil.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
  private final EmailService emailService;
  private final Clock clock;

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  public ApiExceptionHandler(EmailService emailService, Clock clock) {
    this.emailService = emailService;
    this.clock = clock;
  }

  /**
   * Catch/mop up everything else and return a generic Internal Server Error status code.
   *
   * @param t a throwable not handled elsewhere
   * @param request servlet request used to retrieve request info such as path
   * @return error response entity
   */
  @ExceptionHandler({Throwable.class})
  public ResponseEntity<Object> handleThrowable(Throwable t, HttpServletRequest request) {

    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    String path = request.getPathInfo();

    LOG.warn("Unhandled exception when handling REST call to {}", path, t);

    return new ResponseEntity<>(
        ErrorResponse.builder()
            .timestamp(clock.instant())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message("Unhandled exception")
            .path(path)
            .build(),
        status);
  }

  @ExceptionHandler({ConstraintViolationException.class})
  public ResponseEntity<Object> handleConstraintViolationException(
      ConstraintViolationException cve) {
    LOG.warn("Constraint violation - {}", cve.getMessage());

    HttpStatus status = HttpStatus.BAD_REQUEST;

    return new ResponseEntity<>(
        ErrorResponse.builder()
            .timestamp(clock.instant())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(String.format("Constraint violation - %s", cve.getMessage()))
            .build(),
        status);
  }

  @ExceptionHandler({SalesforceUpdateException.class})
  public ResponseEntity<Object> handleSalesforceUpdateException(SalesforceUpdateException sue) {
    LOG.warn("Salesforce Status Update failure - {}", sue.getMessage());

    emailService.sendSalesforceUpdateFailureEmails(sue.getSalesforceErrorList());

    HttpStatus status = HttpStatus.BAD_REQUEST;

    return new ResponseEntity<>(
        ErrorResponse.builder()
            .timestamp(clock.instant())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(String.format("Salesforce Status Update failure - %s", sue.getMessage()))
            .build(),
        status);
  }

  @Override
  protected final ResponseEntity<Object> handleMethodArgumentNotValid(
      final MethodArgumentNotValidException e,
      final HttpHeaders headers,
      final HttpStatus status,
      final WebRequest request) {
    LOG.warn("Constraint violation - {}", e.getMessage());

    return new ResponseEntity<>(
        ErrorResponse.builder()
            .timestamp(clock.instant())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(String.format("Method argument is not valid - %s", e.getMessage()))
            .build(),
        status);
  }
}
