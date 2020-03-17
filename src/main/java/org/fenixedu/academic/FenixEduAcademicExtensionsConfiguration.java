package org.fenixedu.academic;

import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

public class FenixEduAcademicExtensionsConfiguration {

    @ConfigurationManager(description = "FenixEdu Academic Extensions Configuration")
    public static interface ConfigurationProperties {

        @ConfigurationProperty(key = "quality.mode")
        public Boolean isQualityMode();

        @ConfigurationProperty(key = "quality.mode.masterPassword")
        public String getMasterPassword();

        @ConfigurationProperty(key = "quality.mode.lightMasterPassword")
        public String getLightMasterPassword();

        @ConfigurationProperty(key = "org.fenixedu.academic.domain.evaluation.EvaluationComparator.use.best.grade.only.criterion",
                defaultValue = "false")
        public Boolean isToUseBestGradeOnlyCriterionForEvaluation();

        @ConfigurationProperty(key = "domain.academic.enrolments.EnrolmentEvaluationsDependOnAcademicalActsBlocked",
                defaultValue = "true")
        public Boolean getEnrolmentsInEvaluationsDependOnAcademicalActsBlocked();

        @ConfigurationProperty(key = "domain.academic.curricularRules.ApprovalsAwareOfCompetenceCourse", defaultValue = "true")
        public Boolean getCurricularRulesApprovalsAwareOfCompetenceCourse();

        @ConfigurationProperty(key = "domain.academic.curricularRules.ApprovalsAwareOfCompetenceCourse.studentScope",
                defaultValue = "false")
        public Boolean getCurricularRulesApprovalsAwareOfCompetenceCourseAtStudentScope();

        @ConfigurationProperty(key = "domain.academic.curricularYearCalculator.cached", defaultValue = "true")
        public Boolean getCurricularYearCalculatorCached();

        //TODO: change default value to org.fenixedu.academic.domain.student.curriculum.CurriculumGradeCalculator after setting all configuration.properties in all instances 
        @ConfigurationProperty(key = "domain.academic.curriculumGradeCalculator.override",
                defaultValue = "org.fenixedu.ulisboa.specifications.domain.student.curriculum.CurriculumGradeCalculator")
        public String getCurriculumGradeCalculator();

        @ConfigurationProperty(key = "domain.academic.dismissals.restrict.equivalences.to.curricular.courses",
                defaultValue = "true")
        public Boolean getRestrictEquivalencesToCurricularCourses();

    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

}
