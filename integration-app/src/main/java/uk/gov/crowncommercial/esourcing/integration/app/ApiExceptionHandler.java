package uk.gov.crowncommercial.esourcing.integration.app;

import java.time.Clock;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.crowncommercial.esourcing.integration.server.api.ApiUtil;

/*
 * Catches any errors raised by the API layer and returns a suitable error response.
 */
@ControllerAdvice(basePackageClasses = ApiUtil.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler
 {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @Autowired private Clock clock;

  /**
   * Catch/mop up everything else and return a generic Internal Server Error status code.
   *
   * @param t a throwable not handled elsewhere
   * @param request servlet request used to retrieve request info such as path
   * @return error response entity
   */
  @ExceptionHandler({Throwable.class})
  public ResponseEntity<Object> handleThrowable(Throwable t, HttpServletRequest request) {

    if (t instanceof ConstraintViolationException) {
      LOG.warn("Constraint violation - {}", t.getMessage());

      HttpStatus status = HttpStatus.BAD_REQUEST;

      return new ResponseEntity<>(
          ErrorResponse.builder()
              .timestamp(clock.instant())
              .status(status.value())
              .error(status.getReasonPhrase())
              .message(String.format("Constraint violation - %s", t.getMessage()))
              .build(),
          status);
    } else {
      LOG.warn("Unhandled exception when handling REST call to {}", request.getPathInfo(), t);

      HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
      String path = request.getPathInfo();

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
            .message(String.format("Constraint violation - %s", e.getMessage()))
            .build(),
        status);
  }
}
