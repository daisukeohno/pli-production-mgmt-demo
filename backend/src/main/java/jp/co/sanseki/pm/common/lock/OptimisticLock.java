package jp.co.sanseki.pm.common.lock;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jp.co.sanseki.pm.common.error.BusinessException;
import jp.co.sanseki.pm.common.message.MessageIds;
import org.springframework.stereotype.Component;

/**
 * UPD_TMS をバージョンとする楽観的排他制御（CA01-UPD-TMS 相当）。
 *
 * <p>照会時に返した UPD_TMS をクライアントが保持し、更新・削除リクエストで送り返す。
 * 保持値が無い・形式不正・DB の現在値と不一致、または条件付き UPDATE の更新件数が 0 件なら
 * HTTP 409 + M017（他の端末で更新されています。再照会してください。）。
 */
@Component
public class OptimisticLock {

    private final Clock clock;

    public OptimisticLock(Clock clock) {
        this.clock = clock;
    }

    /** クライアントが保持していた UPD_TMS と DB の現在値を比較する。 */
    public LocalDateTime verify(String heldUpdTms, LocalDateTime currentUpdTms) {
        LocalDateTime held = Db2Timestamp.parseOrNull(heldUpdTms);
        if (held == null || !held.equals(currentUpdTms)) {
            throw conflict();
        }
        return held;
    }

    /** 条件付き UPDATE（WHERE UPD_TMS = 保持値）の更新件数を検査する。 */
    public void requireUpdated(int updatedRows) {
        if (updatedRows != 1) {
            throw conflict();
        }
    }

    /** 新しい UPD_TMS（マイクロ秒精度）。直前値以下にならないよう補正する。 */
    public LocalDateTime nextTimestamp(LocalDateTime previous) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        if (previous != null && !now.isAfter(previous)) {
            return previous.plus(1, ChronoUnit.MICROS);
        }
        return now;
    }

    public LocalDateTime now() {
        return nextTimestamp(null);
    }

    private static BusinessException conflict() {
        return BusinessException.conflict(MessageIds.M017, null);
    }
}
