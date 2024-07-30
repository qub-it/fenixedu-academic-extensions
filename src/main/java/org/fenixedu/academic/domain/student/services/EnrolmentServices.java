package org.fenixedu.academic.domain.student.services;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;

import java.util.Collection;
import java.util.stream.Collectors;

//TODO: merge with new EnrolmentServices from specifications
public class EnrolmentServices {

    static public void updateState(final Enrolment enrolment) {
        // TODO legidio 
        // before enabling this, must delete dissertation enrolment wrongly associated with non terminal program conclusions
        // checkForConclusionProcessVersions(enrolment);

        if (!enrolment.isAnnulled()) {
            enrolment.setEnrollmentState(calculateState(enrolment));
        }
    }

    static public EnrollmentState calculateState(final Enrolment enrolment) {
        final Grade finalGrade = enrolment.getGrade();
        return finalGrade.isEmpty() ? EnrollmentState.ENROLLED : finalGrade.getEnrolmentState();
    }

    static public Collection<Shift> getShiftsFor(final Enrolment enrolment, final ExecutionInterval executionInterval) {
        return enrolment.getRegistration().getShiftsFor(enrolment.getExecutionCourseFor(executionInterval));
    }

    static public String getShiftsDescription(final Enrolment enrolment, final ExecutionInterval executionInterval) {
        return getShiftsFor(enrolment, executionInterval).stream().map(s -> s.getNome()).collect(Collectors.joining(", "));
    }

    static public boolean containsAnyShift(final Enrolment enrolment, final ExecutionInterval executionInterval,
            final Collection<Shift> shifts) {
        return getShiftsFor(enrolment, executionInterval).stream().anyMatch(s -> shifts.contains(s));
    }

}
