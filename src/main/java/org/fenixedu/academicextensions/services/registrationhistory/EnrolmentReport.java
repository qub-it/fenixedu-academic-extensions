package org.fenixedu.academicextensions.services.registrationhistory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseServices;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.enrolment.EnrolmentServices;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class EnrolmentReport {

    private Enrolment enrolment;

    private ExecutionInterval executionInterval;

    private EnrolmentEvaluation finalEvaluation;

    public EnrolmentReport(final Enrolment enrolment) {
        this(enrolment, enrolment.getExecutionInterval());
    }

    /**
     * 
     * @param enrolment
     * @param executionInterval if it's not enroled in execution interval (for improvement only purposes)
     */
    public EnrolmentReport(final Enrolment enrolment, final ExecutionInterval executionInterval) {
        this.enrolment = Objects.requireNonNull(enrolment);
        this.executionInterval = Objects.requireNonNull(executionInterval);
        this.finalEvaluation = enrolment.getFinalEnrolmentEvaluation();
    }

    public Enrolment getEnrolment() {
        return this.enrolment;
    }

    public boolean isImprovementOnly() {
        return executionInterval != enrolment.getExecutionInterval();
    }

    public Collection<Shift> getShifts() {
        return EnrolmentServices.getShiftsFor(this.enrolment, this.executionInterval);
    }

    public String getShiftsAsString() {
        return getShifts().stream().map(s -> s.getName()).collect(Collectors.joining(", "));
    }

    public Shift getTheoreticalShift() {
        return getShiftByLoadType(CourseLoadType.THEORETICAL);
    }

    public Shift getLaboratorialShift() {
        return getShiftByLoadType(CourseLoadType.PRACTICAL_LABORATORY);
    }

    public Shift getProblemsShift() {
        return getShiftByLoadType(CourseLoadType.THEORETICAL_PRACTICAL);
    }

    public Shift getSeminaryShift() {
        return getShiftByLoadType(CourseLoadType.SEMINAR);
    }

    public Shift getFieldWorkShift() {
        return getShiftByLoadType(CourseLoadType.FIELD_WORK);
    }

    public Shift getTrainingPeriodShift() {
        return getShiftByLoadType(CourseLoadType.INTERNSHIP);
    }

    public Shift getTutorialOrientationShift() {
        return getShiftByLoadType(CourseLoadType.TUTORIAL_ORIENTATION);
    }

    public Shift getOtherShift() {
        return getShiftByLoadType(CourseLoadType.OTHER);
    }

    private Shift getShiftByLoadType(final String loadTypeCode) {
        return getShifts().stream().filter(s -> s.getCourseLoadType().getCode().equals(loadTypeCode)).findAny().orElse(null);
    }

    public Collection<SchoolClass> getSchoolClasses() {
        return enrolment.getRegistration().getSchoolClassesSet().stream()
                .filter(sc -> sc.getExecutionPeriod() == this.executionInterval).collect(Collectors.toSet());
    }

    public String getSchoolClassesAsString() {
        return getSchoolClasses().stream().map(sc -> sc.getName()).collect(Collectors.joining(", "));
    }

    public CurricularPeriod getCurricularPeriod() {
        return CurricularPeriodServices.getCurricularPeriod(enrolment);
    }

    public AcademicPeriod getAcademicPeriod() {
        return Optional.ofNullable(enrolment.getCurricularCourse().getCompetenceCourse())
                .map(cc -> cc.getAcademicPeriod(executionInterval)).orElse(null);
    }

    public BigDecimal getCredits() {
        return enrolment.getEctsCreditsForCurriculum();
    }

    public Grade getFinalEvaluationGrade() {
        return Optional.ofNullable(this.finalEvaluation).map(ev -> ev.getGrade()).orElse(null);
    }

    public EvaluationSeason getFinalEvaluationSeason() {
        return Optional.ofNullable(this.finalEvaluation).map(ev -> ev.getEvaluationSeason()).orElse(null);
    }

    public ExecutionInterval getExecutionInterval() {
        return executionInterval;
    }

    public DateTime getEnrolmentDate() {
        return enrolment.getCreationDateDateTime();
    }

    public LocalDate getEvaluationDate() {
        return Optional.ofNullable(this.finalEvaluation).map(ev -> finalEvaluation.getExamDateYearMonthDay().toLocalDate())
                .orElse(null);
    }

    public CurriculumGroup getCurriculumGroup() {
        return enrolment.getCurriculumGroup();
    }

    public Integer getEnrolmentsCount() {
        return CompetenceCourseServices.countEnrolmentsUntil(enrolment.getStudentCurricularPlan(),
                enrolment.getCurricularCourse(), enrolment.getExecutionYear());
    }

    public Registration getRegistration() {
        return this.enrolment.getRegistration();
    }

    public Degree getDegree() {
        return this.enrolment.getRegistration().getDegree();
    }

    public Person getPerson() {
        return getRegistration().getPerson();
    }

    public Integer getRegistrationCurricularYear() {
        return RegistrationServices.getCurricularYear(getRegistration(), getExecutionInterval().getExecutionYear()).getResult();
    }

    public String getState() {
        EnrollmentState state = getEnrolment().getEnrollmentState();
        return state == EnrollmentState.NOT_EVALUATED ? EnrollmentState.NOT_APROVED.getDescription() : state.getDescription();
    }

    public String getOptionalCourseDegreeName() {
        Degree enrolmentDegree = this.enrolment.getCurricularCourse().getDegree();
        return enrolmentDegree.equals(this.getDegree()) ? "" : enrolmentDegree.getPresentationName();
    }
}
