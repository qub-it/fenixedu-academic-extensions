package org.fenixedu.academic.domain.evaluation.season;

import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;

@SuppressWarnings("deprecation")
public enum EvaluationSeasonPeriodType {

    GRADE_SUBMISSION {

        @Override
        protected OccupationPeriodType translate() {
            // Note: OccupationPeriodType.XPTO_SPECIAL_SEASON is never persisted
            return OccupationPeriodType.GRADE_SUBMISSION;
        }
    },

    EXAMS {

        @Override
        protected OccupationPeriodType translate() {
            // Note: OccupationPeriodType.XPTO_SPECIAL_SEASON is never persisted
            return OccupationPeriodType.EXAMS;
        }
    };

    public LocalizedString getDescriptionI18N() {
        return AcademicExtensionsUtil.bundleI18N(name());
    }

    abstract protected OccupationPeriodType translate();

    static protected EvaluationSeasonPeriodType get(final OccupationPeriod input) {
        switch (input.getExecutionDegreesSet().iterator().next().getPeriodType()) {

        case EXAMS:
        case EXAMS_SPECIAL_SEASON:
            return EvaluationSeasonPeriodType.EXAMS;
        case GRADE_SUBMISSION:
        case GRADE_SUBMISSION_SPECIAL_SEASON:
            return EvaluationSeasonPeriodType.GRADE_SUBMISSION;
        default:
            throw new RuntimeException();
        }
    }

}