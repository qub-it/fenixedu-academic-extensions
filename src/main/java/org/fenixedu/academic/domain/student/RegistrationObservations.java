package org.fenixedu.academic.domain.student;

import org.apache.commons.lang.StringEscapeUtils;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class RegistrationObservations extends RegistrationObservations_Base {

    private static final String APPEND = "(...)";
    private static final int LIMIT_NUMBER_OF_LINES = 6;
    private static final int LIMIT_NUMBER_OF_CHARS = 200;

    protected RegistrationObservations() {
        setWhenCreated(new DateTime());
        setCreatedBy(Authenticate.getUser().getUsername());
        updateLastModificationData();
    }

    public RegistrationObservations(Registration registration) {
        this();
        setRegistration(registration);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        updateLastModificationData();
    }

    private void updateLastModificationData() {
        setWhenUpdated(new DateTime());
        setUpdatedBy(Authenticate.getUser().getUsername());
    }

    public void delete() {
        setRegistration(null);
        deleteDomainObject();
    }

    public String getAsHtml() {
        return getValue() != null ? StringEscapeUtils.escapeHtml(getValue()).replaceAll("\r\n", "<br>") : null;
    }

    public String getAsLimitedHtml() {
        String value = getValue();
        if (value == null) {
            return null;
        }
        if (value.length() > LIMIT_NUMBER_OF_CHARS) {
            value = value.substring(0, LIMIT_NUMBER_OF_CHARS) + APPEND;
        }
        int nthIndexOfLineBreak = nthIndexOf(value, '\n', LIMIT_NUMBER_OF_LINES);
        if (nthIndexOfLineBreak > -1) {
            value = value.substring(0, nthIndexOfLineBreak) + "\r\n" + APPEND;
        }
        return value.replaceAll("\r\n", "<br>");
    }

    private static int nthIndexOf(String s, char c, int n) {
        int i = -1;
        while (n-- > 0) {
            i = s.indexOf(c, i + 1);
            if (i == -1)
                return -1;
        }
        return i;
    }

    public static void setupDeleteListener() {
        FenixFramework.getDomainModel().registerDeletionListener(Registration.class, registration -> {
            for (RegistrationObservations observations : registration.getRegistrationObservationsSet()) {
                observations.delete();
            }
        });
    }

}
