package org.fenixedu.academic.domain.student.mobility;

import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.Atomic;

public class MobilityScientificArea extends MobilityScientificArea_Base {

    private MobilityScientificArea() {
        super();
        setBennu(Bennu.getInstance());
    }

    private MobilityScientificArea(final String code, final LocalizedString name) {
        this();
        setCode(code);
        setName(name);
    }

    @Atomic
    public static final MobilityScientificArea create(final String code, final LocalizedString name) {
        return new MobilityScientificArea(code, name);
    }

    public boolean isDeletable() {
        return getMobilityRegistrationInformationsSet().isEmpty();
    }

    public void delete() {
        if (!isDeletable()) {
            throw new AcademicExtensionsDomainException("error.MobilityScientificArea.cannot.delete");
        }

        setBennu(null);
        super.deleteDomainObject();
    }

}
