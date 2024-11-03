package tech.sharpbitstudio.web_socket_support.domain;

public enum SystemEventType {
  WS_OPENED("onOpened"),
  WS_CLOSING("onClosing"),
  WS_CLOSED("onClosed"),
  WS_FAILURE("onFailure");

  private final String methodName;

  SystemEventType(String methodName) {
    this.methodName = methodName;
  }

  public String getMethodName() {
    return this.methodName;
  }
}
