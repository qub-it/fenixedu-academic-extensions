package org.fenixedu.academic.domain.person.services;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academicextensions.domain.person.dataShare.DataShareAuthorization;
import org.fenixedu.academicextensions.domain.person.dataShare.DataShareAuthorizationChoice;
import org.fenixedu.academicextensions.domain.person.dataShare.DataShareAuthorizationType;
import org.fenixedu.academicextensions.domain.services.person.DataShareAuthorizationServices;
import org.joda.time.DateTime;

public class PersonServices {

    static public String getDisplayName(final Person input) {
        String result = "";

        if (input != null && input.getProfile() != null) {
            result = input.getProfile().getDisplayName();

            if (result.equals(input.getName()) || !result.trim().contains(" ")) {
                result = input.getFirstAndLastName();
            }
        }

        return result;
    }

    static public boolean getAuthorizeSharingDataWithCGD(final Person person) {
        return DataShareAuthorizationServices.isDataShareAllowed(person, getSharingDataWithCGD());
    }

    static public boolean isSharingDataWithCGDAnswered(final Person person) {
        return DataShareAuthorization.findLatest(person, getSharingDataWithCGD()) != null;
    }

    static private DataShareAuthorizationType getSharingDataWithCGD() {
        return DataShareAuthorizationType.findUnique("CGD");
    }

    static public void setAuthorizeSharingDataWithCGD(final Person person, final boolean authorize) {
        final DataShareAuthorizationType type = getSharingDataWithCGD();
        final DataShareAuthorizationChoice choice = type == null ? null : type.getChoiceSet().stream()
                .filter(i -> i.getAllow() == authorize).findFirst().orElse(null);
        DataShareAuthorization.create(person, type, choice, new DateTime());
    }

}
