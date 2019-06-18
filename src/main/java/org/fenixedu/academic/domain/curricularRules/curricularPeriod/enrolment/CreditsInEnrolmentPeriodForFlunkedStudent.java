package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;

import pt.ist.fenixframework.Atomic;

public class CreditsInEnrolmentPeriodForFlunkedStudent extends CreditsInEnrolmentPeriodForFlunkedStudent_Base {

    protected CreditsInEnrolmentPeriodForFlunkedStudent() {
        super();
    }

    @Atomic
    static public CreditsInEnrolmentPeriodForFlunkedStudent create(final CurricularPeriodConfiguration configuration,
            final BigDecimal credits, final Integer semester, final Integer year) {

        final CreditsInEnrolmentPeriodForFlunkedStudent result = new CreditsInEnrolmentPeriodForFlunkedStudent();
        result.init(configuration, credits, semester);
        result.setYearMin(year);
        result.setYearMax(year);

        return result;
    }

    @Override
    public RuleResult execute(EnrolmentContext enrolmentContext) {

        final Registration registration = enrolmentContext.getRegistration();

        final ExecutionYear year = enrolmentContext.getExecutionYear();
        if (registration.getStartExecutionYear() == year) {
            return createNA();
        }

        final boolean flunked = RegistrationServices.isFlunkedUsingCurricularYear(registration, year);
        return flunked ? super.execute(enrolmentContext) : createNA();
    }

}
