package org.fenixedu.academic.domain.executionPlanning.services;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularYearList;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.OccupationPeriodReference;
import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.academic.util.date.IntervalTools;
import org.fenixedu.academicextensions.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.commons.i18n.I18N;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

abstract public class OccupationPeriodServices {

    static final private Logger logger = LoggerFactory.getLogger(OccupationPeriodServices.class);

    static public Comparator<OccupationPeriod> COMPARATOR = (o1, o2) -> {

        if (o1 != null && o2 != null) {

            return ComparisonChain.start()
                    .compare(o1.getPeriodInterval().getStartMillis(), o2.getPeriodInterval().getStartMillis())
                    .compare(o1.getExecutionDegreesSet().size(), o2.getExecutionDegreesSet().size())
                    .compare(o1.getExternalId(), o2.getExternalId()).result();

        } else {
            return o1 != null ? -1 : 1;
        }
    };

    static private Comparator<Interval> COMPARATOR_INTERVAL = (x, y) -> x.getStart().compareTo(y.getStart());

    static private DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("dd/MM/yyyy").withLocale(I18N.getLocale());

    static private CurricularYearList ALL_CURRICULAR_YEARS = CurricularYearList.internalize("-1");

    static public interface OccupationPeriodPartner {

        public OccupationPeriod getOccupationPeriod();

        public ExecutionSemester getExecutionSemester();

        public Function<OccupationPeriod, OccupationPeriod> setOccupationPeriod();

        // sometimes we have to clean up wrong assumption from trunk code based on OccupationPeriodType
        public Function<OccupationPeriodReference, OccupationPeriodReference> createdReferenceCleanup();
    }

    static private boolean isValid(final OccupationPeriodPartner partner) {
        return partner != null && partner.getOccupationPeriod() != null && partner.getExecutionSemester() != null;
    }

    static public OccupationPeriod createOccupationPeriod(final OccupationPeriodPartner partner, final LocalDate start,
            final LocalDate end, final Set<ExecutionDegree> degrees, final OccupationPeriodType periodType) {

        final OccupationPeriod result = new OccupationPeriod(getInterval(start, end));
        partner.setOccupationPeriod().apply(result);
        createReferences(partner, degrees, periodType);

        checkRules(result);
        return result;
    }

    static private void checkRules(final OccupationPeriod input) {
        final Set<OccupationPeriodReference> references = input.getExecutionDegreesSet();

        if (references.isEmpty()) {
            throw new AcademicExtensionsDomainException("error.OccupationPeriod.required.ExecutionDegree");
        }

        // All OccupationPeriodReference must have executionDegrees of exactly one ExecutionYear
        final ExecutionYear year = references.iterator().next().getExecutionDegree().getExecutionYear();
        for (final OccupationPeriodReference reference : references) {
            if (reference.getExecutionDegree().getExecutionYear() != year) {
                throw new AcademicExtensionsDomainException("error.OccupationPeriod.inconsistent.ExecutionYear");
            }
        }

        if (input.getIntervals().isEmpty()) {
            throw new AcademicExtensionsDomainException("error.OccupationPeriod.required.Interval");
        }
    }

    static private void createReferences(final OccupationPeriodPartner partner, final Set<ExecutionDegree> degrees,
            final OccupationPeriodType periodType) {

        if (isValid(partner) && degrees != null) {

            for (final ExecutionDegree degree : degrees) {
                createReference(partner, degree, periodType);
            }
        }
    }

    static private OccupationPeriodReference createReference(final OccupationPeriodPartner partner, final ExecutionDegree degree,
            final OccupationPeriodType periodType) {

        OccupationPeriodReference result = null;

        if (degree != null && periodType != null) {
            final int semester = partner.getExecutionSemester().getSemester();

            result = new OccupationPeriodReference(partner.getOccupationPeriod(), degree, periodType, semester,
                    ALL_CURRICULAR_YEARS);
            partner.createdReferenceCleanup().apply(result);
        }

        return result;
    }

    static public void deleteOccupationPeriod(final OccupationPeriod occupationPeriod) {
        if (occupationPeriod != null) {

            for (final Iterator<OccupationPeriodReference> iterator =
                    occupationPeriod.getExecutionDegreesSet().iterator(); iterator.hasNext();) {

                final OccupationPeriodReference reference = iterator.next();
                iterator.remove();
                reference.delete();
            }

            occupationPeriod.delete();
        }
    }

    static public Set<OccupationPeriodReference> getReferences(final OccupationPeriodPartner partner) {
        return !isValid(partner) ? Sets.newHashSet() : partner.getOccupationPeriod().getExecutionDegreesSet();
    }

    static public Set<ExecutionDegree> getExecutionDegrees(final OccupationPeriodPartner partner) {
        return getReferences(partner).stream().filter(i -> i.getExecutionDegree() != null).map(i -> i.getExecutionDegree())
                .collect(Collectors.toSet());
    }

    static public Set<ExecutionDegree> addDegree(final OccupationPeriodPartner partner, final ExecutionDegree toAdd) {
        final Set<ExecutionDegree> current = getExecutionDegrees(partner);

        if (toAdd != null) {

            if (current.contains(toAdd)) {
                return current;
            }

            current.add(toAdd);
            editDegrees(partner, current);
        }

        return current;
    }

    static public Set<ExecutionDegree> removeDegree(final OccupationPeriodPartner partner, final ExecutionDegree toRemove) {
        final Set<ExecutionDegree> current = getExecutionDegrees(partner);

        if (toRemove != null) {

            if (!current.contains(toRemove)) {
                return current;
            }

            current.remove(toRemove);
            editDegrees(partner, current);
        }

        return current;
    }

