package org.fenixedu.academic.domain.evaluation.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import org.fenixedu.academic.domain.EnrolmentEvaluation;

public class EnrolmentEvaluationServices {

    static final public String EVALUATION_DATE_FORMAT = "yyyy-MM-dd";
    static final public String EVALUATION_DATE_TIME_FORMAT = EVALUATION_DATE_FORMAT + " HH:mm";

    static final protected Collection<Consumer<EnrolmentEvaluation>> STATE_CHANGE_LISTENERS = new ArrayList<>();

    static public void onStateChange(EnrolmentEvaluation evaluation) {
        STATE_CHANGE_LISTENERS.forEach(c -> c.accept(evaluation));
    }

    static public void registerStateChangeListener(final Consumer<EnrolmentEvaluation> listener) {
        STATE_CHANGE_LISTENERS.add(listener);
    }

}
