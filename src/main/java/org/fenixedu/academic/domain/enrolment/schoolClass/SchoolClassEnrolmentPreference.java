package org.fenixedu.academic.domain.enrolment.schoolClass;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionInterval;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

/**
 * 
 * @author shezad
 *
 */
public class SchoolClassEnrolmentPreference extends SchoolClassEnrolmentPreference_Base
        implements Comparable<SchoolClassEnrolmentPreference> {

    protected SchoolClassEnrolmentPreference() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setLastModificationDate(new DateTime());
    }

    protected SchoolClassEnrolmentPreference(final SchoolClass schoolClass, final Integer preferenceOrder,
            final RegistrationDataByExecutionInterval registrationData) {
        this();

        Objects.requireNonNull(schoolClass, "error.SchoolClassEnrolmentPreference.creation.nullSchoolClass");
        Objects.requireNonNull(preferenceOrder, "error.SchoolClassEnrolmentPreference.creation.nullPreferenceOrder");
        Objects.requireNonNull(registrationData, "error.SchoolClassEnrolmentPreference.creation.nullRegistrationData");

        // check rules
        if (findPreferenceByOrder(preferenceOrder, registrationData).isPresent()) {
            throw new DomainException("error.SchoolClassEnrolmentPreference.creation.alreadyExistsPreferenceWithSameOrder");
        }

        setSchoolClass(schoolClass);
        setPreferenceOrder(preferenceOrder);
        setRegistrationDataByExecutionInterval(registrationData);
    }

    public static void initializePreferencesForRegistration(final RegistrationDataByExecutionInterval registrationData,
            final Collection<SchoolClass> schoolClasses) {

        final Set<SchoolClass> selectedSchoolClasses = registrationData.getSchoolClassEnrolmentPreferencesSet().stream()
                .map(p -> p.getSchoolClass()).collect(Collectors.toSet());

        final AtomicInteger order = new AtomicInteger(selectedSchoolClasses.size());

        schoolClasses.stream().filter(sc -> !selectedSchoolClasses.contains(sc))
                .forEach(sc -> new SchoolClassEnrolmentPreference(sc, order.incrementAndGet(), registrationData));
    }

    public void changePreferenceOrder(boolean increment) {
        final Integer currentOrder = getPreferenceOrder();
        final Integer newOrder = currentOrder + (increment ? -1 : 1);

        final Optional<SchoolClassEnrolmentPreference> preferenceToSwap =
                findPreferenceByOrder(newOrder, getRegistrationDataByExecutionInterval());

        preferenceToSwap.ifPresent(preference -> {
            preference.setPreferenceOrder(currentOrder);
            setPreferenceOrder(newOrder);
        });
    }

    @Override
    public void setPreferenceOrder(Integer preferenceOrder) {
        super.setPreferenceOrder(preferenceOrder);
        setLastModificationDate(new DateTime());
    }

    public static Optional<SchoolClassEnrolmentPreference> findPreferenceByOrder(final Integer preferenceOrder,
            final RegistrationDataByExecutionInterval registrationData) {
        return registrationData.getSchoolClassEnrolmentPreferencesSet().stream()
                .filter(preference -> preference.getPreferenceOrder().equals(preferenceOrder)).findFirst();
    }

    public void delete() {
        setRegistrationDataByExecutionInterval(null);
        setSchoolClass(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    @Override
    public int compareTo(SchoolClassEnrolmentPreference anotherPreference) {
        return getPreferenceOrder().compareTo(anotherPreference.getPreferenceOrder());
    }

}
