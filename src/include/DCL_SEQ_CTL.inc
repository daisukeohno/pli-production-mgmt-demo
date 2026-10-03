/*==================================================================*/
/* DCLGEN TABLE(SANSEKI.SEQ_CTL)                                    */
/*        LIBRARY(PMPLI.SRC.INCLUDE(DCL_SEQ_CTL))                   */
/*        ACTION(REPLACE)                                           */
/*        LANGUAGE(PLI)                                             */
/*        APOST                                                     */
/*  生成日 2000-03-01                                               */
/*==================================================================*/
DCL 1 DCLSEQ-CTL,
      2 SEQ-NAME          CHAR(10),
      2 SEQ-YY            CHAR(2),
      2 SEQ-NO            FIXED BIN(31);

DCL SQLSTATE-SEQ-CTL CHAR(5);
