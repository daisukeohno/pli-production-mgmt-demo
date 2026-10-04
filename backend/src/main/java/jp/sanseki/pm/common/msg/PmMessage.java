package jp.sanseki.pm.common.msg;

import jp.sanseki.pm.common.util.FixedChar;

/**
 * PMMSG（MSG_TABLE: MSG_ID CHAR(4) + MSG_TEXT CHAR(60)）の移植。
 * 文言は PMMSG.inc の INIT リテラルを空白パディングも含めてそのまま保持し、
 * 格納値は PL/I の INIT 代入と同じく CHAR(60) へ右パディング／切り詰めした値とする。
 */
public enum PmMessage {
    M001("M001", "品目コードが不正です。チェックデジットを確認してください。             "),
    M002("M002", "該当する品目は登録されていません。                                      "),
    M003("M003", "品目コードは既に使用されています。                                      "),
    M004("M004", "必須項目が未入力です。                                                  "),
    M005("M005", "登録しました。                                                          "),
    M006("M006", "更新しました。                                                          "),
    M007("M007", "削除しました。                                                          "),
    M008("M008", "数値項目に数字以外が入力されています。                                  "),
    M009("M009", "在庫数量が不正です。                                                    "),
    M010("M010", "入出庫区分は1(入庫)または2(出庫)を入力してください。                    "),
    M011("M011", "在庫が不足しています。出庫数量を確認してください。                     "),
    M012("M012", "入出庫を登録しました。                                                  "),
    M013("M013", "製造指示を登録しました。                                                "),
    M014("M014", "部品構成が登録されていません。                                         "),
    M015("M015", "在庫不足の部品があります。23行目にPF4で一覧を表示します。               "),
    M016("M016", "完成予定日の形式が不正です。YYYY-MM-DDで入力してください。              "),
    M017("M017", "他の端末で更新されています。再照会してください。                       ");

    private final String msgId;
    private final String initLiteral;
    private final String msgTextChar60;

    PmMessage(String msgId, String initLiteral) {
        this.msgId = msgId;
        this.initLiteral = initLiteral;
        String padded = initLiteral + " ".repeat(Math.max(0, 60 - initLiteral.length()));
        this.msgTextChar60 = padded.substring(0, 60);
    }

    public String msgId() {
        return msgId;
    }

    /** PMMSG.inc の INIT リテラル原文。 */
    public String initLiteral() {
        return initLiteral;
    }

    /** MSG_TEXT CHAR(60) の格納値（末尾空白込み）。 */
    public String msgTextChar60() {
        return msgTextChar60;
    }

    /** 画面表示用の文言（CHAR の末尾空白を除去）。 */
    public String msgText() {
        return FixedChar.rtrim(msgTextChar60);
    }

    public static PmMessage of(String msgId) {
        for (PmMessage m : values()) {
            if (m.msgId.equals(FixedChar.rtrim(msgId))) {
                return m;
            }
        }
        throw new IllegalArgumentException("未定義のメッセージID: " + msgId);
    }
}
