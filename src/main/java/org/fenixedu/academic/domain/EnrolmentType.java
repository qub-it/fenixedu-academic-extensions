package org.fenixedu.academic.domain;

import java.util.Set;

import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;

public class EnrolmentType extends EnrolmentType_Base {

    // possible values
    private static final String NORMAL = "NORMAL";
    private static final String FLUNKED = "FLUNKED";

    public EnrolmentType() {
        super();
        setRoot(Bennu.getInstance());
    }

    @Override
    public void setCode(String code) {
        if (find(code) != null) {
            throw new AcademicExtensionsDomainException("error.EnrolmentType.alreadyExistsTypeWithSameCode", code);
        }
        super.setCode(code);
    }

    public boolean isNormal() {
        return NORMAL.equals(getCode());
    }

    public boolean isFlunked() {
        return FLUNKED.equals(getCode());
    }

    public static EnrolmentType find(final String code) {
        return readAll().stream().filter(et -> et.getCode() != null && et.getCode().equals(code)).findFirst().orElse(null);
    }

    public static Set<EnrolmentType> readAll() {
        return Bennu.getInstance().getEnrolmentTypesSet();
    }

    public void delete() {
        setRoot(null);
        super.deleteDomainObject();
    }

}
