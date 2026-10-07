package jp.co.sanseki.pm.common;

import java.time.LocalDateTime;

/**
 * UPD_TMS による楽観的排他制御（カタログ R3）。
 * 照会応答で返した updTms を更新・削除要求で受け取り、更新直前に読み直した UPD_TMS とマイクロ秒まで比較する。
 * updTms が未指定・形式不正（照会せずに更新した場合など）も不一致として M017（カタログ D-11）。
 */
public final class OptimisticLock {

    private OptimisticLock() {
    }

    public static void verify(String requestedUpdTms, LocalDateTime currentUpdTms) {
        boolean matched = Db2Timestamp.tryParse(requestedUpdTms)
                .map(requested -> requested.equals(currentUpdTms))
                .orElse(false);
        if (!matched) {
            throw new BusinessException(MessageCatalog.M017);
        }
    }
}
