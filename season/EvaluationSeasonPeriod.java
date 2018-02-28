package org.fenixedu.academic.domain.evaluation.season;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.OccupationPeriodReference;
import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.ulisboa.specifications.domain.services.OccupationPeriodServices;
import org.fenixedu.ulisboa.specifications.domain.services.OccupationPeriodServices.OccupationPeriodPartner;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class EvaluationSeasonPeriod extends EvaluationSeasonPeriod_Base
        implements Comparable<EvaluationSeasonPeriod>, OccupationPeriodPartner {

    protected EvaluationSeasonPeriod() {
        super();
    }

    @Atomic
    static public EvaluationSeasonPeriod create(final ExecutionSemester executionSemester,
            final EvaluationSeasonPeriodType periodType, final EvaluationSeason evaluationSeason,
            final Set<DegreeType> degreeTypes, final LocalDate start, final LocalDate end) {

        final EvaluationSeasonPeriod result = new EvaluationSeasonPeriod();
        result.setExecutionSemester(executionSemester);
        result.setSeason(evaluationSeason);
        result.setOccupationPeriod(OccupationPeriodServices.createOccupationPeriod(result, start, end,
                result.getInitialExecutionDegrees(degreeTypes), periodType.translate()));
        result.checkRules();
        return result;
    }

    private Set<ExecutionDegree> getInitialExecutionDegrees(final Set<DegreeType> degreeTypes) {
        final Set<ExecutionDegree> result = Sets.newHashSet();

        final ExecutionYear year = getExecutionYear();
        if (year != null && degreeTypes != null && !degreeTypes.isEmpty()) {

            result.addAll(ExecutionDegree.getAllByExecutionYearAndDegreeType(year,
                    degreeTypes.toArray(new DegreeType[degreeTypes.size()])));
        }

        return result;
    }

    @Override
    public Function<OccupationPeriod, OccupationPeriod> setOccupationPeriod() {
        return (occupationPeriod) -> {

            setOccupationPeriod(occupationPeriod);
            return occupationPeriod;
        };
    }

    @Override
    public Function<OccupationPeriodReference, OccupationPeriodReference> createdReferenceCleanup() {
        return (reference) -> {

            if (reference != null) {
                // remove all evaluation seasons wrongly deduced by the type given to the constructor...
                // ...and set the correct evaluation season
                reference.getEvaluationSeasonSet().clear();
                reference.addEvaluationSeason(getSeason());
            }

            return reference;
        };
    }

    private void checkRules() {
        checkConsistencySeason();
        // TODO legidio, rethink this
        // checkDuplicates();
    }

    /**
     * All OccupationPeriodReference must have exactly one season
     */
    private void checkConsistencySeason() {
        for (final OccupationPeriodReference reference : getReferences()) {
            for (final EvaluationSeason season : reference.getEvaluationSeasonSet()) {
                if (season != getSeason()) {
                    throw new AcademicExtensionsDomainException("error.EvaluationSeasonPeriod.evaluationSeason.inconsistent");
                }
            }
        }
    }

    /**
     * For a given ExecutionSemester and EvaluationSeasonPeriodType, one OccupationPeriod (considering all of it's Intervals) can
     * only be duplicated if the EvaluationSeason is different
     */
    private void checkDuplicates() {
        for (final EvaluationSeasonPeriod iter : findBy(getExecutionSemester(), getPeriodType())) {
            if (iter != this && iter.getSeason() == getSeason()) {

                if (iter.getOccupationPeriod().isEqualTo(getOccupationPeriod())) {
                    throw new AcademicExtensionsDomainException("error.EvaluationSeasonPeriod.occupationPeriod.duplicate");
                }
            }
        }
    }

    @Atomic
    public void addDegree(final ExecutionDegree input) {
        OccupationPeriodServices.addDegree(this, input);
        checkRules();
    }

    @Atomic
    public void removeDegree(final ExecutionDegree input) {
        OccupationPeriodServices.removeDegree(this, input);
        checkRules();
    }

    @Atomic
    public void addInterval(final LocalDate start, final LocalDate end) {
        OccupationPeriodServices.addInterval(this, start, end);
        checkRules();
    }

    @Atomic
    public void removeInterval(final LocalDate start, final LocalDate end) {
        OccupationPeriodServices.removeInterval(this, start, end);
        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {
        super.setExecutionSemester(null);
        super.setSeason(null);

        final OccupationPeriod occupationPeriod = getOccupationPeriod();
        super.setOccupationPeriod(null);
        OccupationPeriodServices.deleteOccupationPeriod(occupationPeriod);

        AcademicExtensionsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        deleteDomainObject();
    }

    static public Set<EvaluationSeasonPeriod> findBy(final ExecutionYear executionYear,
            final EvaluationSeasonPeriodType periodType) {

        final Set<EvaluationSeasonPeriod> result = Sets.<EvaluationSeasonPeriod> newHashSet();
        if (executionYear != null && periodType != null) {

            for (final ExecutionSemester semester : executionYear.getExecutionPeriodsSet()) {
                result.addAll(findBy(semester, periodType));
            }
        }

        return result;
    }

    static public Set<EvaluationSeasonPeriod> findBy(final ExecutionSemester semester,
            final EvaluationSeasonPeriodType periodType) {

        final Set<EvaluationSeasonPeriod> result = Sets.<EvaluationSeasonPeriod> newHashSet();
        if (semester != null && periodType != null) {

            for (final EvaluationSeasonPeriod period : semester.getEvaluationSeasonPeriodSet()) {

                if (period.getPeriodType() == periodType) {
                    result.add(period);
                }
            }
        }

        return result;
    }

    @SuppressWarnings("deprecation")
    static public enum EvaluationSeasonPeriodType {

        GRADE_SUBMISSION {

            @Override
            protected OccupationPeriodType translate() {
                // Note: OccupationPeriodType.XPTO_SPECIAL_SEASON is never persisted
                return OccupationPeriodType.GRADE_SUBMISSION;
            }
        },

        EXAMS {

            @Override
            protected OccupationPeriodType translate() {
                // Note: OccupationPeriodType.XPTO_SPECIAL_SEASON is never persisted
                return OccupationPeriodType.EXAMS;
            }
        };

        public LocalizedString getDescriptionI18N() {
            return AcademicExtensionsUtil.bundleI18N(name());
        }

        abstract protected OccupationPeriodType translate();

        static protected EvaluationSeasonPeriodType get(final OccupationPeriod input) {
            switch (input.getExecutionDegreesSet().iterator().next().getPeriodType()) {

            case EXAMS:
            case EXAMS_SPECIAL_SEASON:
                return EvaluationSeasonPeriodType.EXAMS;
            case GRADE_SUBMISSION:
            case GRADE_SUBMISSION_SPECIAL_SEASON:
                return EvaluationSeasonPeriodType.GRADE_SUBMISSION;
            default:
                throw new RuntimeException();
            }
        }

    }

    public EvaluationSeasonPeriodType getPeriodType() {
        return EvaluationSeasonPeriodType.get(getOccupationPeriod());
    }

    private ExecutionYear getExecutionYear() {
        return getExecutionSemester().getExecutionYear();
    }

    static public String getIntervalsDescription(final Set<EvaluationSeasonPeriod> input) {
        final List<Interval> intervals = Lists.newLinkedList();

        for (final EvaluationSeasonPeriod period : input) {
            // TODO legidio, make sure it's unique
            intervals.addAll(period.getIntervals());
        }

        return OccupationPeriodServices.getIntervalsDescription(intervals);
    }

    public String getIntervalsDescription() {
        return OccupationPeriodServices.getIntervalsDescription(getIntervals());
    }

    @Override
    public int compareTo(final EvaluationSeasonPeriod other) {
        final OccupationPeriod o1 = this.getOccupationPeriod();
        final OccupationPeriod o2 = other.getOccupationPeriod();
        return OccupationPeriodServices.COMPARATOR.compare(o1, o2);
    }

    public String getDegreesDescription() {
        final StringBuilder result = new StringBuilder();

        final Map<DegreeType, Set<ExecutionDegree>> mapped = Maps.<DegreeType, Set<ExecutionDegree>> newLinkedHashMap();
        getExecutionDegrees().stream()
                .sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATORY_BY_DEGREE_TYPE_AND_NAME_AND_EXECUTION_YEAR).forEach(i -> {

                    final DegreeType key = i.getDegreeType();
                    if (!mapped.containsKey(key)) {
                        mapped.put(key, Sets.<ExecutionDegree> newLinkedHashSet());
                    }

                    mapped.get(key).add(i);
                });

        for (final Iterator<Entry<DegreeType, Set<ExecutionDegree>>> iterator = mapped.entrySet().iterator(); iterator
                .hasNext();) {
            final Entry<DegreeType, Set<ExecutionDegree>> entry = iterator.next();

            int size = entry.getValue().size();
            if (size != 0) {
                result.append(size);
                result.append(" - ");
                result.append(entry.getKey().getName().getContent());

                if (iterator.hasNext()) {
                    result.append(", ");
                }
            }
        }

        return result.toString();
    }

    public Set<ExecutionDegree> getExecutionDegrees() {
        return OccupationPeriodServices.getExecutionDegrees(this);
    }

    private Set<OccupationPeriodReference> getReferences() {
        return OccupationPeriodServices.getReferences(this);
    }

    public List<Interval> getIntervals() {
        return OccupationPeriodServices.getIntervals(this);
    }

    public boolean isContainingDate(final LocalDate date) {
        return OccupationPeriodServices.isContainingDate(this, date);
    }

}
