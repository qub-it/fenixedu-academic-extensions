package org.fenixedu.academic.domain.student.mobility;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.SchoolPeriodDuration;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;

public class MobilityRegistrationInformation extends MobilityRegistrationInformation_Base {

    private static final Comparator<MobilityRegistrationInformation> COMPARATOR_BY_BEGIN = (x, y) -> {

        if (x.getBegin() != null && y.getBegin() != null) {
            return x.getBegin().compareTo(y.getBegin());
        }

        if (x.getBegin() == null && y.getBegin() == null) {
            return 0;
        }

        return x.getBegin() != null ? 1 : 0;
    };

    private static final Comparator<MobilityRegistrationInformation> COMPARATOR_BY_BEGIN_DATE = (x, y) -> {

        if (x.getBeginDate() != null && y.getBeginDate() != null) {
            return x.getBeginDate().compareTo(y.getBeginDate());
        }

        if (x.getBeginDate() == null && y.getBeginDate() == null) {
            return 0;
        }

        return x.getBeginDate() != null ? 1 : 0;

    };

    public static final Comparator<MobilityRegistrationInformation> COMPARATOR_BY_MOST_RECENT = COMPARATOR_BY_BEGIN
            .thenComparing(COMPARATOR_BY_BEGIN_DATE).thenComparing(DomainObjectUtil.COMPARATOR_BY_ID).reversed();

    protected MobilityRegistrationInformation() {
        super();
        setBennu(Bennu.getInstance());
    }

    public static MobilityRegistrationInformation createOutgoing(final Registration registration, boolean national,
            SchoolPeriodDuration programDuration, ExecutionInterval begin, ExecutionInterval end, LocalDate beginDate,
            LocalDate endDate, MobilityActivityType mobilityActivityType, MobilityProgramType mobilityProgramType,
            Unit countryUnit, Unit foreignInstitutionUnit, String remarks) {

        final MobilityRegistrationInformation result = new MobilityRegistrationInformation();
        result.setRegistration(registration);
        result.editOutgoing(national, programDuration, begin, end, beginDate, endDate, mobilityActivityType, mobilityProgramType,
                countryUnit, foreignInstitutionUnit, remarks);
        return result;
    }

    public static MobilityRegistrationInformation createIncoming(final Registration registration, boolean national,
            SchoolPeriodDuration programDuration, ExecutionInterval begin, ExecutionInterval end, LocalDate beginDate,
            LocalDate endDate, MobilityActivityType mobilityActivityType, MobilityProgramType mobilityProgramType,
            Unit countryUnit, Unit foreignInstitutionUnit, String remarks, MobilityProgrammeLevel originMobilityProgrammeLevel,
            String otherOriginMobilityProgrammeLevel, boolean degreeBased, DegreeCurricularPlan degreeCurricularPlan,
            CourseGroup branchCourseGroup, MobilityScientificArea mobilityScientificArea,
            MobilityProgrammeLevel incomingMobilityProgrammeLevel, String otherIncomingMobilityProgrammeLevel) {

        final MobilityRegistrationInformation result = new MobilityRegistrationInformation();
        result.setRegistration(registration);
        result.editIncoming(national, programDuration, begin, end, beginDate, endDate, mobilityActivityType, mobilityProgramType,
                countryUnit, foreignInstitutionUnit, remarks, originMobilityProgrammeLevel, otherOriginMobilityProgrammeLevel,
                degreeBased, degreeCurricularPlan, branchCourseGroup, mobilityScientificArea, incomingMobilityProgrammeLevel,
                otherIncomingMobilityProgrammeLevel);
        return result;
    }

    public void editOutgoing(boolean national, SchoolPeriodDuration programDuration, ExecutionInterval begin,
            ExecutionInterval end, LocalDate beginDate, LocalDate endDate, MobilityActivityType mobilityActivityType,
            MobilityProgramType mobilityProgramType, Unit countryUnit, Unit foreignInstitutionUnit, String remarks) {

        setIncoming(false);
        setNational(national);
        setProgramDuration(programDuration);
        setBegin(begin);
        setEnd(end);
        setBeginDate(beginDate);
        setEndDate(endDate);
        setMobilityActivityType(mobilityActivityType);
        setMobilityProgramType(mobilityProgramType);
        setCountryUnit(countryUnit);
        setForeignInstitutionUnit(foreignInstitutionUnit);
        setRemarks(remarks);

        checkRules();
        checkRulesForOutgoing();
    }

