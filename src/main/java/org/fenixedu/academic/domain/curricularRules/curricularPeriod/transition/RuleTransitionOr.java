package org.fenixedu.academic.domain.curricularRules.curricularPeriod.transition;

import java.math.BigDecimal;
import java.util.List;

import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodRule;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.RuleTransition;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;

public class RuleTransitionOr extends RuleTransitionOr_Base {

    static final private Logger logger = LoggerFactory.getLogger(CurricularPeriodConfiguration.class);

    protected RuleTransitionOr() {
        super();
    }

    protected void init(final CurricularPeriodConfiguration configuration, final List<RuleTransition> children) {
        super.init(configuration, BigDecimal.valueOf(children.size()), null, null);
        children.forEach(x -> addChildRule(x));
        checkRules();
    }

    private void checkRules() {
        if (getChildrenSet().isEmpty()) {
            throw new DomainException("error.RuleTransitionOr.at.least.one.child.is.required");
        }
    }

    @Atomic
    static public RuleTransitionOr create(final CurricularPeriodConfiguration configuration,
            final List<RuleTransition> children) {
        final RuleTransitionOr result = new RuleTransitionOr();
        result.init(configuration, children);

        return result;
    }

    @Override
    public RuleResult execute(final Curriculum curriculum) {

        final List<RuleResult> falseResults = Lists.newArrayList();
        for (final RuleTransition child : getChildrenSet()) {
            final RuleResult childResult = child.execute(curriculum);
            if (childResult.isTrue()) {
                return childResult;
            } else {
                falseResults.add(childResult);
            }
        }

        RuleResult result = RuleResult.createInitialFalse();
        final Registration registration = curriculum.getStudentCurricularPlan().getRegistration();
        for (final RuleResult iter : falseResults) {
            result = result.and(iter);
            logger.debug("[RULE !true] [REG][{}][{}]", registration.getNumber(), CurricularPeriodRule.getMessages(iter));
        }

        return result;
    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName());
    }

}
