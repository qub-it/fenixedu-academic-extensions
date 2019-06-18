package org.fenixedu.academic.domain.enrolment.period;

import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public enum AcademicEnrolmentPeriodType {

    /*
     * Attention: this order will affect the order in enrollment proccess
     */
    INITIAL_SCHOOL_CLASS, CURRICULAR_COURSE, SCHOOL_CLASS_PREFERENCE, SCHOOL_CLASS, SHIFT;

    public boolean isCurricularCourses() {
        return this == CURRICULAR_COURSE;
    }

    public boolean isShift() {
        return this == SHIFT;
    }

    public boolean isSchoolClass() {
        return this == SCHOOL_CLASS;
    }

    public LocalizedString getDescriptionI18N() {
        return AcademicExtensionsUtil.bundleI18N(getClass().getSimpleName() + "." + name());
    }

}
