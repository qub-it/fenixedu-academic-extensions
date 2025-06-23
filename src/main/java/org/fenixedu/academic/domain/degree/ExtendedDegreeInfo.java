package org.fenixedu.academic.domain.degree;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeInfo;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.dml.DynamicField;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ExtendedDegreeInfo extends ExtendedDegreeInfo_Base {

    private static final String SCIENTIFIC_AREAS = "scientificAreas";
    private static final String STUDY_PROGRAMME_DURATION = "studyProgrammeDuration";
    private static final String STUDY_REGIME = "studyRegime";
    private static final String STUDY_PROGRAMME_REQUIREMENTS = "studyProgrammeRequirements";
    private static final String HIGHER_EDUCATION_ACCESS = "higherEducationAccess";
    private static final String PROFESSIONAL_STATUTES = "professionalStatus";
    private static final String SUPPLEMENT_EXTRA_INFORMATION = "supplementExtraInformation";
    private static final String SUPPLEMENT_OTHER_SOURCES = "supplementOtherSources";

    public static void setupDeleteListener() {
        FenixFramework.getDomainModel().registerDeletionListener(DegreeInfo.class, degreeInfo -> {
            ExtendedDegreeInfo edi = degreeInfo.getExtendedDegreeInfo();
            degreeInfo.setExtendedDegreeInfo(null);
            if (edi != null) {
                edi.delete();
            }
        });
    }

    public static void setupCreationListener() {
        Signal.register(DegreeInfo.DEGREE_INFO_CREATION_EVENT, (final DomainObjectEvent<DegreeInfo> event) -> {
            DegreeInfo degreeInfo = event.getInstance();
            if (degreeInfo.getExtendedDegreeInfo() != null) {
                return; // @diogo-simoes 22MAR2016 // Only apply for new DegreeInfos created outside the ExtendedDegreeInfo scope
            }
            final ExtendedDegreeInfo mostRecent = findMostRecent(degreeInfo.getExecutionYear(), degreeInfo.getDegree());
            if (mostRecent != null) {
                new ExtendedDegreeInfo(degreeInfo, mostRecent);
            } else {
                new ExtendedDegreeInfo(degreeInfo);
            }
        });
    }

    public ExtendedDegreeInfo() {
        super();
        setBennu(Bennu.getInstance());
    }

    public ExtendedDegreeInfo(final DegreeInfo degreeInfo) {
        this();
        setDegreeInfo(degreeInfo);
    }

    public ExtendedDegreeInfo(final DegreeInfo degreeInfo, final ExtendedDegreeInfo olderEdi) {
        this(degreeInfo);

        setScientificAreas(olderEdi.getScientificAreas());
        setStudyRegime(olderEdi.getStudyRegime());
        setStudyProgrammeDuration(olderEdi.getStudyProgrammeDuration());
        setStudyProgrammeRequirements(olderEdi.getStudyProgrammeRequirements());
        setHigherEducationAccess(olderEdi.getHigherEducationAccess());
        setProfessionalStatus(olderEdi.getProfessionalStatus());
        setSupplementExtraInformation(olderEdi.getSupplementExtraInformation());
        setSupplementOtherSources(olderEdi.getSupplementOtherSources());
    }

    @Override
    public LocalizedString getScientificAreas() {
        return findFieldByCode(SCIENTIFIC_AREAS).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getScientificAreas);
    }

    @Override
    public void setScientificAreas(final LocalizedString scientificAreas) {
        findFieldByCode(SCIENTIFIC_AREAS).ifPresent(dF -> dF.edit(scientificAreas));
        super.setScientificAreas(scientificAreas);
    }

    @Override
    public LocalizedString getStudyRegime() {
        return findFieldByCode(STUDY_REGIME).map(dF -> dF.getValue(LocalizedString.class)).orElseGet(super::getStudyRegime);
    }

    @Override
    public void setStudyRegime(final LocalizedString studyRegime) {
        findFieldByCode(STUDY_REGIME).ifPresent(dF -> dF.edit(studyRegime));
        super.setStudyRegime(studyRegime);
    }

    @Override
    public LocalizedString getStudyProgrammeDuration() {
        return findFieldByCode(STUDY_PROGRAMME_DURATION).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getStudyProgrammeDuration);
    }

    @Override
    public void setStudyProgrammeDuration(final LocalizedString studyProgrammeDuration) {
        findFieldByCode(STUDY_PROGRAMME_DURATION).ifPresent(dF -> dF.edit(studyProgrammeDuration));
        super.setStudyProgrammeDuration(studyProgrammeDuration);
    }

    @Override
    public LocalizedString getStudyProgrammeRequirements() {
        return findFieldByCode(STUDY_PROGRAMME_REQUIREMENTS).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getStudyProgrammeRequirements);
    }

    @Override
    public void setStudyProgrammeRequirements(final LocalizedString studyProgrammeRequirements) {
        findFieldByCode(STUDY_PROGRAMME_REQUIREMENTS).ifPresent(dF -> dF.edit(studyProgrammeRequirements));
        super.setStudyProgrammeRequirements(studyProgrammeRequirements);
    }

    @Override
    public LocalizedString getHigherEducationAccess() {
        return findFieldByCode(HIGHER_EDUCATION_ACCESS).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getHigherEducationAccess);
    }

    @Override
    public void setHigherEducationAccess(final LocalizedString higherEducationAccess) {
        findFieldByCode(HIGHER_EDUCATION_ACCESS).ifPresent(dF -> dF.edit(higherEducationAccess));
        super.setHigherEducationAccess(higherEducationAccess);
    }

    @Override
    public LocalizedString getProfessionalStatus() {
        return findFieldByCode(PROFESSIONAL_STATUTES).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getProfessionalStatus);
    }

    @Override
    public void setProfessionalStatus(final LocalizedString professionalStatus) {
        findFieldByCode(PROFESSIONAL_STATUTES).ifPresent(dF -> dF.edit(professionalStatus));
        super.setProfessionalStatus(professionalStatus);
    }

    @Override
    public LocalizedString getSupplementExtraInformation() {
        return findFieldByCode(SUPPLEMENT_EXTRA_INFORMATION).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getSupplementExtraInformation);
    }

    @Override
    public void setSupplementExtraInformation(final LocalizedString supplementExtraInformation) {
        findFieldByCode(SUPPLEMENT_EXTRA_INFORMATION).ifPresent(dF -> dF.edit(supplementExtraInformation));
        super.setSupplementExtraInformation(supplementExtraInformation);
    }

    @Override
    public LocalizedString getSupplementOtherSources() {
        return findFieldByCode(SUPPLEMENT_OTHER_SOURCES).map(dF -> dF.getValue(LocalizedString.class))
                .orElseGet(super::getSupplementOtherSources);
    }

    @Override
    public void setSupplementOtherSources(final LocalizedString supplementOtherSources) {
        findFieldByCode(SUPPLEMENT_OTHER_SOURCES).ifPresent(dF -> dF.edit(supplementOtherSources));
        super.setSupplementOtherSources(supplementOtherSources);
    }

    private Optional<DynamicField> findFieldByCode(final String code) {
        return Optional.ofNullable(DynamicField.findField(this.getDegreeInfo(), code));
    }

    public void delete() {
        setDegreeInfo(null);
        setBennu(null);
        deleteDomainObject();
    }

    /**
     * @return The ExtendedDegreeInfo instance associated with the MOST recent DegreeInfo.
     */
    @Atomic
    public static ExtendedDegreeInfo getMostRecent(final ExecutionYear executionYear, final Degree degree) {
        DegreeInfo di = degree.getMostRecentDegreeInfo(executionYear);
        if (di.getExtendedDegreeInfo() == null) {
            final ExtendedDegreeInfo mostRecent = findMostRecent(executionYear, degree);
            return mostRecent != null ? new ExtendedDegreeInfo(di, mostRecent) : new ExtendedDegreeInfo(di);
        }
        return di.getExtendedDegreeInfo();
    }

    /**
     * @return The ExtendedDegreeInfo instance associated with a DegreeInfo instance specific to the specified ExecutionYear. If
     *         no such DegreeInfo exists, a new one is created by cloning the most recent DegreeInfo. The same process is applied
     *         to the ExtendedDegreeInfo.
     */
    @Atomic
    public static ExtendedDegreeInfo getOrCreate(final ExecutionYear executionYear, final Degree degree) {
        DegreeInfo di = degree.getDegreeInfoFor(executionYear);
        if (di == null) {
            DegreeInfo mrdi = degree.getMostRecentDegreeInfo(executionYear);
            di = mrdi != null ? new DegreeInfo(mrdi, executionYear) : new DegreeInfo(degree, executionYear);

            if (mrdi != null) {
                final DegreeInfo finalDegreeInfo = di;
                mrdi.getDynamicFieldSet().stream().filter(df -> StringUtils.isNotBlank(df.getValue()))
                        .map(df -> df.getDescriptor().getCode()).forEach(
                                code -> DynamicField.setFieldValue(finalDegreeInfo, code, DynamicField.getFieldValue(mrdi, code)));
            }
        }
        if (di.getExtendedDegreeInfo() == null) {
            final ExtendedDegreeInfo mostRecent = findMostRecent(executionYear, degree);
            return mostRecent != null ? new ExtendedDegreeInfo(di, mostRecent) : new ExtendedDegreeInfo(di);
        }
        return di.getExtendedDegreeInfo();
    }

    public static ExtendedDegreeInfo findMostRecent(final ExecutionYear executionYear, final Degree degree) {
        return degree.getDegreeInfosSet().stream().filter(di -> di.getExecutionYear().isBeforeOrEquals(executionYear))
                .sorted((di1, di2) -> ExecutionYear.REVERSE_COMPARATOR_BY_YEAR.compare(di1.getExecutionYear(),
                        di2.getExecutionYear())).map(di -> di.getExtendedDegreeInfo()).filter(Objects::nonNull).findFirst()
                .orElse(null);
    }

}
