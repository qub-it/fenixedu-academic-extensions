package org.fenixedu.academic.domain.student;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.fenixedu.academic.domain.student.Registration;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class RegistrationObservations extends RegistrationObservations_Base {

    private static final String APPEND = "(...)";
    private static final int LIMIT_NUMBER_OF_LINES = 6;
    private static final int LIMIT_NUMBER_OF_CHARS = 200;

    public RegistrationObservations(Registration registration) {
        super();
        setRegistration(registration);
    }

    @Override
    public Registration getRegistration() {
        return super.getRegistration();
    }

    public static void setupDeleteListener() {
        FenixFramework.getDomainModel().registerDeletionListener(Registration.class, registration -> {
            for (RegistrationObservations observations : registration.getRegistrationObservationsSet()) {
                observations.delete();
            }
        });
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

    static Comparator<Object> compareByModifiedDate = (x, y) -> {
        DateTime xUpdateDate = readVersioningUpdateDate(x);
        DateTime yUpdateDate = readVersioningUpdateDate(y);
        
        if (xUpdateDate == null || yUpdateDate == null) {
            return 0;
        }
        return xUpdateDate.compareTo(yUpdateDate);
    };

    public static List<RegistrationObservations> getLastThreeSortedObservations(Registration registration) {
        return registration.getRegistrationObservationsSet().stream().sorted(compareByModifiedDate.reversed()).limit(3)
                .collect(Collectors.toList());
    }

    public static List<RegistrationObservations> getReverseSortedObservations(Registration registration) {
        return registration.getRegistrationObservationsSet().stream().sorted(compareByModifiedDate.reversed())
                .collect(Collectors.toList());
    }

    public static int nthIndexOf(String s, char c, int n) {
        int i = -1;
        while (n-- > 0) {
            i = s.indexOf(c, i + 1);
            if (i == -1)
                return -1;
        }
        return i;
    }
    
    public static <T> String readVersioningUpdatorUsername(T obj) {
        try {
            Object versioningUpdatedBy = PropertyUtils.getProperty(obj, "versioningUpdatedBy");

            if (versioningUpdatedBy == null) {
                return null;
            }

            return (String) PropertyUtils.getProperty(versioningUpdatedBy, "username");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> DateTime readVersioningUpdateDate(T obj) {
        try {
            Object versioningUpdateDate = PropertyUtils.getProperty(obj, "versioningUpdateDate");

            if (versioningUpdateDate == null) {
                return null;
            }

            return (DateTime) PropertyUtils.getProperty(versioningUpdateDate, "date");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    
}
