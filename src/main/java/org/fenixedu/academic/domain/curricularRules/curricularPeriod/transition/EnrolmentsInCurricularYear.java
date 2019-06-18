package org.fenixedu.academic.domain.curricularRules.curricularPeriod.transition;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;

import pt.ist.fenixframework.Atomic;

public class EnrolmentsInCurricularYear extends EnrolmentsInCurricularYear_Base {

    protected EnrolmentsInCurricularYear() {
        super();
    }

    @Override
    public RuleResult execute(Curriculum curriculum) {
        final ExecutionYear executionYear = curriculum.getExecutionYear() == null ? ExecutionYear.findCurrent(curriculum
                .getStudentCurricularPlan().getRegistration().getDegree().getCalendar()) : curriculum.getExecutionYear();

        final int curricularYear = getYearMin();
        if (curriculum.getStudentCurricularPlan().getEnrolmentsByExecutionYear(executionYear).stream()
                .filter(x -> CurricularPeriodServices.getCurricularYear(x) == curricularYear).count() >= getValue().longValue()) {
            return createTrue();

        }

        return createFalseLabelled();
    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(), getValue().toString(),
                String.valueOf(getYearMin()));
    }

    @Atomic
    static public EnrolmentsInCurricularYear create(final CurricularPeriodConfiguration configuration, final BigDecimal value,
            final Integer year) {

        final EnrolmentsInCurricularYear result = new EnrolmentsInCurricularYear();
        result.init(configuration, value, year, year);

        return result;
    }

}
