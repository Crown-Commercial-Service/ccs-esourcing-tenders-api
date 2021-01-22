package uk.gov.crowncommercial.esourcing.app;

import java.time.Clock;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.crowncommercial.esourcing.server.api.ApiUtil;

/*
 * Catches any errors raised by the API layer and returns a suitable error response.
 */
@ControllerAdvice(basePackageClasses = ApiUtil.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @Autowired
  private Clock clock;

  /**
   * Catch/mop up everything else and return a generic Internal Server Error status code.
   * 
   * @param t a throwable not handled elsewhere
   * @param request servlet request used to retrieve request info such as path
   * @return error response entity
   */
  @ExceptionHandler({Throwable.class})
  public ResponseEntity<Object> handleThrowable(Throwable t, HttpServletRequest request) {
    
    LOG.warn("Unhandled exception when handling REST call to {}", request.getPathInfo(), t);
    
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    String path = request.getPathInfo();
    
    return new ResponseEntity<Object>(
        ErrorResponse.builder().timestamp(clock.instant()).status(status.value())
            .error(status.getReasonPhrase()).message("Unhandled exception").path(path).build(),
        status);
  }
}
