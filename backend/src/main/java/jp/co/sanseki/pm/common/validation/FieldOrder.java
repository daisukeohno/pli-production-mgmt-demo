package jp.co.sanseki.pm.common.validation;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.validation.FieldError;

/**
 * DTO（record）の宣言順＝BMS 画面の項目順として、エラー項目の優先順位を決める。
 * 同一項目に複数の違反がある場合は必須（M004）を優先する。
 */
public final class FieldOrder {

    private FieldOrder() {
    }

    public static Comparator<FieldError> comparing(Class<?> target) {
        List<String> order = target != null && target.isRecord()
                ? Arrays.stream(target.getRecordComponents()).map(RecordComponent::getName).toList()
                : List.of();
        return Comparator
                .comparingInt((FieldError e) -> {
                    int idx = order.indexOf(e.getField());
                    return idx < 0 ? Integer.MAX_VALUE : idx;
                })
                .thenComparing(e -> "M004".equals(e.getDefaultMessage()) ? 0 : 1);
    }
}
