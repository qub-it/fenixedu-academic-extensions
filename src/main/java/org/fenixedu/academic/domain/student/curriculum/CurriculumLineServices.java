package org.fenixedu.academic.domain.student.curriculum;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.Equivalence;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.i18n.LocalizedString.Builder;
import org.joda.time.YearMonthDay;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import pt.ist.fenixframework.dml.runtime.RelationAdapter;

abstract public class CurriculumLineServices {

    static private RelationAdapter<Dismissal, Credits> ON_DISMISSAL_DELETION = new RelationAdapter<Dismissal, Credits>() {

        @Override
        public void beforeRemove(final Dismissal dismissal, final Credits credits) {
            // avoid internal invocation with null
            if (dismissal == null || credits == null) {
                return;
            }

            credits.setReason(null);
        }
    };

    static {
        Dismissal.getRelationCreditsDismissalEquivalence().addListener(ON_DISMISSAL_DELETION);
    }

    static public boolean isOptionalByGroup(final CurriculumLine line) {
        final CurriculumGroup group = line == null ? null : line.getCurriculumGroup();
        final CourseGroup groupModule = group == null ? null : group.getDegreeModule();
        return groupModule == null ? false : groupModule.getIsOptional();
    }

    static public void setRemarks(final CurriculumLine curriculumLine, final String remarks) {
        CurriculumLineExtendedInformation.findOrCreate(curriculumLine).setRemarks(remarks);
    }

    static public String getRemarks(final CurriculumLine curriculumLine) {
        return curriculumLine.getExtendedInformation() == null ? null : curriculumLine.getExtendedInformation().getRemarks();
    }

    //TODO: rename getOverridenCurricularYear and move CurricularPeriodServices.getCurricularYear/semester to this service
    static public void setCurricularYear(final CurriculumLine curriculumLine, final Integer curricularYear) {
        CurriculumLineExtendedInformation.findOrCreate(curriculumLine).setCurricularYear(curricularYear);
    }

    //TODO: rename to getOverridenCurricularYear and move CurricularPeriodServices.getCurricularYear/semester to this service
    static public Integer getCurricularYear(final CurriculumLine curriculumLine) {
        return curriculumLine.getExtendedInformation() == null ? null : curriculumLine.getExtendedInformation()
                .getCurricularYear();
    }

    static public void setExcludedFromAverage(CurriculumLine curriculumLine, Boolean excludedFromAverage) {
        CurriculumLineExtendedInformation.findOrCreate(curriculumLine).setExcludedFromAverage(excludedFromAverage);
    }

    static public boolean isExcludedFromAverage(CurriculumLine curriculumLine) {
        return curriculumLine.getExtendedInformation() != null
                && curriculumLine.getExtendedInformation().getExcludedFromAverage() != null
                && curriculumLine.getExtendedInformation().getExcludedFromAverage().booleanValue();
    }

    static public void setExcludedFromCurriculum(final CurriculumLine input, final boolean value) {
        CurriculumLineExtendedInformation.findOrCreate(input).setExcludedFromCurriculum(value);
    }

    static public boolean isExcludedFromCurriculum(final CurriculumLine input) {
        return input.getExtendedInformation() != null && input.getExtendedInformation().getExcludedFromCurriculum();
    }

    static public void setEctsCredits(CurriculumLine curriculumLine, BigDecimal ectsCredits) {
        CurriculumLineExtendedInformation.findOrCreate(curriculumLine).setEctsCredits(ectsCredits);
    }

    static public BigDecimal getEctsCredits(CurriculumLine curriculumLine) {
        return curriculumLine.getExtendedInformation() == null ? null : curriculumLine.getExtendedInformation().getEctsCredits();
    }

    static public void setWeight(CurriculumLine curriculumLine, BigDecimal weight) {
        CurriculumLineExtendedInformation.findOrCreate(curriculumLine).setWeight(weight);
    }

    static public BigDecimal getWeight(CurriculumLine curriculumLine) {
        return curriculumLine.getExtendedInformation() == null ? null : curriculumLine.getExtendedInformation().getWeight();
    }

