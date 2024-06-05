package org.fenixedu.academic.domain.student.services;

import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;

@Deprecated
public class StatuteServices {

    @Deprecated
    static public Collection<StatuteType> findStatuteTypes(final Registration registration,
            final ExecutionInterval executionInterval) {
        return StatuteType.findforRegistration(registration, executionInterval).collect(Collectors.toSet());
    }

}
