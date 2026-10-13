------------------------------------------------------------------
-- MSG_MST  メッセージマスタ（移行先で追加）
--   PMMSG.inc（MSG_TABLE: MSG_ID CHAR(4) / MSG_TEXT CHAR(60)）をテーブル化したもの。
--   起動時に PMMSG.inc から投入する。
------------------------------------------------------------------
CREATE TABLE SANSEKI.MSG_MST
  (
    MSG_ID     CHAR(4)      NOT NULL,
    MSG_TEXT   VARCHAR(60)  NOT NULL,
    PRIMARY KEY (MSG_ID)
  );
