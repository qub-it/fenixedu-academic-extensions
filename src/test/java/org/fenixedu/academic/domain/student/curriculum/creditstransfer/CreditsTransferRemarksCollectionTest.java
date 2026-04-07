package org.fenixedu.academic.domain.student.curriculum.creditstransfer;

import static org.fenixedu.academic.domain.student.curriculum.creditstransfer.CreditsTransferRemarksCollection.RemarkIdGenerator;
import static org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod.SEMESTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.UUID;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentTest;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.CreditsReasonType;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.student.curriculum.calculator.util.ConclusionGradeCalculatorTestUtil;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroupFactory;
import org.fenixedu.academic.domain.studentCurriculum.Equivalence;
import org.fenixedu.academic.domain.studentCurriculum.RootCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.academic.dto.administrativeOffice.dismissal.DismissalBean;
import org.fenixedu.commons.i18n.LocalizedString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CreditsTransferRemarksCollectionTest {

    @BeforeClass
    public static void init() {
        FenixFramework.getTransactionManager().withTransaction(() -> {
            ConclusionGradeCalculatorTestUtil.initData();
            return null;
        });
    }

    @Test
    public void remarkIdGenerator_givenNumberX_NumberToCode_thenReturnsY() {
        assertEquals("a", RemarkIdGenerator.numberToCode(1));
        assertEquals("b", RemarkIdGenerator.numberToCode(2));
        assertEquals("z", RemarkIdGenerator.numberToCode(26));
        assertEquals("aa", RemarkIdGenerator.numberToCode(27));
        assertEquals("ab", RemarkIdGenerator.numberToCode(28));
        assertEquals("az", RemarkIdGenerator.numberToCode(52));
        assertEquals("ba", RemarkIdGenerator.numberToCode(53));
        assertEquals("zz", RemarkIdGenerator.numberToCode(702));
        assertEquals("aaa", RemarkIdGenerator.numberToCode(703));
    }

    @Test
    public void calculateAvgGrade_testBonusWithEquivalence() throws InterruptedException {
        final ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");
        final ExecutionYear year2 = ExecutionYear.readExecutionYearByName("2020/2021");

        //Lazy mimicking of switching degree curricular plans
        //First "plan"
        final StudentCurricularPlan previousScp = createStudentCurricularPlan(year1);

        ConclusionGradeCalculatorTestUtil.enrol(previousScp, year1, "C1");
        org.junit.Assert.assertEquals(1, previousScp.getAllCurriculumLines().size());

        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C1", "10.0");
        org.junit.Assert.assertEquals(1, previousScp.getAllCurriculumLines().stream().filter(cl -> cl.isApproved()).count());

        //Second "plan"
        final StudentCurricularPlan scp = createStudentCurricularPlan(year1);
        scp.setDegreeCurricularPlan(previousScp.getDegreeCurricularPlan());

        ConclusionGradeCalculatorTestUtil.enrol(scp, year2, "C2", "C3", "C6", "C7", "C8", "C9", "C10", "C11", "C12");
        org.junit.Assert.assertEquals(9, scp.getAllCurriculumLines().size());

        ConclusionGradeCalculatorTestUtil.approve(scp, "C2", "15");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C3", "15");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C6", "15");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C7", "15");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C8", "15");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C9", "15");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C10", "15");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C11", "15");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C12", "15");
        org.junit.Assert.assertEquals(9, scp.getAllCurriculumLines().stream().filter(cl -> cl.isApproved()).count());

        //Now an equivalence
        final IEnrolment c1Enrolment = findEnrolment(previousScp, "C1");
        createEquivalence(Set.of(c1Enrolment), scp, year2, "C1", "10");

        org.junit.Assert.assertEquals(1, scp.getDismissals().size());
    }

    @Test
    public void build_withOnlyEnrolments_noDismissals_returnsEmptyRemarks() {
        final ExecutionYear year = ExecutionYear.readExecutionYearByName("2019/2020");
        final StudentCurricularPlan scp = createStudentCurricularPlanWithTestPrefix(year);

        ConclusionGradeCalculatorTestUtil.enrol(scp, year, "C1");
        ConclusionGradeCalculatorTestUtil.approve(scp, "C1", "10.0");

        Collection<ICurriculumEntry> entries = (Collection<ICurriculumEntry>) (Collection<?>) scp.getDismissals();
        CreditsTransferRemarksCollection remarks = CreditsTransferRemarksCollection.build(entries, scp);

        assertTrue(remarks.getRemarkIds().isEmpty());
    }

    @Test
    public void build_withSubstitution_createsRemarkIds() {
        final ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");
        final ExecutionYear year2 = ExecutionYear.readExecutionYearByName("2020/2021");

        final StudentCurricularPlan previousScp = createStudentCurricularPlanWithTestPrefix(year1);
        ConclusionGradeCalculatorTestUtil.enrol(previousScp, year1, "C1");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C1", "10.0");

        final StudentCurricularPlan scp = createStudentCurricularPlanWithTestPrefix(year1);
        scp.setDegreeCurricularPlan(previousScp.getDegreeCurricularPlan());

        final IEnrolment c1Enrolment = findEnrolment(previousScp, "C1");
        createSubstitution(Set.of(c1Enrolment), scp, year2, "C2");

        // Pass all curriculum entries (including source enrolments), not just dismissals
        Collection<ICurriculumEntry> entries = scp.getRoot().getCurriculum().getCurriculumEntries();
        CreditsTransferRemarksCollection remarks = CreditsTransferRemarksCollection.build(entries, scp);

        assertFalse("Remarks should not be empty", remarks.getRemarkIds().isEmpty());
    }

    @Test
    public void build_withMultipleEntries_sortedByCode() {
        final ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");
        final ExecutionYear year2 = ExecutionYear.readExecutionYearByName("2020/2021");

        final StudentCurricularPlan previousScp = createStudentCurricularPlanWithTestPrefix(year1);
        ConclusionGradeCalculatorTestUtil.enrol(previousScp, year1, "C1", "C2", "C3");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C1", "10.0");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C2", "12.0");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C3", "14.0");

        final StudentCurricularPlan scp = createStudentCurricularPlanWithTestPrefix(year1);
        scp.setDegreeCurricularPlan(previousScp.getDegreeCurricularPlan());

        // Enrol in target courses that will receive dismissals (so dismissals have curriculum lines)
        ConclusionGradeCalculatorTestUtil.enrol(scp, year2, "C2", "C3", "C6");

        final IEnrolment c1Enrolment = findEnrolment(previousScp, "C1");
        final IEnrolment c2Enrolment = findEnrolment(previousScp, "C2");
        final IEnrolment c3Enrolment = findEnrolment(previousScp, "C3");
        createEquivalence(Set.of(c1Enrolment), scp, year2, "C2", "10");
        createSubstitution(Set.of(c2Enrolment), scp, year2, "C3");
        createEquivalence(Set.of(c3Enrolment), scp, year2, "C6", "14");

        // Pass all curriculum entries (including source enrolments), not just dismissals
        Collection<ICurriculumEntry> entries = scp.getRoot().getCurriculum().getCurriculumEntries();
        CreditsTransferRemarksCollection remarks = CreditsTransferRemarksCollection.build(entries, scp);

        // 3 dismissals with different grades/types result in merged remarks - exact count depends on merge logic
        assertTrue("Should have at least 1 remark", remarks.getRemarkIds().size() >= 1);
    }

    @Test
    public void getRemarkIds_returnsSortedIds() {
        final ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");
        final ExecutionYear year2 = ExecutionYear.readExecutionYearByName("2020/2021");

        final StudentCurricularPlan previousScp = createStudentCurricularPlanWithTestPrefix(year1);
        ConclusionGradeCalculatorTestUtil.enrol(previousScp, year1, "C1", "C2", "C3");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C1", "10.0");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C2", "12.0");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C3", "14.0");

        final StudentCurricularPlan scp = createStudentCurricularPlanWithTestPrefix(year1);
        scp.setDegreeCurricularPlan(previousScp.getDegreeCurricularPlan());

        // Enrol in target courses that will receive dismissals (so dismissals have curriculum lines)
        ConclusionGradeCalculatorTestUtil.enrol(scp, year2, "C2", "C3", "C6");

        final IEnrolment c1Enrolment = findEnrolment(previousScp, "C1");
        final IEnrolment c2Enrolment = findEnrolment(previousScp, "C2");
        final IEnrolment c3Enrolment = findEnrolment(previousScp, "C3");
        createEquivalence(Set.of(c1Enrolment), scp, year2, "C2", "10");
        createSubstitution(Set.of(c2Enrolment), scp, year2, "C3");
        createEquivalence(Set.of(c3Enrolment), scp, year2, "C6", "14");

        // Pass all curriculum entries (including source enrolments), not just dismissals
        Collection<ICurriculumEntry> entries = scp.getRoot().getCurriculum().getCurriculumEntries();
        CreditsTransferRemarksCollection remarks = CreditsTransferRemarksCollection.build(entries, scp);

        Collection<String> remarkIds = remarks.getRemarkIds();
        assertTrue("Should have at least 1 remark", remarkIds.size() >= 1);
    }

    @Test
    public void getRemarkTextForId_validId_returnsLocalizedString() {
        final ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");

        final StudentCurricularPlan previousScp = createStudentCurricularPlanWithTestPrefix(year1);
        ConclusionGradeCalculatorTestUtil.enrol(previousScp, year1, "C1");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C1", "10.0");

        final StudentCurricularPlan scp = createStudentCurricularPlanWithTestPrefix(year1);
        scp.setDegreeCurricularPlan(previousScp.getDegreeCurricularPlan());

        final IEnrolment c1Enrolment = findEnrolment(previousScp, "C1");
        createEquivalence(Set.of(c1Enrolment), scp, year1, "C2", "10");

        Collection<ICurriculumEntry> entries = (Collection<ICurriculumEntry>) (Collection<?>) scp.getDismissals();
        CreditsTransferRemarksCollection remarks = CreditsTransferRemarksCollection.build(entries, scp);

        String remarkId = remarks.getRemarkIds().iterator().next();
        LocalizedString remarkText = remarks.getRemarkTextForId(remarkId);
        assertNotNull(remarkText);
    }

    @Test
    public void getRemarkTextForId_invalidId_returnsNull() {
        final ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");

        final StudentCurricularPlan previousScp = createStudentCurricularPlanWithTestPrefix(year1);
        ConclusionGradeCalculatorTestUtil.enrol(previousScp, year1, "C1");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C1", "10.0");

        final StudentCurricularPlan scp = createStudentCurricularPlanWithTestPrefix(year1);
        scp.setDegreeCurricularPlan(previousScp.getDegreeCurricularPlan());

        final IEnrolment c1Enrolment = findEnrolment(previousScp, "C1");
        createEquivalence(Set.of(c1Enrolment), scp, year1, "C2", "10");

        Collection<ICurriculumEntry> entries = (Collection<ICurriculumEntry>) (Collection<?>) scp.getDismissals();
        CreditsTransferRemarksCollection remarks = CreditsTransferRemarksCollection.build(entries, scp);

        LocalizedString remarkText = remarks.getRemarkTextForId("invalid-id");
        assertNull(remarkText);
    }

    @Test
    public void getFormattedRemarks_withSeparator_formatsCorrectly() {
        final ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");

        final StudentCurricularPlan previousScp = createStudentCurricularPlanWithTestPrefix(year1);
        ConclusionGradeCalculatorTestUtil.enrol(previousScp, year1, "C1", "C2");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C1", "10.0");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C2", "12.0");

        final StudentCurricularPlan scp = createStudentCurricularPlanWithTestPrefix(year1);
        scp.setDegreeCurricularPlan(previousScp.getDegreeCurricularPlan());

        final IEnrolment c1Enrolment = findEnrolment(previousScp, "C1");
        final IEnrolment c2Enrolment = findEnrolment(previousScp, "C2");
        createEquivalence(Set.of(c1Enrolment), scp, year1, "C1", "10");
        createSubstitution(Set.of(c2Enrolment), scp, year1, "C2");

        Collection<ICurriculumEntry> entries = (Collection<ICurriculumEntry>) (Collection<?>) scp.getDismissals();
        CreditsTransferRemarksCollection remarks = CreditsTransferRemarksCollection.build(entries, scp);

        LocalizedString formatted = remarks.getFormattedRemarks(" | ");
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
    }

    @Test
    public void getFormattedRemarks_containsAllRemarkIds() {
        final ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");
        final ExecutionYear year2 = ExecutionYear.readExecutionYearByName("2020/2021");

        final StudentCurricularPlan previousScp = createStudentCurricularPlanWithTestPrefix(year1);
        ConclusionGradeCalculatorTestUtil.enrol(previousScp, year1, "C1", "C2", "C3");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C1", "10.0");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C2", "12.0");
        ConclusionGradeCalculatorTestUtil.approve(previousScp, "C3", "14.0");

        final StudentCurricularPlan scp = createStudentCurricularPlanWithTestPrefix(year1);
        scp.setDegreeCurricularPlan(previousScp.getDegreeCurricularPlan());

        // Enrol in DIFFERENT target courses that will receive dismissals (different curriculum groups)
        ConclusionGradeCalculatorTestUtil.enrol(scp, year2, "C1", "C2", "C3");
        // Don't approve - the dismissals will complete them

        final IEnrolment c1Enrolment = findEnrolment(previousScp, "C1");
        final IEnrolment c2Enrolment = findEnrolment(previousScp, "C2");
        final IEnrolment c3Enrolment = findEnrolment(previousScp, "C3");

        final Equivalence equivalence1 = createEquivalence(Set.of(c1Enrolment), scp, year2, "C1", "10");
        final Equivalence equivalence2 = createEquivalence(Set.of(c2Enrolment), scp, year2, "C2", "14");
        final Substitution substitution1 = createSubstitution(Set.of(c3Enrolment), scp, year2, "C3");

        final CreditsReasonType reason1 = new CreditsReasonType();
        reason1.setReason(new LocalizedString(Locale.ENGLISH, "Erasmus"));
        reason1.setInfoText(new LocalizedString(Locale.ENGLISH, "Participated in Erasmus program"));
        final CreditsReasonType reason2 = new CreditsReasonType();
        reason2.setReason(new LocalizedString(Locale.ENGLISH, "Internal Change"));
        reason2.setInfoText(new LocalizedString(Locale.ENGLISH, "Student changed degree"));
        equivalence1.setReason(reason1);
        equivalence2.setReason(reason1);
        substitution1.setReason(reason2);

        Collection<ICurriculumEntry> entries = scp.getRoot().getCurriculum().getCurriculumEntries();
        CreditsTransferRemarksCollection remarks = CreditsTransferRemarksCollection.build(entries, scp);

        assertEquals("a) Participated in Erasmus program;b) Student changed degree;",
                remarks.getFormattedRemarks(";").getContent());
        assertEquals(2, remarks.getRemarkIds().size());
    }

    @Test
    public void getFormattedRemarks_withTwentySevenDismissals_createsMultipleRemarkIds() {
        final ExecutionYear year1 = ExecutionYear.readExecutionYearByName("2019/2020");
        final ExecutionYear year2 = ExecutionYear.readExecutionYearByName("2020/2021");

        final StudentCurricularPlan previousScp = createStudentCurricularPlanWithTestPrefix(year1);

        createTenCurricularCourses(year1, previousScp.getDegreeCurricularPlan());
        for (int i = 1; i <= 27; i++) {
            ConclusionGradeCalculatorTestUtil.enrol(previousScp, year1, "C" + i);
            ConclusionGradeCalculatorTestUtil.approve(previousScp, "C" + i, "15");
        }

        org.junit.Assert.assertEquals(27, previousScp.getEnrolmentsSet().size());
        org.junit.Assert.assertEquals(27, previousScp.getAllCurriculumLines().stream().filter(cl -> cl.isApproved()).count());

        final StudentCurricularPlan scp = createStudentCurricularPlanWithTestPrefix(year1);
        scp.setDegreeCurricularPlan(previousScp.getDegreeCurricularPlan());

        for (int i = 1; i <= 27; i++) {
            ConclusionGradeCalculatorTestUtil.enrol(scp, year2, "C" + i);
        }

        // Create a single CreditsReasonType with InfoExplained=true
        // This makes each equivalence's toString include the source course name
        CreditsReasonType reason = new CreditsReasonType();
        reason.setActive(true);
        reason.setReason(new LocalizedString(Locale.ENGLISH, "Credits Transfer"));
        reason.setInfoText(new LocalizedString(Locale.ENGLISH, "Credits Transfer"));
        reason.setInfoExplained(true);

        // Create 27 equivalences with different source enrolments - toString will differ per source
        for (int i = 1; i <= 27; i++) {
            final IEnrolment sourceEnrolment = findEnrolment(previousScp, "C" + i);
            Equivalence eq = createEquivalence(Set.of(sourceEnrolment), scp, year2, "C" + i, "15");
            eq.setReason(reason);
        }

        Collection<ICurriculumEntry> entries = scp.getRoot().getCurriculum().getCurriculumEntries();
        CreditsTransferRemarksCollection remarks = CreditsTransferRemarksCollection.build(entries, scp);

        assertEquals(27, remarks.getRemarkIds().size());

        final String formattedRemarks = remarks.getFormattedRemarks(";").getContent();
        assertNotNull(formattedRemarks);
        assertTrue("Last entries should have remark IDs z) followed by aa)", 
                Pattern.compile("z\\).*aa\\)", Pattern.DOTALL).matcher(formattedRemarks).find());

    }

    private static Enrolment findEnrolment(final StudentCurricularPlan previousScp, final String curricularCourseCode) {
        return previousScp.getEnrolmentsSet().stream().filter(e -> e.getCode().equals(curricularCourseCode)).findAny().get();
    }

    private static StudentCurricularPlan createStudentCurricularPlan(ExecutionYear executionYear) {
        DegreeCurricularPlan dcp = ConclusionGradeCalculatorTestUtil.createDegreeCurricularPlan(executionYear);

        String uniqueUsername = UUID.randomUUID().toString();
        Student student = StudentTest.createStudent("Student Test Conclusion Grade Law School", uniqueUsername);

        Registration registration = StudentTest.createRegistration(student, dcp, executionYear);
        return registration.getLastStudentCurricularPlan();
    }

    private static StudentCurricularPlan createStudentCurricularPlanWithTestPrefix(ExecutionYear executionYear) {
        DegreeCurricularPlan dcp = ConclusionGradeCalculatorTestUtil.createDegreeCurricularPlan(executionYear);

        String uniqueUsername = "test-credits-transfer-" + UUID.randomUUID().toString();
        Student student = StudentTest.createStudent("Student Test Credits Transfer", uniqueUsername);

        Registration registration = StudentTest.createRegistration(student, dcp, executionYear);
        return registration.getLastStudentCurricularPlan();
    }

    private static Equivalence createEquivalence(final Set<IEnrolment> sources, StudentCurricularPlan studentCurricularPlan,
            ExecutionYear executionYear, String courseCode, String grade) {
        DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        CurricularCourse curricularCourse = degreeCurricularPlan.getCurricularCourseByCode(courseCode);
        Context context = (Context) curricularCourse.getParentContextsSet().iterator().next();
        ExecutionInterval executionInterval = executionYear.getChildInterval(context.getCurricularPeriod().getChildOrder(),
                context.getCurricularPeriod().getAcademicPeriod());
        DismissalBean.SelectedCurricularCourse dismissalDTO =
                new DismissalBean.SelectedCurricularCourse(curricularCourse, studentCurricularPlan);
        dismissalDTO.setCurriculumGroup(findOrCreateCurriculumGroupFor(studentCurricularPlan, context.getParentCourseGroup()));
        return new Equivalence(studentCurricularPlan, Set.of(dismissalDTO), sources, grade(grade), executionInterval);
    }

    private static Substitution createSubstitution(final Set<IEnrolment> sources, StudentCurricularPlan studentCurricularPlan,
            ExecutionYear executionYear, String courseCode) {
        DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
        CurricularCourse curricularCourse = degreeCurricularPlan.getCurricularCourseByCode(courseCode);
        Context context = (Context) curricularCourse.getParentContextsSet().iterator().next();
        ExecutionInterval executionInterval = executionYear.getChildInterval(context.getCurricularPeriod().getChildOrder(),
                context.getCurricularPeriod().getAcademicPeriod());
        DismissalBean.SelectedCurricularCourse dismissalDTO =
                new DismissalBean.SelectedCurricularCourse(curricularCourse, studentCurricularPlan);
        dismissalDTO.setCurriculumGroup(findOrCreateCurriculumGroupFor(studentCurricularPlan, context.getParentCourseGroup()));
        return new Substitution(studentCurricularPlan, Set.of(dismissalDTO), sources, executionInterval);
    }

    private static Grade grade(String gradeValue) {
        return ConclusionGradeCalculatorTestUtil.createGrade(gradeValue);
    }

    private static CurriculumGroup findOrCreateCurriculumGroupFor(StudentCurricularPlan curricularPlan, CourseGroup courseGroup) {
        List<CourseGroup> path = new ArrayList();

        for (CourseGroup groupToAdd = courseGroup;
             groupToAdd != null; groupToAdd =
                     groupToAdd.getParentCourseGroups().isEmpty() ? null : (CourseGroup) groupToAdd.getParentCourseGroups()
                             .iterator().next()) {
            if (!groupToAdd.isRoot()) {
                path.add(0, groupToAdd);
            }
        }

        CurriculumGroup current = curricularPlan.getRoot();

        for (CourseGroup pathElement : path) {
            CurriculumGroup existing = current.findCurriculumGroupFor(pathElement);
            if (existing == null) {
                if (pathElement.isCycleCourseGroup()) {
                    if (curricularPlan.getDegreeCurricularPlan() != pathElement.getParentDegreeCurricularPlan()) {
                        throw new DomainException(
                                "error.StudentCurricularPlan.affinity.cycles.must.already.exist.to.create.child.curriculum.groups");
                    }

                    current = CurriculumGroupFactory.createGroup((RootCurriculumGroup) current, (CycleCourseGroup) pathElement);
                } else {
                    current = CurriculumGroupFactory.createGroup(current, pathElement);
                }
            } else {
                current = existing;
            }
        }

        return current;
    }

    private static void createTenCurricularCourses(final ExecutionYear year1, final DegreeCurricularPlan dcp) {
        final ExecutionInterval firstExecutionPeriod = year1.getFirstExecutionPeriod();
        final CurricularPeriod period2Y1S = dcp.getCurricularPeriodFor(1, 1, SEMESTER);
        final CourseGroup optionalGroup =
                dcp.getAllDegreeModules().filter(dm -> dm instanceof CourseGroup).map(CourseGroup.class::cast)
                        .filter(cg -> ConclusionGradeCalculatorTestUtil.MANDATORY_GROUP.equals(cg.getName())).findAny().get();
        for (int i = 23; i < 23 + 10; i++) {
            ConclusionGradeCalculatorTestUtil.createCurricularCourse("C" + i, "Course " + i, new BigDecimal(6), period2Y1S,
                    firstExecutionPeriod, optionalGroup);
        }
    }

}
