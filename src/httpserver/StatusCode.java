package httpserver;

enum StatusCode {
  OK(200, "OK"),
  REDIRECT(302, "Found"),
  BAD_REQUEST(400, "Bad Request"),
  NOT_FOUND(404, "Not Found"),
  INTERNAL_SERVER_ERROR(500, "Internal Server Error");

  public int code;
  public String msg;

  StatusCode(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  @Override
  public String toString() {
    return this.code + " " + this.msg;
  }
}