    static private void editDegrees(final OccupationPeriodPartner partner, final Set<ExecutionDegree> degrees) {

        // Step 1, Remove unwanted references
        final Set<OccupationPeriodReference> references = getReferences(partner);

        for (final Iterator<OccupationPeriodReference> iterator = references.iterator(); iterator.hasNext();) {
            final OccupationPeriodReference reference = iterator.next();
            final ExecutionDegree degree = reference.getExecutionDegree();

            if (degrees.contains(degree)) {
                // nothing to be done, existing references will not be updated 
                degrees.remove(degree);

            } else {
                iterator.remove();
                reference.delete();
            }
        }

        // Step 2, Add new references
        if (!degrees.isEmpty()) {
            createReferences(partner, degrees, getOccupationPeriodType(partner));
        }

        checkRules(partner.getOccupationPeriod());
    }

    static public void addInterval(final OccupationPeriodPartner partner, final LocalDate start, final LocalDate end) {
        final List<Interval> current = getIntervals(partner);

        final Interval toAdd = getInterval(start, end);
        if (toAdd != null) {

            for (final Iterator<Interval> iterator = current.iterator(); iterator.hasNext();) {
                final Interval interval = iterator.next();

                if (equals(interval, toAdd)) {
                    throw new AcademicExtensionsDomainException("error.OccupationPeriod.duplicate.Interval",
                            getIntervalDescription(interval));
                }

                // special case when we want to edit the only existing interval
                // if that's the case, we remove the existing and exit the loop; we'll add the new interval after this
                if (current.size() == 1) {

                    if (interval.contains(toAdd)) {
                        iterator.remove();
                        break;
                    }

                    if (toAdd.contains(interval)) {
                        iterator.remove();
                        break;
                    }
                }

                if (interval.contains(toAdd.getStart())) {
                    throw new AcademicExtensionsDomainException("error.OccupationPeriod.inconsistent.Interval.start",
                            getIntervalDescription(interval));
                }

                if (interval.contains(toAdd.getEnd())) {
                    throw new AcademicExtensionsDomainException("error.OccupationPeriod.inconsistent.Interval.end",
                            getIntervalDescription(interval));
                }
            }

            current.add(toAdd);
            editIntervals(partner, current);
        }
    }

    static public void removeInterval(final OccupationPeriodPartner partner, final LocalDate start, final LocalDate end) {
        final List<Interval> current = getIntervals(partner);

        final Interval toRemove = getInterval(start, end);
        if (toRemove != null) {

            for (final Iterator<Interval> iterator = current.iterator(); iterator.hasNext();) {
                final Interval interval = iterator.next();

                if (equals(interval, toRemove)) {
                    iterator.remove();
                }
            }

            if (current.size() == getIntervals(partner).size()) {
                throw new AcademicExtensionsDomainException("error.OccupationPeriod.interval.not.found");
            }

            editIntervals(partner, current);
        }
    }

    static private void editIntervals(final OccupationPeriodPartner partner, final List<Interval> intervals) {
        if (intervals.isEmpty()) {
            throw new AcademicExtensionsDomainException("error.OccupationPeriod.required.Interval");
        }
        Collections.sort(intervals, COMPARATOR_INTERVAL);

        final OccupationPeriod occupationPeriod = partner.getOccupationPeriod();
        occupationPeriod.editDates(intervals.iterator());
        checkRules(occupationPeriod);
    }

    static private boolean equals(final Interval o1, final Interval o2) {
        return o2.getStart().equals(o1.getStart()) && o2.getEnd().equals(o1.getEnd());
    }

    static private Interval getInterval(final LocalDate start, final LocalDate end) {
        Interval result = null;

        try {
            result = IntervalTools.getInterval(start, end);
            result = Interval.parse(result.toString());
        } catch (final Throwable t) {
            result = null;
        }

        return result;
    }

    static public List<Interval> getIntervals(final OccupationPeriodPartner partner) {
        return !isValid(partner) ? Lists.newArrayList() : partner.getOccupationPeriod().getIntervals();
    }

    static public String getIntervalsDescription(final List<Interval> intervals) {
        final StringBuilder result = new StringBuilder();

        Collections.sort(intervals, COMPARATOR_INTERVAL);
        for (final Iterator<Interval> iterator = intervals.iterator(); iterator.hasNext();) {

            result.append(getIntervalDescription(iterator.next()));

            if (iterator.hasNext()) {
                result.append(" ; ");
            }
        }

        return result.toString();
    }

    static private String getIntervalDescription(final Interval interval) {
        final StringBuilder result = new StringBuilder();

        result.append(DATE_TIME_FORMATTER.print(interval.getStart()));
        result.append(" <-> ");
        result.append(DATE_TIME_FORMATTER.print(interval.getEnd()));

        return result.toString();
    }

    @SuppressWarnings("deprecation")
    static private OccupationPeriodType getOccupationPeriodType(final OccupationPeriodPartner partner) {
        final Set<OccupationPeriodReference> references = getReferences(partner);
        final OccupationPeriodType periodType = references.iterator().next().getPeriodType();
        if (!references.stream().allMatch(i -> i.getPeriodType() == periodType)) {
            throw new AcademicExtensionsDomainException("error.OccupationPeriod.inconsistent.PeriodType");
        }

        return periodType;
    }

    static public boolean isContainingDate(final OccupationPeriodPartner partner, final LocalDate date) {
        return !isValid(partner) ? false : partner.getOccupationPeriod()
                .nestedOccupationPeriodsContainsDay(new YearMonthDay(date));
    }

}