    public void editIncoming(boolean national, SchoolPeriodDuration programDuration, ExecutionInterval begin,
            ExecutionInterval end, LocalDate beginDate, LocalDate endDate, MobilityActivityType mobilityActivityType,
            MobilityProgramType mobilityProgramType, Unit countryUnit, Unit foreignInstitutionUnit, String remarks,
            MobilityProgrammeLevel originMobilityProgrammeLevel, String otherOriginMobilityProgrammeLevel, boolean degreeBased,
            DegreeCurricularPlan degreeCurricularPlan, CourseGroup branchCourseGroup,
            MobilityScientificArea mobilityScientificArea, MobilityProgrammeLevel incomingMobilityProgrammeLevel,
            String otherIncomingMobilityProgrammeLevel) {

        setIncoming(true);
        setNational(national);
        setProgramDuration(programDuration);
        setBegin(begin);
        setEnd(end);
        setBeginDate(beginDate);
        setEndDate(endDate);
        setMobilityActivityType(mobilityActivityType);
        setMobilityProgramType(mobilityProgramType);
        setCountryUnit(countryUnit);
        setForeignInstitutionUnit(foreignInstitutionUnit);
        setRemarks(remarks);

        setOriginMobilityProgrammeLevel(originMobilityProgrammeLevel);
        setOtherOriginMobilityProgrammeLevel(otherOriginMobilityProgrammeLevel);
        setDegreeBased(degreeBased);

        if (degreeBased) {
            setDegree(degreeCurricularPlan != null ? degreeCurricularPlan.getDegree() : null);
            setDegreeCurricularPlan(degreeCurricularPlan);
            setBranchCourseGroup(branchCourseGroup);

            setMobilityScientificArea(null);
            setIncomingMobilityProgrammeLevel(null);
            setOtherIncomingMobilityProgrammeLevel(null);

        } else {
            setDegree(null);
            setDegreeCurricularPlan(null);
            setBranchCourseGroup(null);

            setMobilityScientificArea(mobilityScientificArea);
            setIncomingMobilityProgrammeLevel(incomingMobilityProgrammeLevel);
            setOtherIncomingMobilityProgrammeLevel(otherIncomingMobilityProgrammeLevel);
        }

        checkRules();
        checkRulesForIncoming();
    }

    private void checkRules() {
        if (getRegistration() == null) {
            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.registration.required");
        }

        if (getBegin() == null) {
            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.begin.required");
        }

        if (getEnd() == null) {
            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.end.required");
        }

        if (getBegin().isAfter(getEnd())) {
            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.end.must.be.after.begin");
        }

        if (getBegin().getExecutionYear().isBefore(getRegistration().getStartExecutionYear())
                || getEnd().getExecutionYear().isBefore(getRegistration().getStartExecutionYear())) {
            throw new AcademicExtensionsDomainException(
                    "error.MobilityRegistrationInformation.begin.and.end.must.be.after.registration.begin");
        }

        if (getMobilityProgramType() == null) {
            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.mobilityProgramType.required");
        }

        if (getMobilityActivityType() == null) {
            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.mobilityActivityType.required");
        }

        checkOverlaps();

    }

    private void checkOverlaps() {

        for (final MobilityRegistrationInformation information : MobilityRegistrationInformation.findAll(getRegistration())) {

            if (information == this) {
                continue;
            }

            if (information.getBegin() == null || information.getEnd() == null) {
                throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.period.overlaps");
            }

            if (getBegin().isAfter(information.getEnd()) || getEnd().isBefore(information.getBegin())) {
                continue;
            }

            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.period.overlaps");

        }

    }

    protected void checkRulesForOutgoing() {

        if (getCountryUnit() == null) {
            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.countryUnit.required");
        }

        if (getForeignInstitutionUnit() == null) {
            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.foreignInstitutionUnit.required");
        }

    }

    private void checkRulesForIncoming() {

        if (getProgramDuration() == null) {
            throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.programDuration.required");
        }

        if (getDegreeBased()) {

            if (getDegree() == null) {
                throw new AcademicExtensionsDomainException("error.MobilityRegistrationInformation.degree.required");
            }

            if (getDegreeCurricularPlan() == null) {
                throw new AcademicExtensionsDomainException(
                        "error.MobilityRegistrationInformation.degreeCurricularPlan.required");
            }

        } else {

            if (getMobilityScientificArea() == null) {
                throw new AcademicExtensionsDomainException(
                        "error.MobilityRegistrationInformation.mobilityScientificArea.required");
            }

            if (getIncomingMobilityProgrammeLevel() == null) {
                throw new AcademicExtensionsDomainException(
                        "error.MobilityRegistrationInformation.incomingMobilityProgrammeLevel.required");
            }

        }

        if (getOriginMobilityProgrammeLevel() == null) {
            throw new AcademicExtensionsDomainException(
                    "error.MobilityRegistrationInformation.originMobilityProgrammeLevel.required");
        }

    }

    public boolean isDeletable() {
        return true;
    }

    public boolean isIncoming() {
        return getIncoming();
    }

