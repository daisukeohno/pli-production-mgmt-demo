/*==================================================================*/
/* PMCOMM   COMMAREA定義（PM01/PM02/PM03共通）                       */
/*                                                                  */
/* 変更履歴                                                          */
/*   1998-04-12  初版作成                              SE 田中      */
/*   2002-08-30  PM02用エリア追加                      SE 鈴木      */
/*   2007-01-18  PM03用エリア追加（製造指示）          SE 高橋      */
/*   2010-06-21  CA-UPD-TMSを楽観排他制御用に転用      SE 佐藤      */
/*               （旧CA-LOCK-FLGは使用しないがエリアは残置）          */
/*==================================================================*/
DCL 1 COMMAREA BASED(PTR_COMMAREA),
      2 CA-HEADER,
        3 CA-SCRN-ID       CHAR(4),      /* 直前に表示した画面ID PM01/PM02/PM03 */
        3 CA-FUNC-CD       CHAR(1),      /* 機能コード 1:照会 2:登録 3:更新 4:削除 */
        3 CA-FIRST-TIME    CHAR(1),      /* 'Y':初回起動 'N':2回目以降 */
      2 CA-PM01,                          /* --- PM01 品目マスタ保守用 --- */
        3 CA01-ITEM-CD     CHAR(8),      /* 選択中の品目コード */
        3 CA01-PAGE-NO     FIXED BIN(15),/* 現在の表示頁 */
        3 CA01-TOP-ITEM-CD CHAR(8),      /* 当該頁の先頭品目コード（PF7/PF8用） */
        3 CA01-UPD-TMS     CHAR(26),     /* 更新前に読んだUPD_TMS（楽観排他制御） */
        3 CA01-LOCK-FLG    CHAR(1),      /* 旧方式の排他フラグ。現在は未使用（デッドエリア） */
      2 CA-PM02,                          /* --- PM02 在庫照会・入出庫登録用 --- */
        3 CA02-ITEM-CD     CHAR(8),
        3 CA02-IO-KBN      CHAR(1),      /* '1':入庫 '2':出庫 */
      2 CA-PM03,                          /* --- PM03 製造指示登録用 --- */
        3 CA03-ITEM-CD     CHAR(8),      /* 製造する製品コード */
        3 CA03-ORDER-QTY   FIXED DEC(9,0),
        3 CA03-DUE-DATE    CHAR(10),     /* YYYY-MM-DD */
        3 CA03-NG-CNT      FIXED BIN(15),/* 在庫不足部品の件数（エラー一覧用） */
        3 CA03-NG-LIST(10),              /* 在庫不足部品一覧（最大10件） */
          4 CA03-NG-ITEM-CD   CHAR(8),
          4 CA03-NG-NEED-QTY  FIXED DEC(9,0),
          4 CA03-NG-STOCK-QTY FIXED DEC(9,0),
      2 CA-MSG,
        3 CA-MSG-ID        CHAR(4),      /* PMMSGのメッセージIDを格納 */
        3 CA-MSG-FLD-POS   FIXED BIN(15);/* エラー項目のカーソル位置（BMS項目順） */

/* 旧COMMAREA長定義。現在はDCL不要だがコピー句の互換性のため残置 */
/* DCL CA-LENGTH FIXED BIN(31) VALUE(LENGTH(COMMAREA));            */
