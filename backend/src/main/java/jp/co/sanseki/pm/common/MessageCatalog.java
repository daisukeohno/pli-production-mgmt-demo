package jp.co.sanseki.pm.common;

import org.springframework.http.HttpStatus;

/**
 * メッセージ ID と文言（PMMSG.inc の移植）。
 * <p>
 * M001〜M017 の文言は PMMSG.inc の INIT 値から CHAR(60) の末尾空白だけを除いたもので、一字一句変えない。
 * M018 は新システムで追加したもの（カタログ §8 D-06：製造指示番号の連番 9999 超過）。
 * HTTP ステータスは API 契約（docs/migration/api-contract.md §3）による。成功メッセージは 200/201 で返す。
 */
public enum MessageCatalog {

    M001("品目コードが不正です。チェックデジットを確認してください。", HttpStatus.BAD_REQUEST),
    M002("該当する品目は登録されていません。", HttpStatus.NOT_FOUND),
    M003("品目コードは既に使用されています。", HttpStatus.CONFLICT),
    M004("必須項目が未入力です。", HttpStatus.BAD_REQUEST),
    M005("登録しました。", HttpStatus.CREATED),
    M006("更新しました。", HttpStatus.OK),
    M007("削除しました。", HttpStatus.OK),
    M008("数値項目に数字以外が入力されています。", HttpStatus.BAD_REQUEST),
    M009("在庫数量が不正です。", HttpStatus.BAD_REQUEST),
    M010("入出庫区分は1(入庫)または2(出庫)を入力してください。", HttpStatus.BAD_REQUEST),
    M011("在庫が不足しています。出庫数量を確認してください。", HttpStatus.UNPROCESSABLE_ENTITY),
    M012("入出庫を登録しました。", HttpStatus.OK),
    M013("製造指示を登録しました。", HttpStatus.CREATED),
    M014("部品構成が登録されていません。", HttpStatus.UNPROCESSABLE_ENTITY),
    M015("在庫不足の部品があります。23行目にPF4で一覧を表示します。", HttpStatus.UNPROCESSABLE_ENTITY),
    M016("完成予定日の形式が不正です。YYYY-MM-DDで入力してください。", HttpStatus.BAD_REQUEST),
    M017("他の端末で更新されています。再照会してください。", HttpStatus.CONFLICT),
    /** 新規（D-06）。PMMSG.inc には存在しない。 */
    M018("製造指示番号の連番が上限(9999)を超えました。", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String text;
    private final HttpStatus httpStatus;

    MessageCatalog(String text, HttpStatus httpStatus) {
        this.text = text;
        this.httpStatus = httpStatus;
    }

    public String id() {
        return name();
    }

    public String text() {
        return text;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public boolean isSuccess() {
        return httpStatus.is2xxSuccessful();
    }
}
