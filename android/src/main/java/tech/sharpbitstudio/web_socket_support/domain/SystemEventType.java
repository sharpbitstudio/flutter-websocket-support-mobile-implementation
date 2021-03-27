package tech.sharpbitstudio.web_socket_support.domain;

import lombok.Getter;

public enum SystemEventType {

  WS_OPENED("onOpened"),
  WS_CLOSING("onClosing"),
  WS_CLOSED("onClosed"),
  WS_FAILURE("onFailure");

  @Getter
  private final String methodName;

  SystemEventType(String methodName) {
    this.methodName = methodName;
  }
}
