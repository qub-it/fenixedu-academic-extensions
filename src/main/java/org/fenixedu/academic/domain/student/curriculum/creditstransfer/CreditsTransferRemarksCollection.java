package org.fenixedu.academic.domain.student.curriculum.creditstransfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.i18n.LocalizedString.Builder;

public class CreditsTransferRemarksCollection {

    private static final Comparator<ICurriculumEntry> CURRICULUM_ENTRY_COMPARATOR_BY_CODE = (x, y) -> {

        if (StringUtils.isBlank(x.getCode()) && StringUtils.isBlank(y.getCode())) {
            return 0;
        }

        if (StringUtils.isBlank(x.getCode())) {
            return 1;
        }

        if (StringUtils.isBlank(y.getCode())) {
            return -1;
        }

        return x.getCode().compareTo(y.getCode());
    };

    private char nextRemarkId = 'a';

    private Collection<CreditsTransferRemarkEntry> entries = new LinkedHashSet<>();

    private CreditsTransferRemarksCollection() {

    }

    private void addEntry(CreditsTransferRemarkEntry entry) {
        if (entry != CreditsTransferRemarkEntry.EMPTY && !entry.toLocalizedString().isEmpty()) {
            final CreditsTransferRemarkEntry existingEntry =
                    entries.stream().filter(e -> e.matches(entry)).findFirst().orElse(null);
            if (existingEntry != null) {
                existingEntry.merge(entry);
            } else {
                this.entries.add(entry);
            }
        }
    }

    private void sortAndAssignRemarkIds(Collection<ICurriculumEntry> curriculumEntries) {
        final List<ICurriculumEntry> sortedCurriculumEntries = new ArrayList<>(curriculumEntries);
        final Comparator<CreditsTransferRemarkEntry> comparator = (left, right) -> {
            final Integer leftMinIndex = left.getAllCurriculumEntries().stream().map(e -> sortedCurriculumEntries.indexOf(e))
                    .min(Comparator.naturalOrder()).orElse(0);
            final Integer rightMinIndex = right.getAllCurriculumEntries().stream().map(e -> sortedCurriculumEntries.indexOf(e))
                    .min(Comparator.naturalOrder()).orElse(0);

            return leftMinIndex.compareTo(rightMinIndex);
        };

        this.entries = this.entries.stream().sorted(comparator).collect(Collectors.toCollection(LinkedHashSet::new));
        this.entries.forEach(e -> e.setRemarkId(String.valueOf(nextRemarkId++)));
    }

    public String getRemarkIdsFor(ICurriculumEntry entry) {
        return entries.stream().filter(e -> e.appliesTo(entry)).map(e -> e.getFormattedRemarkId())
                .collect(Collectors.joining(" "));
    }

    public Collection<String> getRemarkIds() {
        return this.entries.stream().map(e -> e.getRemarkId()).sorted().collect(Collectors.toList());
    }

    public LocalizedString getRemarkTextForId(String remarkId) {
        return this.entries.stream().filter(e -> Objects.equals(e.getRemarkId(), remarkId)).findFirst()
                .map(r -> r.toLocalizedString()).orElse(null);
    }

    public LocalizedString getFormattedRemarks(String separator) {
        final Builder builder = new Builder();
        this.entries.stream().forEach(e -> builder.append(e.toLocalizedString()).append(separator));

        return builder.build();
    }

    public static CreditsTransferRemarksCollection build(final Collection<ICurriculumEntry> curriculumEntries,
            final StudentCurricularPlan studentCurricularPlan) {
        final CreditsTransferRemarksCollection remarks = new CreditsTransferRemarksCollection();
        curriculumEntries.stream().sorted(CURRICULUM_ENTRY_COMPARATOR_BY_CODE)
                .flatMap(e -> CreditsTransferRemarkEntry.buildEntries(e, studentCurricularPlan).stream())
                .forEach(re -> remarks.addEntry(re));
        remarks.sortAndAssignRemarkIds(curriculumEntries);

        return remarks;
    }
}