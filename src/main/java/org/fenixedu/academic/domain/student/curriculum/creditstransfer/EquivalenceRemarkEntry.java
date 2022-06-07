package org.fenixedu.academic.domain.student.curriculum.creditstransfer;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.organizationalStructure.UnitUtils;
import org.fenixedu.academic.domain.student.curriculum.CreditsReasonType;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

class EquivalenceRemarkEntry extends CreditsTransferRemarkEntry {

    private ListMultimap<Unit, IEnrolment> sourcesByUnit = ArrayListMultimap.create();

    public EquivalenceRemarkEntry(CreditsReasonType reasonType, Class<?> creditsType, Dismissal dismissal) {
        super(reasonType, creditsType, dismissal);

        getDismissal().getCredits().getIEnrolments().stream()
                .sorted(ICurriculumEntry.COMPARATOR_BY_EXECUTION_PERIOD_AND_NAME_AND_ID)
                .forEach(i -> sourcesByUnit.put(i.getAcademicUnit(), i));
    }

    private Dismissal getDismissal() {
        return (Dismissal) getEntry();
    }

    public Multimap<Unit, IEnrolment> getSourcesByUnit() {
        return sourcesByUnit;
    }

    public static EquivalenceRemarkEntry build(Dismissal dismissal) {
        return new EquivalenceRemarkEntry(dismissal.getCredits().getReason(), dismissal.getCredits().getClass(), dismissal);
    }

    @Override
    public boolean matches(CreditsTransferRemarkEntry entry) {
        return toLocalizedString().compareTo(entry.toLocalizedString()) == 0;
    }

    @Override
    public void merge(CreditsTransferRemarkEntry entry) {
        getMergedEntries().add(entry);
    }

    @Override
    protected String toString(Locale locale) {
        final String remarkIdPrefix = getFormattedRemarkId() + (StringUtils.isNotBlank(getFormattedRemarkId()) ? " " : "");

        if (getReasonType() == null) {
            return remarkIdPrefix + AcademicExtensionsUtil.bundleI18N("label.creditsTransfer").getContent(locale);
        }

        if (getReasonType().getInfoHidden()) {
            return "";
        }

        final StringBuilder result = new StringBuilder();
        result.append(remarkIdPrefix);
        result.append(getReasonType().getInfoText().getContent(locale));

        if (getReasonType().getInfoExplained()) {
            final String prefix =
                    AcademicExtensionsUtil.bundleI18N("info.CreditsReasonType.explained.Equivalence").getContent(locale);
            result.append(" - ").append(prefix).append(": ");

            final Map<Unit, Collection<IEnrolment>> sourcesByUnitMap = sourcesByUnit.asMap();
            final String institutionSources = sourcesByUnitMap.keySet().stream().sorted((x, y) -> {
                if (x == UnitUtils.readInstitutionUnit()) {
                    return -1;
                }

                final String leftInstitutionName = Optional.ofNullable(x.getNameI18n().getContent(locale))
                        .orElse(x.getNameI18n().getContent(Locale.getDefault()));
                final String rightInstitutionName = Optional.ofNullable(y.getNameI18n().getContent(locale))
                        .orElse(y.getNameI18n().getContent(Locale.getDefault()));
                return x == UnitUtils.readInstitutionUnit() ? -1 : leftInstitutionName.compareTo(rightInstitutionName);
            }).map(k -> {
                final StringBuilder sourcesBlock = new StringBuilder();
                if (k != UnitUtils.readInstitutionUnit()) {
                    sourcesBlock.append(getFormattedUnit(k, locale, getReasonType().getInfoExplainedWithCountry(),
                            getReasonType().getInfoExplainedWithInstitution())).append(": ");
                }

                sourcesBlock.append(sourcesByUnitMap.get(k).stream().map(d -> {
                    final String formattedEcts = getFormattedEcts(d, locale);
                    return getFormattedName(d).getContent(locale)
                            + (StringUtils.isNotBlank(formattedEcts) ? " " + formattedEcts : "");
                }).collect(Collectors.joining(", ")));

                return sourcesBlock.toString();
            }).collect(Collectors.joining("; "));
            result.append(institutionSources);
        }

        return result.toString();
    }

    private LocalizedString getFormattedName(IEnrolment entry) {
        return entry instanceof Enrolment ? ((Enrolment) entry).getCurricularCourse()
                .getNameI18N(entry.getExecutionInterval()) : entry.getName();
    }
}