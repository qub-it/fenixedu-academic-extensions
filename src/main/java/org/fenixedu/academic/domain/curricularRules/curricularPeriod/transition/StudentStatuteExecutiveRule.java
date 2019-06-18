package org.fenixedu.academic.domain.curricularRules.curricularPeriod.transition;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.services.StatuteServices;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.Atomic;

public class StudentStatuteExecutiveRule extends StudentStatuteExecutiveRule_Base {

    protected StudentStatuteExecutiveRule() {
        super();
    }

    @Override
    public boolean isExecutive() {
        return true;
    }

    @Atomic
    static public StudentStatuteExecutiveRule create(final CurricularPeriodConfiguration configuration,
            final StatuteType statuteType) {

        final StudentStatuteExecutiveRule result = new StudentStatuteExecutiveRule();
        result.init(configuration, statuteType);
        return result;
    }

    protected void init(final CurricularPeriodConfiguration configuration, final StatuteType statuteType) {
        super.init(configuration,
                (BigDecimal) BigDecimal.ZERO/* credits; 0 hack, this rule does not need ects, but its a superclass requirement  */,
                (Integer) null /* yearMin */, (Integer) null /* yearMax */);
        super.setStatuteTypeForRuleTransition(statuteType);
        checkRules();
    }

    public StatuteType getStatuteType() {
        return super.getStatuteTypeForRuleTransition();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getStatuteType() == null) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".statuteType.required");
        }
    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(),
                getStatuteType().getName().getContent());
    }

    @Override
    public RuleResult execute(final Curriculum curriculum) {
        final Registration registration = curriculum.getStudentCurricularPlan().getRegistration();
        final ExecutionYear executionYear = curriculum.getExecutionYear() == null ? ExecutionYear
                .findCurrent(registration.getDegree().getCalendar()) : curriculum.getExecutionYear();

        for (final StatuteType iter : StatuteServices.findStatuteTypes(registration, executionYear)) {
            if (getStatuteType() == iter) {
                return createTrue();
            }
        }

        return createFalseLabelled(
                getMessagesSuffix("label." + this.getClass().getSimpleName() + ".suffix", executionYear.getQualifiedName()));
    }

}
