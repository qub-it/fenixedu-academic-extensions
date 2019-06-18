package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.enrolment.OptionalDegreeModuleToEnrol;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class CreditsEnroledAsFirstTime extends CreditsEnroledAsFirstTime_Base {

    protected CreditsEnroledAsFirstTime() {
        super();
    }

    @Atomic
    static public CreditsEnroledAsFirstTime create(final CurricularPeriodConfiguration configuration, final BigDecimal credits) {

        final CreditsEnroledAsFirstTime result = new CreditsEnroledAsFirstTime();
        result.init(configuration, credits, (Integer) null /* semester */);

        return result;
    }

    @Override
    public RuleResult execute(EnrolmentContext enrolmentContext) {

        BigDecimal total = BigDecimal.ZERO;

        for (final IDegreeModuleToEvaluate degreeModuleToEvaluate : enrolmentContext.getDegreeModulesToEvaluate()) {

            if (!degreeModuleToEvaluate.isLeaf()) {
                continue;
            }

            if (isFirstTimeEnrolment(enrolmentContext, degreeModuleToEvaluate)) {
                total = total.add(BigDecimal.valueOf(degreeModuleToEvaluate.getEctsCredits()));
            }
        }

        return total.compareTo(getCredits()) <= 0 ? createTrue() : createFalseLabelled(total);
    }

    private boolean isFirstTimeEnrolment(EnrolmentContext enrolmentContext, IDegreeModuleToEvaluate degreeModuleToEvaluate) {

        final Registration registration = enrolmentContext.getStudentCurricularPlan().getRegistration();

        for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {

            final Collection<Enrolment> filteredEnrolments =
                    studentCurricularPlan
                            .getEnrolmentsSet()
                            .stream()
                            .filter(e -> enrolmentContext.isToEvaluateRulesByYear() ? e.getExecutionYear().isBefore(
                                    enrolmentContext.getExecutionYear()) : e.getExecutionPeriod().isBefore(
                                    enrolmentContext.getExecutionPeriod())).collect(Collectors.toSet());

            for (final CurricularCourse curricularCourse : collectCurricularCoursesToInspect(degreeModuleToEvaluate)) {

                if (filteredEnrolments.stream().anyMatch(e -> e.getCurricularCourse() == curricularCourse)) {
                    return false;
                }

            }

        }

        return true;

    }

    private Collection<CurricularCourse> collectCurricularCoursesToInspect(IDegreeModuleToEvaluate degreeModuleToEvaluate) {

        final Collection<CurricularCourse> result = Sets.newHashSet();

        //TODO: find cleaner way to collect all curricular courses without casting to subclass
        if (degreeModuleToEvaluate instanceof OptionalDegreeModuleToEnrol) {
            final OptionalDegreeModuleToEnrol optionalDegreeModuleToEnrol = (OptionalDegreeModuleToEnrol) degreeModuleToEvaluate;
            result.addAll(getAllCurricularCourses(optionalDegreeModuleToEnrol.getCurricularCourse()));
        } else {
            result.addAll(getAllCurricularCourses((CurricularCourse) degreeModuleToEvaluate.getDegreeModule()));
        }

        return result;

    }

    private Collection<CurricularCourse> getAllCurricularCourses(CurricularCourse curricularCourse) {
        return curricularCourse.getCompetenceCourse().getAssociatedCurricularCoursesSet();
    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(), getCredits().toString());
    }

}
