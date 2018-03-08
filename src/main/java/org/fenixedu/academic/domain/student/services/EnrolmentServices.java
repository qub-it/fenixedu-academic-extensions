package org.fenixedu.academic.domain.student.services;

import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;

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

    static public Collection<Shift> getShiftsFor(final Enrolment enrolment, final ExecutionSemester executionSemester) {
        return enrolment.getRegistration().getShiftsFor(enrolment.getExecutionCourseFor(executionSemester));
    }

    static public String getShiftsDescription(final Enrolment enrolment, final ExecutionSemester executionSemester) {
        return getShiftsFor(enrolment, executionSemester).stream().map(s -> s.getNome()).collect(Collectors.joining(", "));
    }

    static public boolean containsAnyShift(final Enrolment enrolment, final ExecutionSemester executionSemester,
            final Collection<Shift> shifts) {
        return getShiftsFor(enrolment, executionSemester).stream().anyMatch(s -> shifts.contains(s));
    }

}
