package no.ruter.taas.exception;

/**
 * Exception typically used to communicate HTTP errors out of APIs.
 */
public class HttpErrorException extends RuntimeException {

  private int httpStatusCode;

  public HttpErrorException(int httpStatusCode, String message) {
    super(message);
    this.httpStatusCode = httpStatusCode;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }
}