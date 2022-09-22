package org.fenixedu.academic.domain.evaluation.season;

import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;

public enum EvaluationSeasonPeriodType {

    GRADE_SUBMISSION, EXAMS;

    public LocalizedString getDescriptionI18N() {
        return AcademicExtensionsUtil.bundleI18N(name());
    }

}