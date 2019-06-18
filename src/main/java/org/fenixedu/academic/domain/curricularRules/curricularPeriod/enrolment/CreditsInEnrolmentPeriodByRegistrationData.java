package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;

import pt.ist.fenixframework.Atomic;

public class CreditsInEnrolmentPeriodByRegistrationData extends CreditsInEnrolmentPeriodByRegistrationData_Base {

    protected CreditsInEnrolmentPeriodByRegistrationData() {
        super();
    }

    @Atomic
    static public CreditsInEnrolmentPeriodByRegistrationData create(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits, final Integer semester, final Integer year) {

        final CreditsInEnrolmentPeriodByRegistrationData result = new CreditsInEnrolmentPeriodByRegistrationData();
        result.init(configuration, credits, semester);
        result.setYearMin(year);
        result.setYearMax(year);

        return result;
    }

    @Override
    public RuleResult execute(final EnrolmentContext enrolmentContext) {

        final Registration registration = enrolmentContext.getRegistration();

        final Set<RegistrationDataByExecutionYear> datas = registration.getRegistrationDataByExecutionYearSet().stream()
                .filter(i -> i.getEnrolmentDate() != null).collect(Collectors.toSet());

        if (isReingression(registration) || datas.size() > 1) {
            return createNA();
        }

        return super.execute(enrolmentContext);
    }

    private boolean isReingression(Registration registration) {
        return registration.getRegistrationDataByExecutionYearSet().stream().anyMatch(x -> x.isReingression());
    }

}
