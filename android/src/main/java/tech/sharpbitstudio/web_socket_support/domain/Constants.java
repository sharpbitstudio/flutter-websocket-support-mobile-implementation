package tech.sharpbitstudio.web_socket_support.domain;

public class Constants {

  private Constants() {
  }

  public static final String METHOD_PLATFORM_VERSION = "getPlatformVersion";

  // incoming methods
  public static final String IN_METHOD_NAME_CONNECT = "connect";
  public static final String IN_METHOD_NAME_DISCONNECT = "disconnect";
  public static final String IN_METHOD_NAME_SEND_STRING_MSG = "sendStringMessage";
  public static final String IN_METHOD_NAME_SEND_BYTE_ARRAY_MSG = "sendByteArrayMessage";

  // outgoing methods
  public static final String OUT_METHOD_NAME_ON_STRING_MSG = "onStringMessage";
  public static final String OUT_METHOD_NAME_ON_BYTE_ARRAY_MSG = "onByteArrayMessage";

  // method arguments
  public static final String ARGUMENT_CODE = "code";
  public static final String ARGUMENT_REASON = "reason";
  public static final String ARGUMENT_URL = "serverUrl";
  public static final String ARGUMENT_OPTIONS = "options";
}
