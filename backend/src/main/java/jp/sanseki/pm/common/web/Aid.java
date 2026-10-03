package jp.sanseki.pm.common.web;

/**
 * EIBAID 相当の注意キー。
 * PF9（旧・一括出力）はデッドコードのため定義しない（受信した場合は 400 Bad Request）。
 */
public enum Aid {
    ENTER,
    CLEAR,
    PF3,
    PF4,
    PF7,
    PF8
}
