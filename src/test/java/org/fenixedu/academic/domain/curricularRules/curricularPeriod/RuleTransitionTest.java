package org.fenixedu.academic.domain.curricularRules.curricularPeriod;

import static org.fenixedu.academic.domain.DegreeCurricularPlanTest.createDegreeCurricularPlan;
import static org.fenixedu.academic.domain.curricularRules.util.ConclusionRulesTestUtil.createCurricularCourse;
import static org.fenixedu.academic.domain.student.RegistrationServicesTest.enrol;
import static org.fenixedu.academic.domain.student.RegistrationTest.createRegistration;

import java.math.BigDecimal;
import java.util.Locale;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EnrolmentTest;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionIntervalTest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@Ignore // Just for compiling purposes, remove this annotation if the test file grows with non-private methods
@RunWith(FenixFrameworkRunner.class)
public class RuleTransitionTest {

    private static ExecutionYear executionYear;
    private static ExecutionInterval firstSemester;
    private static DegreeCurricularPlan degreeCurricularPlan;
    private static CurricularCourse curricularCourse1;
    private static CurricularCourse curricularCourse2;
    private static StudentCurricularPlan studentCurricularPlan;
    private static GradeScale gradeScale;

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ExecutionIntervalTest.initRootCalendarAndExecutionYears();
            EnrolmentTest.init();

            executionYear = ExecutionYear.findCurrent(null);
            firstSemester = executionYear.getFirstExecutionPeriod();

            gradeScale = GradeScale.create("TYPE20", new LocalizedString(Locale.getDefault(), "Type 20"), new BigDecimal("0"),
                    new BigDecimal("9.49"), new BigDecimal("9.50"), new BigDecimal("20"), false, true);

            degreeCurricularPlan = createDegreeCurricularPlan("Rule Transition Degree");

            CurricularPeriod firstYearPeriod =
                    new CurricularPeriod(AcademicPeriod.YEAR, 1, degreeCurricularPlan.getDegreeStructure());
            CurricularPeriod firstSemesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 1, firstYearPeriod);
            CurricularPeriod secondSemesterPeriod = new CurricularPeriod(AcademicPeriod.SEMESTER, 2, firstYearPeriod);
            CourseGroup courseGroup =
                    new CourseGroup(degreeCurricularPlan.getRoot(), "Mandatory", "Mandatory", executionYear, null, null);
            curricularCourse1 = createCurricularCourse("C1", "Course 1", new BigDecimal(6), firstSemesterPeriod, firstSemester,
                    courseGroup);
            curricularCourse2 = createCurricularCourse("C2", "Course 2", new BigDecimal(6), secondSemesterPeriod, firstSemester,
                    courseGroup);

            degreeCurricularPlan.createExecutionDegree(executionYear);

            Student student = StudentTest.createStudent("Student A", "studentA");
            Registration registration = createRegistration(student, degreeCurricularPlan, executionYear);
            studentCurricularPlan = registration.getLastStudentCurricularPlan();

            enrol(studentCurricularPlan, executionYear, curricularCourse1);
            enrol(studentCurricularPlan, executionYear, curricularCourse2);

            return null;
        });
    }

//    @Test
//    public void testRuleTransition_filterCurricularYearEntriesBySemester() {
//        assertTrue(getCourses(RuleTransition.filterCurricularYearEntriesBySemester(getCurriculum(), 1)).isEmpty());
//        assertTrue(getCourses(RuleTransition.filterCurricularYearEntriesBySemester(getCurriculum(), 2)).isEmpty());
//
//        // Curricular year entries are approved enrolments taken from a year before the requested one
//        approveEnrolment(studentCurricularPlan, curricularCourse1);
//
//        assertEquals(Set.of(curricularCourse1), getCourses(getCurriculum()));
//        assertEquals(Set.of(curricularCourse1),
//                getCourses(RuleTransition.filterCurricularYearEntriesBySemester(getCurriculum(), 1)));
//        assertTrue(getCourses(RuleTransition.filterCurricularYearEntriesBySemester(getCurriculum(), 2)).isEmpty());
//
//        approveEnrolment(studentCurricularPlan, curricularCourse2);
//
//        assertEquals(Set.of(curricularCourse1, curricularCourse2), getCourses(getCurriculum()));
//        assertEquals(Set.of(curricularCourse1),
//                getCourses(RuleTransition.filterCurricularYearEntriesBySemester(getCurriculum(), 1)));
//        assertEquals(Set.of(curricularCourse2),
//                getCourses(RuleTransition.filterCurricularYearEntriesBySemester(getCurriculum(), 2)));
//    }

    // Helpers

//    private static Curriculum getCurriculum() {
//        // Get next year's Curriculum so that it contains current year entries
//        return studentCurricularPlan.getRoot().getCurriculum(executionYear.getNext().getExecutionYear());
//    }
//
//    private static void approveEnrolment(StudentCurricularPlan studentCurricularPlan, CurricularCourse curricularCourse) {
//        Enrolment enrolment = studentCurricularPlan.getEnrolments(curricularCourse).iterator().next();
//        EnrolmentEvaluation evaluation = enrolment.getEvaluationsSet().iterator().next();
//        evaluation.setGrade(Grade.createGrade("10", gradeScale));
//        evaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
//    }
//
//    private static Set<CurricularCourse> getCourses(Curriculum curriculum) {
//        return curriculum.getCurricularYearEntries().stream().map(entry -> ((Enrolment) entry).getCurricularCourse())
//                .collect(Collectors.toSet());
//    }
}
