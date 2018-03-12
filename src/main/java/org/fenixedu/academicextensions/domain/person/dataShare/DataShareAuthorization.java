package org.fenixedu.academicextensions.domain.person.dataShare;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class DataShareAuthorization extends DataShareAuthorization_Base {

    protected DataShareAuthorization() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Atomic
    public void delete() {
        setRoot(null);

        setPerson(null);
        setType(null);
        setChoice(null);

        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        super.deleteDomainObject();
    }

    @Override
    protected void checkForDeletionBlockers(final Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    protected void init(final Person person, final DataShareAuthorizationType type, final DataShareAuthorizationChoice choice,
            final DateTime since) {

        setPerson(person);
        setType(type);
        setChoice(choice);
        setSince(since);

        checkRules();
    }

    public void checkRules() {

        if (getPerson() == null) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorization.person.required");
        }

        if (getType() == null) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorization.type.required");
        }

        if (getChoice() == null) {
            throw new AcademicExtensionsDomainException("error.DataShareAuthorization.choice.required");
        }
    }

    @Atomic
    static public DataShareAuthorization create(final Person person, final DataShareAuthorizationType type,
            final DataShareAuthorizationChoice choice, final DateTime since) {

        final DataShareAuthorization result = new DataShareAuthorization();
        result.init(person, type, choice, since);
        return result;
    }

    @Atomic
    public DataShareAuthorization edit(final Person person, final DataShareAuthorizationType type,
            final DataShareAuthorizationChoice choice, final DateTime since) {

        this.init(person, type, choice, since);
        return this;
    }

    static public Set<DataShareAuthorization> find(final Person person, final DataShareAuthorizationType type,
            final DateTime when) {

        final Stream<DataShareAuthorization> universe =
                person != null ? person.getDataShareAuthorizationSet().stream() : type != null ? type.getAuthorizationSet()
                        .stream() : Bennu.getInstance().getDataShareAuthorizationSet().stream();
        return universe

                .filter(i -> person == null || i.getPerson() == person)

                .filter(i -> type == null || i.getType() == type)

                .filter(i -> when == null || i.getSince().isBefore(when))

                .collect(Collectors.toSet());
    }

    static public DataShareAuthorization findLatest(final Person person, final DataShareAuthorizationType type) {
        final Set<DataShareAuthorization> found = find(person, type, new DateTime());
        return found.stream().max(Comparator.comparing(DataShareAuthorization::getSince)).orElse(null);
    }

    public boolean isLast() {
        final DataShareAuthorization active = findLatest(getPerson(), getType());
        return active == this;
    }

    public boolean getAllow() {
        return getChoice().getAllow();
    }

    public boolean getAllowComercialUse() {
        return getChoice().getAllowComercialUse();
    }

    public boolean getAllowProfessionalUse() {
        return getChoice().getAllowProfessionalUse();
    }

}
