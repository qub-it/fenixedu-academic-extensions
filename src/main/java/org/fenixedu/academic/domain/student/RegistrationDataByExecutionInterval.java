package org.fenixedu.academic.domain.student;

import java.util.Optional;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.bennu.core.domain.Bennu;

/**
 * 
 * @author shezad
 *
 */
public class RegistrationDataByExecutionInterval extends RegistrationDataByExecutionInterval_Base {

    protected RegistrationDataByExecutionInterval() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    protected RegistrationDataByExecutionInterval(final Registration registration, final ExecutionInterval executionInterval) {
        this();
        setRegistration(registration);
        setExecutionInterval(executionInterval);
    }

    public static RegistrationDataByExecutionInterval getOrCreateRegistrationDataByInterval(final Registration registration,
            final ExecutionInterval executionInterval) {
        final Optional<RegistrationDataByExecutionInterval> existingData =
                registration.getRegistrationDataByExecutionIntervalsSet().stream()
                        .filter(registrationData -> registrationData.getExecutionInterval() == executionInterval).findAny();
        return existingData.isPresent() ? existingData.get() : new RegistrationDataByExecutionInterval(registration,
                executionInterval);
    }

    public void delete() {
        getSchoolClassEnrolmentPreferencesSet().clear();
        setExecutionInterval(null);
        setRegistration(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

}
