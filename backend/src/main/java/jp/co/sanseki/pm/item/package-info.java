/**
 * PM01 品目マスタ保守（ITEM_MST）。
 * <p>
 * 層構成：{@code ItemController}（REST）→ {@code ItemService}（業務ルール・トランザクション）→ {@link jp.co.sanseki.pm.item.ItemRepository}（SQL）。
 * Controller / Service は PM01 のチケットで docs/migration/api-contract.md に従って実装する。
 */
package jp.co.sanseki.pm.item;
