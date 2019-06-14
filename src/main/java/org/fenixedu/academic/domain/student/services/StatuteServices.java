package org.fenixedu.academic.domain.student.services;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;

import com.google.common.collect.Sets;

public class StatuteServices {

    static public Collection<StatuteType> findStatuteTypes(final Registration registration,
            final ExecutionInterval executionInterval) {

        if (executionInterval instanceof ExecutionYear) {
            return findStatuteTypes(registration, (ExecutionYear) executionInterval);
        }

        if (executionInterval instanceof ExecutionSemester) {
            return findStatuteTypes(registration, (ExecutionSemester) executionInterval);
        }

        return Collections.emptySet();
    }

    static private Collection<StatuteType> findStatuteTypes(final Registration registration, final ExecutionYear executionYear) {

        final Set<StatuteType> result = Sets.newHashSet();
        for (final ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
            result.addAll(findStatuteTypes(registration, executionSemester));
        }

        return result;

    }

    static private Collection<StatuteType> findStatuteTypes(final Registration registration,
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
