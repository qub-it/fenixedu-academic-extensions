package org.fenixedu.academicextensions.services.registrationhistory;

import static org.fenixedu.academic.domain.student.RegistrationDataServices.getRegistrationData;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.CurriculumLineServices;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.student.curriculum.conclusion.RegistrationConclusionServices;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.joda.time.LocalDate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class RegistrationHistoryReportService {

    private Set<ExecutionYear> enrolmentExecutionYears = Sets.newHashSet();
    private Set<DegreeType> degreeTypes = Sets.newHashSet();
    private Set<Degree> degrees = Sets.newHashSet();
    private Set<RegistrationRegimeType> regimeTypes = Sets.newHashSet();
    private Set<RegistrationProtocol> registrationProtocols = Sets.newHashSet();
    private Set<IngressionType> ingressionTypes = Sets.newHashSet();
    private Set<RegistrationStateType> registrationStateTypes = Sets.newHashSet();
    private Set<StatuteType> statuteTypes = Sets.newHashSet();
    private Boolean firstTimeOnly;
    private Boolean withEnrolments;
    private Boolean withAnnuledEnrolments;
    private Boolean dismissalsOnly;
    private Boolean improvementEnrolmentsOnly;
    private Integer studentNumber;
    private Collection<ProgramConclusion> programConclusionsToFilter = Sets.newHashSet();
    private Set<ExecutionYear> graduatedExecutionYears = Sets.newHashSet();
    private LocalDate graduationPeriodStartDate;
    private LocalDate graduationPeriodEndDate;
    private String programConclusionNumber;

    private Boolean registrationStateSetInExecutionYear;
    private Boolean registrationStateLastInExecutionYear;

    private Collection<CompetenceCourse> competenceCourses = Sets.newHashSet();

    private Multimap<ExecutionYear, Registration> registrationsWithEnrolmentsCache = HashMultimap.create();
    private Multimap<ExecutionYear, Registration> registrationsWithAnnulledOnlyEnrolmentsCache = HashMultimap.create();
    private Multimap<ExecutionYear, Registration> registrationsWithActiveEnrolmentsCache = HashMultimap.create();

    private List<Integer> getStudentNumbers() {
        final List<Integer> result = Lists.newArrayList();

        if (this.studentNumber != null) {
            result.add(this.studentNumber);
        }

        return result;
    }

    private Set<Registration> getRegistrations() {
        final Set<Registration> result = Sets.newHashSet();

        for (final Integer number : getStudentNumbers()) {

            result.addAll(Registration.readByNumber(number));
            if (result.isEmpty()) {

                final Student student = Student.readStudentByNumber(number);
                if (student != null) {

                    result.addAll(student.getRegistrationsSet());
                }
            }
        }

        return result;
    }

    public void filterEnrolmentExecutionYears(Collection<ExecutionYear> executionYears) {
        this.enrolmentExecutionYears.addAll(executionYears);
    }

    public void filterGraduatedExecutionYears(Collection<ExecutionYear> executionYears) {
        this.graduatedExecutionYears.addAll(executionYears);
    }

    public void filterDegreeTypes(Collection<DegreeType> degreeTypes) {
        this.degreeTypes.addAll(degreeTypes);
    }

    public void filterDegrees(Collection<Degree> degrees) {
        this.degrees.addAll(degrees);
    }

    public void filterRegimeTypes(Collection<RegistrationRegimeType> regimeTypes) {
        this.regimeTypes.addAll(regimeTypes);
    }

    public void filterRegistrationProtocols(Collection<RegistrationProtocol> protocols) {
        this.registrationProtocols.addAll(protocols);
    }

    public void filterIngressionTypes(Collection<IngressionType> ingressionTypes) {
        this.ingressionTypes.addAll(ingressionTypes);
    }

    public void filterRegistrationStateTypes(Collection<RegistrationStateType> registrationStateTypes) {
        this.registrationStateTypes.addAll(registrationStateTypes);
    }

    public void filterStatuteTypes(Collection<StatuteType> statuteTypes) {
        this.statuteTypes.addAll(statuteTypes);
    }

    public void filterFirstTimeOnly(Boolean firstTime) {
        this.firstTimeOnly = firstTime;
    }

    public void filterWithEnrolments(final Boolean input) {
        this.withEnrolments = input;
    }

    public void filterWithAnnuledEnrolments(final Boolean input) {
        this.withAnnuledEnrolments = input;
    }

    public void filterDismissalsOnly(Boolean dismissalsOnly) {
        this.dismissalsOnly = dismissalsOnly;
    }

    public void filterImprovementEnrolmentsOnly(Boolean improvementsEnrolmentsOnly) {
        this.improvementEnrolmentsOnly = improvementsEnrolmentsOnly;
    }

    public void filterStudentNumber(Integer studentNumber) {
        this.studentNumber = studentNumber;
    }

    public void filterGraduationPeriodStartDate(final LocalDate startDate) {
        this.graduationPeriodStartDate = startDate;
    }

    public void filterGraduationPeriodEndDate(final LocalDate endDate) {
        this.graduationPeriodEndDate = endDate;
    }

    public void filterProgramConclusions(Set<ProgramConclusion> programConclusions) {
        this.programConclusionsToFilter.addAll(programConclusions);
    }

    public void filterRegistrationStateSetInExecutionYear(final Boolean input) {
        this.registrationStateSetInExecutionYear = input;
    }

    public void filterRegistrationStateLastInExecutionYear(final Boolean input) {
        this.registrationStateLastInExecutionYear = input;
    }

    public void filterCompetenceCourses(final Collection<CompetenceCourse> competenceCourses) {
        this.competenceCourses.addAll(competenceCourses);
    }

    public void filterProgramConclusionNumber(final String number) {
        this.programConclusionNumber = number;
    }

    public Collection<RegistrationHistoryReport> generateReport() {
        try {
            prepareCaches();

            final Set<RegistrationHistoryReport> result = Sets.newHashSet();

            for (final ExecutionYear executionYear : this.enrolmentExecutionYears) {
                result.addAll(process(executionYear, buildSearchUniverse(executionYear)));
            }

            return result;

        } finally {
            clearCaches();
        }
    }

    private void prepareCaches() {
        this.enrolmentExecutionYears.forEach(ey -> {
            final Set<Registration> withEnrolments = ey.getExecutionPeriodsSet().stream()
                    .flatMap(ei -> ei.getEnrolmentsSet().stream()).map(e -> e.getRegistration()).collect(Collectors.toSet());
            this.registrationsWithEnrolmentsCache.putAll(ey, withEnrolments);

            final Set<Registration> withActiveEnrolments =
                    ey.getExecutionPeriodsSet().stream().flatMap(ei -> ei.getEnrolmentsSet().stream())
                            .filter(e -> !e.isAnnulled()).map(e -> e.getRegistration()).collect(Collectors.toSet());
            this.registrationsWithActiveEnrolmentsCache.putAll(ey, withActiveEnrolments);

            final Set<Registration> withAnnuledEnrolmentsOnly = new HashSet<>(withEnrolments);
            withAnnuledEnrolmentsOnly.removeAll(withActiveEnrolments);
            this.registrationsWithAnnulledOnlyEnrolmentsCache.putAll(ey, withAnnuledEnrolmentsOnly);

        });
    }

    private void clearCaches() {
        this.registrationsWithEnrolmentsCache.clear();
        this.registrationsWithActiveEnrolmentsCache.clear();
        this.registrationsWithAnnulledOnlyEnrolmentsCache.clear();
    }

    public Collection<EnrolmentReport> generateEnrolmentsReport() {
        final Predicate<Enrolment> competenceCourseFilter = e -> this.competenceCourses.isEmpty()
                || this.competenceCourses.contains(e.getCurricularCourse().getCompetenceCourse());

        final Set<EnrolmentReport> result = Sets.newHashSet();
        final Collection<RegistrationHistoryReport> historyReports = generateReport();
        result.addAll(historyReports.stream().flatMap(r -> r.getEnrolments().stream().filter(competenceCourseFilter))
                .map(e -> new EnrolmentReport(e)).collect(Collectors.toSet()));
        result.addAll(historyReports.stream()
                .flatMap(r -> r.getImprovementEvaluations().stream()
                        .filter(ev -> ev.getExecutionInterval() != ev.getEnrolment().getExecutionInterval()
                                && competenceCourseFilter.test(ev.getEnrolment())))
                .map(ev -> new EnrolmentReport(ev.getEnrolment(), ev.getExecutionInterval())).collect(Collectors.toSet()));

        return result;
    }

    public Collection<EnrolmentEvaluationReport> generateEvaluationsReport() {
        return generateEnrolmentsReport().stream().flatMap(r -> r.getEnrolment().getEvaluationsSet().stream())
                .filter(ev -> this.enrolmentExecutionYears.contains(ev.getExecutionInterval().getExecutionYear()))
                .map(ev -> new EnrolmentEvaluationReport(ev)).collect(Collectors.toSet());
    }

    public Collection<CurriculumEntryReport> generateApprovalsReport() {
        final Collection<ICurriculum> curriculums =
                generateReport().stream().map(r -> RegistrationServices.getCurriculum(r.getRegistration(), (ExecutionYear) null))
                        .distinct().collect(Collectors.toList());

        final Collection<CurriculumEntryReport> result = Sets.newHashSet();
        curriculums.stream().forEach(c -> result.addAll(
                c.getCurriculumEntries().stream().map(ce -> new CurriculumEntryReport(c, ce)).collect(Collectors.toSet())));

        return result;
    }

    private Set<ProgramConclusion> calculateProgramConclusions() {
        final Set<ProgramConclusion> result = Sets.newHashSet();

        final Set<DegreeType> degreeTypesToProcess =
                this.degreeTypes.isEmpty() ? DegreeType.all().collect(Collectors.toSet()) : this.degreeTypes;

        for (final DegreeType degreeType : degreeTypesToProcess) {
            for (final Degree degree : degreeType.getDegreeSet()) {
                for (final DegreeCurricularPlan degreeCurricularPlan : degree.getDegreeCurricularPlansSet()) {
                    result.addAll(ProgramConclusion.conclusionsFor(degreeCurricularPlan).collect(Collectors.toSet()));
                }
            }
        }

        return result;
    }

    private Predicate<RegistrationHistoryReport> filterGraduated() {
        return report -> report.getProgramConclusionsToReport().stream().anyMatch(pc -> isValidGraduated(report, pc));
    }

    private boolean isValidGraduated(RegistrationHistoryReport report, ProgramConclusion programConclusion) {

        final RegistrationConclusionBean conclusionBean = report.getConclusionReportFor(programConclusion);

        if (conclusionBean == null) {
            return false;
        }

        if (!conclusionBean.isConcluded()) {
            return false;
        }

        final ExecutionYear conclusionYear = conclusionBean.getConclusionYear();
        final LocalDate conclusionDate = conclusionBean.getConclusionDate().toLocalDate();

        if (!this.graduatedExecutionYears.contains(conclusionYear)) {
            return false;
        }

        if (graduationPeriodStartDate != null && conclusionDate.isBefore(graduationPeriodStartDate)) {
            return false;
        }

        if (graduationPeriodEndDate != null && conclusionDate.isAfter(graduationPeriodEndDate)) {
            return false;
        }

        String number = conclusionBean.getConclusionNumber();
        if (StringUtils.isNotBlank(programConclusionNumber) && !Objects.equals(programConclusionNumber, number)) {
            return false;
        }

        return true;
    }

    private Predicate<RegistrationHistoryReport> filterPredicate() {
        Predicate<RegistrationHistoryReport> result = r -> true;

        final Predicate<RegistrationHistoryReport> protocolFilter =
                r -> this.registrationProtocols.contains(r.getRegistrationProtocol());
        if (!this.registrationProtocols.isEmpty()) {
            result = result.and(protocolFilter);
        }

        final Predicate<RegistrationHistoryReport> ingressionTypeFilter =
                r -> this.ingressionTypes.contains(r.getIngressionType());
        if (!this.ingressionTypes.isEmpty()) {
            result = result.and(ingressionTypeFilter);
        }

        final Predicate<RegistrationHistoryReport> regimeTypeFilter = r -> this.regimeTypes.contains(r.getRegimeType());
        if (!this.regimeTypes.isEmpty()) {
            result = result.and(regimeTypeFilter);
        }

        final Predicate<RegistrationHistoryReport> firstTimeFilter =
                r -> this.firstTimeOnly && r.isFirstTime() || !this.firstTimeOnly && !r.isFirstTime();
        if (this.firstTimeOnly != null) {
            result = result.and(firstTimeFilter);
        }

        final Predicate<RegistrationHistoryReport> degreeTypeFilter = r -> this.degreeTypes.contains(r.getDegreeType());
        if (!this.degreeTypes.isEmpty()) {
            result = result.and(degreeTypeFilter);
        }

        final Predicate<RegistrationHistoryReport> degreeFilter = r -> this.degrees.contains(r.getDegree());
        if (!this.degrees.isEmpty()) {
            result = result.and(degreeFilter);
        }

        final Predicate<RegistrationHistoryReport> statuteTypeFilter =
                r -> r.getStudentStatutes().stream().anyMatch(s -> this.statuteTypes.contains(s.getType()));
        if (!this.statuteTypes.isEmpty()) {
            result = result.and(statuteTypeFilter);
        }

        if (!this.registrationStateTypes.isEmpty()) {
            Predicate<RegistrationHistoryReport> lastStateFilter = null;

            if (Boolean.TRUE.equals(this.registrationStateLastInExecutionYear)) {
                lastStateFilter = r -> r.getLastRegistrationState() != null
                        && this.registrationStateTypes.contains(r.getLastRegistrationState().getType());
            } else {
                lastStateFilter = r -> checkRegistrationStatesIntersection(r);
            }

            result = result.and(lastStateFilter);

            if (this.registrationStateSetInExecutionYear != null && this.registrationStateSetInExecutionYear) {

                final Predicate<RegistrationHistoryReport> registrationStateFilter = r -> r.getAllLastRegistrationStates()
                        .stream().filter(state -> state.getExecutionInterval().getExecutionYear() == r.getExecutionYear())
                        .anyMatch(st -> this.registrationStateTypes.contains(st.getType()));

                result = result.and(registrationStateFilter);
            }
        }

        final Predicate<RegistrationHistoryReport> graduatedFilter = filterGraduated();
        if (!this.graduatedExecutionYears.isEmpty()) {
            result = result.and(graduatedFilter);
        }

        if (this.withEnrolments != null) {
            if (this.withEnrolments) {
                final Predicate<RegistrationHistoryReport> withEnrolmentsFilter = r -> hasActiveEnrolments(r);
                result = result.and(withEnrolmentsFilter);
            } else {
                final Predicate<RegistrationHistoryReport> noEnrolmentsFilter =
                        r -> Boolean.TRUE.equals(this.withAnnuledEnrolments) ? hasAllAnnuledEnrolments(r) : hasNoEnrolments(r);
                result = result.and(noEnrolmentsFilter);
            }
        }

        return result;
    }

    private boolean checkRegistrationStatesIntersection(RegistrationHistoryReport r) {
        return !Sets.intersection(this.registrationStateTypes, r.getAllLastRegistrationStates().stream().map(b -> b.getType())
                .filter(Objects::nonNull).collect(Collectors.toSet())).isEmpty();
    }

    private boolean hasActiveEnrolments(final RegistrationHistoryReport report) {
        final ExecutionYear executionYear = report.getExecutionYear();
        final Registration registration = report.getRegistration();

        if (this.dismissalsOnly != null && this.dismissalsOnly.booleanValue()) {
            boolean hasDismissal = executionYear.getExecutionPeriodsSet().stream().flatMap(ep -> ep.getCreditsSet().stream())
                    .map(c -> c.getStudentCurricularPlan().getRegistration()).anyMatch(r -> r == registration);

            if (hasDismissal) {
                return true;
            }
        }

        if (this.improvementEnrolmentsOnly != null && this.improvementEnrolmentsOnly.booleanValue()) {
            boolean hasImprovement = executionYear.getExecutionPeriodsSet().stream()
                    .flatMap(e -> e.getEnrolmentEvaluationsSet().stream().map(ev -> ev.getRegistration()))
                    .anyMatch(r -> r == registration);

            if (hasImprovement) {
                return true;
            }
        }

        return Boolean.TRUE.equals(this.withAnnuledEnrolments) ? registrationsWithEnrolmentsCache.containsEntry(executionYear,
                registration) : registrationsWithActiveEnrolmentsCache.containsEntry(executionYear, registration);

    }

    private boolean hasAllAnnuledEnrolments(final RegistrationHistoryReport report) {
        return registrationsWithAnnulledOnlyEnrolmentsCache.containsEntry(report.getExecutionYear(), report.getRegistration());
    }

    private boolean hasNoEnrolments(final RegistrationHistoryReport report) {
        return !registrationsWithActiveEnrolmentsCache.containsEntry(report.getExecutionYear(), report.getRegistration());
    }

    private Collection<RegistrationHistoryReport> process(final ExecutionYear executionYear, final Set<Registration> universe) {

        final Predicate<RegistrationHistoryReport> filterPredicate = filterPredicate();

        final Set<ProgramConclusion> programConclusionsToReport = calculateProgramConclusions().stream()
                .filter(pc -> this.programConclusionsToFilter.isEmpty() || this.programConclusionsToFilter.contains(pc))
                .collect(Collectors.toSet());

        return universe.stream().filter(r -> r.getRegistrationYear().isBeforeOrEquals(executionYear))

                .map(r -> buildReport(r, executionYear, programConclusionsToReport))

                .filter(filterPredicate)

                .collect(Collectors.toSet());
    }

    private Set<Registration> buildSearchUniverse(final ExecutionYear executionYear) {

        final Set<Registration> result = Sets.newHashSet();

        final Set<Registration> chosen = getRegistrations();
        final Predicate<Registration> studentNumberFilter = r -> chosen.isEmpty() || chosen.contains(r);
        final Predicate<Registration> registrationCompetenceCourseFilter =
                this.competenceCourses.isEmpty() ? r -> true : r -> r.getEnrolments(executionYear).stream()
                        .anyMatch(e -> this.competenceCourses.contains(e.getCurricularCourse().getCompetenceCourse()));
        final Predicate<Enrolment> enrolmentCompetenceCourseFilter = this.competenceCourses
                .isEmpty() ? e -> true : e -> this.competenceCourses.contains(e.getCurricularCourse().getCompetenceCourse());

        if (this.dismissalsOnly != null && this.dismissalsOnly.booleanValue()) {
            result.addAll(executionYear.getExecutionPeriodsSet().stream().flatMap(ep -> ep.getCreditsSet().stream())
                    .map(c -> c.getStudentCurricularPlan().getRegistration())
                    .filter(studentNumberFilter.and(registrationCompetenceCourseFilter)).collect(Collectors.toSet()));
        }

        if (this.improvementEnrolmentsOnly != null && this.improvementEnrolmentsOnly.booleanValue()) {
            result.addAll(executionYear.getExecutionPeriodsSet().stream()
                    .flatMap(e -> e.getEnrolmentEvaluationsSet().stream().map(ev -> ev.getRegistration()))
                    .filter(studentNumberFilter.and(registrationCompetenceCourseFilter)).collect(Collectors.toSet()));
        }

        final boolean withEnrolments = this.withEnrolments != null && this.withEnrolments.booleanValue();

        if (this.withEnrolments == null || withEnrolments) {
            Stream<Enrolment> stream = executionYear.getExecutionPeriodsSet().stream()
                    .flatMap(semester -> semester.getEnrolmentsSet().stream()).filter(enrolmentCompetenceCourseFilter);

            if (withEnrolments) {
                stream = stream.filter(e -> Boolean.TRUE.equals(this.withAnnuledEnrolments) || !e.isAnnulled());
            }

            if (this.firstTimeOnly != null && this.firstTimeOnly) {
                stream = stream.filter(enrolment -> enrolment.getRegistration().getRegistrationYear() == executionYear);
            }

            // @formatter:off
            result.addAll(stream.map(enrolment -> enrolment.getRegistration()).filter(studentNumberFilter)
                    .filter(reg -> getRegistrationData(reg, executionYear) != null).collect(Collectors.toSet()));
            // @formatter:on

        } else if (!this.withEnrolments && Boolean.TRUE.equals(this.withAnnuledEnrolments)) {
            Stream<Enrolment> stream = executionYear.getExecutionPeriodsSet().stream()
                    .flatMap(semester -> semester.getEnrolmentsSet().stream()).filter(enrolmentCompetenceCourseFilter);

            stream = stream.filter(e -> e.isAnnulled());

            if (this.firstTimeOnly != null && this.firstTimeOnly) {
                stream = stream.filter(enrolment -> enrolment.getRegistration().getRegistrationYear() == executionYear);
            }

            // @formatter:off
            result.addAll(
                    stream.map(enrolment -> enrolment.getRegistration()).filter(studentNumberFilter).collect(Collectors.toSet()));
            // @formatter:on
        }

        if (this.firstTimeOnly != null && this.firstTimeOnly) {
            result.addAll(executionYear.getStudentsSet().stream()
                    .filter(studentNumberFilter.and(registrationCompetenceCourseFilter)).collect(Collectors.toSet()));
        }

        if (this.registrationStateTypes != null && !this.registrationStateTypes.isEmpty()) {
            result.addAll(this.registrationStateTypes.stream().flatMap(st -> st.getRegistrationStatesSet().stream())
                    .map(s -> s.getRegistration()).distinct().filter(studentNumberFilter.and(registrationCompetenceCourseFilter))
                    .collect(Collectors.toSet()));
        }

        return result;
    }

    private RegistrationHistoryReport buildReport(final Registration registration, final ExecutionYear executionYear,
            final Set<ProgramConclusion> programConclusionsToReport) {

        final RegistrationHistoryReport result = new RegistrationHistoryReport(registration, executionYear);
        result.setProgramConclusionsToReport(programConclusionsToReport);
        return result;
    }

    static protected void addEnrolmentsAndCreditsCount(final RegistrationHistoryReport report) {
        final Collection<Enrolment> enrolmentsByYear = report.getEnrolments();

        final Predicate<Enrolment> normalFilter = normalEnrolmentFilter(report);
        final Predicate<Enrolment> extraCurricularFilter = extraCurricularEnrolmentFilter();
        final Predicate<Enrolment> standaloneFilter = standaloneEnrolmentFilter();
        final Predicate<Enrolment> affinityFilter = affinityEnrolmentFilter();

        report.setEnrolmentsCount(countFiltered(enrolmentsByYear, normalFilter));
        report.setEnrolmentsCredits(sumCredits(enrolmentsByYear, normalFilter));

        report.setExtraCurricularEnrolmentsCount(countFiltered(enrolmentsByYear, extraCurricularFilter));
        report.setExtraCurricularEnrolmentsCredits(sumCredits(enrolmentsByYear, extraCurricularFilter));

        report.setStandaloneEnrolmentsCount(countFiltered(enrolmentsByYear, standaloneFilter));
        report.setStandaloneEnrolmentsCredits(sumCredits(enrolmentsByYear, standaloneFilter));

        report.setAffinityEnrolmentsCount(countFiltered(enrolmentsByYear, affinityFilter));
        report.setAffinityEnrolmentsCredits(sumCredits(enrolmentsByYear, affinityFilter));
    }

    static private Predicate<Enrolment> standaloneEnrolmentFilter() {
        return e -> e.isStandalone();
    }

    static private Predicate<Enrolment> extraCurricularEnrolmentFilter() {
        return e -> e.isExtraCurricular();
    }

    static private Predicate<Enrolment> normalEnrolmentFilter(RegistrationHistoryReport result) {
        return e -> CurriculumLineServices.isNormal(e);
    }

    static private Predicate<Enrolment> affinityEnrolmentFilter() {
        return e -> CurriculumLineServices.isAffinity(e);
    }

    static private int countFiltered(Collection<Enrolment> enrolments, Predicate<Enrolment> filter) {
        return (int) enrolments.stream().filter(filter.and(e -> !e.isAnnulled())).count();
    }

    static private BigDecimal sumCredits(Collection<Enrolment> enrolments, Predicate<Enrolment> filter) {
        return enrolments.stream().filter(filter.and(e -> !e.isAnnulled())).map(e -> e.getEctsCreditsForCurriculum())
                .reduce((x, y) -> x.add(y)).orElse(BigDecimal.ZERO);
    }

    static protected BigDecimal calculateExecutionYearWeightedAverage(final RegistrationHistoryReport report) {
        final Collection<Enrolment> enrolmentsByYear = report.getEnrolments();

        BigDecimal gradesSum = BigDecimal.ZERO;
        BigDecimal creditsSum = BigDecimal.ZERO;
        for (final Enrolment enrolment : enrolmentsByYear.stream().filter(normalEnrolmentFilter(report))
                .filter(e -> e.isApproved() && e.getGrade().isNumeric()).collect(Collectors.toSet())) {
            gradesSum = gradesSum.add(enrolment.getGrade().getNumericValue().multiply(enrolment.getEctsCreditsForCurriculum()));
            creditsSum = creditsSum.add(enrolment.getEctsCreditsForCurriculum());
        }

        return gradesSum.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : gradesSum.divide(creditsSum, MathContext.DECIMAL128)
                .setScale(3, RoundingMode.HALF_UP);
    }

    static protected BigDecimal calculateExecutionYearSimpleAverage(final RegistrationHistoryReport report) {
        final Collection<Enrolment> enrolmentsByYear = report.getEnrolments();

        BigDecimal gradesSum = BigDecimal.ZERO;
        int total = 0;
        for (final Enrolment enrolment : enrolmentsByYear.stream().filter(normalEnrolmentFilter(report))
                .filter(e -> e.isApproved() && e.getGrade().isNumeric()).collect(Collectors.toSet())) {
            gradesSum = gradesSum.add(enrolment.getGrade().getNumericValue());
            total++;
        }

        return gradesSum.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : gradesSum
                .divide(BigDecimal.valueOf(total), MathContext.DECIMAL128).setScale(3, RoundingMode.HALF_UP);
    }

    static protected BigDecimal calculateAverage(Registration registration) {
        final Curriculum curriculum = (Curriculum) RegistrationServices.getCurriculum(registration, null);
        final Grade grade = curriculum.getUnroundedGrade();

        return grade.isNumeric() ? grade.getNumericValue().setScale(5, RoundingMode.DOWN) : BigDecimal.ZERO;
    }

    static protected void addConclusion(final RegistrationHistoryReport report) {
        final Map<ProgramConclusion, RegistrationConclusionBean> conclusions =
                RegistrationConclusionServices.getConclusions(report.getRegistration());
        for (final ProgramConclusion iter : report.getProgramConclusionsToReport()) {
            if (!conclusions.containsKey(iter)) {
                report.addEmptyConclusion(iter);
            } else {
                report.addConclusion(iter, conclusions.get(iter));
            }
        }
    }

    static protected void addExecutionYearMandatoryCoursesData(final RegistrationHistoryReport report) {
        final Collection<Enrolment> enrolments = report.getEnrolments();
        if (!enrolments.isEmpty()) {

            final Integer registrationYear = report.getCurricularYear();
            if (registrationYear != null) {

                boolean enroledMandatoryFlunked = false;
                boolean enroledMandatoryInAdvance = false;
                BigDecimal creditsMandatoryEnroled = BigDecimal.ZERO;
                BigDecimal creditsMandatoryApproved = BigDecimal.ZERO;

                for (final Enrolment iter : enrolments) {

                    if (!CurriculumLineServices.isNormal(iter)) {
                        continue;
                    }

                    final boolean isOptionalByGroup = CurriculumLineServices.isOptionalByGroup(iter);
                    if (isOptionalByGroup) {
                        continue;
                    }

                    final int enrolmentYear = CurricularPeriodServices.getCurricularYear(iter);
                    if (enrolmentYear < registrationYear) {
                        enroledMandatoryFlunked = true;

                    } else if (enrolmentYear > registrationYear) {
                        enroledMandatoryInAdvance = true;

                    } else {

                        final BigDecimal ects = iter.getEctsCreditsForCurriculum();
                        creditsMandatoryEnroled = creditsMandatoryEnroled.add(ects);

                        if (iter.isApproved()) {
                            creditsMandatoryApproved = creditsMandatoryApproved.add(ects);
                        }
                    }
                }

                report.setExecutionYearEnroledMandatoryFlunked(enroledMandatoryFlunked);
                report.setExecutionYearEnroledMandatoryInAdvance(enroledMandatoryInAdvance);
                report.setExecutionYearCreditsMandatoryEnroled(creditsMandatoryEnroled);
                report.setExecutionYearCreditsMandatoryApproved(creditsMandatoryApproved);
            }
        }
    }

}
