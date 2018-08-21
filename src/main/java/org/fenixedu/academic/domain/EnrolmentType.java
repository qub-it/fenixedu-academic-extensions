package org.fenixedu.academic.domain;

import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;

public class EnrolmentType extends EnrolmentType_Base {

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

    public static EnrolmentType find(final String code) {
        return Bennu.getInstance().getEnrolmentTypesSet().stream().filter(et -> et.getCode() != null && et.getCode().equals(code))
                .findFirst().orElse(null);
    }

    public void delete() {
        setRoot(null);
        super.deleteDomainObject();
    }

}
