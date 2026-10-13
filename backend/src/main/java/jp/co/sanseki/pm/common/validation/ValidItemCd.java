package jp.co.sanseki.pm.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** 品目コード8桁＋チェックデジット（PMUTL01 VALID-CHK-DGT）。空は対象外（必須は @NotBlank で判定）。 */
@Documented
@Constraint(validatedBy = ValidItemCdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidItemCd {

    String message() default "M001";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
