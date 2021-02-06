package tech.sharpbitstudio.web_socket_support.handlers;

import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebSocketStreamHandler implements StreamHandler {

  private final BiConsumer<Object, EventSink> onListenConsumer;
  private final Consumer<Object> onCancelConsumer;


  public WebSocketStreamHandler(BiConsumer<Object, EventSink> onListenConsumer,
      Consumer<Object> onCancelConsumer) {
    this.onListenConsumer = onListenConsumer;
    this.onCancelConsumer = onCancelConsumer;
  }

  @Override
  public void onListen(Object arguments, EventSink events) {
    onListenConsumer.accept(arguments, events);
  }

  @Override
  public void onCancel(Object arguments) {
    onCancelConsumer.accept(arguments);
  }
}
