package org.fenixedu.academic.domain.person.services;

import org.fenixedu.academic.domain.Person;

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

}
