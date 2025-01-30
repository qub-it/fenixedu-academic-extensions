package org.fenixedu.academic.domain.student;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.FenixEduAcademicExtensionsConfiguration;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.student.curriculum.CreditsReasonType;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.CurriculumConfigurationInitializer;
import org.fenixedu.academic.domain.student.curriculum.CurriculumConfigurationInitializer.CurricularYearResult;
import org.fenixedu.academic.domain.student.curriculum.CurriculumLineServices;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.EnrolmentWrapper;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.dml.runtime.RelationAdapter;

public class RegistrationServices {

    static final private Logger logger = LoggerFactory.getLogger(RegistrationServices.class);

    static private RelationAdapter<DegreeModule, CurriculumModule> CREDITS_CREATION_DISABLE_ACCUMULATED =
            new RelationAdapter<DegreeModule, CurriculumModule>() {

                @Override
                public void afterAdd(final DegreeModule degreeModule, final CurriculumModule curriculumModule) {
                    if (degreeModule == null || curriculumModule == null) {
                        return;
                    }

                    if (!(curriculumModule instanceof Dismissal)) {
                        return;
                    }

                    final Dismissal dismissal = (Dismissal) curriculumModule;
                    final StudentCurricularPlan dismissalPlan = dismissal.getStudentCurricularPlan();
                    final Registration dismissalReg = dismissalPlan.getRegistration();

                    if (!isCurriculumAccumulated(dismissalReg)) {
                        return;
                    }

                    //  hasIntertwinedCredits BUT from the same Registration
                    for (final IEnrolment iEnrolment : dismissal.getSourceIEnrolments()) {
                        if (iEnrolment instanceof Enrolment) {
                            final Enrolment enrolment = (Enrolment) iEnrolment;
                            if (!CurriculumLineServices.isExcludedFromCurriculum(enrolment)) {

                                final StudentCurricularPlan enrolmentPlan = enrolment.getStudentCurricularPlan();
                                if (dismissalReg == enrolmentPlan.getRegistration() && dismissalPlan != enrolmentPlan) {
                                    setCurriculumAccumulated(dismissalReg, false);
                                    return;
                                }
                            }
                        }
                    }
                }
            };

    public static void initialize() {
        Dismissal.getRelationDegreeModuleCurriculumModule().addListener(CREDITS_CREATION_DISABLE_ACCUMULATED);
    }

    static private String getCacheKey(final Registration registration, final ExecutionYear year) {
        return String.format("%s#%s", registration.getExternalId(), year == null ? "null" : year.getExternalId());
    }

    static final private Cache<String, ICurriculum> CACHE_CURRICULUMS =
            CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(10 * 1000).expireAfterWrite(2, TimeUnit.MINUTES).build();

    static public ICurriculum getCurriculum(final Registration registration, final ExecutionYear executionYear) {
        final String key = getCacheKey(registration, executionYear);

        try {
            return CACHE_CURRICULUMS.get(key, new Callable<ICurriculum>() {
                @Override
                public ICurriculum call() {
                    logger.debug(String.format("Miss on Curriculum cache [%s %s]", new DateTime(), key));
                    return registration.getCurriculum(executionYear);
                }
            });

        } catch (final Throwable t) {
            logger.error(String.format("Unable to get Curriculum [%s %s %s]", new DateTime(), key, t.getLocalizedMessage()));
            return null;
        }
    }

    static final private int CACHE_CURRICULAR_YEAR_EXPIRE_MINUTES =
            FenixEduAcademicExtensionsConfiguration.getConfiguration().getCurricularYearCalculatorCached() ? 5 : 0;

    static final private Cache<String, CurricularYearResult> CACHE_CURRICULAR_YEAR = CacheBuilder.newBuilder().concurrencyLevel(4)
            .maximumSize(10 * 1000).expireAfterWrite(CACHE_CURRICULAR_YEAR_EXPIRE_MINUTES, TimeUnit.MINUTES).build();

    static public CurricularYearResult getCurricularYear(final Registration registration, final ExecutionYear executionYear) {
        final String key = getCacheKey(registration, executionYear);

        try {

            return CACHE_CURRICULAR_YEAR.get(key, new Callable<CurricularYearResult>() {
                @Override
                public CurricularYearResult call() {
                    logger.debug(String.format("Miss on Registration CurricularYear cache [%s %s]", new DateTime(), key));
                    return CurriculumConfigurationInitializer
                            .calculateCurricularYear((Curriculum) getCurriculum(registration, executionYear));
                }
            });

        } catch (final Throwable t) {
            logger.error(String.format("Unable to get Registration CurricularYear [%s %s %s]", new DateTime(), key,
                    t.getLocalizedMessage()));
            return null;
        }
    }

