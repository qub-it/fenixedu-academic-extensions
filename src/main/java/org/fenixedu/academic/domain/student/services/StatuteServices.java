package org.fenixedu.academic.domain.student.services;

import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;

public class StatuteServices {

    static public Collection<StatuteType> findStatuteTypes(final Registration registration,
            final ExecutionSemester executionSemester) {

        return registration.getStudent().getStudentStatutesSet().stream()
                .filter(s -> s.isValidInExecutionPeriod(executionSemester)
                        && (s.getRegistration() == null || s.getRegistration() == registration))
                .map(s -> s.getType()).collect(Collectors.toSet());
    }

    static public String getVisibleStatuteTypesDescription(final Registration registration,
            final ExecutionSemester executionSemester) {
        return findVisibleStatuteTypes(registration, executionSemester).stream().map(s -> s.getName().getContent()).distinct()
                .collect(Collectors.joining(", "));

    }

    static public Collection<StatuteType> findVisibleStatuteTypes(final Registration registration,
            final ExecutionSemester executionSemester) {
        return findStatuteTypes(registration, executionSemester).stream().filter(s -> s.getVisible()).collect(Collectors.toSet());
    }

}
