package jp.co.sanseki.pm.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/** SANSEKI.ITEM_MST 品目マスタ（DCL_ITEM_MST.inc 相当）。CHAR 列は DB 上で右側空白パディングされる。 */
@Entity
@Table(schema = "SANSEKI", name = "ITEM_MST")
public class ItemMst implements Persistable<String> {

    public static final String DEL_FLG_ACTIVE = "0";
    public static final String DEL_FLG_DELETED = "1";

    @Id
    @Column(name = "ITEM_CD", length = 8, columnDefinition = "CHAR(8)")
    private String itemCd;

    @Column(name = "ITEM_NAME", length = 20, columnDefinition = "CHAR(20)")
    private String itemName;

    @Column(name = "ITEM_KBN", length = 1, columnDefinition = "CHAR(1)")
    private String itemKbn;

    @Column(name = "STOCK_UNIT", length = 4, columnDefinition = "CHAR(4)")
    private String stockUnit;

    @Column(name = "DEL_FLG", length = 1, columnDefinition = "CHAR(1)")
    private String delFlg;

    @Column(name = "UPD_TMS")
    private LocalDateTime updTms;

    @Column(name = "CRT_TMS")
    private LocalDateTime crtTms;

    @Transient
    private boolean isNew;

    protected ItemMst() {
    }

    public static ItemMst newItem(String itemCd, String itemName, String itemKbn, String stockUnit, LocalDateTime now) {
        ItemMst item = new ItemMst();
        item.itemCd = itemCd;
        item.itemName = itemName;
        item.itemKbn = itemKbn;
        item.stockUnit = stockUnit;
        item.delFlg = DEL_FLG_ACTIVE;
        item.updTms = now;
        item.crtTms = now;
        item.isNew = true;
        return item;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    @Override
    public String getId() {
        return itemCd;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getItemCd() {
        return itemCd;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemKbn() {
        return itemKbn;
    }

    public String getStockUnit() {
        return stockUnit;
    }

    public String getDelFlg() {
        return delFlg;
    }

    public LocalDateTime getUpdTms() {
        return updTms;
    }

    public LocalDateTime getCrtTms() {
        return crtTms;
    }
}
