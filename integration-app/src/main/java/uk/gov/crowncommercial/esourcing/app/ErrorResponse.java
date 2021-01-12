package uk.gov.crowncommercial.esourcing.app;

import java.time.Instant;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * For returning customised JSON error responses. This mirrors the object returned by standard
 * Spring Boot error handling but is a class and not simply a map of key value pairs.
 */
@JsonDeserialize(builder = ErrorResponse.Builder.class)
public final class ErrorResponse {

  private final Instant timestamp;
  private final int status;
  private final String error;
  private final String message;
  private final String path;

  private ErrorResponse(Builder b) {
    this.timestamp = b.timestamp;
    this.status = b.status;
    this.error = b.error;
    this.message = b.message;
    this.path = b.path;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public int getStatus() {
    return status;
  }

  public String getError() {
    return error;
  }

  public String getMessage() {
    return message;
  }

  public String getPath() {
    return path;
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static final class Builder {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    private Builder() {

    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder status(int status) {
      this.status = status;
      return this;
    }

    public Builder error(String error) {
      this.error = error;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder path(String path) {
      this.path = path;
      return this;
    }

    public ErrorResponse build() {
      return new ErrorResponse(this);
    }

  }

}