    static public LocalizedString getCurriculumEntryDescription(final ICurriculumEntry entry,
            final StudentCurricularPlan studentCurricularPlan, final boolean includeDismissalInfoForReasonWithInfoHidden,
            final boolean hideCreditsReasonDetails) {

        final Builder result = new LocalizedString().builder();
        final Set<CurriculumLine> lines = entry.getCurriculumLinesForCurriculum(studentCurricularPlan);

        if (lines.isEmpty()) {
            if (Enrolment.class.isAssignableFrom(entry.getClass())) {
                add(result, "label.approvalType.Enrolment");
            }

        } else {

            // TODO legidio, will probably be asked to group Dismissals by CreditsReasonType, to avoid lines to long
            lines.stream().sorted(COMPARATOR).forEach(line -> {

                if (line.isDismissal()) {
                    final Dismissal dismissal = (Dismissal) line;
                    final Credits credits = dismissal.getCredits();
                    final CreditsReasonType reason = credits == null ? null : credits.getReason();
                    final LocalizedString info =
                            reason == null ? null : reason.getInfo(entry, dismissal, hideCreditsReasonDetails);

                    if (info != null && !info.isEmpty()) {
                        add(result, info);

                    } else if (reason == null || includeDismissalInfoForReasonWithInfoHidden) {

                        if (Substitution.class.isAssignableFrom(credits.getClass())) {
                            add(result, "label.approvalType.Substitution");

                        } else if (Equivalence.class.isAssignableFrom(credits.getClass())) {
                            add(result, "label.approvalType.Equivalence");
                        }
                    }
                }
            });
        }

        final LocalizedString built = CreditsReasonType.removeDuplicateItemDescriptions(result.build());
        // null forces hidden; empty forces fallback
        return built.isEmpty() ? null : built;
    }

   
    static private void add(final Builder builder, final String key) {
        add(builder, AcademicExtensionsUtil.bundleI18N(key));
    }

    static private void add(final Builder builder, final LocalizedString localizedString) {
        if (!builder.build().anyMatch(i -> i.contains(localizedString.getContent()))) {
            builder.append(localizedString, CreditsReasonType.SEPARATOR);
        }
    }

    static private Comparator<CurriculumLine> COMPARATOR_CONTEXT = (o1, o2) -> {
        final Optional<Context> c1 = CurriculumLineServices.getParentContexts(o1).stream().sorted(Context::compareTo).findFirst();
        final Optional<Context> c2 = CurriculumLineServices.getParentContexts(o2).stream().sorted(Context::compareTo).findFirst();

        if (c1.isPresent() && !c2.isPresent()) {
            return -1;
        }

        if (!c1.isPresent() && c2.isPresent()) {
            return 1;
        }

        if (!c1.isPresent() && !c2.isPresent()) {
            return 0;
        }

        return c1.get().compareTo(c2.get());
    };

    static private Comparator<CurriculumLine> COMPARATOR_FULL_PATH = (o1, o2) -> {
        final List<CurriculumGroup> fullPath1 = collectFullPath(o1.getCurriculumGroup());
        final List<CurriculumGroup> fullPath2 = collectFullPath(o2.getCurriculumGroup());

        for (int i = 0; i < Math.max(fullPath1.size(), fullPath2.size()); i++) {
            final CurriculumGroup group1 = i >= fullPath1.size() ? null : fullPath1.get(i);
            final CurriculumGroup group2 = i >= fullPath2.size() ? null : fullPath2.get(i);

            if (group1 == null && group2 != null) {
                return -1;
            }

            if (group1 != null && group2 == null) {
                return 1;
            }

            if (group1 == group2) {
                continue;
            }

            if (group1.getDegreeModule() == null && group2.getDegreeModule() != null) {
                return 1;
            }

            if (group1.getDegreeModule() != null && group2.getDegreeModule() == null) {
                return -1;
            }

            if (group1.getDegreeModule() == null && group2.getDegreeModule() == null) {
                return 0;
            }

            final Context context1 = group1.getDegreeModule().getParentContextsSet().iterator().next();
            final Context context2 = group2.getDegreeModule().getParentContextsSet().iterator().next();
            return context1.compareTo(context2);
        }

        return 0;
    };

