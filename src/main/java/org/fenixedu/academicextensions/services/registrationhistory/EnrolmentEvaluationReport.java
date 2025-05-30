package org.fenixedu.academicextensions.services.registrationhistory;

import java.util.Objects;
import java.util.Optional;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationConfiguration;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.joda.time.LocalDate;

public class EnrolmentEvaluationReport {

    private EnrolmentEvaluation enrolmentEvaluation;

    public EnrolmentEvaluationReport(final EnrolmentEvaluation evaluation) {
        this.enrolmentEvaluation = Objects.requireNonNull(evaluation);
    }

    public EnrolmentEvaluation getEnrolmentEvaluation() {
        return this.enrolmentEvaluation;
    }

    public ExecutionInterval getExecutionInterval() {
        return enrolmentEvaluation.getExecutionInterval();
    }

    public LocalDate getEvaluationDate() {
        return Optional.ofNullable(enrolmentEvaluation.getExamDateYearMonthDay()).map(v -> v.toLocalDate()).orElse(null);
    }

    public Grade getGrade() {
        return enrolmentEvaluation.getGrade();
    }

    public Boolean getImprovedPreviousGrade() {
        if (!enrolmentEvaluation.getEvaluationSeason().isImprovement()) {
            return null;
        }

        //find final evaluation excluding improvement (for conclusion)
        return EvaluationConfiguration.getInstance().getEnrolmentEvaluationForConclusionDate(enrolmentEvaluation.getEnrolment())
                .filter(ev -> ev.isApproved()).map(ev -> enrolmentEvaluation.getGrade().compareTo(ev.getGrade()) > 0)
                .orElse(false);
    }

    public EvaluationSeason getEvaluationSeason() {
        return this.enrolmentEvaluation.getEvaluationSeason();
    }

    public Registration getRegistration() {
        return enrolmentEvaluation.getRegistration();
    }

    public Person getPerson() {
        return getRegistration().getPerson();
    }

    public Enrolment getEnrolment() {
        return enrolmentEvaluation.getEnrolment();
    }

    public AcademicPeriod getAcademicPeriod() {
        return Optional.ofNullable(getEnrolment().getCurricularCourse().getCompetenceCourse())
                .map(cc -> cc.getAcademicPeriod(getExecutionInterval())).orElse(null);
    }

    public Degree getDegree() {
        return getRegistration().getDegree();
    }

    public Integer getCurricularYear() {
        return RegistrationServices.getCurricularYear(getRegistration(), getExecutionInterval().getExecutionYear()).getResult();
    }
}
