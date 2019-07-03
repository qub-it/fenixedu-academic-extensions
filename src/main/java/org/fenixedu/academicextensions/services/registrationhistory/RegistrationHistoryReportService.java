package org.fenixedu.academicextensions.services.registrationhistory;

import static org.fenixedu.academic.domain.student.RegistrationDataServices.getRegistrationData;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.exceptions.AcademicExtensionsDomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.CurriculumGradeCalculator;
import org.fenixedu.academic.domain.student.curriculum.CurriculumLineServices;
import org.fenixedu.academic.domain.student.curriculum.conclusion.RegistrationConclusionInformation;
import org.fenixedu.academic.domain.student.curriculum.conclusion.RegistrationConclusionServices;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.academic.dto.student.RegistrationStateBean;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.LocalDate;

import com.google.common.collect.ArrayListMultimap;
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

    private Boolean registrationStateSetInExecutionYear;
    private Boolean registrationStateLastInExecutionYear;

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

    public Collection<RegistrationHistoryReport> generateReport() {
        final Set<RegistrationHistoryReport> result = Sets.newHashSet();

        for (final ExecutionYear executionYear : this.enrolmentExecutionYears) {
            result.addAll(process(executionYear, buildSearchUniverse(executionYear)));
        }

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

        return report -> {

            if (programConclusionsToFilter.isEmpty()) {
                return report.getProgramConclusionsToReport().stream().anyMatch(pc -> isValidGraduated(report, pc));
            } else {
                return report.getProgramConclusionsToReport().stream().allMatch(pc -> isValidGraduated(report, pc));
            }

        };
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
                r -> (this.firstTimeOnly && r.isFirstTime()) || (!this.firstTimeOnly && !r.isFirstTime());
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
                        && this.registrationStateTypes.contains(r.getLastRegistrationState().getStateType());
            } else {
                lastStateFilter = r -> checkRegistrationStatesIntersection(r);
            }

            result = result.and(lastStateFilter);

            if (this.registrationStateSetInExecutionYear != null && this.registrationStateSetInExecutionYear) {

                final Predicate<RegistrationHistoryReport> registrationStateFilter =
                        r -> r.getAllLastRegistrationStates().stream().filter(b -> checkRegistrationStateIsInExecutionYear(r, b))
                                .anyMatch(b -> this.registrationStateTypes.contains(b.getStateType()));

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

        if (!this.registrationStateTypes.isEmpty()) {
        }

        return result;
    }

    private boolean checkRegistrationStatesIntersection(RegistrationHistoryReport r) {
        return !Sets
                .intersection(this.registrationStateTypes,
                        r.getAllLastRegistrationStates().stream().map(b -> b.getStateType()).collect(Collectors.toSet()))
                .isEmpty();
    }

    private boolean checkRegistrationStateIsInExecutionYear(final RegistrationHistoryReport r, final RegistrationStateBean b) {
        if (b.getStateType() == RegistrationStateType.CONCLUDED) {
            return RegistrationServices.getConclusionExecutionYear(b.getRegistration()) == r.getExecutionYear();
        }

        final ExecutionInterval executionInterval = b.getExecutionInterval();
        if (executionInterval != null) {
            return executionInterval.convert(ExecutionYear.class) == r.getExecutionYear();
        }
        return ExecutionYear.readByDateTime(b.getStateDate().toLocalDate().toDateTimeAtStartOfDay()) == r.getExecutionYear();
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

        return report.getEnrolmentsIncludingAnnuled().stream()
                .anyMatch(e -> Boolean.TRUE.equals(this.withAnnuledEnrolments) || !e.isAnnulled());
    }

    private boolean hasAllAnnuledEnrolments(final RegistrationHistoryReport report) {
        return !report.getEnrolmentsIncludingAnnuled().isEmpty() && isAllAnnuledEnrolments(report);
    }

    private boolean hasNoEnrolments(final RegistrationHistoryReport report) {
        return report.getEnrolments().isEmpty() || isAllAnnuledEnrolments(report);
    }

    private Collection<RegistrationHistoryReport> process(final ExecutionYear executionYear, final Set<Registration> universe) {

        final Predicate<RegistrationHistoryReport> filterPredicate = filterPredicate();

        final Set<ProgramConclusion> programConclusionsToReport = calculateProgramConclusions().stream()
                .filter(pc -> this.programConclusionsToFilter.isEmpty() || this.programConclusionsToFilter.contains(pc))
                .collect(Collectors.toSet());

        return buildSearchUniverse(executionYear).stream().filter(r -> r.getRegistrationYear().isBeforeOrEquals(executionYear))

                .map(r -> buildReport(r, executionYear, programConclusionsToReport))

                .filter(filterPredicate)

                .collect(Collectors.toSet());
    }

    private boolean isAllAnnuledEnrolments(final RegistrationHistoryReport report) {
        return report.getEnrolments().stream().allMatch(e -> e.isAnnulled());
    }

    private Set<Registration> buildSearchUniverse(final ExecutionYear executionYear) {

        final Set<Registration> result = Sets.newHashSet();

        final Set<Registration> chosen = getRegistrations();
        final Predicate<Registration> studentNumberFilter = r -> chosen.isEmpty() || chosen.contains(r);

        if (this.dismissalsOnly != null && this.dismissalsOnly.booleanValue()) {
            result.addAll(executionYear.getExecutionPeriodsSet().stream().flatMap(ep -> ep.getCreditsSet().stream())
                    .map(c -> c.getStudentCurricularPlan().getRegistration()).filter(studentNumberFilter)
                    .collect(Collectors.toSet()));
        }

        if (this.improvementEnrolmentsOnly != null && this.improvementEnrolmentsOnly.booleanValue()) {
            result.addAll(executionYear.getExecutionPeriodsSet().stream()
                    .flatMap(e -> e.getEnrolmentEvaluationsSet().stream().map(ev -> ev.getRegistration()))
                    .filter(studentNumberFilter).collect(Collectors.toSet()));
        }

        final boolean withEnrolments = this.withEnrolments != null && this.withEnrolments.booleanValue();

        if (this.withEnrolments == null || withEnrolments) {
            Stream<Enrolment> stream =
                    executionYear.getExecutionPeriodsSet().stream().flatMap(semester -> semester.getEnrolmentsSet().stream());

            if (withEnrolments) {
                stream = stream.filter(e -> Boolean.TRUE.equals(this.withAnnuledEnrolments) || !e.isAnnulled());
            }

            if (this.firstTimeOnly != null && this.firstTimeOnly) {
                stream = stream.filter(enrolment -> enrolment.getRegistration().getRegistrationYear() == executionYear);
            }

            // @formatter:off
            result.addAll(stream.map(enrolment -> enrolment.getRegistration())
                        .filter(studentNumberFilter)
                        .filter(reg -> getRegistrationData(reg, executionYear) != null)
                        .collect(Collectors.toSet()));
            // @formatter:on

        } else if (!this.withEnrolments && Boolean.TRUE.equals(this.withAnnuledEnrolments)) {
            Stream<Enrolment> stream =
                    executionYear.getExecutionPeriodsSet().stream().flatMap(semester -> semester.getEnrolmentsSet().stream());

            stream = stream.filter(e -> e.isAnnulled());

            if (this.firstTimeOnly != null && this.firstTimeOnly) {
                stream = stream.filter(enrolment -> enrolment.getRegistration().getRegistrationYear() == executionYear);
            }

            // @formatter:off
            result.addAll(stream.map(enrolment -> enrolment.getRegistration())
                        .filter(studentNumberFilter)
                        .collect(Collectors.toSet()));
            // @formatter:on
        }

        if (this.firstTimeOnly != null && this.firstTimeOnly) {
            result.addAll(executionYear.getStudentsSet().stream().filter(studentNumberFilter).collect(Collectors.toSet()));
        }

        if (this.registrationStateTypes != null && !this.registrationStateTypes.isEmpty()) {

            result.addAll(
                    Bennu.getInstance().getRegistrationsSet().stream().filter(studentNumberFilter).collect(Collectors.toSet()));
        }

        if (result.isEmpty()) {
            throw new AcademicExtensionsDomainException("error.RegistrationHistoryReportService.insufficient.search.parameters");
        }

        return result;
    }

    private boolean isRegistrationStateConcluded(final RegistrationState s) {
        return s.getStateType() == RegistrationStateType.CONCLUDED;
    }

    private boolean isRegistrationStateActive(final RegistrationState s) {
        return s.isActive();
    }

    private boolean isRegistrationConcludedInExecutionYear(final Registration registration, final ExecutionYear executionYear) {
        for (final RegistrationConclusionInformation conclusionInformation : RegistrationConclusionServices
                .inferConclusion(registration)) {
            if (!conclusionInformation.isConcluded()) {
                continue;
            }

            if (conclusionInformation.isScholarPart()) {
                continue;
            }

            if (conclusionInformation.getRegistrationConclusionBean().getConclusionYear() == null
                    && conclusionInformation.getRegistrationConclusionBean().getConclusionDate() == null) {
                continue;
            }

            if (conclusionInformation.getConclusionYear() != executionYear) {
                continue;
            }

            if (this.programConclusionsToFilter == null || this.programConclusionsToFilter.isEmpty()) {
                return true;
            }

            return this.programConclusionsToFilter.contains(conclusionInformation.getProgramConclusion());
        }

        return false;
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
        } ;

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
        } ;

        return gradesSum.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : gradesSum
                .divide(BigDecimal.valueOf(total), MathContext.DECIMAL128).setScale(3, RoundingMode.HALF_UP);
    }

    static protected BigDecimal calculateAverage(Registration registration) {
        final Curriculum curriculum = (Curriculum) RegistrationServices.getCurriculum(registration, null);
        return ((CurriculumGradeCalculator) curriculum.getGradeCalculator()).calculateAverage(curriculum).setScale(5,
                RoundingMode.DOWN);
    }

    //TODO: refactor to use RegistrationConclusionServices.inferConclusion => refactor method to allow return non conclued 
    static protected void addConclusion(final RegistrationHistoryReport report) {

        final Multimap<ProgramConclusion, RegistrationConclusionBean> conclusions = ArrayListMultimap.create();
        for (final StudentCurricularPlan studentCurricularPlan : report.getRegistration().getStudentCurricularPlansSet()) {
            for (final ProgramConclusion programConclusion : ProgramConclusion.conclusionsFor(studentCurricularPlan)
                    .collect(Collectors.toSet())) {
                conclusions.put(programConclusion, new RegistrationConclusionBean(studentCurricularPlan, programConclusion));
            }
        }

        for (final ProgramConclusion iter : report.getProgramConclusionsToReport()) {

            if (!conclusions.containsKey(iter)) {
                report.addEmptyConclusion(iter);

            } else {

                final Collection<RegistrationConclusionBean> conclusionsByProgramConclusion = conclusions.get(iter);
                if (conclusionsByProgramConclusion.size() == 1) {
                    report.addConclusion(iter, conclusionsByProgramConclusion.iterator().next());

                } else {
                    report.addConclusion(iter,
                            conclusionsByProgramConclusion.stream()
                                    .sorted(RegistrationConclusionServices.CONCLUSION_BEAN_COMPARATOR_BY_OLDEST_PROCESSED)
                                    .findFirst().get());
                }
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
