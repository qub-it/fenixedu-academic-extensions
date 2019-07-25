package org.fenixedu.academic.domain.curricularRules;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.StudentSchoolClassCurricularRuleExecutor;
import org.fenixedu.academic.domain.curricularRules.executors.verifyExecutors.VerifyRuleExecutor;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.collect.Lists;

public class StudentSchoolClassCurricularRule extends StudentSchoolClassCurricularRule_Base {

    protected StudentSchoolClassCurricularRule() {
        super();
    }

    public StudentSchoolClassCurricularRule(final DegreeModule toApplyRule, final CourseGroup contextCourseGroup,
            final ExecutionSemester begin, final ExecutionSemester end, final Boolean schoolClassMustContainCourse,
            final Boolean courseMustHaveFreeShifts, final Boolean enrolInShiftIfUnique,
            final Boolean allAvailableShiftsMustBeEnrolled, final String schoolClassNames) {

        this();
        init(toApplyRule, contextCourseGroup, begin, end, CurricularRuleType.CUSTOM);
        setSchoolClassMustContainCourse(schoolClassMustContainCourse);
        setCourseMustHaveFreeShifts(courseMustHaveFreeShifts);
        setEnrolInShiftIfUnique(enrolInShiftIfUnique);
        setAllAvailableShiftsMustBeEnrolled(allAvailableShiftsMustBeEnrolled);
        setSchoolClassNames(schoolClassNames);
    }

    public void edit(CourseGroup contextCourseGroup, final Boolean schoolClassMustContainCourse,
            final Boolean courseMustHaveFreeShifts, final Boolean enrolInShiftIfUnique,
            final Boolean allAvailableShiftsMustBeEnrolled, final String schoolClassNames) {
        setContextCourseGroup(contextCourseGroup);
        setSchoolClassMustContainCourse(schoolClassMustContainCourse);
        setCourseMustHaveFreeShifts(courseMustHaveFreeShifts);
        setEnrolInShiftIfUnique(enrolInShiftIfUnique);
        setAllAvailableShiftsMustBeEnrolled(allAvailableShiftsMustBeEnrolled);
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
    @SuppressWarnings("unchecked")
    public List<GenericPair<Object, Boolean>> getLabel() {
        final StringBuilder label = new StringBuilder();
        if (getSchoolClassMustContainCourse()) {
            label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE,
                    "label.StudentSchoolClassCurricularRule.schoolClassMustContainCourse"));
        }
        if (getCourseMustHaveFreeShifts()) {
            if (label.length() > 0) {
                label.append(", ");
            }
            label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE,
                    "label.StudentSchoolClassCurricularRule.courseMustHaveFreeShifts"));
        }
        if (getEnrolInShiftIfUnique()) {
            if (label.length() > 0) {
                label.append(", ");
            }
            label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE,
                    "label.StudentSchoolClassCurricularRule.enrolInShiftIfUnique"));
        }
        if (getAllAvailableShiftsMustBeEnrolled()) {
            if (label.length() > 0) {
                label.append(", ");
            }
            label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE,
                    "label.StudentSchoolClassCurricularRule.allAvailableShiftsMustBeEnrolled"));
        }
        if (StringUtils.isNotBlank(getSchoolClassNames())) {
            if (label.length() > 0) {
                label.append(", ");
            }
            label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE,
                    "label.StudentSchoolClassCurricularRule.schoolClassNames", getSchoolClassNames()));
        }

        if (getContextCourseGroup() != null) {
            label.append(", ");
            label.append(BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, "label.inGroup"));
            label.append(" ");
            label.append(getContextCourseGroup().getOneFullName());
        }

        return Lists.newArrayList(new GenericPair<Object, Boolean>(label, false));
    }

    public Collection<SchoolClass> getSchoolClasses(final ExecutionInterval executionInterval) {

        if (StringUtils.isNotBlank(getSchoolClassNames())) {

            final ExecutionDegree executionDegree = getDegreeModuleToApplyRule().getParentDegreeCurricularPlan()
                    .getExecutionDegreeByYear(executionInterval.getExecutionYear());

            if (executionDegree != null) {
                final Collection<SchoolClass> result = new HashSet<>();
                final String[] schoolClassNamesSplitted =
                        getSchoolClassNames().trim().replace(';', '/').replace(',', '/').split("/");
                for (final String schoolClassName : schoolClassNamesSplitted) {
                    result.addAll(executionDegree.getSchoolClassesSet().stream()
                            .filter(sc -> sc.getExecutionInterval() == executionInterval
                                    && schoolClassName.trim().equalsIgnoreCase((String) sc.getEditablePartOfName()))
                            .collect(Collectors.toSet()));
                }
                return result;
            }

        }

        return Collections.emptyList();
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

}
