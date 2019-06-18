package org.fenixedu.academic.domain.enrolment.period;

import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public enum AutomaticEnrolment {

    NO, YES_EDITABLE, YES_UNEDITABLE;

    public boolean isAutomatic() {
        return this != NO;
    }

    public boolean isEditable() {
        return this != YES_UNEDITABLE;
    }

    public LocalizedString getDescriptionI18N() {
        return AcademicExtensionsUtil.bundleI18N(getClass().getSimpleName() + "." + name());
    }
}
