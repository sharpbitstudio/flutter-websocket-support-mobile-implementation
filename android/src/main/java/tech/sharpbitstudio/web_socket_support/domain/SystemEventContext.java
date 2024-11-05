package tech.sharpbitstudio.web_socket_support.domain;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public final class SystemEventContext implements Serializable {

  private final int closeCode;
  private final String closeReason;
  private final String throwableType;
  private final String errorMessage;
  private final String causeMessage;

  SystemEventContext(int closeCode, String closeReason, String throwableType,
      String errorMessage, String causeMessage) {
    this.closeCode = closeCode;
    this.closeReason = closeReason;
    this.throwableType = throwableType;
    this.errorMessage = errorMessage;
    this.causeMessage = causeMessage;
  }

  public static SystemEventContextBuilder builder() {
    return new SystemEventContextBuilder();
  }

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

  public int getCloseCode() {
    return this.closeCode;
  }

  public String getCloseReason() {
    return this.closeReason;
  }

  public String getThrowableType() {
    return this.throwableType;
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }

  public String getCauseMessage() {
    return this.causeMessage;
  }

  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof SystemEventContext)) {
      return false;
    }
    final SystemEventContext other = (SystemEventContext) o;
    if (this.getCloseCode() != other.getCloseCode()) {
      return false;
    }
    final Object thisCloseReason = this.getCloseReason();
    final Object otherCloseReason = other.getCloseReason();
    if (!Objects.equals(thisCloseReason, otherCloseReason)) {
      return false;
    }
    final Object thisThrowableType = this.getThrowableType();
    final Object otherThrowableType = other.getThrowableType();
    if (!Objects.equals(thisThrowableType, otherThrowableType)) {
      return false;
    }
    final Object thisErrorMessage = this.getErrorMessage();
    final Object otherErrorMessage = other.getErrorMessage();
    if (!Objects.equals(thisErrorMessage, otherErrorMessage)) {
      return false;
    }
    final Object thisCauseMessage = this.getCauseMessage();
    final Object otherCauseMessage = other.getCauseMessage();
    return Objects.equals(thisCauseMessage, otherCauseMessage);
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = result * PRIME + this.getCloseCode();
    final Object closeReason2 = this.getCloseReason();
    result = result * PRIME + (closeReason2 == null ? 43 : closeReason2.hashCode());
    final Object throwableType2 = this.getThrowableType();
    result = result * PRIME + (throwableType2 == null ? 43 : throwableType2.hashCode());
    final Object errorMessage2 = this.getErrorMessage();
    result = result * PRIME + (errorMessage2 == null ? 43 : errorMessage2.hashCode());
    final Object causeMessage2 = this.getCauseMessage();
    result = result * PRIME + (causeMessage2 == null ? 43 : causeMessage2.hashCode());
    return result;
  }

  @NonNull
  public String toString() {
    return "SystemEventContext(closeCode=" + this.getCloseCode() + ", closeReason="
        + this.getCloseReason() + ", throwableType=" + this.getThrowableType() + ", errorMessage="
        + this.getErrorMessage() + ", causeMessage=" + this.getCauseMessage() + ")";
  }

  public static class SystemEventContextBuilder {

    private int closeCode;
    private String closeReason;
    private String throwableType;
    private String errorMessage;
    private String causeMessage;

    SystemEventContextBuilder() {
    }

    public SystemEventContextBuilder closeCode(int closeCode) {
      this.closeCode = closeCode;
      return this;
    }

    public SystemEventContextBuilder closeReason(String closeReason) {
      this.closeReason = closeReason;
      return this;
    }

    public SystemEventContextBuilder throwableType(String throwableType) {
      this.throwableType = throwableType;
      return this;
    }

    public SystemEventContextBuilder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public SystemEventContextBuilder causeMessage(String causeMessage) {
      this.causeMessage = causeMessage;
      return this;
    }

    public SystemEventContext build() {
      return new SystemEventContext(closeCode, closeReason, throwableType, errorMessage,
          causeMessage);
    }

    @NonNull
    public String toString() {
      return "SystemEventContext.SystemEventContextBuilder(closeCode=" + this.closeCode
          + ", closeReason=" + this.closeReason + ", throwableType=" + this.throwableType
          + ", errorMessage=" + this.errorMessage + ", causeMessage=" + this.causeMessage + ")";
    }
  }
}
