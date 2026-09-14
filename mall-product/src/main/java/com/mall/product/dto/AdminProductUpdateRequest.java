package com.mall.product.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AdminProductUpdateRequest {
    @NotNull @Min(1)
    private Long productId;
    @NotNull @Min(1)
    private Long categoryId;
    @NotBlank @Size(max = 128)
    private String name;
    @NotBlank @Size(max = 255)
    private String subtitle;
    @NotBlank @Size(max = 255)
    @Pattern(regexp = "^/images/.+", message = "必须是 /images/ 下的静态图片路径")
    private String imagePath;
    @NotNull @Min(0)
    private Integer sortOrder;
    @NotNull @Min(0) @Max(1)
    private Integer status;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
