package org.fenixedu.academicextensions.domain.person.dataShare;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
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

        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        super.deleteDomainObject();
    }

    @Override
    protected void checkForDeletionBlockers(final Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    protected void init(final Person person, final DataShareAuthorizationType type, final DateTime since) {
        setPerson(person);
        setType(type);
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
        result.init(person, type, new DateTime());
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
        return find(person, type, null).stream()
                .max(Comparator.comparing(DataShareAuthorization::getSince).thenComparing(DataShareAuthorization::getExternalId))
                .orElse(null);
    }

    private void superSetAllow(Boolean allow) {
        super.setAllow(allow);
    }

    @Override
    public void setAllow(Boolean allow) {
        super.setAllow(allow);
        Person person = getPerson();
        if (Boolean.FALSE.equals(allow) && getType().getDataShareAuthorizationTypeParent() != null) {
            DataShareAuthorizationType dataShareAuthorizationTypeParent = getType().getDataShareAuthorizationTypeParent();
            DataShareAuthorization parentAuthorization = findLatest(person, dataShareAuthorizationTypeParent);
            Optional<DataShareAuthorizationType> anyChildStillAllowed =
                    dataShareAuthorizationTypeParent
                            .getDataShareAuthorizationTypeChildrenSet().stream().filter(x -> x.isActive()
                                    && findLatest(person, x) != null && Boolean.TRUE.equals(findLatest(person, x).getAllow()))
                            .findAny();
            // Parent authorization may not exist if the user has not yet answered the parent authorization
            // but only childs
            if (!anyChildStillAllowed.isPresent() && parentAuthorization != null) {
                parentAuthorization.superSetAllow(false);
            }
        }
        for (DataShareAuthorizationType childType : getType().getDataShareAuthorizationTypeChildrenSet()) {
            if (childType.isActive()) {
                DataShareAuthorization.create(person, childType, allow);
            }
        }

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
