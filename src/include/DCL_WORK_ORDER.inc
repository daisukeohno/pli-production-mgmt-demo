/*==================================================================*/
/* DCLGEN TABLE(SANSEKI.WORK_ORDER)                                 */
/*        LIBRARY(PMPLI.SRC.INCLUDE(DCL_WORK_ORDER))                */
/*        ACTION(REPLACE)                                           */
/*        LANGUAGE(PLI)                                             */
/*        APOST                                                     */
/*  生成日 2015-04-01  ※STATUS列は2009-12-10追加分を手動反映（佐藤）    */
/*==================================================================*/
DCL 1 DCLWORK-ORDER,
      2 WORK-ORDER-NO     CHAR(7),
      2 ITEM-CD           CHAR(8),
      2 ORDER-QTY         FIXED DEC(9,0),
      2 DUE-DATE          CHAR(10),
      2 STATUS            CHAR(1),
      2 CRT-TMS           CHAR(26);

DCL SQLSTATE-WORK-ORDER CHAR(5);
