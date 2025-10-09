package org.fenixedu.academic.domain.curricularRules.curricularPeriod.enrolment;

import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createRegistration;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.enrol;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.EnrolmentPeriodRestrictions;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.EnrolmentPeriodRestrictionsExecutorLogic;
import org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.exceptions.EnrollmentDomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationRegime;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.StudentStatute;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CreditsInCurricularPeriodTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private Registration registration;

    private StudentCurricularPlan studentCurricularPlan;

    private CurricularPeriodConfiguration periodConfig1Y;

    private ExecutionYear executionYear;

    private StatuteType statuteType;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionRulesTestUtil.init();
            EnrolmentPeriodRestrictionsExecutorLogic.configure();
            return null;
        });
    }

    @Before
    public void setUp() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            this.executionYear = ExecutionYear.readExecutionYearByName("2022/2023");

            final DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan(executionYear);
            new EnrolmentPeriodRestrictions(degreeCurricularPlan.getRoot(), executionYear);

            periodConfig1Y =
                    CurricularPeriodConfiguration.create(CurricularPeriodServices.getCurricularPeriod(degreeCurricularPlan, 1));

            this.registration = createRegistration(degreeCurricularPlan, executionYear);
            this.studentCurricularPlan = this.registration.getLastStudentCurricularPlan();

            this.statuteType = StatuteType.create(UUID.randomUUID().toString(),
                    new LocalizedString.Builder().with(Locale.getDefault(), "Statute").build());

            return null;
        });
    }

    @Test
    public void givenConfigWithMaxCredits10InYearOne_whenStudentIsEnrolingIn6_thenSuccess() {
        CreditsInCurricularPeriod.createForYearInterval(periodConfig1Y, BigDecimal.TEN, 1, 1);

        enrol(studentCurricularPlan, executionYear, "C1");
    }

    @Test
    public void givenConfigWithMaxCredits10InYearOne_whenStudentIsEnrolingIn12_thenFail() {
        CreditsInCurricularPeriod.createForYearInterval(periodConfig1Y, BigDecimal.TEN, 1, 1);

        exceptionRule.expect(EnrollmentDomainException.class);
        enrol(studentCurricularPlan, executionYear, "C1");
        enrol(studentCurricularPlan, executionYear, "C2");
    }

    @Test
    public void givenConfigWithMaxCredits10InYearOneAndStatute_whenStudentIsEnrollingIn12WithNonMatchingStatute_thenSuccess() {
        CreditsInCurricularPeriod.createForYearInterval(periodConfig1Y, BigDecimal.TEN, 1, 1).addStatuteTypes(this.statuteType);

        enrol(studentCurricularPlan, executionYear, "C1");
        enrol(studentCurricularPlan, executionYear, "C2");
    }

    @Test
    public void givenConfigWithMaxCredits10InYearOneAndStatute_whenStudentIsEnrollingIn12WithMatchingStatute_thenFail() {
        CreditsInCurricularPeriod.createForYearInterval(periodConfig1Y, BigDecimal.TEN, 1, 1).addStatuteTypes(this.statuteType);

        new StudentStatute(registration.getStudent(), this.statuteType, executionYear.getFirstExecutionPeriod(),
                executionYear.getLastExecutionPeriod(), null, null, null, this.registration);

        exceptionRule.expect(EnrollmentDomainException.class);
        enrol(studentCurricularPlan, executionYear, "C1");
        enrol(studentCurricularPlan, executionYear, "C2");
    }

    @Test
    public void givenConfigWithMaxCredits10InYearOneForFlunked_whenNonFlunkedStudentIsEnrollingIn12_thenSuccess() {
        CreditsInCurricularPeriod.createForYearInterval(periodConfig1Y, BigDecimal.TEN, 1, 1).setApplyToFlunkedStudents(true);

        enrol(studentCurricularPlan, executionYear, "C1");
        enrol(studentCurricularPlan, executionYear, "C2");
    }

    @Test
    public void givenConfigWithMaxCredits10InYearOneForFlunked_whenFlunkedStudentIsEnrollingIn12_thenFail() {
        CreditsInCurricularPeriod.createForYearInterval(periodConfig1Y, BigDecimal.TEN, 1, 1).setApplyToFlunkedStudents(true);

        final ExecutionYear executionYear2324 = ExecutionYear.readExecutionYearByName("2023/2024");

        final RegistrationDataByExecutionYear dataByYear2223 =
                RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registration, executionYear);
        dataByYear2223.setActive(true);
        dataByYear2223.edit(new LocalDate());
        final RegistrationDataByExecutionYear dataByYear2324 =
                RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registration, executionYear2324);
        dataByYear2324.setActive(true);
        dataByYear2324.edit(new LocalDate());

        enrol(studentCurricularPlan, executionYear, "C1");

        exceptionRule.expect(EnrollmentDomainException.class);
        enrol(studentCurricularPlan, executionYear2324, "C1");
        enrol(studentCurricularPlan, executionYear2324, "C2");
    }

    @Test
    public void givenConfigWithMaxCredits10InYearOneForPartialRegime_whenFullRegimeStudentIsEnrollingIn12_thenSuccess() {
        CreditsInCurricularPeriod.createForYearInterval(periodConfig1Y, BigDecimal.TEN, 1, 1).setApplyToPartialRegime(true);

        enrol(studentCurricularPlan, executionYear, "C1");
        enrol(studentCurricularPlan, executionYear, "C2");
    }

    @Test
    public void givenConfigWithMaxCredits10InYearOneForPartialRegime_whenPartialRegimeStudentIsEnrollingIn12_thenFail() {
        CreditsInCurricularPeriod.createForYearInterval(periodConfig1Y, BigDecimal.TEN, 1, 1).setApplyToPartialRegime(true);

        try {
            Authenticate.mock(registration.getPerson().getUser(), "testing");
            new RegistrationRegime(registration, executionYear, RegistrationRegimeType.PARTIAL_TIME);
        } finally {
            Authenticate.unmock();
        }

        exceptionRule.expect(EnrollmentDomainException.class);
        enrol(studentCurricularPlan, executionYear, "C1");
        enrol(studentCurricularPlan, executionYear, "C2");
    }

}
