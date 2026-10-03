------------------------------------------------------------------
-- STOCK  在庫
--
-- 変更履歴
--   1998-04-10  初版作成                SE  田中
--   2001-07-20  STOCK_QTYをDECIMALへ変更（旧INTEGERから移行）  SE  鈴木
------------------------------------------------------------------
CREATE TABLE SANSEKI.STOCK
  (
    ITEM_CD     CHAR(8)      NOT NULL,   -- 品目コード
    STOCK_QTY   DEC(9,0)     NOT NULL,   -- 在庫数量（消耗品区分のみマイナス可）
    UPD_TMS     TIMESTAMP    NOT NULL,
    PRIMARY KEY (ITEM_CD)
  )
  IN SANSEKI.TS003;

ALTER TABLE SANSEKI.STOCK
  ADD FOREIGN KEY FK_STOCK_ITEM (ITEM_CD)
      REFERENCES SANSEKI.ITEM_MST (ITEM_CD);
