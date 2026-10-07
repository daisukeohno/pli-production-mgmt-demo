-- db2/ddl/01〜05 の移植（H2 / PostgreSQL 共通）
-- ・スキーマ SANSEKI、列名・型・桁・NOT NULL・既定値・主キー・外部キー・索引は DB2 定義のまま
-- ・CHAR(n) は固定長のまま、DEC(p,s) は DECIMAL(p,s)、TIMESTAMP は DB2 と同じマイクロ秒精度 TIMESTAMP(6)
-- ・DB2 固有の表スペース指定（IN SANSEKI.TS00n）は対象外
-- ・索引名はスキーマ修飾しない（PostgreSQL の索引は表と同じスキーマに作られる）

CREATE SCHEMA SANSEKI;

-- ITEM_MST  品目マスタ（db2/ddl/01_item_mst.sql）
CREATE TABLE SANSEKI.ITEM_MST
  (
    ITEM_CD      CHAR(8)        NOT NULL,   -- 品目コード（8桁、末尾チェックデジット）
    ITEM_NAME    CHAR(20)       NOT NULL,   -- 品目名
    ITEM_KBN     CHAR(1)        NOT NULL,   -- 品目区分 1:製品 2:部品 9:消耗品
    STOCK_UNIT   CHAR(4)        NOT NULL,   -- 単位（個、KG等）
    DEL_FLG      CHAR(1)        DEFAULT '0' NOT NULL,  -- 削除フラグ 0:有効 1:削除済（論理削除）
    UPD_TMS      TIMESTAMP(6)   NOT NULL,   -- 更新タイムスタンプ（楽観的排他制御キー）
    CRT_TMS      TIMESTAMP(6)   NOT NULL,   -- 作成タイムスタンプ
    PRIMARY KEY (ITEM_CD)
  );

CREATE INDEX IX_ITEM_KBN
  ON SANSEKI.ITEM_MST (ITEM_KBN, ITEM_CD);

-- BOM  部品構成マスタ（db2/ddl/02_bom.sql）
CREATE TABLE SANSEKI.BOM
  (
    PARENT_ITEM_CD  CHAR(8)       NOT NULL,   -- 親品目コード（製品）
    CHILD_ITEM_CD   CHAR(8)       NOT NULL,   -- 子品目コード（部品）
    QTY_PER         DECIMAL(7,2)  NOT NULL,   -- 員数
    YIELD_RATE      DECIMAL(5,2)  NOT NULL,   -- 歩留まり率（％）例）95.00
    UPD_TMS         TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (PARENT_ITEM_CD, CHILD_ITEM_CD)
  );

ALTER TABLE SANSEKI.BOM
  ADD CONSTRAINT FK_BOM_PARENT FOREIGN KEY (PARENT_ITEM_CD)
      REFERENCES SANSEKI.ITEM_MST (ITEM_CD);

ALTER TABLE SANSEKI.BOM
  ADD CONSTRAINT FK_BOM_CHILD FOREIGN KEY (CHILD_ITEM_CD)
      REFERENCES SANSEKI.ITEM_MST (ITEM_CD);

-- STOCK  在庫（db2/ddl/03_stock.sql）
CREATE TABLE SANSEKI.STOCK
  (
    ITEM_CD     CHAR(8)        NOT NULL,   -- 品目コード
    STOCK_QTY   DECIMAL(9,0)   NOT NULL,   -- 在庫数量（消耗品区分のみマイナス可）
    UPD_TMS     TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (ITEM_CD)
  );

ALTER TABLE SANSEKI.STOCK
  ADD CONSTRAINT FK_STOCK_ITEM FOREIGN KEY (ITEM_CD)
      REFERENCES SANSEKI.ITEM_MST (ITEM_CD);

-- WORK_ORDER  製造指示（db2/ddl/04_work_order.sql）
CREATE TABLE SANSEKI.WORK_ORDER
  (
    WORK_ORDER_NO  CHAR(7)       NOT NULL,   -- 製造指示番号 'W'+西暦下2桁+4桁連番（カタログ D-04）
    ITEM_CD        CHAR(8)       NOT NULL,   -- 製造対象品目コード（製品）
    ORDER_QTY      DECIMAL(9,0)  NOT NULL,   -- 指示数量
    DUE_DATE       DATE          NOT NULL,   -- 完成予定日
    STATUS         CHAR(1)       DEFAULT '0' NOT NULL,  -- 0:登録済 1:取消済
    CRT_TMS        TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (WORK_ORDER_NO)
  );

ALTER TABLE SANSEKI.WORK_ORDER
  ADD CONSTRAINT FK_WKORD_ITEM FOREIGN KEY (ITEM_CD)
      REFERENCES SANSEKI.ITEM_MST (ITEM_CD);

-- SEQ_CTL  採番制御（db2/ddl/05_seq_ctl.sql）
CREATE TABLE SANSEKI.SEQ_CTL
  (
    SEQ_NAME   CHAR(10)    NOT NULL,   -- 採番対象名（例：'WORK_ORDER'）
    SEQ_YY     CHAR(2)     NOT NULL,   -- 年（西暦下2桁）
    SEQ_NO     INTEGER     NOT NULL,   -- 現在の連番値
    PRIMARY KEY (SEQ_NAME)
  );
