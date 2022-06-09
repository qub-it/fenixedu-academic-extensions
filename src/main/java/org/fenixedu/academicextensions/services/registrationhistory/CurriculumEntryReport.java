package org.fenixedu.academicextensions.services.registrationhistory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.CreditsReasonType;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.util.CurricularPeriodLabelFormatter;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;

public class CurriculumEntryReport {

    private ICurriculum curriculum;

    private ICurriculumEntry curriculumEntry;

    public CurriculumEntryReport(final ICurriculum curriculum, final ICurriculumEntry curriculumEntry) {
        this.curriculum = Objects.requireNonNull(curriculum);
        this.curriculumEntry = Objects.requireNonNull(curriculumEntry);
    }

    public ExecutionInterval getExecutionInterval() {
        return curriculumEntry.getExecutionInterval();
    }

    public String getCode() {
        return this.curriculumEntry.getCode();
    }

    public ICurriculumEntry getCurriculumEntry() {
        return this.curriculumEntry;
    }

    public LocalizedString getName() {
        return this.curriculumEntry.getPresentationName();
    }

    public Grade getGrade() {
        return this.curriculumEntry.getGrade();
    }

    public BigDecimal getCredits() {
        return this.curriculumEntry.getEctsCreditsForCurriculum();
    }

    public AcademicPeriod getAcademicPeriod() {
        if (curriculumEntry instanceof CurriculumLine) {
            return Optional.ofNullable(((CurriculumLine) curriculumEntry).getCurricularCourse()).map(c -> c.getCompetenceCourse())
                    .map(cc -> cc.getAcademicPeriod()).orElse(null);
        }

        return null;
    }

    public DateTime getCreationDate() {
        return this.curriculumEntry.getCreationDateDateTime();
    }

    public boolean isDismissal() {
        return ((Curriculum) curriculum).getDismissalRelatedEntries().contains(this.curriculumEntry);
    }

    public Collection<CurricularPeriod> getCurricularPeriods() {
        final Set<CurriculumLine> targetLines =
                curriculumEntry.getCurriculumLinesForCurriculum(curriculum.getStudentCurricularPlan());

        if (!targetLines.isEmpty()) {
            return targetLines.stream().map(l -> CurricularPeriodServices.getCurricularPeriod(l)).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        return curriculumEntry instanceof CurriculumLine ? Stream
                .of(CurricularPeriodServices.getCurricularPeriod((CurriculumLine) curriculumEntry)).filter(Objects::nonNull)
                .collect(Collectors.toSet()) : Collections.emptySet();
    }

    public String getCurricularPeriodsAsString() {
        return getCurricularPeriods().stream().map(cp -> CurricularPeriodLabelFormatter.getFullLabel(cp, true))
                .collect(Collectors.joining("; "));
    }

    public Collection<CurriculumGroup> getCurriculumGroups() {
        final Set<CurriculumLine> targetLines =
                curriculumEntry.getCurriculumLinesForCurriculum(curriculum.getStudentCurricularPlan());

        if (!targetLines.isEmpty()) {
            return targetLines.stream().map(l -> l.getCurriculumGroup()).collect(Collectors.toSet());
        }

        return curriculumEntry instanceof CurriculumLine ? Collections
                .singleton(((CurriculumLine) curriculumEntry).getCurriculumGroup()) : Collections.emptySet();

    }

    public String getCurriculumGroupsAsString() {
        return getCurriculumGroups().stream().map(cg -> cg.getFullPath()).collect(Collectors.joining("; "));
    }

    public Integer getCurriculumTotalApprovals() {
        return this.curriculum.getCurriculumEntries().size();
    }

    public BigDecimal getCurriculumSimpleAverage() {
        final OptionalDouble average = this.curriculum.getCurriculumEntries().stream().filter(e -> e.getGrade().isNumeric())
                .map(e -> e.getGrade().getNumericValue()).mapToDouble(v -> v.doubleValue()).average();

        return average.isPresent() ? BigDecimal.valueOf(average.getAsDouble()) : null;
    }

    public Registration getRegistration() {
        return curriculum.getStudentCurricularPlan().getRegistration();
    }

    public Person getPerson() {
        return getRegistration().getPerson();
    }

    public Degree getDegree() {
        return getRegistration().getDegree();
    }

    public String getEntryType() {
        final Set<CurriculumLine> targetLines =
                curriculumEntry.getCurriculumLinesForCurriculum(curriculum.getStudentCurricularPlan());
        return targetLines.isEmpty() ? AcademicExtensionsUtil.bundle(curriculumEntry.getClass().getName()) : targetLines.stream()
                .filter(l -> l instanceof Dismissal).map(Dismissal.class::cast)
                .map(d -> AcademicExtensionsUtil.bundle(d.getCredits().getClass().getName())).distinct().sorted()
                .collect(Collectors.joining(", "));
    }

    public Collection<CreditsReasonType> getCreditsReasonTypes() {
        return this.curriculumEntry.getCurriculumLinesForCurriculum(curriculum.getStudentCurricularPlan()).stream()
                .filter(l -> l.isDismissal()).map(Dismissal.class::cast).map(d -> d.getCredits().getReason())
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public String getCreditsReasonTypeAsString() {
        return getCreditsReasonTypes().stream().map(r -> r.getReason().getContent()).collect(Collectors.joining("; "));
    }

}
