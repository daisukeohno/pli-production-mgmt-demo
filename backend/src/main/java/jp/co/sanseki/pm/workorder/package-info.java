/**
 * PM03 製造指示登録（WORK_ORDER / BOM / SEQ_CTL）。
 * <p>
 * 層構成：{@code WorkOrderController} → {@code WorkOrderService} → {@link jp.co.sanseki.pm.workorder.WorkOrderRepository}、
 * {@link jp.co.sanseki.pm.workorder.BomRepository}、{@link jp.co.sanseki.pm.workorder.SeqCtlRepository}。
 * Controller / Service は PM03 のチケットで docs/migration/api-contract.md に従って実装する。
 */
package jp.co.sanseki.pm.workorder;
