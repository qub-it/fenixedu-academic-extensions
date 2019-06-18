package org.fenixedu.academic.domain.curricularRules;

import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.AnyCurricularCourseExceptionsExecutorLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;

abstract public class AnyCurricularCourseExceptionsInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AnyCurricularCourseExceptionsInitializer.class);

    @Atomic
    public static void init() {
        AnyCurricularCourseExceptionsExecutorLogic.configure();
        AnyCurricularCourseExceptionsConfiguration.init();
    }

}