    @Atomic
    public void delete() {
        setRegistration(null);
        setBegin(null);
        setEnd(null);
        setMobilityProgramType(null);
        setMobilityActivityType(null);
        setCountryUnit(null);
        setForeignInstitutionUnit(null);
        setMobilityScientificArea(null);
        setIncomingMobilityProgrammeLevel(null);
        setOriginMobilityProgrammeLevel(null);

        setDegree(null);
        setDegreeCurricularPlan(null);
        setBranchCourseGroup(null);

        setBennu(null);

        deleteDomainObject();
    }

    public Country getCountry() {
        return getCountryUnit() != null ? Country
                .readByTwoLetterCode(getCountryUnit().getAcronym()) : getCountryByForeignInstitutionUnit();
    }

    @Deprecated
    private Country getCountryByForeignInstitutionUnit() {

        //legacy
        if (getForeignInstitutionUnit() == null) {
            return null;
        }

        final Set<Unit> countries =
                getForeignInstitutionUnit().getParentUnits().stream().filter(u -> u.isCountryUnit()).collect(Collectors.toSet());

        if (countries.size() > 1) {
            throw new AcademicExtensionsDomainException(
                    "error.MobilityRegistrationInformation.found.more.than.one.parent.country.of.foreign.unit");
        }

        if (countries.size() == 1) {
            Unit countryUnit = countries.iterator().next();
            if ((countryUnit).getCountry() != null) {
                return (countryUnit).getCountry();
            } else if (!Strings.isNullOrEmpty((countryUnit).getAcronym())) {
                return Country.readByTwoLetterCode((countryUnit).getAcronym());
            }
        }

        return null;
    }

    public boolean hasCountry() {
        return getCountry() != null;
    }

    public boolean isIncomingStudent() {
        return getIncoming();
    }

    public LocalizedString getMobilityStudentTypeDescription() {
        if (isIncomingStudent()) {
            return AcademicExtensionsUtil.bundleI18N("label.MobilityRegistrationInformationBean.incomingStudent");
        }

        return AcademicExtensionsUtil.bundleI18N("label.MobilityRegistrationInformationBean.outgoingStudent");
    }

    public boolean isValid(final ExecutionYear executionYear) {

        //Legacy - compatibility reasons only
        if (getBegin() == null || getEnd() == null) {
            return true;
        }

        return !executionYear.isAfter(getEnd().getExecutionYear()) && !executionYear.isBefore(getBegin().getExecutionYear());

    }

    private boolean isBeforeOrEquals(final ExecutionYear executionYear) {

        //Legacy - compatibility reasons only
        if (getBegin() == null || getEnd() == null) {
            return true;
        }

        return getEnd().getExecutionYear().isBeforeOrEquals(executionYear);
    }

    @Atomic
    public void markAsMainInformation() {
        getRegistration().getMobilityRegistrationInformationsSet().stream()
                .filter(m -> m.isIncoming() == isIncoming() && m.getNational() == getNational())
                .forEach(m -> m.setMainInformation(false));
        setMainInformation(true);
    }

    public static Set<MobilityRegistrationInformation> findAll(final Registration registration) {
        return registration.getMobilityRegistrationInformationsSet();
    }

    public static boolean hasAnyInternationalOutgoingMobility(final Registration registration) {
        return hasAnyInternationalOutgoingMobilityUntil(registration, null);
    }

    public static boolean hasAnyInternationalOutgoingMobilityUntil(final Registration registration, final ExecutionYear until) {
        return registration.getMobilityRegistrationInformationsSet().stream()
                .filter(m -> until == null || m.isBeforeOrEquals(until)).anyMatch(m -> !m.isIncoming() && !m.getNational());
    }

    public static MobilityRegistrationInformation findMainInternationalOutgoingInformation(Registration registration) {
        return findMainInternationalOutgoingInformationUntil(registration, null);
    }

    public static MobilityRegistrationInformation findMainInternationalOutgoingInformationUntil(Registration registration,
            ExecutionYear until) {
        return findInternationalOutgoingInformationsUntil(registration, until).stream().filter(m -> m.getMainInformation())
                .findFirst().orElse(null);
    }

    public static Collection<MobilityRegistrationInformation> findInternationalOutgoingInformations(Registration registration) {
        return findInternationalOutgoingInformationsUntil(registration, null);

    }

    public static Collection<MobilityRegistrationInformation> findInternationalOutgoingInformationsUntil(
            Registration registration, ExecutionYear until) {
        return registration.getMobilityRegistrationInformationsSet().stream()
                .filter(m -> (until == null || m.isBeforeOrEquals(until)) && !m.isIncoming() && !m.getNational())
                .collect(Collectors.toSet());
    }

    public static MobilityRegistrationInformation findInternationalIncomingInformation(final Registration registration,
            final ExecutionYear executionYear) {
        return registration.getMobilityRegistrationInformationsSet().stream()
                .filter(m -> m.isIncoming() && !m.getNational() && m.isValid(executionYear)).sorted(COMPARATOR_BY_MOST_RECENT)
                .findFirst().orElse(null);
    }

}
