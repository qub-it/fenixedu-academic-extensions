package org.fenixedu.academic.domain.curricularRules.curricularPeriod.transition;

import java.math.BigDecimal;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;

import pt.ist.fenixframework.Atomic;

public class ApprovedCourses extends ApprovedCourses_Base {

    protected ApprovedCourses() {
        super();
    }

    public BigDecimal getApprovals() {
        return super.getValue();
    }

    @Atomic
    static public ApprovedCourses create(final CurricularPeriodConfiguration configuration, final BigDecimal approvals) {
        final ApprovedCourses result = new ApprovedCourses();
        result.init(configuration, approvals, null /*yearMin*/, null /*yearMax*/);

        return result;
    }

    @Atomic
    static public ApprovedCourses createForSemester(final CurricularPeriodConfiguration configuration, final BigDecimal approvals,
            final Integer semester) {

        final ApprovedCourses result = new ApprovedCourses();
        result.init(configuration, approvals, null /*yearMin*/, null /*yearMax*/);
        result.setSemester(semester);

        return result;
    }

    @Override
    public String getLabel() {

        final String approvals = getApprovals().toString();

        if (getSemester() != null) {
            return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName() + ".semester",
                    approvals.toString(), getSemester().toString());

        } else {

            return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(), approvals);
        }
    }

    @Override
    public RuleResult execute(final Curriculum input) {
        final Curriculum curriculum = prepareCurriculum(input);

        final Predicate<ICurriculumEntry> previousYears = i -> CurricularPeriodServices
                .getCurricularYear((CurriculumLine) i) < getConfiguration().getCurricularPeriod().getChildOrder().intValue();

        final BigDecimal total = BigDecimal.valueOf(curriculum.getCurricularYearEntries().stream().filter(previousYears).count());

        return total.compareTo(getApprovals()) >= 0 ? createTrue() : createFalseLabelled(
                getMessagesSuffix("label." + this.getClass().getSimpleName() + ".suffix", total.toPlainString()));
    }

}
