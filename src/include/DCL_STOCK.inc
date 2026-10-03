/*==================================================================*/
/* DCLGEN TABLE(SANSEKI.STOCK)                                      */
/*        LIBRARY(PMPLI.SRC.INCLUDE(DCL_STOCK))                     */
/*        ACTION(REPLACE)                                           */
/*        LANGUAGE(PLI)                                             */
/*        APOST                                                     */
/*  生成日 2015-04-01                                               */
/*==================================================================*/
DCL 1 DCLSTOCK,
      2 ITEM-CD           CHAR(8),
      2 STOCK-QTY         FIXED DEC(9,0),
      2 UPD-TMS           CHAR(26);

DCL SQLSTATE-STOCK CHAR(5);
