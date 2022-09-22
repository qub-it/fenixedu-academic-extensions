package org.fenixedu.academic.domain.evaluation.season;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.util.date.IntervalTools;
import org.fenixedu.commons.i18n.I18N;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EvaluationSeasonPeriod extends EvaluationSeasonPeriod_Base {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormat.forPattern("dd/MM/yyyy").withLocale(I18N.getLocale());

    private static final Comparator<Interval> COMPARATOR_INTERVAL = (x, y) -> x.getStart().compareTo(y.getStart());

    protected EvaluationSeasonPeriod() {
        super();
    }

    static public EvaluationSeasonPeriod create(final ExecutionInterval executionInterval,
            final EvaluationSeasonPeriodType periodType, final EvaluationSeason evaluationSeason,
            final Set<DegreeType> degreeTypes, final LocalDate start, final LocalDate end) {

        final EvaluationSeasonPeriod result = new EvaluationSeasonPeriod();
        result.setExecutionSemester(executionInterval);
        result.setSeason(evaluationSeason);
        result.setPeriodType(periodType);
        result.addInterval(start, end);

        final Set<ExecutionDegree> initialExecutionDegrees =
                getInitialExecutionDegrees(executionInterval.getExecutionYear(), degreeTypes);
        result.getExecutionDegreesSet().addAll(initialExecutionDegrees);

        return result;
    }

    private static Set<ExecutionDegree> getInitialExecutionDegrees(final ExecutionYear year, final Set<DegreeType> degreeTypes) {
        if (year != null && !degreeTypes.isEmpty()) {
            return year.getExecutionDegreesSet().stream().filter(ed -> degreeTypes.contains(ed.getDegreeType()))
                    .collect(Collectors.toSet());

        }
        return Set.of();
    }

    public void addDegree(final ExecutionDegree input) {
        getExecutionDegreesSet().add(input);
    }

    public void removeDegree(final ExecutionDegree input) {
        getExecutionDegreesSet().remove(input);
    }

    public void addInterval(final LocalDate start, final LocalDate end) {
        final List<Interval> intervals = getIntervals();
        intervals.add(IntervalTools.getInterval(start, end));
        intervals.sort(COMPARATOR_INTERVAL);
        setIntervalsRaw(intervalsGson.toJsonTree(intervals));
    }

    public void removeInterval(final LocalDate start, final LocalDate end) {
        final List<Interval> intervals = getIntervals();
        intervals.removeIf(i -> i.getStart().toLocalDate().equals(start) && i.getEnd().toLocalDate().equals(end));
        if (intervals.isEmpty()) {
            throw new AcademicExtensionsDomainException("error.OccupationPeriod.required.Interval");
        }
        intervals.sort(COMPARATOR_INTERVAL);
        setIntervalsRaw(intervalsGson.toJsonTree(intervals));
    }

    public void delete() {
        super.setExecutionSemester(null);
        super.setSeason(null);
        getExecutionDegreesSet().clear();
        deleteDomainObject();
    }

    public static Set<EvaluationSeasonPeriod> findBy(final ExecutionYear executionYear) {
        return executionYear != null ? executionYear.getExecutionPeriodsSet().stream()
                .flatMap(es -> es.getEvaluationSeasonPeriodSet().stream()).collect(Collectors.toSet()) : new HashSet<>();
    }

    @Deprecated
    @Override
    public ExecutionInterval getExecutionSemester() {
        return super.getExecutionSemester();
    }

    public ExecutionInterval getExecutionInterval() {
        return super.getExecutionSemester();
    }

    public static String getIntervalsDescription(final Set<EvaluationSeasonPeriod> input) {
        final List<Interval> intervals = input.stream().flatMap(p -> p.getIntervals().stream()).collect(Collectors.toList());
        return getIntervalsDescription(intervals);
    }

    public String getIntervalsDescription() {
        return getIntervalsDescription(getIntervals());
    }

    static private String getIntervalsDescription(final List<Interval> intervals) {
        final String result = intervals.stream().sorted(Comparator.comparing(Interval::getStart))
                .map(i -> DATE_TIME_FORMATTER.print(i.getStart()) + " <-> " + DATE_TIME_FORMATTER.print(i.getEnd()))
                .collect(Collectors.joining("; "));

        return result;
    }

    public Set<ExecutionDegree> getExecutionDegrees() {
        return getExecutionDegreesSet();
    }

    public List<Interval> getIntervals() {
        final List<Interval> result = new ArrayList<>();
        if (getIntervalsRaw() != null) {
            getIntervalsRaw().getAsJsonArray().forEach(elem -> result.add(intervalsGson.fromJson(elem, Interval.class)));
        }
        return result;
    }

    public boolean isContainingDate(final LocalDate date) {
        return getIntervals().stream().anyMatch(i -> i.contains(date.toDateTimeAtStartOfDay()));
    }

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

}
