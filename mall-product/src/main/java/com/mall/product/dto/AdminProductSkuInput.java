package com.mall.product.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class AdminProductSkuInput {
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)*$", message = "仅支持字母、数字、中划线和下划线")
    private String skuCode;

    @NotBlank
    @Size(max = 64)
    private String skuName;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @NotBlank
    @Size(max = 32)
    private String unit;

    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    private Boolean defaultSku;

    @NotNull
    @Min(0)
    private Integer sortOrder;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;

    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Boolean getDefaultSku() { return defaultSku; }
    public void setDefaultSku(Boolean defaultSku) { this.defaultSku = defaultSku; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
