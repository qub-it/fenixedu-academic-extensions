package org.fenixedu.academic;

import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

public class FenixEduAcademicExtensionsConfiguration {

    @ConfigurationManager(description = "FenixEdu Academic Extensions Configuration")
    public static interface ConfigurationProperties {

        @ConfigurationProperty(key = "org.fenixedu.academic.domain.evaluation.EvaluationComparator.use.best.grade.only.criterion",
                defaultValue = "false")
        public Boolean isToUseBestGradeOnlyCriterionForEvaluation();

    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

}
