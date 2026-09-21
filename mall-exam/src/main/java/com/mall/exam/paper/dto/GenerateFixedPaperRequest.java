package com.mall.exam.paper.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class GenerateFixedPaperRequest {
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]{0,63}")
    public String paperCode;
}
