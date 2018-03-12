package org.fenixedu.academicextensions.domain.services.person;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academicextensions.domain.person.dataShare.DataShareAuthorization;
import org.fenixedu.academicextensions.domain.person.dataShare.DataShareAuthorizationType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.Group;

abstract public class DataShareAuthorizationServices {

    static public boolean isDataShareAllowed(final Person person, final DataShareAuthorizationType type) {
        final DataShareAuthorization authorization = DataShareAuthorization.findLatest(person, type);
        return authorization != null && authorization.getAllow();
    }

    static public boolean isDataShareAllowedForComercialUse(final Person person, final DataShareAuthorizationType type) {
        final DataShareAuthorization authorization = DataShareAuthorization.findLatest(person, type);
        return authorization != null && authorization.getAllowComercialUse();
    }

    static public boolean isDataShareAllowedForProfessionalUse(final Person person, final DataShareAuthorizationType type) {
        final DataShareAuthorization authorization = DataShareAuthorization.findLatest(person, type);
        return authorization != null && authorization.getAllowProfessionalUse();
    }

    static public Set<DataShareAuthorization> findLatest(final Person person) {
        final Set<DataShareAuthorization> result = new HashSet<>();

        for (final DataShareAuthorizationType type : Bennu.getInstance().getDataShareAuthorizationTypeSet()) {
            final DataShareAuthorization auth = DataShareAuthorization.findLatest(person, type);
            if (auth != null) {
                result.add(auth);
            }
        }

        return result;
    }

    static public Set<DataShareAuthorizationType> findActiveNotAnswered(final Person person) {
        final Set<DataShareAuthorizationType> answered =
                findLatest(person).stream().map(i -> i.getType()).collect(Collectors.toSet());

        return Bennu.getInstance().getDataShareAuthorizationTypeSet().stream().filter(
                i -> i.isActive() && !answered.contains(i) && Group.parse(i.getGroupExpression()).isMember(person.getUser()))
                .collect(Collectors.toSet());
    }

}