    static private List<CurriculumGroup> collectFullPath(final CurriculumGroup input) {
        final List<CurriculumGroup> result = Lists.newArrayList();

        if (input != null) {
            result.addAll(collectFullPath(input.getCurriculumGroup()));
            result.add(input);
        }

        return result;
    }

    static public Comparator<CurriculumLine> COMPARATOR = (o1, o2) -> {
        final ComparatorChain comparatorChain = new ComparatorChain();
        comparatorChain.addComparator(COMPARATOR_FULL_PATH);
        comparatorChain.addComparator(COMPARATOR_CONTEXT);
        comparatorChain.addComparator(ICurriculumEntry.COMPARATOR_BY_EXECUTION_PERIOD_AND_NAME_AND_ID);

        return comparatorChain.compare(o1, o2);
    };

    static public Collection<Context> getParentContexts(final CurriculumLine curriculumLine) {

        if (curriculumLine.getDegreeModule() == null) {
            return Collections.emptySet();
        }

        final CurricularCourse curricularCourse =
                curriculumLine instanceof OptionalEnrolment ? ((OptionalEnrolment) curriculumLine)
                        .getOptionalCurricularCourse() : curriculumLine.getCurricularCourse();

        return curricularCourse.getParentContextsByExecutionYear(curriculumLine.getExecutionYear()).stream()
                .filter(c -> c.getParentCourseGroup() == curriculumLine.getCurriculumGroup().getDegreeModule())
                .collect(Collectors.toSet());
    }

    static public YearMonthDay getAcademicActDate(final CurriculumLine input, final boolean forConclusion) {
        YearMonthDay result = null;

        if (Enrolment.class.isAssignableFrom(input.getClass())) {
            result = getAcademicActDate((Enrolment) input, forConclusion);

        } else if (Dismissal.class.isAssignableFrom(input.getClass())) {
            result = getAcademicActDate((Dismissal) input);
        }

        return result;
    }

    static private YearMonthDay getAcademicActDate(final Dismissal input) {
        return input.getCredits().getOfficialDate() != null ? input.getCredits().getOfficialDate().toDateTimeAtStartOfDay()
                .toYearMonthDay() : input.getCreationDateDateTime().toYearMonthDay();
    }

    static private YearMonthDay getAcademicActDate(final Enrolment input, final boolean forConclusion) {
        if (input.isAnnulled()) {
            return null;
        }

        if (forConclusion && input.isApproved()) {
            return input.calculateConclusionDate();
        }

        final EnrolmentEvaluation enrolmentEvaluation = getLatestEnrolmentEvaluationForAcademicAct(input.getEvaluationsSet());
        return enrolmentEvaluation == null ? null : enrolmentEvaluation.getExamDateYearMonthDay();
    }

    static private EnrolmentEvaluation getLatestEnrolmentEvaluationForAcademicAct(
            final Collection<EnrolmentEvaluation> evaluations) {
        return evaluations == null ? null : evaluations.stream().filter(i -> i.isFinal())
                .max(Comparator.comparing(EnrolmentEvaluation::getExamDateYearMonthDay)).orElse(null);
    }

    static public boolean isSourceOfAnyCredits(final ICurriculumEntry entry, final StudentCurricularPlan studentCurricularPlan) {
        return studentCurricularPlan.getCreditsSet().stream().anyMatch(c -> c.getIEnrolments().contains(entry));
    }

    /**
     * @deprecated use CurriculumLine.isAffinity
     */
    @Deprecated
    static public boolean isAffinity(final CurriculumLine line) {
        return line.isAffinity();
    }

    static public boolean isEvaluated(final CurriculumLine line) {
        if (line != null) {
            if (line instanceof Dismissal) {
                return true;
            }
            if (line instanceof Enrolment) {
                final Enrolment enrolment = (Enrolment) line;
                return enrolment.getEvaluationsSet().stream()
                        .anyMatch(ev -> ev.getCompetenceCourseMarkSheet() != null || ev.isFinal());
            }
        }

        return false;
    }

    /**
     * @deprecated use CurriculumLine.isNormal
     */
    @Deprecated
    static public boolean isNormal(final CurriculumLine line) {
        return line.isNormal();
    }

}
