/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.pay.wechat.nativepay;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Native 下单业务参数。{@code appid}、{@code mchid} 与 {@code notify_url}
 * 由应用上下文注入，避免单次请求误配。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NativePrepayRequest {
    private final String description;
    private final String outTradeNo;
    private final @Nullable OffsetDateTime timeExpire;
    private final @Nullable String attach;
    private final @Nullable String goodsTag;
    private final @Nullable Boolean supportFapiao;
    private final Amount amount;
    private final @Nullable Detail detail;
    private final @Nullable SceneInfo sceneInfo;
    private final @Nullable SettleInfo settleInfo;

    private NativePrepayRequest(Builder builder) {
        description = WechatPayValidationUtils.requireText(
                ValidationUtils.requireNonNull(builder.description, "description is required"),
                "description", 1, 127);
        outTradeNo = WechatPayValidationUtils.requireOutTradeNo(
                ValidationUtils.requireNonNull(builder.outTradeNo, "outTradeNo is required"));
        timeExpire = builder.timeExpire;
        attach = builder.attach;
        goodsTag = builder.goodsTag;
        supportFapiao = builder.supportFapiao;
        amount = new Amount(WechatPayValidationUtils.requirePositive(
                ValidationUtils.requireNonNull(builder.amount, "amount is required"),
                "amount"), "CNY");
        detail = builder.detail;
        sceneInfo = builder.sceneInfo;
        settleInfo = builder.profitSharing == null
                ? null : new SettleInfo(builder.profitSharing);

        if (detail != null) {
            int detailBytes = JsonCodec.defaultCodec().writeBytes(detail).length;
            ValidationUtils.requireTrue(detailBytes <= 6144,
                    "detail must not exceed 6144 compact JSON bytes");
        }
    }

    /**
     * 创建 Native 下单请求构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回商品描述。
     *
     * @return 商品描述
     */
    @JsonProperty("description")
    public String description() {
        return description;
    }

    /**
     * 返回商户订单号。
     *
     * @return 商户订单号
     */
    @JsonProperty("out_trade_no")
    public String outTradeNo() {
        return outTradeNo;
    }

    /**
     * 返回支付结束时间。
     *
     * @return 支付结束时间；未配置时为 {@code null}
     */
    @JsonProperty("time_expire")
    public @Nullable OffsetDateTime timeExpire() {
        return timeExpire;
    }

    /**
     * 返回商户数据包。
     *
     * @return 商户数据包；未配置时为 {@code null}
     */
    @JsonProperty("attach")
    public @Nullable String attach() {
        return attach;
    }

    /**
     * 返回订单优惠标记。
     *
     * @return 订单优惠标记；未配置时为 {@code null}
     */
    @JsonProperty("goods_tag")
    public @Nullable String goodsTag() {
        return goodsTag;
    }

    /**
     * 返回电子发票入口标识。
     *
     * @return 电子发票入口标识；未配置时为 {@code null}
     */
    @JsonProperty("support_fapiao")
    public @Nullable Boolean supportFapiao() {
        return supportFapiao;
    }

    /**
     * 返回订单金额。
     *
     * @return 订单金额
     */
    @JsonProperty("amount")
    public Amount amount() {
        return amount;
    }

    /**
     * 返回商品详情。
     *
     * @return 商品详情；未配置时为 {@code null}
     */
    @JsonProperty("detail")
    public @Nullable Detail detail() {
        return detail;
    }

    /**
     * 返回支付场景信息。
     *
     * @return 场景信息；未配置时为 {@code null}
     */
    @JsonProperty("scene_info")
    public @Nullable SceneInfo sceneInfo() {
        return sceneInfo;
    }

    /**
     * 返回结算信息。
     *
     * @return 结算信息；未配置时为 {@code null}
     */
    @JsonProperty("settle_info")
    public @Nullable SettleInfo settleInfo() {
        return settleInfo;
    }

    /**
     * Native 下单请求构建器。
     */
    public static final class Builder {
        private @Nullable String description;
        private @Nullable String outTradeNo;
        private @Nullable OffsetDateTime timeExpire;
        private @Nullable String attach;
        private @Nullable String goodsTag;
        private @Nullable Boolean supportFapiao;
        private @Nullable Long amount;
        private @Nullable Detail detail;
        private @Nullable SceneInfo sceneInfo;
        private @Nullable Boolean profitSharing;

        private Builder() {
        }

        /**
         * 配置商品描述。
         *
         * @param value 商品描述
         * @return 当前构建器
         */
        public Builder description(String value) {
            description = WechatPayValidationUtils.requireText(value,
                    "description", 1, 127);
            return this;
        }

        /**
         * 配置商户订单号。
         *
         * @param value 商户订单号
         * @return 当前构建器
         */
        public Builder outTradeNo(String value) {
            outTradeNo = WechatPayValidationUtils.requireOutTradeNo(value);
            return this;
        }

        /**
         * 配置支付结束时间。
         *
         * @param value 支付结束时间
         * @return 当前构建器
         */
        public Builder timeExpire(OffsetDateTime value) {
            timeExpire = ValidationUtils.requireNonNull(value,
                    "timeExpire must not be null");
            return this;
        }

        /**
         * 配置商户数据包。
         *
         * @param value 商户数据包
         * @return 当前构建器
         */
        public Builder attach(String value) {
            attach = WechatPayValidationUtils.requireText(value, "attach", 0, 128);
            return this;
        }

        /**
         * 配置订单优惠标记。
         *
         * @param value 订单优惠标记
         * @return 当前构建器
         */
        public Builder goodsTag(String value) {
            goodsTag = WechatPayValidationUtils.requireText(value, "goodsTag", 1, 32);
            return this;
        }

        /**
         * 配置是否开放电子发票入口。
         *
         * @param value 是否开放电子发票入口
         * @return 当前构建器
         */
        public Builder supportFapiao(boolean value) {
            supportFapiao = value;
            return this;
        }

        /**
         * 配置订单总金额。
         *
         * @param value 订单总金额，单位为分
         * @return 当前构建器
         */
        public Builder amount(long value) {
            amount = WechatPayValidationUtils.requirePositive(value, "amount");
            return this;
        }

        /**
         * 配置商品详情。
         *
         * @param value 商品详情
         * @return 当前构建器
         */
        public Builder detail(Detail value) {
            detail = ValidationUtils.requireNonNull(value, "detail must not be null");
            return this;
        }

        /**
         * 配置支付场景信息。
         *
         * @param value 支付场景信息
         * @return 当前构建器
         */
        public Builder sceneInfo(SceneInfo value) {
            sceneInfo = ValidationUtils.requireNonNull(value, "sceneInfo must not be null");
            return this;
        }

        /**
         * 配置是否标记为分账订单。
         *
         * @param value 是否标记为分账订单
         * @return 当前构建器
         */
        public Builder profitSharing(boolean value) {
            profitSharing = value;
            return this;
        }

        /**
         * 校验并创建不可变请求。
         *
         * @return Native 下单请求
         */
        public NativePrepayRequest build() {
            return new NativePrepayRequest(this);
        }
    }

    /**
     * 订单金额。
     *
     * @param total 总金额，单位为分
     * @param currency 固定为 CNY
     */
    public record Amount(
            @JsonProperty("total") long total,
            @JsonProperty("currency") String currency) {
        /**
         * 校验订单金额与币种。
         */
        public Amount {
            WechatPayValidationUtils.requirePositive(total, "amount.total");
            ValidationUtils.requireTrue("CNY".equals(currency),
                    "amount.currency must be CNY");
        }
    }

    /**
     * 可选商品详情。
     *
     * @param costPrice 订单原价
     * @param invoiceId 商品小票 ID
     * @param goodsDetail 单品列表
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detail(
            @JsonProperty("cost_price") @Nullable Long costPrice,
            @JsonProperty("invoice_id") @Nullable String invoiceId,
            @JsonProperty("goods_detail") @Nullable List<GoodsDetail> goodsDetail) {

        /**
         * 校验详情并复制单品列表。
         */
        public Detail {
            if (costPrice != null) {
                WechatPayValidationUtils.requirePositive(costPrice, "costPrice");
            }
            if (invoiceId != null) {
                WechatPayValidationUtils.requireText(invoiceId, "invoiceId", 1, 32);
            }
            if (goodsDetail != null) {
                ValidationUtils.requireNotEmpty(goodsDetail,
                        "goodsDetail must contain at least one item");
                goodsDetail = List.copyOf(goodsDetail);
            }
            ValidationUtils.requireTrue(costPrice != null || invoiceId != null
                            || goodsDetail != null,
                    "detail must contain at least one field");
        }

        /**
         * 创建商品详情构建器。
         *
         * @return 新构建器
         */
        public static DetailBuilder builder() {
            return new DetailBuilder();
        }
    }

    /**
     * 商品详情构建器。
     */
    public static final class DetailBuilder {
        private @Nullable Long costPrice;
        private @Nullable String invoiceId;
        private final List<GoodsDetail> goodsDetail = new ArrayList<>();

        private DetailBuilder() {
        }

        /**
         * 配置订单原价。
         *
         * @param value 订单原价，单位为分
         * @return 当前构建器
         */
        public DetailBuilder costPrice(long value) {
            costPrice = WechatPayValidationUtils.requirePositive(value, "costPrice");
            return this;
        }

        /**
         * 配置商品小票 ID。
         *
         * @param value 商品小票 ID
         * @return 当前构建器
         */
        public DetailBuilder invoiceId(String value) {
            invoiceId = WechatPayValidationUtils.requireText(value, "invoiceId", 1, 32);
            return this;
        }

        /**
         * 追加一项单品信息。
         *
         * @param value 待追加单品
         * @return 当前构建器
         */
        public DetailBuilder addGoodsDetail(GoodsDetail value) {
            goodsDetail.add(ValidationUtils.requireNonNull(value,
                    "goodsDetail must not be null"));
            return this;
        }

        /**
         * 校验并创建商品详情。
         *
         * @return 不可变商品详情
         */
        public Detail build() {
            return new Detail(costPrice, invoiceId,
                    goodsDetail.isEmpty() ? null : List.copyOf(goodsDetail));
        }
    }

    /**
     * 单品信息。
     *
     * @param merchantGoodsId 商户侧商品编码
     * @param wechatpayGoodsId 微信支付商品编码
     * @param goodsName 商品名称
     * @param quantity 商品数量
     * @param unitPrice 商品单价，单位为分
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GoodsDetail(
            @JsonProperty("merchant_goods_id") String merchantGoodsId,
            @JsonProperty("wechatpay_goods_id") @Nullable String wechatpayGoodsId,
            @JsonProperty("goods_name") @Nullable String goodsName,
            @JsonProperty("quantity") long quantity,
            @JsonProperty("unit_price") long unitPrice) {

        /**
         * 校验单品信息。
         */
        public GoodsDetail {
            merchantGoodsId = WechatPayValidationUtils.requireMerchantGoodsId(
                    merchantGoodsId);
            if (wechatpayGoodsId != null) {
                WechatPayValidationUtils.requireText(wechatpayGoodsId,
                        "wechatpayGoodsId", 1, 32);
            }
            if (goodsName != null) {
                WechatPayValidationUtils.requireText(goodsName, "goodsName", 1, 256);
            }
            WechatPayValidationUtils.requirePositive(quantity, "quantity");
            WechatPayValidationUtils.requirePositive(unitPrice, "unitPrice");
        }

        /**
         * 创建单品信息构建器。
         *
         * @return 新构建器
         */
        public static GoodsDetailBuilder builder() {
            return new GoodsDetailBuilder();
        }
    }

    /**
     * 单品信息构建器。
     */
    public static final class GoodsDetailBuilder {
        private @Nullable String merchantGoodsId;
        private @Nullable String wechatpayGoodsId;
        private @Nullable String goodsName;
        private @Nullable Long quantity;
        private @Nullable Long unitPrice;

        private GoodsDetailBuilder() {
        }

        /**
         * 配置商户侧商品编码。
         *
         * @param value 商户侧商品编码
         * @return 当前构建器
         */
        public GoodsDetailBuilder merchantGoodsId(String value) {
            merchantGoodsId = value;
            return this;
        }

        /**
         * 配置微信支付商品编码。
         *
         * @param value 微信支付商品编码
         * @return 当前构建器
         */
        public GoodsDetailBuilder wechatpayGoodsId(String value) {
            wechatpayGoodsId = value;
            return this;
        }

        /**
         * 配置商品名称。
         *
         * @param value 商品名称
         * @return 当前构建器
         */
        public GoodsDetailBuilder goodsName(String value) {
            goodsName = value;
            return this;
        }

        /**
         * 配置商品数量。
         *
         * @param value 商品数量
         * @return 当前构建器
         */
        public GoodsDetailBuilder quantity(long value) {
            quantity = value;
            return this;
        }

        /**
         * 配置商品单价。
         *
         * @param value 商品单价，单位为分
         * @return 当前构建器
         */
        public GoodsDetailBuilder unitPrice(long value) {
            unitPrice = value;
            return this;
        }

        /**
         * 校验并创建单品信息。
         *
         * @return 不可变单品信息
         */
        public GoodsDetail build() {
            return new GoodsDetail(
                    ValidationUtils.requireNonNull(merchantGoodsId,
                            "merchantGoodsId is required"),
                    wechatpayGoodsId, goodsName,
                    ValidationUtils.requireNonNull(quantity, "quantity is required"),
                    ValidationUtils.requireNonNull(unitPrice, "unitPrice is required"));
        }
    }

    /**
     * 支付场景信息。
     *
     * @param payerClientIp 用户终端 IP
     * @param deviceId 商户端设备号
     * @param storeInfo 门店信息
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SceneInfo(
            @JsonProperty("payer_client_ip") String payerClientIp,
            @JsonProperty("device_id") @Nullable String deviceId,
            @JsonProperty("store_info") @Nullable StoreInfo storeInfo) {

        /**
         * 校验场景信息。
         */
        public SceneInfo {
            payerClientIp = WechatPayValidationUtils.requireIpAddress(
                    payerClientIp, "payerClientIp");
            if (deviceId != null) {
                WechatPayValidationUtils.requireText(deviceId, "deviceId", 1, 32);
            }
        }

        /**
         * 创建场景信息构建器。
         *
         * @return 新构建器
         */
        public static SceneInfoBuilder builder() {
            return new SceneInfoBuilder();
        }
    }

    /**
     * 场景信息构建器。
     */
    public static final class SceneInfoBuilder {
        private @Nullable String payerClientIp;
        private @Nullable String deviceId;
        private @Nullable StoreInfo storeInfo;

        private SceneInfoBuilder() {
        }

        /**
         * 配置用户终端 IP。
         *
         * @param value 用户终端 IP
         * @return 当前构建器
         */
        public SceneInfoBuilder payerClientIp(String value) {
            payerClientIp = value;
            return this;
        }

        /**
         * 配置商户端设备号。
         *
         * @param value 商户端设备号
         * @return 当前构建器
         */
        public SceneInfoBuilder deviceId(String value) {
            deviceId = value;
            return this;
        }

        /**
         * 配置门店信息。
         *
         * @param value 门店信息
         * @return 当前构建器
         */
        public SceneInfoBuilder storeInfo(StoreInfo value) {
            storeInfo = ValidationUtils.requireNonNull(value, "storeInfo must not be null");
            return this;
        }

        /**
         * 校验并创建场景信息。
         *
         * @return 不可变场景信息
         */
        public SceneInfo build() {
            return new SceneInfo(ValidationUtils.requireNonNull(payerClientIp,
                    "payerClientIp is required"), deviceId, storeInfo);
        }
    }

    /**
     * 商户门店信息。
     *
     * @param id 门店编号
     * @param name 门店名称
     * @param areaCode 地区编码
     * @param address 详细地址
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StoreInfo(
            @JsonProperty("id") String id,
            @JsonProperty("name") @Nullable String name,
            @JsonProperty("area_code") @Nullable String areaCode,
            @JsonProperty("address") @Nullable String address) {

        /**
         * 校验门店信息。
         */
        public StoreInfo {
            id = WechatPayValidationUtils.requireText(id, "storeInfo.id", 1, 32);
            if (name != null) {
                WechatPayValidationUtils.requireText(name, "storeInfo.name", 1, 256);
            }
            if (areaCode != null) {
                WechatPayValidationUtils.requireText(areaCode,
                        "storeInfo.areaCode", 1, 32);
            }
            if (address != null) {
                WechatPayValidationUtils.requireText(address,
                        "storeInfo.address", 1, 512);
            }
        }

        /**
         * 创建门店信息构建器。
         *
         * @return 新构建器
         */
        public static StoreInfoBuilder builder() {
            return new StoreInfoBuilder();
        }
    }

    /**
     * 门店信息构建器。
     */
    public static final class StoreInfoBuilder {
        private @Nullable String id;
        private @Nullable String name;
        private @Nullable String areaCode;
        private @Nullable String address;

        private StoreInfoBuilder() {
        }

        /**
         * 配置门店编号。
         *
         * @param value 门店编号
         * @return 当前构建器
         */
        public StoreInfoBuilder id(String value) {
            id = value;
            return this;
        }

        /**
         * 配置门店名称。
         *
         * @param value 门店名称
         * @return 当前构建器
         */
        public StoreInfoBuilder name(String value) {
            name = value;
            return this;
        }

        /**
         * 配置地区编码。
         *
         * @param value 地区编码
         * @return 当前构建器
         */
        public StoreInfoBuilder areaCode(String value) {
            areaCode = value;
            return this;
        }

        /**
         * 配置门店详细地址。
         *
         * @param value 门店详细地址
         * @return 当前构建器
         */
        public StoreInfoBuilder address(String value) {
            address = value;
            return this;
        }

        /**
         * 校验并创建门店信息。
         *
         * @return 不可变门店信息
         */
        public StoreInfo build() {
            return new StoreInfo(ValidationUtils.requireNonNull(id,
                    "storeInfo.id is required"), name, areaCode, address);
        }
    }

    /**
     * 结算信息。
     *
     * @param profitSharing 是否为分账订单
     */
    public record SettleInfo(@JsonProperty("profit_sharing") boolean profitSharing) {
    }
}
