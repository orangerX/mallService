package com.mall.admin.exam;
import com.mall.admin.security.AdminPrincipal;
import com.mall.common.api.*;
import com.mall.exam.admin.dto.AdminExamRequests.*;
import com.mall.exam.admin.dto.AdminExamResponses.*;
import com.mall.exam.admin.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.mall.exam.importer.ExamImportService;
import com.mall.exam.importer.dto.*;
import com.mall.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Locale;
@RestController @Validated @RequestMapping("/admin/api/exam/imports")
public class AdminExamImportController {
    private static final long MAX_BYTES=5L*1024*1024;
    private final ExamImportService imports;
    public AdminExamImportController(ExamImportService imports) { this.imports=imports; }
    @PostMapping(value="/preview",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportPreviewResponse> preview(@RequestParam @Min(1) long sourceId,
            @RequestParam("file") MultipartFile file,@AuthenticationPrincipal AdminPrincipal admin) {
        if(file.isEmpty() || file.getSize()>MAX_BYTES) throw invalid();
        String name=file.getOriginalFilename();
        if(name==null) throw invalid();
        name=name.replace('\\','/'); name=name.substring(name.lastIndexOf('/')+1);
        if(name.length()>255 || name.indexOf('\0')>=0 || !name.toLowerCase(Locale.ROOT).matches(".+\\.(json|csv)")) throw invalid();
        try { return ApiResponse.success(imports.preview(sourceId,name,file.getBytes(),admin.getAdminId())); }
        catch(IOException e) { throw invalid(); }
    }
    @PostMapping("/commit") public ApiResponse<Void> commit(@Valid @RequestBody ImportCommitRequest body,@AuthenticationPrincipal AdminPrincipal admin) {
        imports.commit(body.getBatchId(),admin.getAdminId());return ApiResponse.success();
    }
    private static BusinessException invalid() { return new BusinessException(HttpStatus.BAD_REQUEST,ErrorCode.VALIDATION_ERROR); }
}
