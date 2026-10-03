/*==================================================================*/
/* DCLGEN TABLE(SANSEKI.BOM)                                        */
/*        LIBRARY(PMPLI.SRC.INCLUDE(DCL_BOM))                       */
/*        ACTION(REPLACE)                                           */
/*        LANGUAGE(PLI)                                             */
/*        APOST                                                     */
/*  生成日 2015-04-01                                               */
/*==================================================================*/
DCL 1 DCLBOM,
      2 PARENT-ITEM-CD    CHAR(8),
      2 CHILD-ITEM-CD     CHAR(8),
      2 QTY-PER           FIXED DEC(7,2),
      2 YIELD-RATE        FIXED DEC(5,2),
      2 UPD-TMS           CHAR(26);

DCL SQLSTATE-BOM CHAR(5);
