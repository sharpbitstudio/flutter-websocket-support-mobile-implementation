package android.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ArrayMap<K, V> implements Map<K, V> {

  private static class Value<KE, VE>{
    private final KE key;
    private VE value;

    public Value(KE key, VE value) {
      this.key = key;
      this.value = value;
    }

    public KE getKey() {
      return this.key;
    }

    public VE getValue() {
      return this.value;
    }

    public void setValue(VE value) {
      this.value = value;
    }

    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof ArrayMap.Value)) {
        return false;
      }
      final Value<?, ?> other = (Value<?, ?>) o;
      if (!other.canEqual((Object) this)) {
        return false;
      }
      final Object this$key = this.getKey();
      final Object other$key = other.getKey();
      if (this$key == null ? other$key != null : !this$key.equals(other$key)) {
        return false;
      }
      final Object this$value = this.getValue();
      final Object other$value = other.getValue();
      if (this$value == null ? other$value != null : !this$value.equals(other$value)) {
        return false;
      }
      return true;
    }

    protected boolean canEqual(final Object other) {
      return other instanceof ArrayMap.Value;
    }

    public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      final Object $key = this.getKey();
      result = result * PRIME + ($key == null ? 43 : $key.hashCode());
      final Object $value = this.getValue();
      result = result * PRIME + ($value == null ? 43 : $value.hashCode());
      return result;
    }

    public String toString() {
      return "ArrayMap.Value(key=" + this.getKey() + ", value=" + this.getValue() + ")";
    }
  }

  private int size;
  private final Value<K, V>[] values = new Value[10];

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Nullable
  @Override
  public V get(@Nullable Object key) {
    for (int i = 0; i < size; i++) {
      if (values[i] != null) {
        if (values[i].getKey().equals(key)) {
          return values[i].getValue();
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    boolean insert = true;
    for (int i = 0; i < size; i++) {
      if (values[i].getKey().equals(key)) {
        values[i].setValue(value);
        insert = false;
      }
    }
    if (insert) {
      values[size++] = new Value<K, V>(key, value);
    }
    return value;
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return false;
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    return false;
  }


  @Nullable
  @Override
  public V remove(@Nullable Object key) {
    return null;
  }

  @Override
  public void putAll(@NonNull Map<? extends K, ? extends V> m) {

  }

  @Override
  public void clear() {

  }

  @NonNull
  @Override
  public Set<K> keySet() {
    return null;
  }

  @NonNull
  @Override
  public Collection<V> values() {
    return null;
  }

  @NonNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return null;
  }
}
