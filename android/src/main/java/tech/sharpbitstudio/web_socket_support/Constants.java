package tech.sharpbitstudio.web_socket_support;

public class Constants {

  public static final String METHOD_PLATFORM_VERSION = "getPlatformVersion";

  // incoming methods
  public static final String IN_METHOD_NAME_CONNECT = "connect";
  public static final String IN_METHOD_NAME_DISCONNECT = "disconnect";
  public static final String IN_METHOD_NAME_SEND_TEXT_MSG = "sendTextMessage";
  public static final String IN_METHOD_NAME_SEND_BYTE_MSG = "sendByteMessage";

  // outgoing methods
  public static final String OUT_METHOD_NAME_ON_TEXT_MSG = "onTextMessage";
  public static final String OUT_METHOD_NAME_ON_BYTE_MSG = "onByteMessage";

  // method arguments
  public static final String ARGUMENT_URL = "serverUrl";
  public static final String ARGUMENT_OPTIONS = "options";
  public static final String ARGUMENT_TEXT_MESSAGE = "textMessage";
  public static final String ARGUMENT_BYTE_MESSAGE = "byteMessage";
}
