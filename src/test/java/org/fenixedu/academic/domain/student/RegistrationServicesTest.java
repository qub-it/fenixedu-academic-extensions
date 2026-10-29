package org.fenixedu.academic.domain.student;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createCurricularCourse;
import static org.fenixedu.academic.domain.student.RegistrationTest.createRegistration;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@Ignore // Just for compiling purposes, remove this annotation if the test file grows with non-private methods
@RunWith(FenixFrameworkRunner.class)
public class RegistrationServicesTest {

    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static DegreeCurricularPlan degreeCurricularPlan;
    private static Registration registration;
    private static StudentCurricularPlan studentCurricularPlan;
    private static Enrolment enrolment;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            EnrolmentTest.init();

            executionYear = ExecutionYear.findCurrent(null);
            firstSemester = executionYear.getFirstExecutionPeriod();
            degreeCurricularPlan = createDegreeCurricularPlan("Registration Services Degree");

            CurricularPeriod firstYearPeriod =
                    new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
            CurricularPeriod firstSemesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, firstYearPeriod);
            CourseGroup courseGroup =
                    new CourseGroup(degreeCurricularPlan.getRoot(), "Mandatory", "Mandatory", executionYear, null, null);
            CurricularCourse curricularCourse = createCurricularCourse("CA", "Course A", new BigDecimal(6), firstSemesterPeriod,
                    firstSemester, courseGroup);

            degreeCurricularPlan.createExecutionDegree(executionYear);

            Student student = StudentTest.createStudent("Student A", "studentA");
            registration = createRegistration(student, degreeCurricularPlan, executionYear);
            studentCurricularPlan = registration.getLastStudentCurricularPlan();

            enrol(studentCurricularPlan, firstSemester, curricularCourse);
            enrolment = studentCurricularPlan.getEnrolmentsSet().iterator().next();

            return null;
        });
    }

//    @Test
//    public void testRegistrationServices_hasCreditsAcrossOwnPlans() {
//        assertFalse(RegistrationServices.hasCreditsAcrossOwnPlans(registration));
//
//        // Credits sourced from an enrolment of the registration's own plan
//        newCredits(studentCurricularPlan, firstSemester, enrolment);
//        assertTrue(RegistrationServices.hasCreditsAcrossOwnPlans(registration));
//
//        // Credits of another registration, sourced from a plan that doesn't belong to it
//        Student otherStudent = StudentTest.createStudent("Other Student", "otherStudent");
//        Registration otherRegistration = createRegistration(otherStudent, degreeCurricularPlan, executionYear);
//        newCredits(otherRegistration.getLastStudentCurricularPlan(), firstSemester, enrolment);
//
//        assertFalse(RegistrationServices.hhasCreditsAcrossOwnPlans(otherRegistration));
//    }

    // Helpers

    public static void enrol(StudentCurricularPlan studentCurricularPlan, ExecutionInterval executionInterval,
            CurricularCourse curricularCourse) {
        Context context = curricularCourse.getParentContextsSet().iterator().next();
        EnrolmentTest.createEnrolment(studentCurricularPlan, executionInterval, context, "admin");
    }

//    private static Credits newCredits(StudentCurricularPlan studentCurricularPlan, ExecutionInterval executionInterval,
//            IEnrolment enrolment) {
//        Credits credits = new Credits();
//        credits.setStudentCurricularPlan(studentCurricularPlan);
//        credits.setExecutionPeriod(executionInterval);
//
//        EnrolmentWrapper.create(credits, enrolment);
//
//        return credits;
//    }
}
