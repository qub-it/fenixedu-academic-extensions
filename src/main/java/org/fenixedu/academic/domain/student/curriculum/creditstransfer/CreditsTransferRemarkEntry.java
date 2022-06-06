package org.fenixedu.academic.domain.student.curriculum.creditstransfer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.organizationalStructure.PartyType;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.curriculum.CreditsReasonType;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.Equivalence;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.commons.i18n.LocalizedString;

abstract class CreditsTransferRemarkEntry {
    
    private static class EmptyRemarkEntry extends CreditsTransferRemarkEntry {

        public EmptyRemarkEntry() {
            super(null, null, null);
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        public boolean matches(CreditsTransferRemarkEntry entry) {
            return false;
        }

        @Override
        public void merge(CreditsTransferRemarkEntry entry) {
            throw new UnsupportedOperationException("Unable to merge empty entries");
        }

        @Override
        protected String toString(Locale locale) {
            return "";
        }

    }

    private String remarkId;

    public static CreditsTransferRemarkEntry EMPTY = new EmptyRemarkEntry();

    private CreditsReasonType reasonType;

    private Class<?> creditsType;

    private ICurriculumEntry entry;

    private Collection<CreditsTransferRemarkEntry> mergedEntries = new LinkedHashSet<>();

    public CreditsTransferRemarkEntry(CreditsReasonType reasonType, Class<?> creditsType, ICurriculumEntry entry) {
        this.reasonType = reasonType;
        this.creditsType = creditsType;
        this.entry = entry;
    }

    public static Collection<CreditsTransferRemarkEntry> buildEntries(ICurriculumEntry entry, StudentCurricularPlan studentCurricularPlan) {

        final List<CurriculumLine> targetLines = entry.getCurriculumLinesForCurriculum(studentCurricularPlan).stream()
                .sorted(CurriculumLine.COMPARATOR_BY_FULL_PATH_NAME_AND_ID).collect(Collectors.toList());
        if (targetLines.isEmpty()) {
            return Collections.singleton(CreditsTransferRemarkEntry.EMPTY);
        }

        final Collection<CreditsTransferRemarkEntry> result = new LinkedHashSet<CreditsTransferRemarkEntry>();
        targetLines.stream().filter(d -> d.isDismissal()).map(Dismissal.class::cast).forEach(d -> {
            result.add(buildEntry(entry, d, studentCurricularPlan));
        });

        return result;
    }

    private static CreditsTransferRemarkEntry buildEntry(ICurriculumEntry entry, Dismissal dismissal,
            StudentCurricularPlan studentCurricularPlan) {
        if (dismissal.getCredits() instanceof Substitution) {
            return SubstitutionRemarkEntry.build(entry, dismissal);
        } else if (dismissal.getCredits() instanceof Equivalence) {
            return EquivalenceRemarkEntry.build(dismissal);
        } else {
            throw new RuntimeException("Unexpected credits type " + dismissal.getCredits().getClass());
        }
    }

    public CreditsReasonType getReasonType() {
        return reasonType;
    }

    public Class<?> getCreditsType() {
        return creditsType;
    }

    public ICurriculumEntry getEntry() {
        return entry;
    }

    public boolean isEmpty() {
        return false;
    }

    public Collection<CreditsTransferRemarkEntry> getMergedEntries() {
        return mergedEntries;
    }

    public String getRemarkId() {
        return remarkId;
    }

    public String getFormattedRemarkId() {
        return Optional.ofNullable(getRemarkId()).map(v -> v + ")").orElse("");
    }

    public LocalizedString toLocalizedString() {
        final LocalizedString.Builder builder = new LocalizedString.Builder();
        CoreConfiguration.supportedLocales().forEach(l -> builder.with(l, toString(l)));

        return builder.build();
    }

    protected String getFormattedEcts(ICurriculumEntry entry, Locale locale) {
        final BigDecimal ects = entry.getEctsCreditsForCurriculum();
        if (getReasonType().getInfoExplainedWithEcts() && ects != null && ects.compareTo(BigDecimal.ZERO) > 0) {
            return AcademicExtensionsUtil.bundleI18N("info.CreditsReasonType.explained.ects", "" + ects).getContent(locale);
        }

        return "";
    }

    protected String getFormattedUnit(Unit unit, Locale locale, boolean withCountry, boolean withInstitution) {
        final StringBuilder result = new StringBuilder();

        final List<Unit> fullPath = new ArrayList<Unit>();
        if (unit != null) {
            fullPath.addAll(unit.getParentUnitsPath());
            Collections.reverse(fullPath);
            fullPath.add(unit);
        }

        final Collection<PartyType> acceptedPartyTypes = new HashSet<>();
        if (withCountry) {
            acceptedPartyTypes.add(PartyType.of(PartyTypeEnum.COUNTRY).get());
        }

        if (withInstitution) {
            acceptedPartyTypes.add(PartyType.of(PartyTypeEnum.SCHOOL).get());
            acceptedPartyTypes.add(PartyType.of(PartyTypeEnum.UNIVERSITY).get());
        }

        final Collection<Unit> filteredUnits =
                fullPath.stream().filter(u -> acceptedPartyTypes.contains(u.getPartyType())).collect(Collectors.toList());
        if (!filteredUnits.isEmpty()) {
            result.append(
                    filteredUnits.stream()
                            .map(u -> Optional.ofNullable(u.getNameI18n().getContent(locale)).map(v -> v)
                                    .orElse(u.getNameI18n().getContent(Locale.getDefault())))
                            .collect(Collectors.joining(" > ")));
        }

        return result.toString();
    }

    protected String getFormattedReason(Locale locale) {
        return getReasonType().getInfoText().getContent(locale);
    }

    void setRemarkId(String id) {
        this.remarkId = id;
    }

    public boolean appliesTo(ICurriculumEntry entry) {
        return getEntry() == entry || getMergedEntries().stream().anyMatch(e -> e.appliesTo(entry));
    }

    public Collection<ICurriculumEntry> getAllCurriculumEntries() {
        return getAllCurriculumEntries(this);
    }

    private Collection<ICurriculumEntry> getAllCurriculumEntries(CreditsTransferRemarkEntry entry) {
        return Stream
                .concat(Stream.of(entry.getEntry()),
                        entry.getMergedEntries().stream().flatMap(me -> getAllCurriculumEntries(me).stream()))
                .collect(Collectors.toSet());
    }

    protected abstract String toString(Locale locale);

    public abstract boolean matches(final CreditsTransferRemarkEntry entry);

    public abstract void merge(final CreditsTransferRemarkEntry entry);

}