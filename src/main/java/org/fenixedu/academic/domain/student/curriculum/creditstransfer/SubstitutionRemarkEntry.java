package org.fenixedu.academic.domain.student.curriculum.creditstransfer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.curriculum.CreditsReasonType;
import org.fenixedu.academic.domain.student.curriculum.CurriculumLineServices;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.ExternalEnrolment;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;

class SubstitutionRemarkEntry extends CreditsTransferRemarkEntry {

    private Unit unit;

    private Collection<Dismissal> dismissals = new LinkedHashSet<Dismissal>();

    public SubstitutionRemarkEntry(CreditsReasonType reasonType, Class<?> creditsType, ICurriculumEntry entry,
            Dismissal dismissal) {
        super(reasonType, creditsType, entry);
        dismissals.add(dismissal);
    }

    public static SubstitutionRemarkEntry build(ICurriculumEntry entry, final Dismissal dismissal) {
        final SubstitutionRemarkEntry result = new SubstitutionRemarkEntry(dismissal.getCredits().getReason(),
                dismissal.getCredits().getClass(), entry, dismissal);

        if (entry instanceof ExternalEnrolment) {
            result.unit = ((ExternalEnrolment) entry).getAcademicUnit();
        }

        return result;

    }

    public Unit getUnit() {
        return unit;
    }

    public Collection<Dismissal> getDismissals() {
        return dismissals;
    }

    @Override
    protected String toString(Locale locale) {
        final String remarkIdPrefix = getFormattedRemarkId() + (StringUtils.isNotBlank(getFormattedRemarkId()) ? " " : "");

        if (getReasonType() == null) {
            final String formattedUnit = getFormattedUnit(unit, locale, true, true);
            return remarkIdPrefix + AcademicExtensionsUtil.bundleI18N("label.creditsTransfer").getContent(locale)
                    + (StringUtils.isNotBlank(formattedUnit) ? ", " + formattedUnit : "");
        }

        if (getReasonType().getInfoHidden()) {
            return "";
        }

        final StringBuilder result = new StringBuilder();
        result.append(remarkIdPrefix);
        result.append(getFormattedReason(locale));
        final String formattedUnit = getFormattedUnit(unit, locale, getReasonType().getInfoExplainedWithCountry(),
                getReasonType().getInfoExplainedWithInstitution());
        result.append(StringUtils.isNotBlank(formattedUnit) ? ", " + formattedUnit : "");

        if (getReasonType().getInfoExplained()) {
            final String prefix =
                    AcademicExtensionsUtil.bundleI18N("info.CreditsReasonType.explained.Substitution").getContent(locale);
            result.append(" - ").append(prefix).append(": ");
            result.append(dismissals.stream().sorted(CurriculumLineServices.COMPARATOR).map(d -> {
                final String formattedEcts = getFormattedEcts(d, locale);
                return d.getName().getContent(locale) + (StringUtils.isNotBlank(formattedEcts) ? " " + formattedEcts : "");
            }).collect(Collectors.joining(", ")));
        }

        return result.toString();
    }

    @Override
    public boolean matches(CreditsTransferRemarkEntry entry) {
        if (entry instanceof SubstitutionRemarkEntry && getReasonType() != null && getReasonType().getInfoExplained()) {
            final SubstitutionRemarkEntry otherEntry = (SubstitutionRemarkEntry) entry;

            final Set<IEnrolment> sources =
                    dismissals.stream().flatMap(d -> d.getCredits().getIEnrolments().stream()).collect(Collectors.toSet());
            final Set<IEnrolment> otherSources = otherEntry.getDismissals().stream()
                    .flatMap(d -> d.getCredits().getIEnrolments().stream()).collect(Collectors.toSet());

            return Objects.equals(getReasonType(), otherEntry.getReasonType()) && Objects.equals(getUnit(), otherEntry.getUnit())
                    && sources.equals(otherSources);
        }

        return toLocalizedString().compareTo(entry.toLocalizedString()) == 0;
    }

    @Override
    public void merge(CreditsTransferRemarkEntry entry) {
        if (entry instanceof SubstitutionRemarkEntry) {
            final SubstitutionRemarkEntry otherEntry = (SubstitutionRemarkEntry) entry;
            getDismissals().addAll(otherEntry.getDismissals());
        }

        getMergedEntries().add(entry);
    }

}