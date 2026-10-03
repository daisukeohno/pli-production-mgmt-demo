------------------------------------------------------------------
-- BOM  部品構成マスタ（1階層のみ。製品-部品の親子関係のみを持つ）
--
-- 変更履歴
--   1999-02-15  初版作成                           SE  田中
--   2005-09-01  YIELD_RATE（歩留まり率）追加       SE  高橋
------------------------------------------------------------------
CREATE TABLE SANSEKI.BOM
  (
    PARENT_ITEM_CD  CHAR(8)     NOT NULL,   -- 親品目コード（製品）
    CHILD_ITEM_CD   CHAR(8)     NOT NULL,   -- 子品目コード（部品）
    QTY_PER         DEC(7,2)    NOT NULL,   -- 員数（親1単位あたりの子部品必要数）
    YIELD_RATE      DEC(5,2)    NOT NULL,   -- 歩留まり率（％）例）95.00
    UPD_TMS         TIMESTAMP   NOT NULL,
    PRIMARY KEY (PARENT_ITEM_CD, CHILD_ITEM_CD)
  )
  IN SANSEKI.TS002;

ALTER TABLE SANSEKI.BOM
  ADD FOREIGN KEY FK_BOM_PARENT (PARENT_ITEM_CD)
      REFERENCES SANSEKI.ITEM_MST (ITEM_CD);

ALTER TABLE SANSEKI.BOM
  ADD FOREIGN KEY FK_BOM_CHILD (CHILD_ITEM_CD)
      REFERENCES SANSEKI.ITEM_MST (ITEM_CD);
