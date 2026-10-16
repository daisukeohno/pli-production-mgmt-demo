------------------------------------------------------------------
-- 現行 DB2 DDL（db2/ddl/01〜05）の PostgreSQL 移植
--   ・列名・型・桁数・NOT NULL・DEFAULT・主キー・外部キーは現行どおり
--   ・CHAR(n) は bpchar（空白埋め固定長）のまま維持。比較は DB2 同様に末尾空白を無視する
--   ・DB2 の TIMESTAMP（マイクロ秒精度）は TIMESTAMP(6)
--   ・表領域指定（IN SANSEKI.TSnnn）は PostgreSQL に無いため削除
------------------------------------------------------------------

-- 01_item_mst.sql  品目マスタ
CREATE TABLE ITEM_MST
  (
    ITEM_CD      CHAR(8)        NOT NULL,   -- 品目コード（8桁、末尾チェックデジット）
    ITEM_NAME    CHAR(20)       NOT NULL,   -- 品目名
    ITEM_KBN     CHAR(1)        NOT NULL,   -- 品目区分 1:製品 2:部品 9:消耗品
    STOCK_UNIT   CHAR(4)        NOT NULL,   -- 単位（個、KG等）
    DEL_FLG      CHAR(1)        NOT NULL DEFAULT '0',  -- 削除フラグ 0:有効 1:削除済（論理削除）
    UPD_TMS      TIMESTAMP(6)   NOT NULL,   -- 更新タイムスタンプ（楽観的排他制御キー）
    CRT_TMS      TIMESTAMP(6)   NOT NULL,   -- 作成タイムスタンプ
    PRIMARY KEY (ITEM_CD)
  );

CREATE INDEX IX_ITEM_KBN
  ON ITEM_MST (ITEM_KBN, ITEM_CD);

-- 02_bom.sql  部品構成マスタ
CREATE TABLE BOM
  (
    PARENT_ITEM_CD  CHAR(8)       NOT NULL,   -- 親品目コード（製品）
    CHILD_ITEM_CD   CHAR(8)       NOT NULL,   -- 子品目コード（部品）
    QTY_PER         DECIMAL(7,2)  NOT NULL,   -- 員数
    YIELD_RATE      DECIMAL(5,2)  NOT NULL,   -- 歩留まり率（％）
    UPD_TMS         TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (PARENT_ITEM_CD, CHILD_ITEM_CD)
  );

ALTER TABLE BOM
  ADD CONSTRAINT FK_BOM_PARENT FOREIGN KEY (PARENT_ITEM_CD)
      REFERENCES ITEM_MST (ITEM_CD);

ALTER TABLE BOM
  ADD CONSTRAINT FK_BOM_CHILD FOREIGN KEY (CHILD_ITEM_CD)
      REFERENCES ITEM_MST (ITEM_CD);

-- 03_stock.sql  在庫
CREATE TABLE STOCK
  (
    ITEM_CD     CHAR(8)        NOT NULL,   -- 品目コード
    STOCK_QTY   DECIMAL(9,0)   NOT NULL,   -- 在庫数量（消耗品区分のみマイナス可）
    UPD_TMS     TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (ITEM_CD)
  );

ALTER TABLE STOCK
  ADD CONSTRAINT FK_STOCK_ITEM FOREIGN KEY (ITEM_CD)
      REFERENCES ITEM_MST (ITEM_CD);

-- 04_work_order.sql  製造指示
CREATE TABLE WORK_ORDER
  (
    WORK_ORDER_NO  CHAR(7)       NOT NULL,   -- 製造指示番号 'W'+年度下2桁+4桁連番
    ITEM_CD        CHAR(8)       NOT NULL,   -- 製造対象品目コード（製品）
    ORDER_QTY      DECIMAL(9,0)  NOT NULL,   -- 指示数量
    DUE_DATE       DATE          NOT NULL,   -- 完成予定日
    STATUS         CHAR(1)       NOT NULL DEFAULT '0',  -- 0:登録済 1:取消済
    CRT_TMS        TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (WORK_ORDER_NO)
  );

ALTER TABLE WORK_ORDER
  ADD CONSTRAINT FK_WKORD_ITEM FOREIGN KEY (ITEM_CD)
      REFERENCES ITEM_MST (ITEM_CD);

-- 05_seq_ctl.sql  採番制御
CREATE TABLE SEQ_CTL
  (
    SEQ_NAME   CHAR(10)    NOT NULL,   -- 採番対象名（例：'WORK_ORDER'）
    SEQ_YY     CHAR(2)     NOT NULL,   -- 年度（西暦下2桁）
    SEQ_NO     INTEGER     NOT NULL,   -- 現在の連番値
    PRIMARY KEY (SEQ_NAME)
  );
