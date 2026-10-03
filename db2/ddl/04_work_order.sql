------------------------------------------------------------------
-- WORK_ORDER  製造指示
--
-- 変更履歴
--   2000-03-01  初版作成                             SE  高橋
--   2009-12-10  STATUS列追加（指示取消対応のため）   SE  佐藤
------------------------------------------------------------------
CREATE TABLE SANSEKI.WORK_ORDER
  (
    WORK_ORDER_NO  CHAR(7)     NOT NULL,   -- 製造指示番号 'W'+年度下2桁+4桁連番
    ITEM_CD        CHAR(8)     NOT NULL,   -- 製造対象品目コード（製品）
    ORDER_QTY      DEC(9,0)    NOT NULL,   -- 指示数量
    DUE_DATE       DATE        NOT NULL,   -- 完成予定日
    STATUS         CHAR(1)     NOT NULL DEFAULT '0',  -- 0:登録済 1:取消済
    CRT_TMS        TIMESTAMP   NOT NULL,
    PRIMARY KEY (WORK_ORDER_NO)
  )
  IN SANSEKI.TS004;

ALTER TABLE SANSEKI.WORK_ORDER
  ADD FOREIGN KEY FK_WKORD_ITEM (ITEM_CD)
      REFERENCES SANSEKI.ITEM_MST (ITEM_CD);
