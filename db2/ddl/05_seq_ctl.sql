------------------------------------------------------------------
-- SEQ_CTL  採番制御
--
-- 変更履歴
--   2000-03-01  初版作成   SE  高橋
------------------------------------------------------------------
CREATE TABLE SANSEKI.SEQ_CTL
  (
    SEQ_NAME   CHAR(10)    NOT NULL,   -- 採番対象名（例：'WORK_ORDER'）
    SEQ_YY     CHAR(2)     NOT NULL,   -- 年度（西暦下2桁）
    SEQ_NO     INTEGER     NOT NULL,   -- 現在の連番値
    PRIMARY KEY (SEQ_NAME)
  )
  IN SANSEKI.TS005;
