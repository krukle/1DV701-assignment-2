package httpserver;

/**
 * Enum representation of HTTP status codes. 
 */
enum StatusCode {
  OK(200, "OK"),
  REDIRECT(302, "Found"),
  BAD_REQUEST(400, "Bad Request"),
  NOT_FOUND(404, "Not Found"),
  INTERNAL_SERVER_ERROR(500, "Internal Server Error");

  public int code;
  public String msg;

  /**
   * <p>Constructor for StatusCode.</p>
   * <p>See link for description of status codes:</p>
   * https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
   *
   * @param code Status code.
   * @param msg Status message.
   */
  StatusCode(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  @Override
  public String toString() {
    return this.code + " " + this.msg;
  }
}