package jp.co.sanseki.pm.pm01;

import jp.co.sanseki.pm.common.paging.PageMove;
import jp.co.sanseki.pm.pm01.Pm01Dto.DeleteResponse;
import jp.co.sanseki.pm.pm01.Pm01Dto.ItemRequest;
import jp.co.sanseki.pm.pm01.Pm01Dto.ItemResponse;
import jp.co.sanseki.pm.pm01.Pm01Dto.ListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * PM01 品目マスタ保守 API。
 * <pre>
 * GET    /api/pm01/items?cursor=&amp;pageNo=&amp;move=prev|next   一覧（PF7=prev / PF8=next）
 * GET    /api/pm01/items/{itemCd}                         機能コード1：照会
 * POST   /api/pm01/items                                  機能コード2：登録
 * PUT    /api/pm01/items/{itemCd}                         機能コード3：更新（body.updTms 必須）
 * DELETE /api/pm01/items/{itemCd}?updTms=                 機能コード4：削除（論理削除）
 * </pre>
 */
@RestController
@RequestMapping("/api/pm01/items")
public class Pm01Controller {

    private final Pm01Service service;

    public Pm01Controller(Pm01Service service) {
        this.service = service;
    }

    @GetMapping
    public ListResponse list(@RequestParam(required = false) String cursor,
                             @RequestParam(defaultValue = "1") int pageNo,
                             @RequestParam(required = false) String move) {
        return service.list(PageMove.of(move), cursor, pageNo);
    }

    @GetMapping("/{itemCd}")
    public ItemResponse inquire(@PathVariable String itemCd) {
        return service.inquire(itemCd);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse register(@RequestBody ItemRequest request) {
        return service.register(request);
    }

    @PutMapping("/{itemCd}")
    public ItemResponse update(@PathVariable String itemCd, @RequestBody ItemRequest request) {
        return service.update(itemCd, request);
    }

    @DeleteMapping("/{itemCd}")
    public DeleteResponse delete(@PathVariable String itemCd, @RequestParam(required = false) String updTms) {
        return service.delete(itemCd, updTms);
    }
}
