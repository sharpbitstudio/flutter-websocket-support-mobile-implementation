package tech.sharpbitstudio.web_socket_support.domain;

import androidx.collection.ArrayMap;
import java.io.Serializable;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SystemEventContext implements Serializable {

  int closeCode;
  String closeReason;
  String throwableType;
  String errorMessage;
  String causeMessage;

  public Map<String, Object> toMap() {
    Map<String, Object> result = new ArrayMap<>();
    if (closeCode > 0) {
      result.put("code", closeCode);
    }
    if (closeReason != null) {
      result.put("reason", closeReason);
    }
    if (throwableType != null) {
      result.put("throwableType", throwableType);
    }
    if (errorMessage != null) {
      result.put("errorMessage", errorMessage);
    }
    if (causeMessage != null) {
      result.put("causeMessage", causeMessage);
    }
    return result;
  }
}
