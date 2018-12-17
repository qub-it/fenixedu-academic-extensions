package org.fenixedu.academic.domain.student.mobility;

import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class MobilityProgrammeLevel extends MobilityProgrammeLevel_Base {

    private MobilityProgrammeLevel() {
        super();
        setBennu(Bennu.getInstance());
    }

    protected MobilityProgrammeLevel(final String code, final LocalizedString name, final boolean otherLevel) {
        this();

        setCode(code);
        setName(name);
        setOtherLevel(otherLevel);
    }

    public boolean isOtherLevel() {
        return getOtherLevel();
    }

    public static MobilityProgrammeLevel create(final String code, final LocalizedString name, final boolean otherLevel) {
        return new MobilityProgrammeLevel(code, name, otherLevel);
    }

    public boolean isDeletable() {
        return getMobilityRegistrationInformationsSet().isEmpty() && getMobilityRegistrationInformationsForOriginSet().isEmpty();
    }

    public void delete() {
        if (!isDeletable()) {
            throw new AcademicExtensionsDomainException("error.MobilityProgrammeLevel.cannot.delete");
        }

        setBennu(null);
        super.deleteDomainObject();
    }

}
