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
import org.fenixedu.academic.domain.evaluation.EnrolmentEvaluationServices;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.joda.time.DateTime;

public class EnrolmentEvaluationReport {

    private EnrolmentEvaluation evaluation;

    public EnrolmentEvaluationReport(final EnrolmentEvaluation evaluation) {
        this.evaluation = Objects.requireNonNull(evaluation);
    }

    public ExecutionInterval getExecutionInterval() {
        return evaluation.getExecutionInterval();
    }

    public DateTime getEvaluationDate() {
        return EnrolmentEvaluationServices.getExamDateTime(evaluation);
    }

    public Grade getGrade() {
        return evaluation.getGrade();
    }

    public Boolean getImprovedPreviousGrade() {
        if (!evaluation.getEvaluationSeason().isImprovement()) {
            return null;
        }

        //find final evaluation excluding improvement (for conclusion)
        return EvaluationConfiguration.getInstance().getEnrolmentEvaluationForConclusionDate(evaluation.getEnrolment())
                .filter(ev -> ev.isApproved()).map(ev -> evaluation.getGrade().compareTo(ev.getGrade()) > 0).orElse(false);
    }

    public EvaluationSeason getEvaluationSeason() {
        return this.evaluation.getEvaluationSeason();
    }

    public Registration getRegistration() {
        return evaluation.getRegistration();
    }

    public Person getPerson() {
        return getRegistration().getPerson();
    }

    public Enrolment getEnrolment() {
        return evaluation.getEnrolment();
    }

    public AcademicPeriod getAcademicPeriod() {
        return Optional.ofNullable(getEnrolment().getCurricularCourse().getCompetenceCourse())
                .map(cc -> cc.getAcademicPeriod(getExecutionInterval())).orElse(null);
    }

    public Degree getDegree() {
        return getRegistration().getDegree();
    }

}
