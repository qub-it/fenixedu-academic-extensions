package org.fenixedu.academic.domain.enrolment.period;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EnrolmentType;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.CurriculumConfigurationInitializer.CurricularYearResult;
import org.fenixedu.academic.domain.student.services.StatuteServices;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class AcademicEnrolmentPeriod extends AcademicEnrolmentPeriod_Base {

    protected AcademicEnrolmentPeriod() {
        super();
        setBennu(Bennu.getInstance());
    }

    protected AcademicEnrolmentPeriod(final DateTime startDate, final DateTime endDate, final Boolean firstTimeRegistration,
            final Boolean restrictToSelectedStatutes, final Boolean restrictToSelectedIngressionTypes,
            final Integer minStudentNumber, final Integer maxStudentNumber, final Integer curricularYear,
            final Boolean schoolClassSelectionMandatory, final AcademicEnrolmentPeriodType enrolmentPeriodType,
            final Boolean allowEnrolWithDebts, final AutomaticEnrolment automaticEnrolment,
            final ExecutionInterval executionInterval) {
        this();
        setStartDate(startDate);
        setEndDate(endDate);
        setFirstTimeRegistration(firstTimeRegistration);
        setRestrictToSelectedStatutes(restrictToSelectedStatutes);
        setRestrictToSelectedIngressionTypes(restrictToSelectedIngressionTypes);
        setMinStudentNumber(minStudentNumber);
        setMaxStudentNumber(maxStudentNumber);
        setCurricularYear(curricularYear);
        setSchoolClassSelectionMandatory(schoolClassSelectionMandatory);
        setEnrolmentPeriodType(enrolmentPeriodType);
        setAllowEnrolWithDebts(allowEnrolWithDebts);
        setAutomaticEnrolment(automaticEnrolment);
        setExecutionSemester(executionInterval);

        checkRules();
    }

    private void checkRules() {

        if (getStartDate() == null) {
            throw new AcademicExtensionsDomainException("error.AcademicEnrolmentPeriod.startDate.required");
        }

        if (getEndDate() == null) {
            throw new AcademicExtensionsDomainException("error.AcademicEnrolmentPeriod.endDate.required");
        }

        if (getEnrolmentPeriodType() == null) {
            throw new AcademicExtensionsDomainException("error.AcademicEnrolmentPeriod.enrolmentPeriodType.required");
        }

        if (getExecutionInterval() == null) {
            throw new AcademicExtensionsDomainException("error.AcademicEnrolmentPeriod.executionSemester.required");
        }

        if (getStartDate().isAfter(getEndDate())) {
            throw new AcademicExtensionsDomainException("error.AcademicEnrolmentPeriod.startDate.before.endDate");
        }

        if (getMaxStudentNumber() != null && getMinStudentNumber() != null && getMinStudentNumber() > getMaxStudentNumber()) {
            throw new AcademicExtensionsDomainException("error.AcademicEnrolmentPeriod.minNumber.must.be.lower.maxNumber");
        }

    }

    @Atomic
    public void edit(final DateTime startDate, final DateTime endDate, final Boolean firstTimeRegistration,
            final Boolean restrictToSelectedStatutes, final Boolean restrictToSelectedIngressionTypes,
            final Integer minStudentNumber, final Integer maxStudentNumber, final Integer curricularYear,
            final Boolean schoolClassSelectionMandatory, final AcademicEnrolmentPeriodType enrolmentPeriodType,
            final AutomaticEnrolment automaticEnrolment, final Boolean allowEnrolWithDebts,
            final ExecutionInterval executionInterval) {
        setStartDate(startDate);
        setEndDate(endDate);
        setFirstTimeRegistration(firstTimeRegistration);
        setRestrictToSelectedStatutes(restrictToSelectedStatutes);
        setRestrictToSelectedIngressionTypes(restrictToSelectedIngressionTypes);
        setMinStudentNumber(minStudentNumber);
        setMaxStudentNumber(maxStudentNumber);
        setCurricularYear(curricularYear);
        setSchoolClassSelectionMandatory(schoolClassSelectionMandatory);
        setEnrolmentPeriodType(enrolmentPeriodType);
        setAutomaticEnrolment(automaticEnrolment);
        setAllowEnrolWithDebts(allowEnrolWithDebts);
        setExecutionSemester(executionInterval);

        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {
        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        setBennu(null);
        setExecutionSemester(null);
        getDegreeCurricularPlansSet().clear();
        getStatuteTypesSet().clear();
        getIngressionTypesSet().clear();
        getEnrolmentTypesSet().clear();

        deleteDomainObject();
    }

    public static Stream<AcademicEnrolmentPeriod> readAll() {
        return Bennu.getInstance().getAcademicEnrolmentPeriodsSet().stream();
    }

    public static Stream<AcademicEnrolmentPeriod> findByType(AcademicEnrolmentPeriodType type) {
        return readAll().filter(p -> p.getEnrolmentPeriodType() == type);
    }

    @Atomic
    public static AcademicEnrolmentPeriod create(final DateTime startDate, final DateTime endDate,
            final Boolean firstTimeRegistration, final Boolean restrictToSelectedStatutes,
            final Boolean restrictToSelectedIngressionTypes, final Integer minStudentNumber, final Integer maxStudentNumber,
            final Integer curricularYear, final Boolean schoolClassSelectionMandatory,
            final AcademicEnrolmentPeriodType enrolmentPeriodType, final Boolean allowEnrolWithDebts,
            final AutomaticEnrolment automaticEnrolment, final ExecutionInterval executionInterval) {
        AcademicEnrolmentPeriod period = new AcademicEnrolmentPeriod(startDate, endDate, firstTimeRegistration,
                restrictToSelectedStatutes, restrictToSelectedIngressionTypes, minStudentNumber, maxStudentNumber, curricularYear,
                schoolClassSelectionMandatory, enrolmentPeriodType, allowEnrolWithDebts, automaticEnrolment, executionInterval);

        return period;
    }

    @Override
    public Boolean getSchoolClassSelectionMandatory() {
        return super.getSchoolClassSelectionMandatory() != null && super.getSchoolClassSelectionMandatory();
    }

    @Deprecated
    @Override
    public ExecutionInterval getExecutionSemester() {
        return super.getExecutionSemester();
    }

    public ExecutionInterval getExecutionInterval() {
        return super.getExecutionSemester();
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionInterval().getExecutionYear();
    }

    public boolean isForCurricularCourses() {
        return getEnrolmentPeriodType() == AcademicEnrolmentPeriodType.CURRICULAR_COURSE;
    }

    public boolean isForClasses() {
        return getEnrolmentPeriodType() == AcademicEnrolmentPeriodType.SCHOOL_CLASS;
    }

    public boolean isForShift() {
        return getEnrolmentPeriodType() == AcademicEnrolmentPeriodType.SHIFT;
    }

    public boolean isOpen() {
        return containsDate(new DateTime());
    }

    public boolean isUpcoming() {
        return getStartDate().isAfterNow();
    }

    public boolean isAutomatic() {
        return getAutomaticEnrolment() != null && getAutomaticEnrolment().isAutomatic();
    }

    public boolean isEditable() {
        return getAutomaticEnrolment() == null || getAutomaticEnrolment().isEditable();
    }

    private boolean containsDate(final DateTime date) {
        return !(getStartDate().isAfter(date) || getEndDate().isBefore(date));
    }

    private boolean isValidRegistration(final Registration input, final boolean skipRegistrationState) {
        return skipRegistrationState || input.hasActiveLastState(getExecutionInterval());
    }

    private boolean isValidStudentNumber(final Registration input) {
        final Integer number = input.getNumber();
        return (getMinStudentNumber() == null || number >= getMinStudentNumber())

                && (getMaxStudentNumber() == null || number <= getMaxStudentNumber());
    }

    /**
     * Returns StatuteTypes that grant access to this enrolment period
     */
    private boolean isValidStatuteTypes(final Set<StatuteType> input) {
        final Set<StatuteType> configured = getStatuteTypesSet();
        if (configured.isEmpty()) {
            return true;
        }

        if (getRestrictToSelectedStatutes()) {
            // mandatory
            return !Sets.intersection(input, configured).isEmpty();

        } else {
            // blocking
            return input.isEmpty() || !Sets.difference(input, configured).isEmpty();
        }
    }

    /**
     * Returns IngressionTypes that grant access to this enrolment period
     */
    private boolean isValidIngressionTypes(final IngressionType input) {
        final Set<IngressionType> configured = getIngressionTypesSet();
        if (configured.isEmpty()) {
            return true;
        }

        if (getRestrictToSelectedIngressionTypes() != null && getRestrictToSelectedIngressionTypes()) {
            // mandatory
            return configured.contains(input);
        } else {
            // blocking
            return input == null || !configured.contains(input);
        }
    }

    private boolean isValidEnrolmentTypes(final Registration input) {
        final Set<EnrolmentType> configured = getEnrolmentTypesSet();
        if (configured.isEmpty()) {
            return true;
        }

        // for now, this level of test only makes sense for school classes
        if (isForClasses()) {
            for (final EnrolmentType enrolmentType : configured) {
                final boolean flunked = RegistrationServices.isFlunkedUsingCurricularYear(input, getExecutionYear());
                if (enrolmentType.isFlunked() && flunked) {
                    return true;
                }
                if (enrolmentType.isNormal() && !flunked /* add here future 'not' conditions*/) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    private boolean isValidCurricularYear(final int input) {
        return getCurricularYear() == null || getCurricularYear().equals(input);
    }

    private boolean isValidFirstTimeStatus(final StudentCurricularPlan input) {
        return getFirstTimeRegistration() == null
                || getFirstTimeRegistration().equals(input.getRegistration().isFirstTime(getExecutionYear()));
    }

    public boolean isForFirstTimeRegistration() {
        return super.getFirstTimeRegistration() != null && super.getFirstTimeRegistration().booleanValue();
    }

    private boolean isValidStudentCurricularPlan(final StudentCurricularPlan input) {
        return getDegreeCurricularPlansSet().contains(input.getDegreeCurricularPlan());
    }

    static public List<AcademicEnrolmentPeriod> getEnrolmentPeriodsOpenOrUpcoming(final Student student) {

        final List<AcademicEnrolmentPeriod> result = new ArrayList<>();

        for (final Registration registration : student.getRegistrationsSet()) {
            final DegreeCurricularPlan degreeCurricularPlan = registration.getLastDegreeCurricularPlan();
            for (final AcademicEnrolmentPeriod period : degreeCurricularPlan.getAcademicEnrolmentPeriodsSet()) {
                if ((period.isOpen() || period.isUpcoming()) && period.isValidFor(registration, false)) {
                    result.add(period);
                }
            }
        }

        result.sort(Comparator.comparing(AcademicEnrolmentPeriod::getStartDate)
                .thenComparing(AcademicEnrolmentPeriod::getEnrolmentPeriodType)
                .thenComparing(AcademicEnrolmentPeriod::getExternalId));

        return result;
    }

    public boolean isValidFor(final Registration input, final boolean skipRegistrationState) {

        if (!isValidRegistration(input, skipRegistrationState)) {
            return false;
        }

        if (!isValidStudentNumber(input)) {
            return false;
        }

        final Set<StatuteType> studentStatutes = Sets.newHashSet(StatuteServices.findStatuteTypes(input, getExecutionInterval()));
        if (!isValidStatuteTypes(studentStatutes)) {
            return false;
        }

        if (!isValidIngressionTypes(input.getIngressionType())) {
            return false;
        }

        if (!isValidEnrolmentTypes(input)) {
            return false;
        }

        final CurricularYearResult curricularYearResult = RegistrationServices.getCurricularYear(input, getExecutionYear());
        final int studentCurricularYear = curricularYearResult == null ? 0 : curricularYearResult.getResult();
        if (!isValidCurricularYear(studentCurricularYear)) {
            return false;
        }

        final StudentCurricularPlan studentCurricularPlan = input.getLastStudentCurricularPlan();
        if (!isValidFirstTimeStatus(studentCurricularPlan)) {
            return false;
        }

        if (!isValidStudentCurricularPlan(studentCurricularPlan)) {
            return false;
        }

        return true;
    }

}
