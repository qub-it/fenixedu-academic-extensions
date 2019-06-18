package org.fenixedu.academic.domain.student.curriculum;

import java.math.BigDecimal;
import java.util.SortedSet;
import java.util.TreeSet;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.CurricularRuleType;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

public class CurriculumModuleServices {

    static final public Logger logger = LoggerFactory.getLogger(CurriculumModuleServices.class);

    static public BigDecimal getCreditsConcluded(final CurriculumGroup toInspect, final ExecutionInterval interval) {
        if (interval instanceof ExecutionSemester) {
            return getCreditsConcluded(toInspect,
                    ExecutionInterval.assertExecutionIntervalType(ExecutionSemester.class, interval));
        } else {
            return BigDecimal.valueOf(
                    toInspect.getCreditsConcluded(ExecutionInterval.assertExecutionIntervalType(ExecutionYear.class, interval)));
        }
    }

    static private BigDecimal getCreditsConcluded(final CurriculumGroup toInspect, final ExecutionSemester semester) {

        BigDecimal result = BigDecimal.ZERO;

        if (toInspect.isNoCourseGroupCurriculumGroup()) {
            return result;
        }

        for (final CurriculumModule iter : toInspect.getCurriculumModulesSet()) {
            result = result.add(getCreditsConcluded(iter, semester));
        }

        final CreditsLimit rule =
                (CreditsLimit) toInspect.getMostRecentActiveCurricularRule(CurricularRuleType.CREDITS_LIMIT, semester);
        if (rule == null) {
            return result;
        } else {
            return result.min(BigDecimal.valueOf(rule.getMaximumCredits()));
        }
    }

    static private BigDecimal getCreditsConcluded(final CurriculumModule toInspect, final ExecutionSemester semester) {

        BigDecimal result = BigDecimal.ZERO;

        if (CurriculumGroup.class.isAssignableFrom(toInspect.getClass())) {
            result = getCreditsConcluded((CurriculumGroup) toInspect, semester);

        } else if (Enrolment.class.isAssignableFrom(toInspect.getClass())) {
            result = getCreditsConcluded((Enrolment) toInspect, semester);

        } else if (Dismissal.class.isAssignableFrom(toInspect.getClass())) {
            result = getCreditsConcluded((Dismissal) toInspect, semester);
        }

        return result;
    }

    static private BigDecimal getCreditsConcluded(final Enrolment toInspect, final ExecutionSemester semester) {
        return semester == null || toInspect.getExecutionPeriod().isBeforeOrEquals(semester) ? BigDecimal
                .valueOf(toInspect.getAprovedEctsCredits()) : BigDecimal.ZERO;
    }

    static private BigDecimal getCreditsConcluded(final Dismissal toInspect, final ExecutionSemester semester) {
        return semester == null || toInspect.getExecutionPeriod() == null
                || toInspect.getExecutionPeriod().isBeforeOrEquals(semester) && !toInspect.getCredits().isTemporary() ? BigDecimal
                        .valueOf(toInspect.getEctsCredits()) : BigDecimal.ZERO;
    }

    static public BigDecimal getEnroledAndNotApprovedEctsCreditsFor(final CurriculumGroup toInspect,
            final ExecutionInterval interval) {
        if (interval instanceof ExecutionSemester) {
            return getEnroledAndNotApprovedEctsCreditsFor(toInspect,
                    ExecutionInterval.assertExecutionIntervalType(ExecutionSemester.class, interval));
        } else {
            final ExecutionYear year = ExecutionInterval.assertExecutionIntervalType(ExecutionYear.class, interval);
            return year.getExecutionPeriodsSet().stream().map(i -> getEnroledAndNotApprovedEctsCreditsFor(toInspect, i))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    static private BigDecimal getEnroledAndNotApprovedEctsCreditsFor(final CurriculumGroup toInspect,
            final ExecutionSemester semester) {

        BigDecimal result = BigDecimal.ZERO;
        for (final CurriculumModule iter : toInspect.getCurriculumModulesSet()) {
            result = result.add(getEnroledAndNotApprovedEctsCreditsFor(iter, semester));
        }
        return result;
    }

    static private BigDecimal getEnroledAndNotApprovedEctsCreditsFor(final CurriculumModule toInspect,
            final ExecutionSemester semester) {

        BigDecimal result = BigDecimal.ZERO;

        if (CurriculumGroup.class.isAssignableFrom(toInspect.getClass())) {
            result = getEnroledAndNotApprovedEctsCreditsFor((CurriculumGroup) toInspect, semester);

        } else if (Enrolment.class.isAssignableFrom(toInspect.getClass())) {
            result = getEnroledAndNotApprovedEctsCreditsFor((Enrolment) toInspect, semester);
        }

        return result;
    }

    static private BigDecimal getEnroledAndNotApprovedEctsCreditsFor(final Enrolment toInspect,
            final ExecutionSemester semester) {

        // several if conditions to help debugging

        if (CurriculumLineServices.isExcludedFromCurriculum(toInspect)) {
            return BigDecimal.ZERO;
        }

        if (toInspect.isAnnulled()) {
            return BigDecimal.ZERO;
        }

        if (!toInspect.isEnroled() && !toInspect.isNotEvaluated() && !toInspect.isFlunked()) {
            return BigDecimal.ZERO;
        }

        if (!toInspect.isValid(semester)) {
            return BigDecimal.ZERO;
        }

        if (toInspect.isAnual() && !semester.isFirstOfYear()) {
            return BigDecimal.ZERO;
        }

        final BigDecimal result = toInspect.getEctsCreditsForCurriculum();
        logger.debug("{}#UC {}#{} ECTS", toInspect.getCode(), semester.getQualifiedName(), result.toPlainString());
        return result;
    }

    static public YearMonthDay calculateLastAcademicActDate(final CurriculumGroup group, final ExecutionYear executionYear,
            final boolean forConclusion) {
        return calculateLastAcademicActDate(group, new Predicate<CurriculumLine>() {
            @Override
            public boolean apply(final CurriculumLine toEval) {
                return toEval.getExecutionYear() == executionYear;
            }
        }, forConclusion);
    }

    static public YearMonthDay calculateLastAcademicActDate(final CurriculumGroup group, final boolean forConclusion) {
        return calculateLastAcademicActDate(group, new Predicate<CurriculumLine>() {
            @Override
            public boolean apply(final CurriculumLine toEval) {
                return !forConclusion || toEval.isApproved();
            }
        }, forConclusion);
    }

    static private YearMonthDay calculateLastAcademicActDate(final CurriculumGroup group,
            final Predicate<CurriculumLine> predicate, final boolean forConclusion) {
        final SortedSet<YearMonthDay> result = new TreeSet<YearMonthDay>();
        for (final CurriculumLine line : group.getAllCurriculumLines()) {
            if (line.getCurriculumGroup().isNoCourseGroupCurriculumGroup()) {
                continue;
            }

            if (CurriculumLineServices.isAffinity(line)) {
                continue;
            }

            if (!predicate.apply(line)) {
                continue;
            }

            final YearMonthDay lineAcademicActDate = CurriculumLineServices.getAcademicActDate(line, forConclusion);
            if (lineAcademicActDate != null) {
                result.add(lineAcademicActDate);
            }
        }
        return result.isEmpty() ? null : result.last();
    }

}
