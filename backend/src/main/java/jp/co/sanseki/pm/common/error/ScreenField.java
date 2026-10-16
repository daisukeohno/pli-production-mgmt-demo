package jp.co.sanseki.pm.common.error;

/**
 * 画面の入力項目。エラー時のカーソル位置（COMMAREA CA-MSG-FLD-POS）を表す。
 * 各画面（PM01/PM02/PM03）が BMS 項目順に enum で実装する。
 */
public interface ScreenField {

    /** API・フロントエンドで使う項目名（JSON プロパティ名）。 */
    String fieldName();

    /** BMS 項目名（例：F-ITEMCD）。 */
    String bmsName();

    /** BMS 項目順の位置（CA-MSG-FLD-POS）。 */
    int position();
}
