package org.fenixedu.academic.domain.curricularRules;

import static org.fenixedu.academicextensions.util.AcademicExtensionsUtil.BUNDLE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.StudentSchoolClassCurricularRuleExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public class StudentSchoolClassCurricularRule extends StudentSchoolClassCurricularRule_Base {

    protected StudentSchoolClassCurricularRule() {
        super();
    }

    public StudentSchoolClassCurricularRule(final DegreeModule toApplyRule, final CourseGroup contextCourseGroup,
            final ExecutionInterval begin, final ExecutionInterval end, final Boolean schoolClassMustContainCourse,
            final Boolean courseMustHaveFreeShifts, final Boolean enrolInShiftIfUnique,
            final Boolean allAvailableShiftsMustBeEnrolled, final boolean blockEnrolmentIfLessonsOverlap,
            final String schoolClassNames) {

        this();
        init(toApplyRule, contextCourseGroup, begin, end, CurricularRuleType.CUSTOM);
        setSchoolClassMustContainCourse(schoolClassMustContainCourse);
        setCourseMustHaveFreeShifts(courseMustHaveFreeShifts);
        setEnrolInShiftIfUnique(enrolInShiftIfUnique);
        setAllAvailableShiftsMustBeEnrolled(allAvailableShiftsMustBeEnrolled);
        setBlockEnrolmentIfLessonsOverlap(blockEnrolmentIfLessonsOverlap);
        setSchoolClassNames(schoolClassNames);
    }

    public void edit(CourseGroup contextCourseGroup, final Boolean schoolClassMustContainCourse,
            final Boolean courseMustHaveFreeShifts, final Boolean enrolInShiftIfUnique,
            final Boolean allAvailableShiftsMustBeEnrolled, final boolean blockEnrolmentIfLessonsOverlap,
            final String schoolClassNames) {
        setContextCourseGroup(contextCourseGroup);
        setSchoolClassMustContainCourse(schoolClassMustContainCourse);
        setCourseMustHaveFreeShifts(courseMustHaveFreeShifts);
        setEnrolInShiftIfUnique(enrolInShiftIfUnique);
        setAllAvailableShiftsMustBeEnrolled(allAvailableShiftsMustBeEnrolled);
        setBlockEnrolmentIfLessonsOverlap(blockEnrolmentIfLessonsOverlap);
        setSchoolClassNames(schoolClassNames);
    }

    @Override
    public RuleResult evaluate(IDegreeModuleToEvaluate sourceDegreeModuleToEvaluate, EnrolmentContext enrolmentContext) {
        return new StudentSchoolClassCurricularRuleExecutor().execute(this, sourceDegreeModuleToEvaluate, enrolmentContext);
    }

    @Override
    public VerifyRuleExecutor createVerifyRuleExecutor() {
        return VerifyRuleExecutor.NULL_VERIFY_EXECUTOR;
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    protected void removeOwnParameters() {
    }

    @Override
    public List<GenericPair<Object, Boolean>> getLabel() {
        final List<String> labels = new ArrayList<>();

        if (getSchoolClassMustContainCourse()) {
            labels.add(BundleUtil.getString(BUNDLE, "label.StudentSchoolClassCurricularRule.schoolClassMustContainCourse"));
        }
        if (getCourseMustHaveFreeShifts()) {
            labels.add(BundleUtil.getString(BUNDLE, "label.StudentSchoolClassCurricularRule.courseMustHaveFreeShifts"));
        }
        if (getEnrolInShiftIfUnique()) {
            labels.add(BundleUtil.getString(BUNDLE, "label.StudentSchoolClassCurricularRule.enrolInShiftIfUnique"));
        }
        if (getAllAvailableShiftsMustBeEnrolled()) {
            labels.add(BundleUtil.getString(BUNDLE, "label.StudentSchoolClassCurricularRule.allAvailableShiftsMustBeEnrolled"));
        }
        if (getBlockEnrolmentIfLessonsOverlap()) {
            labels.add(BundleUtil.getString(BUNDLE, "label.StudentSchoolClassCurricularRule.blockEnrolmentIfLessonsOverlap"));
        }
        if (StringUtils.isNotBlank(getSchoolClassNames())) {
            labels.add(BundleUtil.getString(BUNDLE, "label.StudentSchoolClassCurricularRule.schoolClassNames",
                    getSchoolClassNames()));
        }

        if (getContextCourseGroup() != null) {
            labels.add(BundleUtil.getString(BUNDLE, "label.inGroup") + " " + getContextCourseGroup().getOneFullName());
        }

        return List.of(new GenericPair<>(String.join(", ", labels), false));
    }

    public Stream<String> getSchoolClassesSplitted() {
        return StringUtils.isBlank(getSchoolClassNames()) ? Stream.empty() : Stream
                .of(getSchoolClassNames().trim().replace(';', '/').replace(',', '/').split("/")).map(String::trim);
    }

    @Override
    public Boolean getSchoolClassMustContainCourse() {
        return super.getSchoolClassMustContainCourse() != null && super.getSchoolClassMustContainCourse();
    }

    @Override
    public Boolean getCourseMustHaveFreeShifts() {
        return super.getCourseMustHaveFreeShifts() != null && super.getCourseMustHaveFreeShifts();
    }

    @Override
    public Boolean getEnrolInShiftIfUnique() {
        return super.getEnrolInShiftIfUnique() != null && super.getEnrolInShiftIfUnique();
    }

    @Override
    public Boolean getAllAvailableShiftsMustBeEnrolled() {
        return super.getAllAvailableShiftsMustBeEnrolled() != null && super.getAllAvailableShiftsMustBeEnrolled();
    }

    @Override
    public Boolean getBlockEnrolmentIfLessonsOverlap() {
        return super.getBlockEnrolmentIfLessonsOverlap() != null && super.getBlockEnrolmentIfLessonsOverlap();
    }

    public static Stream<StudentSchoolClassCurricularRule> findForEnrolment(final Enrolment enrolment,
            final ExecutionInterval executionInterval) {
        return enrolment.getCurricularRules(executionInterval).stream().filter(StudentSchoolClassCurricularRule.class::isInstance)
                .map(StudentSchoolClassCurricularRule.class::cast);
    }
}
