package jp.co.sanseki.pm.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** コード値チェック（例: 品目区分 1/2/9、入出庫区分 1/2）。空は対象外。 */
@Documented
@Constraint(validatedBy = OneOfValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface OneOf {

    String[] value();

    String message() default "M004";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
