package org.fenixedu.academic.domain.curricularRules.curricularPeriod.transition;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.Atomic;

public class EnrolmentYears extends EnrolmentYears_Base {

    protected EnrolmentYears() {
        super();
    }

    public BigDecimal getYears() {
        return super.getValue();
    }

    @Atomic
    static public EnrolmentYears create(final CurricularPeriodConfiguration configuration, final BigDecimal years) {

        final EnrolmentYears result = new EnrolmentYears();
        result.init(configuration, years, null /*yearMin*/, null /*yearMax*/);

        return result;

    }

    @Override
    public String getLabel() {
        return BundleUtil.getString(MODULE_BUNDLE, "label." + this.getClass().getSimpleName(), getYears().toString());
    }

    @Override
    public RuleResult execute(Curriculum curriculum) {
        final Registration registration = curriculum.getStudentCurricularPlan().getRegistration();
        
        final ExecutionYear executionYear = curriculum.getExecutionYear() == null ? ExecutionYear
                .findCurrent(registration.getDegree().getCalendar()) : curriculum.getExecutionYear();

        final Collection<ExecutionYear> enrolmentYears = RegistrationServices.getEnrolmentYears(registration).stream()
                .filter(x -> x.isBefore(executionYear)).collect(Collectors.toSet());

        return enrolmentYears.size() >= getValue().intValue() ? createTrue() : createFalseLabelled(getValue());

    }
}
