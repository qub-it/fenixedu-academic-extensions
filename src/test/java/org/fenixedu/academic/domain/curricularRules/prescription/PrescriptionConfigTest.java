package org.fenixedu.academic.domain.curricularRules.prescription;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.student.RegistrationTest.createRegistration;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@Ignore // Just for compiling purposes, remove this annotation if the test file grows with non-private methods
@RunWith(FenixFrameworkRunner.class)
public class PrescriptionConfigTest {

    private static Registration registration, precedentRegistration;
    private static ExecutionYear executionYear, previousExecutionYear, nextExecutionYear;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            EnrolmentTest.init();

            executionYear = ExecutionYear.findCurrent(null);
            previousExecutionYear = executionYear.getPrevious().getExecutionYear();
            nextExecutionYear = executionYear.getNext().getExecutionYear();

            DegreeCurricularPlan degreeCurricularPlan = createDegreeCurricularPlan("Prescription Config Degree");
            degreeCurricularPlan.createExecutionDegree(executionYear);

            DegreeCurricularPlan precedentDegreeCurricularPlan = createDegreeCurricularPlan("Prescription Config Precedent Degree");
            precedentDegreeCurricularPlan.createExecutionDegree(executionYear);

            Student student = StudentTest.createStudent("Student A", "studentA");
            registration = createRegistration(student, degreeCurricularPlan, executionYear);

            // Create a precedent degree registration
            registration.getDegree().addPrecedentDegrees(precedentDegreeCurricularPlan.getDegree());
            precedentRegistration = createRegistration(student, precedentDegreeCurricularPlan, executionYear);

            return null;
        });
    }

//    @Test
//    public void testPrescriptionConfig_getLastReingressionYearIncludingPrecedentRegistrations() {
//        assertNull(getLastReingressionYear());
//
//        // Create a reingression for the precedent registration
//        createReingression(precedentRegistration, previousExecutionYear);
//        assertEquals(previousExecutionYear, getLastReingressionYear());
//
//        createReingression(registration, executionYear);
//        assertEquals(executionYear, getLastReingressionYear());
//
//        createReingression(registration, nextExecutionYear);
//        assertEquals(nextExecutionYear, getLastReingressionYear());
//    }

    // Helpers

//    private static ExecutionYear getLastReingressionYear() {
//        return PrescriptionConfig.getLastReingressionYearIncludingPrecedentRegistrations(registration);
//    }
//
//    private static void createReingression(Registration registration, ExecutionYear executionYear) {
//        RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registration, executionYear)
//                .createReingression(new LocalDate());
//    }

}
