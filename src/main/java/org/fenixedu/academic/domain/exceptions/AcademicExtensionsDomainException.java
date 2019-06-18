package org.fenixedu.academic.domain.exceptions;

import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.domain.exceptions.DomainException;

public class AcademicExtensionsDomainException extends DomainException {

    private static final long serialVersionUID = 1L;

    public AcademicExtensionsDomainException(String key, String... args) {
        super(AcademicExtensionsUtil.BUNDLE, key, args);
    }

    public AcademicExtensionsDomainException(Throwable cause, String key, String... args) {
        super(cause, AcademicExtensionsUtil.BUNDLE, key, args);
    }

    public static void throwWhenDeleteBlocked(Collection<String> blockers) {
        if (!blockers.isEmpty()) {
            throw new AcademicExtensionsDomainException("key.return.argument",
                    blockers.stream().collect(Collectors.joining(", ")));
        }
    }

}
