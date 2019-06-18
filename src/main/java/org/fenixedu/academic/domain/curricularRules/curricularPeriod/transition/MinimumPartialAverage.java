package org.fenixedu.academic.domain.curricularRules.curricularPeriod.transition;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.Atomic;

public class MinimumPartialAverage extends MinimumPartialAverage_Base {

    protected MinimumPartialAverage() {
        super();
    }

    @Override
    public RuleResult execute(Curriculum curriculum) {
        final StudentCurricularPlan studentCurricularPlan = curriculum.getStudentCurricularPlan();
        final Set<ProgramConclusion> programConclusions =
                ProgramConclusion.conclusionsFor(studentCurricularPlan.getDegreeCurricularPlan()).filter(x -> !x.isTerminal())
                        .collect(Collectors.toSet());

        boolean result = true;
        for (final ProgramConclusion programConclusion : programConclusions) {

            final RegistrationConclusionBean bean =
                    new RegistrationConclusionBean(studentCurricularPlan.getRegistration(), programConclusion);

            if (!bean.getFinalGrade().isNumeric()) {
                continue;
            }

            result &= bean.getFinalGrade().getNumericValue().compareTo(getValue()) >= 0;
        }

        return result ? createTrue() : createFalseLabelled(getValue());
    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(), getValue().toString());
    }

    @Atomic
    static public MinimumPartialAverage create(final CurricularPeriodConfiguration configuration, final BigDecimal average) {

        final MinimumPartialAverage result = new MinimumPartialAverage();
        result.init(configuration, average, null, null);
        return result;
    }

}
