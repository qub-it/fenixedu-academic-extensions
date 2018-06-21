package org.fenixedu.academicextensions.domain.person.dataShare;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.Group;
import org.joda.time.DateTime;

public class DataShareAuthorization extends DataShareAuthorization_Base {

    protected DataShareAuthorization() {
        super();
        setRoot(Bennu.getInstance());
    }

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

    }

    static public DataShareAuthorization create(final Person person, final DataShareAuthorizationType type, boolean allow) {
        final DataShareAuthorization result = new DataShareAuthorization();
        result.init(person, type, null, new DateTime());
        result.setAllow(allow);
        return result;
    }

    static private Set<DataShareAuthorization> find(final Person person, final DataShareAuthorizationType type,
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
        return find(person, type, null).stream().max(Comparator.comparing(DataShareAuthorization::getSince)).orElse(null);
    }

    public Boolean getAllow() {
        return getChoice() != null ? getChoice().getAllow() : super.getAllow(); // TODO: after choice entity removal, we can delete this override 
    }

    static public boolean isDataShareAllowed(final Person person, final DataShareAuthorizationType type) {
        final DataShareAuthorization authorization = DataShareAuthorization.findLatest(person, type);
        return authorization != null && authorization.getAllow() != null && authorization.getAllow();
    }

    static public Set<DataShareAuthorizationType> findActiveAuthorizationTypes(final Person person) {
        return Bennu.getInstance().getDataShareAuthorizationTypeSet().stream()
                .filter(type -> type.isActive() && Group.parse(type.getGroupExpression()).isMember(person.getUser()))
                .collect(Collectors.toSet());
    }

    static public Set<DataShareAuthorizationType> findActiveAuthorizationTypesNotAnswered(final Person person) {
        return findActiveAuthorizationTypes(person).stream().filter(type -> find(person, type, null).isEmpty())
                .collect(Collectors.toSet());
    }

}
