package org.fenixedu.academic.domain.student;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fenixedu.academic.FenixEduAcademicExtensionsConfiguration;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.student.curriculum.CreditsReasonType;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.CurriculumConfigurationInitializer;
import org.fenixedu.academic.domain.student.curriculum.CurriculumConfigurationInitializer.CurricularYearResult;
import org.fenixedu.academic.domain.student.curriculum.CurriculumLineServices;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

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

    private static String getCacheKey(final StudentCurricularPlan studentCurricularPlan) {
        return studentCurricularPlan.getExternalId();
    }

    static final private Cache<String, ICurriculum> CACHE_CURRICULUMS =
            CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(10 * 1000).expireAfterWrite(5, TimeUnit.MINUTES).build();

    static final private Cache<String, ICurriculum> CACHE_CURRICULUM_AT_END_OF_YEAR =
            CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(10 * 1000).expireAfterWrite(5, TimeUnit.MINUTES).build();

    static final private Cache<String, ICurriculum> CACHE_CURRICULUM_FULL =
            CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(10 * 1000).expireAfterWrite(5, TimeUnit.MINUTES).build();

    /**
     * Returns the curriculum for a given registration, optionally filtered to a
     * specific execution year.
     * <p>
     * Results are cached for 5 minutes using cache (keyed by registration
     * and execution year IDs). Cache misses delegate to
     * {@link Registration#getCurriculum(ExecutionYear)}. On any exception the
     * error is logged and {@code null} is returned.
     * <p>
     * <b>Null semantics for {@code executionYear}:</b>
     * <ul>
     *   <li><b>{@code null}</b> — returns the curriculum computed from the
     *       registration <em>last</em> student curricular plan, including all
     *       approved enrolments across all years as well as all dismissals
     *       (credits transfer / equivalence).</li>
     *   <li><b>non-null</b> — returns the curriculum as it stood <em>at the
     *       start</em> of the given execution year. Approved enrolments whose
     *       execution year <b>is strictly before</b> the requested year are
     *       included; approved enrolments belonging to the requested year itself or after
     *       are <b>not</b> included. Dismissals (credits transfer / equivalence)
     *       are included if their year is before or equal to the
     *       requested year. For Dismissals in the same year as requested,
     *       they are included if they have no sources at all or if all sources are before the year.</li>
     * </ul>
     * <p>
     * <b>Corner cases:</b>
     * <ul>
     *   <li>If {@code registration} is {@code null}, a
     *       {@link NullPointerException} is thrown (no null-guard).</li>
     *   <li>If the registration has no student curricular plans, or no plan is
     *       found for the given year, an empty curriculum is returned.</li>
     *   <li>If any exception occurs during computation, the error is logged and
     *       {@code null} is returned.</li>
     * </ul>
     *
     * @param registration the student registration (must not be null)
     * @param executionYear the execution year to filter by, or {@code null}
     *                      for the complete curriculum from the last plan
     * @return the {@link ICurriculum} for the given registration and year, or
     *         {@code null} if an error occurs
     */
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

    /**
     * Returns the curriculum as it stood <em>at the end</em> of the given
     * execution year (i.e. including everything approved up to that year).
     * <p>
     * Internally uses {@code executionYear.getNextExecutionYear()} so that the
     * curriculum reflects all approved enrolments and dismissals for the
     * registration up to and including the given year.
     * <p>
     * Results are cached for 5 minutes (keyed by registration and execution
     * year IDs). Cache misses delegate to
     * {@link Registration#getCurriculum(ExecutionYear)} with the next
     * execution year.
     * <p>
     * <b>Null is NOT allowed</b> for {@code executionYear} — an
     * {@link IllegalArgumentException} is thrown if null is passed.
     *
     * @param registration the student registration (must not be null)
     * @param executionYear the execution year (must not be null)
     * @return the {@link ICurriculum} for the given registration at the end
     *         of the given year, or {@code null} if an error occurs
     */
    public static ICurriculum getCurriculumAtEndOfYear(final Registration registration, final ExecutionYear executionYear) {
        if (executionYear == null) {
            throw new IllegalArgumentException("executionYear must not be null");
        }

        final String key = getCacheKey(registration, executionYear);

        try {
            return CACHE_CURRICULUM_AT_END_OF_YEAR.get(key, () -> {
                logger.debug(String.format("Miss on CurriculumAtEndOfYear cache [%s %s]", new DateTime(), key));
                return registration.getStudentCurricularPlan(executionYear)
                        .getCurriculum(new DateTime(), executionYear.getNextExecutionYear());
            });

        } catch (final Throwable t) {
            logger.error(String.format("Unable to get CurriculumAtEndOfYear [%s %s %s]", new DateTime(), key,
                    t.getLocalizedMessage()));
            return null;
        }
    }

    /**
     * Returns the full curriculum for a given student curricular plan (i.e.
     * all approved enrolments and dismissals across all years).
     * <p>
     * Results are cached for 5 minutes (keyed by student curricular plan ID).
     * <p>
     * <b>Null is NOT allowed</b> — an {@link IllegalArgumentException} is
     * thrown if null is passed.
     *
     * @param studentCurricularPlan the student curricular plan (must not be null)
     * @return the full {@link ICurriculum} for the given plan, or {@code null}
     *         if an error occurs
     */
    public static ICurriculum getFullCurriculum(final StudentCurricularPlan studentCurricularPlan) {
        if (studentCurricularPlan == null) {
            throw new IllegalArgumentException("studentCurricularPlan must not be null");
        }

        final String key = getCacheKey(studentCurricularPlan);

        try {
            return CACHE_CURRICULUM_FULL.get(key, () -> {
                logger.debug(String.format("Miss on FullCurriculum cache [%s %s]", new DateTime(), key));
                return studentCurricularPlan.getCurriculum(new DateTime(), (ExecutionYear) null);
            });

        } catch (final Throwable t) {
            logger.error(String.format("Unable to get FullCurriculum [%s %s %s]", new DateTime(), key, t.getLocalizedMessage()));
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
        CACHE_CURRICULUM_AT_END_OF_YEAR.invalidate(getCacheKey(registration, executionYear));
        CACHE_CURRICULUM_FULL.invalidate(getCacheKey(registration.getStudentCurricularPlan(executionYear)));
    }

    static public boolean isFlunkedUsingCurricularYear(final Registration registration, final ExecutionYear executionYear) {

        if (registration.getRegistrationYear().isAfterOrEquals(executionYear)
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
            final ExecutionDegree executionDegree = r.getActiveDegreeCurricularPlan().findExecutionDegree(ei).orElse(null);
            if (executionDegree != null) {
                int curricularYear = getCurricularYear(r, ei.getExecutionYear()).getResult();
                return executionDegree.getSchoolClassesSet().stream().filter(sc -> sc.getCurricularYear().equals(curricularYear))
                        .collect(Collectors.toSet());
            }
            return Collections.emptyList();
        };
    }

    private static boolean hasCreditsAcrossOwnPlans(final Registration registration) {
        return registration.getStudentCurricularPlanStream().flatMap(scp -> scp.getCreditsSet().stream())
                .flatMap(c -> c.getEnrolmentsSet().stream()).filter(ew -> !ew.getIEnrolment().isExternalEnrolment())
                .map(ew -> (Enrolment) ew.getIEnrolment())
                .anyMatch(e -> registration.getStudentCurricularPlansSet().contains(e.getStudentCurricularPlan()));
    }

    public static final boolean canCollectAllPlansForCurriculum(final Registration registration) {
        return registration.getStudentCurricularPlansSet().size() > 1 && !hasCreditsAcrossOwnPlans(registration);
    }

    public static Curriculum getAllPlansCurriculum(final Registration registration, final ExecutionYear executionYear) {
        Curriculum curriculumSum = Curriculum.createEmpty(executionYear);
        for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
            curriculumSum.add(studentCurricularPlan.getRoot().getCurriculum(executionYear));
        }

        return curriculumSum;
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
