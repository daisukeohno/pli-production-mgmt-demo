package jp.co.sanseki.pm.controller;

import jakarta.validation.Valid;
import jp.co.sanseki.pm.common.message.MessageCatalog;
import jp.co.sanseki.pm.common.message.MessageIds;
import jp.co.sanseki.pm.common.message.MessageResponse;
import jp.co.sanseki.pm.common.paging.CursorPage;
import jp.co.sanseki.pm.common.paging.PageMove;
import jp.co.sanseki.pm.common.validation.ValidItemCd;
import jp.co.sanseki.pm.dto.ItemCreateRequest;
import jp.co.sanseki.pm.dto.ItemListRow;
import jp.co.sanseki.pm.dto.ItemResponse;
import jp.co.sanseki.pm.dto.ItemUpdateRequest;
import jp.co.sanseki.pm.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PM01 品目マスタ保守 API。機能コード（F-FUNC）分岐を資源指向へ置き換えたもの。
 * <ul>
 *   <li>一覧（F-LIST01〜10・PF7/PF8）: GET /api/items?move=first|current|next|prev&amp;top=&amp;pageNo=</li>
 *   <li>1:照会 GET /api/items/{itemCd} / 2:登録 POST / 3:更新 PUT / 4:削除 DELETE（論理削除）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;
    private final MessageCatalog catalog;

    public ItemController(ItemService service, MessageCatalog catalog) {
        this.service = service;
        this.catalog = catalog;
    }

    @GetMapping
    public CursorPage<ItemListRow> list(@RequestParam(required = false) String move,
                                        @RequestParam(required = false) String top,
                                        @RequestParam(defaultValue = "1") int pageNo,
                                        @RequestParam(defaultValue = "" + ItemService.PAGE_SIZE) int size) {
        return service.list(PageMove.parse(move), top, pageNo, Math.min(Math.max(size, 1), 100));
    }

    @GetMapping("/{itemCd}")
    public ItemResponse find(@PathVariable String itemCd) {
        return service.find(itemCd);
    }

    @PostMapping
    public ResponseEntity<MessageResponse<ItemResponse>> create(@RequestBody @Valid ItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(result(MessageIds.M005, service.create(request)));
    }

    @PutMapping("/{itemCd}")
    public MessageResponse<ItemResponse> update(@PathVariable @ValidItemCd String itemCd,
                                                @RequestBody @Valid ItemUpdateRequest request) {
        return result(MessageIds.M006, service.update(itemCd, request));
    }

    @DeleteMapping("/{itemCd}")
    public MessageResponse<Void> delete(@PathVariable String itemCd,
                                        @RequestParam(required = false) String updTms) {
        service.delete(itemCd, updTms);
        return result(MessageIds.M007, null);
    }

    private <T> MessageResponse<T> result(String messageId, T data) {
        return new MessageResponse<>(messageId, catalog.text(messageId), data);
    }
}
