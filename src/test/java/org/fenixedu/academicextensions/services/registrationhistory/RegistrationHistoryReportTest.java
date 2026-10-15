package org.fenixedu.academicextensions.services.registrationhistory;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createCurricularCourse;
import static org.fenixedu.academic.domain.student.RegistrationServicesTest.enrol;
import static org.fenixedu.academic.domain.student.RegistrationTest.createRegistration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.EvaluationSeasonTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class RegistrationHistoryReportTest {

    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CurricularCourse curricularCourse;
    private static Registration registration;
    private static StudentCurricularPlan studentCurricularPlan;
    private static Enrolment enrolment;
    private static RegistrationHistoryReport report;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            EnrolmentTest.init();

            executionYear = ExecutionYear.findCurrent(null);
            firstSemester = executionYear.getFirstExecutionPeriod();
            degreeCurricularPlan = createDegreeCurricularPlan("Registration History Degree");

            CurricularPeriod firstYearPeriod =
                    new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
            CurricularPeriod firstSemesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, firstYearPeriod);
            CourseGroup courseGroup =
                    new CourseGroup(degreeCurricularPlan.getRoot(), "Mandatory", "Mandatory", executionYear, null, null);
            curricularCourse = createCurricularCourse("C1", "Course 1", new BigDecimal(6), firstSemesterPeriod,
                    firstSemester, courseGroup);

            degreeCurricularPlan.createExecutionDegree(executionYear);

            Student student = StudentTest.createStudent("Student A", "studentA");
            registration = createRegistration(student, degreeCurricularPlan, executionYear);
            studentCurricularPlan = registration.getLastStudentCurricularPlan();

            enrol(studentCurricularPlan, executionYear, curricularCourse);
            enrolment = studentCurricularPlan.getEnrolments(curricularCourse).iterator().next();

            report = new RegistrationHistoryReport(registration, executionYear);

            return null;
        });
    }

    @Test
    public void testRegistrationHistoryReport_getImprovementEvaluations_hasImprovementEvaluations() {
        assertFalse(report.getHasImprovementEvaluations());

        EnrolmentEvaluation improvementEvaluation = createImprovementEvaluation(enrolment);

        assertTrue(report.getHasImprovementEvaluations());
        assertEquals(1, report.getImprovementEvaluations().size());
        assertTrue(report.getImprovementEvaluations().contains(improvementEvaluation));

        EnrolmentEvaluation otherImprovementEvaluation = createImprovementEvaluationForAnotherRegistration();

        assertEquals(1, report.getImprovementEvaluations().size());
        assertFalse(report.getImprovementEvaluations().contains(otherImprovementEvaluation));

        RegistrationHistoryReport otherReport = new RegistrationHistoryReport(otherImprovementEvaluation.getRegistration(), executionYear);

        assertTrue(otherReport.getHasImprovementEvaluations());
        assertEquals(1, otherReport.getImprovementEvaluations().size());
        assertTrue(otherReport.getImprovementEvaluations().contains(otherImprovementEvaluation));
        assertFalse(otherReport.getImprovementEvaluations().contains(improvementEvaluation));

        improvementEvaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.ANNULED_OBJ);

        assertFalse(report.getHasImprovementEvaluations());
        assertTrue(otherReport.getHasImprovementEvaluations());
    }

    // Helpers

    private static EnrolmentEvaluation createImprovementEvaluationForAnotherRegistration() {
        Student otherStudent = StudentTest.createStudent("Other Student", "otherStudent");
        Registration otherRegistration = createRegistration(otherStudent, degreeCurricularPlan, executionYear);
        StudentCurricularPlan otherStudentCurricularPlan = otherRegistration.getLastStudentCurricularPlan();

        enrol(otherStudentCurricularPlan, executionYear, curricularCourse);
        Enrolment otherEnrolment = otherStudentCurricularPlan.getEnrolments(curricularCourse).iterator().next();

        return createImprovementEvaluation(otherEnrolment);
    }

    private static EnrolmentEvaluation createImprovementEvaluation(Enrolment enrolment) {
        EvaluationSeason improvement =
                EvaluationSeason.findByCode(EvaluationSeasonTest.IMPROVEMENT_SEASON_CODE).orElseThrow();

        EnrolmentEvaluation evaluation = new EnrolmentEvaluation(enrolment, improvement);
        evaluation.setExecutionInterval(firstSemester);

        return evaluation;
    }

}
