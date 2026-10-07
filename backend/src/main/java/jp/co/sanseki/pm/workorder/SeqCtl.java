package jp.co.sanseki.pm.workorder;

/** SANSEKI.SEQ_CTL の 1 行。SEQ_YY は西暦下 2 桁、SEQ_NO は現在の連番値。 */
public record SeqCtl(String seqName, String seqYy, int seqNo) {

    public static final String WORK_ORDER = "WORK_ORDER";
}
