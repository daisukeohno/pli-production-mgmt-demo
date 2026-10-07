package jp.co.sanseki.pm.workorder;

import java.util.Optional;

import jp.co.sanseki.pm.common.Chars;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SeqCtlRepository {

    static final RowMapper<SeqCtl> ROW_MAPPER = (rs, i) -> new SeqCtl(
            Chars.trimPadding(rs.getString("SEQ_NAME")),
            Chars.trimPadding(rs.getString("SEQ_YY")),
            rs.getInt("SEQ_NO"));

    private final JdbcClient jdbc;

    public SeqCtlRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SeqCtl> findBySeqName(String seqName) {
        return jdbc.sql("SELECT SEQ_NAME, SEQ_YY, SEQ_NO FROM SANSEKI.SEQ_CTL WHERE SEQ_NAME = :seqName")
                .param("seqName", seqName)
                .query(ROW_MAPPER)
                .optional();
    }
}