    static public void invalidateCaches(final Registration registration, final ExecutionYear executionYear) {
        CACHE_CURRICULAR_YEAR.invalidate(getCacheKey(registration, executionYear));
        CACHE_CURRICULUMS.invalidate(getCacheKey(registration, executionYear));
    }

    static public ExecutionYear getConclusionExecutionYear(final Registration registration) {
        try {
            final StudentCurricularPlan lastCurricularPlan = registration.getLastStudentCurricularPlan();

            final ProgramConclusion conclusion =
                    ProgramConclusion.conclusionsFor(lastCurricularPlan).filter(i -> i.isTerminal()).findFirst().get();

            final RegistrationConclusionBean bean = new RegistrationConclusionBean(lastCurricularPlan, conclusion);
            return bean.getConclusionYear();
        } catch (final Throwable t) {
            logger.error("Error trying to determine ConclusionYear: {}", t.getMessage());
        }

        return null;
    }

    static public boolean isFlunkedUsingCurricularYear(final Registration registration, final ExecutionYear executionYear) {

        if (registration.getStartExecutionYear().isAfterOrEquals(executionYear)
                || getStudentCurricularPlan(registration, executionYear) == null) {
            return false;
        }

        final RegistrationDataByExecutionYear previousData = registration.getRegistrationDataByExecutionYearSet().stream()
                .filter(i -> i.getExecutionYear().isBefore(executionYear) && getEnrolmentsCount(i) > 0)
                .max((i, j) -> i.getExecutionYear().compareTo(j.getExecutionYear())).orElse(null);
        if (previousData == null) {
            return false;
        }

        final int currentYear = getCurricularYear(registration, executionYear).getResult();

        final ExecutionYear previousExecutionYear = previousData.getExecutionYear();
        final int previousYear = getCurricularYear(registration, previousExecutionYear).getResult();

        return previousYear == currentYear;
    }

    static private Integer getEnrolmentsCount(final RegistrationDataByExecutionYear data) {
        final StudentCurricularPlan plan = getStudentCurricularPlan(data.getRegistration(), data.getExecutionYear());
        return plan == null ? 0 : Long
                .valueOf(plan.getEnrolmentsByExecutionYear(data.getExecutionYear()).stream().filter(i -> !i.isAnnulled()).count())
                .intValue();
    }

    private static BiFunction<Registration, ExecutionInterval, Collection<SchoolClass>> initialSchoolClassesService =
            defaultInitialSchoolClassesService();

    public static Set<SchoolClass> getSchoolClassesToEnrolBy(final Registration registration,
            final DegreeCurricularPlan degreeCurricularPlan, final ExecutionInterval executionInterval) {

        return registration.getAssociatedAttendsSet().stream()
                .filter(attends -> attends.getExecutionInterval() == executionInterval)
                .flatMap(attends -> attends.getExecutionCourse().getSchoolClassesBy(degreeCurricularPlan).stream())
                .collect(Collectors.toSet());
    }

    public static void registerInitialSchoolClassesService(
            final BiFunction<Registration, ExecutionInterval, Collection<SchoolClass>> service) {
        initialSchoolClassesService = service;
    }

    public static Collection<SchoolClass> getInitialSchoolClassesToEnrolBy(final Registration registration,
            final ExecutionInterval executionInterval) {
        return initialSchoolClassesService.apply(registration, executionInterval);
    }

    private static BiFunction<Registration, ExecutionInterval, Collection<SchoolClass>> defaultInitialSchoolClassesService() {
        return (r, ei) -> {
            final ExecutionDegree executionDegree =
                    r.getActiveDegreeCurricularPlan().getExecutionDegreeByYear(ei.getExecutionYear());
            if (executionDegree != null) {
                int curricularYear = getCurricularYear(r, ei.getExecutionYear()).getResult();
                return executionDegree.getSchoolClassesSet().stream().filter(sc -> sc.getCurricularYear().equals(curricularYear))
                        .collect(Collectors.toSet());
            }
            return Collections.emptyList();
        };
    }

