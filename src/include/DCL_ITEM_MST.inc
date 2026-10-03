/*==================================================================*/
/* DCLGEN TABLE(SANSEKI.ITEM_MST)                                   */
/*        LIBRARY(PMPLI.SRC.INCLUDE(DCL_ITEM_MST))                  */
/*        ACTION(REPLACE)                                           */
/*        LANGUAGE(PLI)                                             */
/*        APOST                                                     */
/*  生成日 2015-04-01  ※以降の列追加は手動反映（田中）                 */
/*==================================================================*/
DCL 1 DCLITEM-MST,
      2 ITEM-CD           CHAR(8),
      2 ITEM-NAME         CHAR(20),
      2 ITEM-KBN          CHAR(1),
      2 STOCK-UNIT        CHAR(4),
      2 DEL-FLG           CHAR(1),
      2 UPD-TMS           CHAR(26),
      2 CRT-TMS           CHAR(26);

/* 品目区分コード値 */
DCL ITEM-KBN-PRODUCT  CHAR(1) STATIC INIT('1');  /* 製品   */
DCL ITEM-KBN-PART     CHAR(1) STATIC INIT('2');  /* 部品   */
DCL ITEM-KBN-SUPPLY   CHAR(1) STATIC INIT('9');  /* 消耗品 */

DCL SQLSTATE-ITEM-MST CHAR(5);
