package org.fenixedu.academic.domain.evaluation.season;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.OccupationPeriodReference;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.executionPlanning.services.OccupationPeriodServices;
import org.fenixedu.academic.domain.executionPlanning.services.OccupationPeriodServices.OccupationPeriodPartner;
import org.fenixedu.academic.util.date.IntervalTools;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import pt.ist.fenixframework.Atomic;

public class EvaluationSeasonPeriod extends EvaluationSeasonPeriod_Base
        implements Comparable<EvaluationSeasonPeriod>, OccupationPeriodPartner {

    protected EvaluationSeasonPeriod() {
        super();
    }

    @Atomic
    static public EvaluationSeasonPeriod create(final ExecutionInterval executionInterval,
            final EvaluationSeasonPeriodType periodType, final EvaluationSeason evaluationSeason,
            final Set<DegreeType> degreeTypes, final LocalDate start, final LocalDate end) {

        final EvaluationSeasonPeriod result = new EvaluationSeasonPeriod();
        result.setExecutionSemester(executionInterval);
        result.setSeason(evaluationSeason);
        result.setPeriodType(periodType);
        result.addInterval(start, end);

        final Set<ExecutionDegree> initialExecutionDegrees = result.getInitialExecutionDegrees(degreeTypes);
        result.getExecutionDegreesSet().addAll(initialExecutionDegrees);

        result.setOccupationPeriod(OccupationPeriodServices.createOccupationPeriod(result, start, end, initialExecutionDegrees,
                periodType.translate()));
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
        for (final EvaluationSeasonPeriod iter : findBy(getExecutionInterval(), getPeriodType())) {
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

        if (getExecutionDegreesSet().isEmpty()) {
            getExecutionDegreesSet().addAll(getExecutionDegreesByOccupationPeriodReferences());
        }
        getExecutionDegreesSet().add(input);
    }

    @Atomic
    public void removeDegree(final ExecutionDegree input) {
        OccupationPeriodServices.removeDegree(this, input);
        checkRules();

        getExecutionDegreesSet().remove(input);
    }

    @Atomic
    public void addInterval(final LocalDate start, final LocalDate end) {
        final List<Interval> intervals = getIntervals();
        intervals.add(IntervalTools.getInterval(start, end));
        intervals.sort(COMPARATOR_INTERVAL);
        setIntervalsRaw(intervalsGson.toJsonTree(intervals));
    }

    @Atomic
    public void removeInterval(final LocalDate start, final LocalDate end) {
        final List<Interval> intervals = getIntervals();
        intervals.removeIf(i -> i.getStart().toLocalDate().equals(start) && i.getEnd().toLocalDate().equals(end));
        if (intervals.isEmpty()) {
            throw new AcademicExtensionsDomainException("error.OccupationPeriod.required.Interval");
        }
        intervals.sort(COMPARATOR_INTERVAL);
        setIntervalsRaw(intervalsGson.toJsonTree(intervals));
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {
        super.setExecutionSemester(null);
        super.setSeason(null);

        getExecutionDegreesSet().clear();

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

    static public Set<EvaluationSeasonPeriod> findBy(final ExecutionInterval interval,
            final EvaluationSeasonPeriodType periodType) {

        final Set<EvaluationSeasonPeriod> result = Sets.<EvaluationSeasonPeriod> newHashSet();
        if (interval != null && periodType != null) {

            for (final EvaluationSeasonPeriod period : interval.getEvaluationSeasonPeriodSet()) {

                if (period.getPeriodType() == periodType) {
                    result.add(period);
                }
            }
        }

        return result;
    }

    static public Set<EvaluationSeasonPeriod> findBy(final ExecutionYear executionYear) {
        return executionYear != null ? executionYear.getExecutionPeriodsSet().stream()
                .flatMap(es -> es.getEvaluationSeasonPeriodSet().stream()).collect(Collectors.toSet()) : new HashSet<>();
    }

    public EvaluationSeasonPeriodType getPeriodType() {
        return super.getPeriodType() != null ? super.getPeriodType() : EvaluationSeasonPeriodType.get(getOccupationPeriod());
    }

    private ExecutionYear getExecutionYear() {
        return getExecutionInterval().getExecutionYear();
    }

    @Deprecated
    @Override
    public ExecutionInterval getExecutionSemester() {
        return super.getExecutionSemester();
    }

    public ExecutionInterval getExecutionInterval() {
        return super.getExecutionSemester();
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
        return !getExecutionDegreesSet().isEmpty() ? getExecutionDegreesSet() : getExecutionDegreesByOccupationPeriodReferences();
    }

    public Set<ExecutionDegree> getExecutionDegreesByOccupationPeriodReferences() {
        return OccupationPeriodServices.getExecutionDegrees(this);
    }

    private Set<OccupationPeriodReference> getReferences() {
        return OccupationPeriodServices.getReferences(this);
    }

    public List<Interval> getIntervals() {
        if (getIntervalsRaw() != null) {
            final List<Interval> result = new ArrayList<>();
            getIntervalsRaw().getAsJsonArray().forEach(elem -> result.add(intervalsGson.fromJson(elem, Interval.class)));
            return result;
        }

        return OccupationPeriodServices.getIntervals(this);
    }

    public boolean isContainingDate(final LocalDate date) {
        return getIntervals().stream().anyMatch(i -> i.contains(date.toDateTimeAtStartOfDay()));
    }

    static private Comparator<Interval> COMPARATOR_INTERVAL = (x, y) -> x.getStart().compareTo(y.getStart());

    private static final Gson intervalsGson;

    static {
        intervalsGson = new GsonBuilder().registerTypeAdapter(Interval.class, new JsonSerializer<Interval>() {

            @Override
            public JsonElement serialize(Interval src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject json = new JsonObject();
                json.addProperty("start", src.getStart().toString());
                json.addProperty("end", src.getEnd().toString());
                return json;
            }
        }).registerTypeAdapter(Interval.class, new JsonDeserializer<Interval>() {

            @Override
            public Interval deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                final JsonObject jsonObject = json.getAsJsonObject();
                final DateTime start = new DateTime(jsonObject.get("start").getAsString());
                final DateTime end = new DateTime(jsonObject.get("end").getAsString());
                return new Interval(start, end);
            }
        }).create();
    }

    public boolean migrateIntervals() {
        if (getIntervalsRaw() == null) {
            final List<Interval> intervals = getIntervals();
            setIntervalsRaw(intervalsGson.toJsonTree(intervals));
            return true;
        }
        return false;
    }

    public boolean migratePeriodType() {
        if (super.getPeriodType() == null) {
            setPeriodType(getPeriodType());
            return true;
        }
        return false;
    }

    public boolean migrateDegrees() {
        if (super.getExecutionDegreesSet().isEmpty()) {
            super.getExecutionDegreesSet().addAll(getExecutionDegreesByOccupationPeriodReferences());
            return true;
        }
        return false;
    }

}