    @Deprecated
    public static void replaceSchoolClass(final Registration registration, final SchoolClass schoolClass,
            final ExecutionInterval executionInterval) {
        SchoolClass.replaceSchoolClass(registration, schoolClass, executionInterval);
    }

    @Deprecated
    public static List<Shift> getAttendingShifts(final SchoolClass schoolClass, final Registration registration) {
        return schoolClass.findShiftsFor(registration).collect(Collectors.toList());
    }

    public static Collection<EnrolmentEvaluation> getImprovementEvaluations(final Registration registration,
            final ExecutionYear executionYear, final Predicate<EnrolmentEvaluation> predicate) {

        final Collection<EnrolmentEvaluation> result = Sets.newHashSet();

        for (final ExecutionInterval executionInterval : executionYear.getExecutionPeriodsSet()) {
            for (final EnrolmentEvaluation evaluation : executionInterval.getEnrolmentEvaluationsSet()) {

                if (evaluation.getEvaluationSeason().isImprovement() && evaluation.getRegistration() == registration
                        && (predicate == null || predicate.test(evaluation))) {
                    result.add(evaluation);
                }
            }
        }

        return result;
    }

    public static boolean hasImprovementEvaluations(final Registration registration, final ExecutionYear executionYear,
            final Predicate<EnrolmentEvaluation> predicate) {

        for (final ExecutionInterval executionInterval : executionYear.getExecutionPeriodsSet()) {
            for (final EnrolmentEvaluation evaluation : executionInterval.getEnrolmentEvaluationsSet()) {

                if (evaluation.getEvaluationSeason().isImprovement() && evaluation.getRegistration() == registration
                        && (predicate == null || predicate.test(evaluation))) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean hasCreditsBetweenPlans(final Registration registration) {
        for (final StudentCurricularPlan scp : registration.getStudentCurricularPlansSet()) {
            for (final Credits credits : scp.getCreditsSet()) {
                for (EnrolmentWrapper wrapper : credits.getEnrolmentsSet()) {
                    if (wrapper.getIEnrolment().isExternalEnrolment()) {
                        continue;
                    }

                    final Enrolment e = (Enrolment) wrapper.getIEnrolment();

                    if (registration.getStudentCurricularPlansSet().contains(e.getStudentCurricularPlan())) {
                        return true;
                    }
                }
            }

        }

        return false;
    }

    public static final boolean canCollectAllPlansForCurriculum(final Registration registration) {
        return registration.getStudentCurricularPlansSet().size() > 1 && !hasCreditsBetweenPlans(registration);
    }

    public static Curriculum getAllPlansCurriculum(final Registration registration, final ExecutionYear executionYear) {
        Curriculum curriculumSum = Curriculum.createEmpty(executionYear);
        for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
            curriculumSum.add(studentCurricularPlan.getRoot().getCurriculum(executionYear));
        }

        return curriculumSum;
    }

    static public Curriculum filterCurricularYearEntriesBySemester(final Curriculum input, final Integer semester) {

        final Curriculum result = Curriculum.createEmpty(input.getCurriculumModule(), input.getExecutionYear());
        result.add(input);

        for (final Iterator<ICurriculumEntry> iterator = result.getCurricularYearEntries().iterator(); iterator.hasNext();) {
            final ICurriculumEntry iter = iterator.next();
            if (semester.intValue() != CurricularPeriodServices.getCurricularSemester((CurriculumLine) iter)) {
                iterator.remove();
            }
        }

        return result;
    }

    public static void setIngressionGradeA(Registration registration, BigDecimal grade) {
        RegistrationExtendedInformation.findOrCreate(registration).setIngressionGradeA(grade);
    }

    public static BigDecimal getIngressionGradeA(Registration registration) {
        return registration.getExtendedInformation() != null ? registration.getExtendedInformation().getIngressionGradeA() : null;
    }

    public static void setIngressionGradeB(Registration registration, BigDecimal grade) {
        RegistrationExtendedInformation.findOrCreate(registration).setIngressionGradeB(grade);
    }

    public static BigDecimal getIngressionGradeB(Registration registration) {
        return registration.getExtendedInformation() != null ? registration.getExtendedInformation().getIngressionGradeB() : null;
    }

    public static void setIngressionGradeC(Registration registration, BigDecimal grade) {
        RegistrationExtendedInformation.findOrCreate(registration).setIngressionGradeC(grade);
    }

    public static BigDecimal getIngressionGradeC(Registration registration) {
        return registration.getExtendedInformation() != null ? registration.getExtendedInformation().getIngressionGradeC() : null;
    }

    public static void setIngressionGradeD(Registration registration, BigDecimal grade) {
        RegistrationExtendedInformation.findOrCreate(registration).setIngressionGradeD(grade);
    }

    public static BigDecimal getIngressionGradeD(Registration registration) {
        return registration.getExtendedInformation() != null ? registration.getExtendedInformation().getIngressionGradeD() : null;
    }

    public static void setInternshipGrade(Registration registration, BigDecimal grade) {
        RegistrationExtendedInformation.findOrCreate(registration).setInternshipGrade(grade);
    }

    public static BigDecimal getInternshipGrade(Registration registration) {
        return registration.getExtendedInformation() != null ? registration.getExtendedInformation().getInternshipGrade() : null;
    }

    public static void setInternshipConclusionDate(Registration registration, LocalDate conclusionDate) {
        RegistrationExtendedInformation.findOrCreate(registration).setInternshipConclusionDate(conclusionDate);
    }

    public static LocalDate getInternshipConclusionDate(Registration registration) {
        return registration.getExtendedInformation() != null ? registration.getExtendedInformation()
                .getInternshipConclusionDate() : null;
    }

    static public void setCurriculumAccumulated(final Registration input, final boolean value) {
        RegistrationExtendedInformation.findOrCreate(input).setCurriculumAccumulated(value);
    }

    static public boolean isCurriculumAccumulated(final Registration input) {
        return input.getExtendedInformation() != null && input.getExtendedInformation().getCurriculumAccumulated();
    }

    public static Collection<ExecutionYear> getEnrolmentYears(Registration registration) {
        //TODO: implement option to check if registration data is valid instead of checking if enrolment date is filled
        return registration.getRegistrationDataByExecutionYearSet().stream().filter(rd -> rd.getEnrolmentDate() != null)
                .map(rd -> rd.getExecutionYear()).collect(Collectors.toSet());
    }

    public static ExecutionYear getLastReingressionYear(final Registration registration) {
        return registration.getReingressions().stream().map(ri -> ri.getExecutionYear())
                .sorted(ExecutionYear.COMPARATOR_BY_BEGIN_DATE.reversed()).findFirst().orElse(null);
    }

    public static ExecutionYear getLastReingressionYearIncludingPrecedentRegistrations(final Registration registration) {
        return Stream.concat(Stream.of(registration), getPrecedentDegreeRegistrations(registration).stream())
                .flatMap(r -> r.getReingressions().stream()).map(ri -> ri.getExecutionYear())
                .sorted(ExecutionYear.COMPARATOR_BY_BEGIN_DATE.reversed()).findFirst().orElse(null);
    }

    public static void setManuallyAssignedNumber(final Registration input, boolean value) {
        RegistrationExtendedInformation.findOrCreate(input).setManuallyAssignedNumber(value);
    }

    public static boolean isManuallyAssignedNumber(final Registration input) {
        return input.getExtendedInformation() != null && input.getExtendedInformation().getManuallyAssignedNumber();
    }

    public static LocalDate getEnrolmentDate(final Registration registration, final ExecutionYear executionYear) {
        final RegistrationDataByExecutionYear dataByExecutionYear =
                RegistrationDataServices.getRegistrationData(registration, executionYear);

        return dataByExecutionYear == null ? null : dataByExecutionYear.getEnrolmentDate();
    }

    public static Collection<ExecutionYear> getEnrolmentYearsIncludingPrecedentRegistrations(final Registration registration) {
        return getEnrolmentYearsIncludingPrecedentRegistrations(registration, null);
    }

    /**
     * 
     * @param untilExecutionYear is inclusive. null does not apply any filtering
     * 
     * @return
     */
    public static Collection<ExecutionYear> getEnrolmentYearsIncludingPrecedentRegistrations(final Registration registration,
            final ExecutionYear untilExecutionYear) {

        final Set<Registration> registrations = Sets.newHashSet();
        registrations.add(registration);
        registrations.addAll(getPrecedentDegreeRegistrations(registration));

        final Set<ExecutionYear> result = Sets.newHashSet();
        for (final Registration it : registrations) {
            result.addAll(RegistrationServices.getEnrolmentYears(it));
        }

        if (untilExecutionYear == null) {
            return result;
        }

        return result.stream().filter(e -> e.isBeforeOrEquals(untilExecutionYear)).collect(Collectors.toSet());
    }

    /**
     * Returns the root registration.
     * 
     * @return This registration if does not have precedent or the oldest precendent registration
     */
    public static Registration getRootRegistration(final Registration registration) {
        final SortedSet<Registration> registrations = Sets.newTreeSet(Registration.COMPARATOR_BY_START_DATE);
        registrations.add(registration);
        registrations.addAll(getPrecedentDegreeRegistrations(registration));

        return registrations.first();
    }

    public static Collection<Registration> getPrecedentDegreeRegistrations(final Registration registration) {

        final Set<Degree> precedentDegreesUntilRoot = getPrecedentDegreesUntilRoot(registration.getDegree());
        final Set<Registration> result = Sets.newHashSet();
        for (final Registration it : registration.getStudent().getRegistrationsSet()) {

            if (registration == it) {
                continue;
            }

            if (it.isConcluded() || it.hasConcluded()) {
                continue;
            }

            if (precedentDegreesUntilRoot.contains(it.getDegree())) {
                result.add(it);
            }
        }

        return result;
    }

    private static Set<Degree> getPrecedentDegreesUntilRoot(final Degree degree) {
        final Set<Degree> result = Sets.newHashSet();
        result.addAll(degree.getPrecedentDegreesSet());

        for (final Degree it : degree.getPrecedentDegreesSet()) {
            result.addAll(getPrecedentDegreesUntilRoot(it));
        }

        return result;
    }

    static public Set<CurriculumLine> getNormalEnroledCurriculumLines(Registration registration, ExecutionYear executionYear,
            boolean applyDismissalFilter, Collection<CreditsReasonType> dismissalTypesToInclude) {
        return getEnroledCurriculumLines(registration, executionYear, l -> CurriculumLineServices.isNormal(l),
                applyDismissalFilter, dismissalTypesToInclude);
    }

    static public Set<CurriculumLine> getEnroledCurriculumLines(Registration registration, ExecutionYear executionYear,
            Predicate<CurriculumLine> lineTypePredicate, boolean filterDismissalByReason,
            Collection<CreditsReasonType> reasonsToInclude) {

        final Predicate<CurriculumLine> isForYear = line -> line.getExecutionYear() == executionYear;

        final Predicate<CurriculumLine> isValid = line -> {

            if (line.isDismissal()) {

                final Dismissal dismissal = (Dismissal) line;

                //dismissals candidate to be considered as enroled (e.g. Erasmus and similar)
                final boolean canBeAccountedAsEnroled = dismissal.getCredits().getIEnrolments().isEmpty()
                        || dismissal.getCredits().getIEnrolments().stream().allMatch(e -> e.getExecutionYear() == executionYear);

                if (!canBeAccountedAsEnroled) {
                    return false;
                }

                return !filterDismissalByReason || (dismissal.getCredits().getReason() != null
                        && reasonsToInclude.contains(dismissal.getCredits().getReason()));

            }

            return !((Enrolment) line).isAnnulled();
        };

        final Set<CurriculumLine> candidateLines = getStudentCurricularPlan(registration, executionYear).getAllCurriculumLines()
                .stream().filter(isForYear.and(lineTypePredicate).and(isValid)).collect(Collectors.toSet());

        final Set<DegreeModule> dismissalDegreeModules =
                candidateLines.stream().filter(line -> line.isDismissal() && line.getDegreeModule() != null)
                        .map(line -> line.getDegreeModule()).collect(Collectors.toSet());

        return candidateLines.stream().filter(
                line -> line.isDismissal() || (line.isEnrolment() && !dismissalDegreeModules.contains(line.getDegreeModule())))
                .collect(Collectors.toSet());
    }

    static public StudentCurricularPlan getStudentCurricularPlan(final Registration registration,
            final ExecutionYear executionYear) {

        if (registration.getStudentCurricularPlansSet().size() == 1) {
            return registration.getLastStudentCurricularPlan();
        }

        final StudentCurricularPlan curricularPlan = registration.getStudentCurricularPlan(executionYear);
        if (curricularPlan != null) {
            return curricularPlan;
        }

        final StudentCurricularPlan firstCurricularPlan = registration.getFirstStudentCurricularPlan();
        return firstCurricularPlan.getStartExecutionYear().isAfterOrEquals(executionYear) ? firstCurricularPlan : null;
    }

}
