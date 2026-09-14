package com.mall.product.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AdminCategoryCreateRequest {
    @NotBlank
    @Size(max = 64)
    private String name;

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "仅支持小写字母、数字和中划线")
    private String code;

    @NotNull
    @Min(0)
    private Integer sortOrder;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
