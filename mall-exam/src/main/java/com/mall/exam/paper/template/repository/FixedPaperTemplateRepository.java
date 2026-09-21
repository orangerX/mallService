package com.mall.exam.paper.template.repository;

import com.mall.exam.paper.template.dto.FixedPaperSummary;
import com.mall.exam.paper.template.mapper.FixedPaperTemplateMapper;
import com.mall.exam.paper.template.model.FixedPaperTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Server-side templates are deliberately separate from student attempt responses. */
@Repository
public class FixedPaperTemplateRepository {
    private final FixedPaperTemplateMapper mapper;

    public FixedPaperTemplateRepository(FixedPaperTemplateMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Loads the latest published version and all its frozen snapshots in section order.
     * A disabled newer publication suppresses older versions; a draft does not.
     */
    @Transactional(readOnly = true)
    public Optional<FixedPaperTemplate> findPublished(String paperCode) {
        if (paperCode == null || paperCode.isBlank()) {
            return Optional.empty();
        }
        FixedPaperTemplate template = mapper.findPublished(paperCode);
        if (template == null) {
            return Optional.empty();
        }
        return Optional.of(template.withSnapshots(
                mapper.findGroups(template.getId()), mapper.findItems(template.getId())));
    }

    /** Lists one selectable publication per stable paper code without answer material. */
    @Transactional(readOnly = true)
    public List<FixedPaperSummary> listPublished() {
        return List.copyOf(mapper.listPublished());
    }
}
