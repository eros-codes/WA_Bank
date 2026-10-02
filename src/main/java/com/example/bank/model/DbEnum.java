package com.example.bank.model;

/**
 * An enum stored as text. The old data mixes "Accepted" and "accepted",
 * so reading ignores case; writing always uses {@link #dbValue()}.
 */
public interface DbEnum {

    String dbValue();

    static <E extends Enum<E> & DbEnum> E parse(Class<E> type, String value) {
        if (value != null) {
            for (E e : type.getEnumConstants()) {
                if (e.dbValue().equalsIgnoreCase(value.strip())) return e;
            }
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + ": " + value);
    }
}
