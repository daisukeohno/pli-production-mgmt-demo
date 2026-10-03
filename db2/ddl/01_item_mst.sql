------------------------------------------------------------------
-- ITEM_MST  品目マスタ
--
-- 変更履歴
--   1998-04-10  初版作成                         SE  田中
--   2003-11-02  ITEM_KBN追加（消耗品区分対応）    SE  鈴木
--   2010-06-21  DEL_FLGによる論理削除方式へ変更   SE  佐藤
------------------------------------------------------------------
CREATE TABLE SANSEKI.ITEM_MST
  (
    ITEM_CD      CHAR(8)        NOT NULL,   -- 品目コード（8桁、末尾チェックデジット）
    ITEM_NAME    CHAR(20)       NOT NULL,   -- 品目名
    ITEM_KBN     CHAR(1)        NOT NULL,   -- 品目区分 1:製品 2:部品 9:消耗品
    STOCK_UNIT   CHAR(4)        NOT NULL,   -- 単位（個、KG等）
    DEL_FLG      CHAR(1)        NOT NULL DEFAULT '0',  -- 削除フラグ 0:有効 1:削除済（論理削除）
    UPD_TMS      TIMESTAMP      NOT NULL,   -- 更新タイムスタンプ（楽観的排他制御キー）
    CRT_TMS      TIMESTAMP      NOT NULL,   -- 作成タイムスタンプ
    PRIMARY KEY (ITEM_CD)
  )
  IN SANSEKI.TS001;

CREATE INDEX SANSEKI.IX_ITEM_KBN
  ON SANSEKI.ITEM_MST (ITEM_KBN, ITEM_CD);
