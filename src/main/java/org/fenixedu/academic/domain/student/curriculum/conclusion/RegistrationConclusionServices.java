package org.fenixedu.academic.domain.student.curriculum.conclusion;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.curriculum.CurriculumModuleServices;
import org.fenixedu.academic.domain.student.curriculum.ProgramConclusionProcess;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.joda.time.YearMonthDay;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

abstract public class RegistrationConclusionServices {

    public static final Comparator<RegistrationConclusionBean> CONCLUSION_BEAN_COMPARATOR_BY_OLDEST_PROCESSED = (x, y) -> {

        if (x.isConclusionProcessed() && !y.isConclusionProcessed()) {
            return -1;
        }

        if (!x.isConclusionProcessed() && y.isConclusionProcessed()) {
            return 1;
        }

        if (x.isConcluded() && !y.isConcluded()) {
            return -1;
        }

        if (!x.isConcluded() && y.isConcluded()) {
            return 1;
        }

        final Comparator<StudentCurricularPlan> planComparator;
        if (!x.isConcluded() && !y.isConcluded()) {
            planComparator = StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_START_DATE.reversed()
                    .thenComparing(DomainObjectUtil.COMPARATOR_BY_ID.reversed());
        } else {
            planComparator = StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_START_DATE
                    .thenComparing(DomainObjectUtil.COMPARATOR_BY_ID);
        }

        return planComparator.compare(x.getCurriculumGroup().getStudentCurricularPlan(),
                y.getCurriculumGroup().getStudentCurricularPlan());

    };

    /**
     * @deprecated use {@link #getConclusions(Registration)}
     */
    @Deprecated
    public static Set<RegistrationConclusionInformation> inferConclusion(final Registration registration) {
        return getConclusions(registration).values().stream().filter(c -> c.isConcluded())
                .map(c -> new RegistrationConclusionInformation(c)).collect(Collectors.toSet());
    }

    /**
     * Motivation: accumulated Registrations can't calculate conclusion date when starting on a root, since it is never concluded
     *
     * This is only used for a suggested conclusion date, it is not used by the domain
     */
    public static YearMonthDay calculateConclusionDate(final RegistrationConclusionBean input) {
        YearMonthDay result = input.calculateConclusionDate();

        if (result == null && input.getCurriculumGroup().isRoot()) {

            for (final CurriculumGroup group : getCurriculumGroupsForConclusion(input.getCurriculumGroup())) {

                final YearMonthDay calculated = CurriculumModuleServices.calculateLastAcademicActDate(group, true);
                if (calculated != null && (result == null || calculated.isAfter(result))) {
                    result = calculated;
                }
            }
        }

        return result;
    }

    public static Set<CurriculumGroup> getCurriculumGroupsForConclusion(final CurriculumGroup curriculumGroup) {
        final Set<CurriculumGroup> result = Sets.newHashSet(curriculumGroup);

        final StudentCurricularPlan scp = curriculumGroup.getStudentCurricularPlan();
        final Registration registration = scp.getRegistration();
        if (RegistrationServices.isCurriculumAccumulated(registration)) {

            for (final StudentCurricularPlan otherScp : registration.getSortedStudentCurricularPlans()) {
                if (otherScp.getStartDateYearMonthDay().isBefore(scp.getStartDateYearMonthDay())) {

                    for (final CurriculumGroup otherGroup : otherGroups(otherScp, curriculumGroup)) {
                        result.add(otherGroup);
                    }
                }
            }
        }

        return result;
    }

    private static List<CurriculumGroup> otherGroups(final StudentCurricularPlan otherScp, final CurriculumGroup originalGroup) {

        final List<CurriculumGroup> result = Lists.newArrayList();
        result.add(otherScp.getRoot());
        result.addAll(otherScp.getAllCurriculumGroups());

        final Predicate<CurriculumGroup> predicate;
        final ProgramConclusion programConclusion = originalGroup.getDegreeModule().getProgramConclusion();
        if (programConclusion == null) {

            // take into account this special case: we might be dealing with all of curriculum, not a specific program conclusion
            // eg: integrated master in IST
            predicate = otherGroup -> originalGroup.isRoot() && otherGroup.isRoot();

        } else {

            predicate = otherGroup -> otherGroup.getDegreeModule() != null
                    && otherGroup.getDegreeModule().getProgramConclusion() == programConclusion;
        }

        return result.stream().filter(predicate).collect(Collectors.toList());
    }

    public static Collection<ProgramConclusionProcess> getProgramConclusionProcesses(StudentCurricularPlan curricularPlan) {
        return Stream.concat(Stream.of(curricularPlan.getRoot()), curricularPlan.getAllCurriculumGroups().stream())
                .filter(cg -> cg.getConclusionProcess() != null && cg.getConclusionProcess() instanceof ProgramConclusionProcess)
                .map(cg -> (ProgramConclusionProcess) cg.getConclusionProcess()).collect(Collectors.toSet());
    }

    public static boolean hasProcessedProgramConclusionInOtherPlan(StudentCurricularPlan curricularPlan,
            ProgramConclusion programConclusion) {
        return curricularPlan.getRegistration().getStudentCurricularPlansSet().stream()
                .anyMatch(scp -> scp != curricularPlan && getProgramConclusionProcesses(scp).stream()
                        .anyMatch(pc -> pc.getGroup().getDegreeModule().getProgramConclusion() == programConclusion));
    }

    public static boolean canProcessProgramConclusionInPreviousPlans(StudentCurricularPlan curricularPlan,
            ProgramConclusion programConclusion) {

        if (getProgramConclusionProcesses(curricularPlan).stream()
                .anyMatch(pcp -> pcp.getGroup().getDegreeModule().getProgramConclusion() == programConclusion)) {
            return false;
        }

        return curricularPlan.getRegistration().getStudentCurricularPlansSet().stream().filter(
                scp -> scp != curricularPlan && scp.getStartExecutionYear().isBefore(curricularPlan.getStartExecutionYear()))
                .anyMatch(scp -> new RegistrationConclusionBean(scp, programConclusion).isConcluded());

    }

    //TODO: move to registration
    public static Map<ProgramConclusion, RegistrationConclusionBean> getConclusions(Registration registration) {

        final Multimap<ProgramConclusion, RegistrationConclusionBean> conclusions = ArrayListMultimap.create();
        registration.getStudentCurricularPlansSet().forEach(scp -> ProgramConclusion.conclusionsFor(scp)
                .forEach(pc -> conclusions.put(pc, new RegistrationConclusionBean(scp, pc))));

        final Map<ProgramConclusion, RegistrationConclusionBean> result = new HashMap<>();
        conclusions.asMap().entrySet().stream().forEach(entry -> result.put(entry.getKey(), entry.getValue().stream()
                .sorted(RegistrationConclusionServices.CONCLUSION_BEAN_COMPARATOR_BY_OLDEST_PROCESSED).findFirst().get()));

        return result;

    }

}
