package org.fenixedu.academic.domain.student.curriculum.conclusion;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.curriculum.ProgramConclusionProcess;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.academic.domain.student.curriculum.CurriculumModuleServices;
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
     * @deprecated use CONCLUSION_BEAN_COMPARATOR_BY_OLDEST_PROCESSED instead
     */
    @Deprecated
    public static final Comparator<RegistrationConclusionBean> CONCLUSION_BEAN_COMPARATOR = (x, y) -> {

        if (x.isConclusionProcessed() && !y.isConclusionProcessed()) {
            return 1;
        }

        if (!x.isConclusionProcessed() && y.isConclusionProcessed()) {
            return -1;
        }

        return StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_START_DATE.compare(x.getStudentCurricularPlan(),
                y.getStudentCurricularPlan());
    };

    //TODO: refactor to allow usage from RegistrationHistoryReportService.addConclusion (allow not concluded to be returned)
    public static Set<RegistrationConclusionInformation> inferConclusion(final Registration registration) {

        final Multimap<ProgramConclusion, RegistrationConclusionBean> conclusions = ArrayListMultimap.create();

        for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
            for (final ProgramConclusion programConclusion : ProgramConclusion.conclusionsFor(studentCurricularPlan)
                    .collect(Collectors.toSet())) {
                final RegistrationConclusionBean conclusionBean =
                        new RegistrationConclusionBean(studentCurricularPlan, programConclusion);
                if (conclusionBean.isConcluded()) {
                    conclusions.put(programConclusion, conclusionBean);
                }
            }
        }

        final Set<RegistrationConclusionInformation> result = Sets.newHashSet();

        for (final ProgramConclusion programConclusion : conclusions.keySet()) {
            final Collection<RegistrationConclusionBean> conclusionsByProgramConclusion = conclusions.get(programConclusion);
            if (conclusionsByProgramConclusion.size() == 1) {
                result.add(new RegistrationConclusionInformation(conclusionsByProgramConclusion.iterator().next()));
            } else {
                result.add(new RegistrationConclusionInformation(conclusionsByProgramConclusion.stream()
                        .sorted(RegistrationConclusionServices.CONCLUSION_BEAN_COMPARATOR.reversed()).findFirst().get()));
            }
        }

        return result;
    }

    /**
     * Motivation: accumulated Registrations can't calculate conclusion date when starting on a root, since it is never concluded
     * 
     * This is only used for a suggested conclusion date, it is not used by the domain
     */
    static public YearMonthDay calculateConclusionDate(final RegistrationConclusionBean input) {
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

    static public Set<CurriculumGroup> getCurriculumGroupsForConclusion(final CurriculumGroup curriculumGroup) {
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

    static private List<CurriculumGroup> otherGroups(final StudentCurricularPlan otherScp, final CurriculumGroup originalGroup) {

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

    static public Collection<ProgramConclusionProcess> getProgramConclusionProcesses(StudentCurricularPlan curricularPlan) {
        return Stream.concat(Stream.of(curricularPlan.getRoot()), curricularPlan.getAllCurriculumGroups().stream())
                .filter(cg -> cg.getConclusionProcess() != null && cg.getConclusionProcess() instanceof ProgramConclusionProcess)
                .map(cg -> (ProgramConclusionProcess) cg.getConclusionProcess()).collect(Collectors.toSet());
    }

    static public boolean hasProcessedProgramConclusionInOtherPlan(StudentCurricularPlan curricularPlan,
            ProgramConclusion programConclusion) {
        return curricularPlan.getRegistration().getStudentCurricularPlansSet().stream()
                .anyMatch(scp -> scp != curricularPlan && getProgramConclusionProcesses(scp).stream()
                        .anyMatch(pc -> pc.getGroup().getDegreeModule().getProgramConclusion() == programConclusion));
    }

    static public boolean canProcessProgramConclusionInPreviousPlans(StudentCurricularPlan curricularPlan,
            ProgramConclusion programConclusion) {

        if (getProgramConclusionProcesses(curricularPlan).stream()
                .anyMatch(pcp -> pcp.getGroup().getDegreeModule().getProgramConclusion() == programConclusion)) {
            return false;
        }

        return curricularPlan.getRegistration().getStudentCurricularPlansSet().stream().filter(
                scp -> scp != curricularPlan && scp.getStartExecutionYear().isBefore(curricularPlan.getStartExecutionYear()))
                .anyMatch(scp -> new RegistrationConclusionBean(scp, programConclusion).isConcluded());

    }

}
