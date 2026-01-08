package org.fenixedu.academic.domain.enrolment;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcessVersion;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import pt.ist.fenixframework.dml.runtime.RelationAdapter;
import pt.ist.fenixframework.dml.runtime.RelationListener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EnrolmentServices extends org.fenixedu.academic.domain.student.services.EnrolmentServices {

    static private RelationListener<DegreeModule, CurriculumModule> ON_ENROLMENT_DELETION =
            new RelationAdapter<DegreeModule, CurriculumModule>() {

                @Override
                public void beforeRemove(final DegreeModule degreeModule, final CurriculumModule module) {
                    // avoid internal invocation with null 
                    if (module == null || degreeModule == null) {
                        return;
                    }

                    if (!(module instanceof Enrolment)) {
                        return;
                    }

                    final Enrolment enrolment = (Enrolment) module;
                    removeConclusionProcessVersionsExceptLast(enrolment);
                    checkForConclusionProcessVersions(enrolment);
                }
            };

    public static void initialize() {
        CurriculumModule.getRelationDegreeModuleCurriculumModule().addListener(ON_ENROLMENT_DELETION);
    }

    static public String getPresentationName(final Enrolment enrolment) {
        final String code =
                !StringUtils.isEmpty(enrolment.getCurricularCourse().getCode()) ? enrolment.getCurricularCourse().getCode()
                        + " - " : "";

        if (enrolment instanceof OptionalEnrolment) {
            final OptionalEnrolment optionalEnrolment = (OptionalEnrolment) enrolment;
            return optionalEnrolment.getOptionalCurricularCourse().getNameI18N(enrolment.getExecutionPeriod()).getContent() + " ("
                    + code
                    + optionalEnrolment.getCurricularCourse().getNameI18N(optionalEnrolment.getExecutionPeriod()).getContent()
                    + ")";
        } else {
            return code + enrolment.getName().getContent();
        }
    }

    static public void checkForConclusionProcessVersions(final Enrolment enrolment) {
        if (enrolment.isApproved() && !enrolment.getConclusionProcessVersionsSet().isEmpty()) {
            final Registration registration = enrolment.getRegistration();

            throw new DomainException("error.conclusionProcess.revertion.required",
                    "\"" + registration.getNumber() + " - " + registration.getPerson().getPresentationName() + "\"",
                    "\"" + getPresentationName(enrolment) + "\"", enrolment.getConclusionProcessVersionsSet().stream()
                    .map(i -> "\"" + i.getConclusionProcess().getGroup().getDegreeModule().getProgramConclusion().getName()
                            .getContent() + "\"").distinct().collect(Collectors.joining("; ")));
        }
    }

    static private void removeConclusionProcessVersionsExceptLast(final Enrolment enrolment) {
        for (final Iterator<ConclusionProcessVersion> iter = enrolment.getConclusionProcessVersionsSet().iterator(); iter
                .hasNext();) {
            final ConclusionProcessVersion version = iter.next();

            if (version.getConclusionProcess().getLastVersion() != version) {
                iter.remove();
            }
        }
    }

    static public Set<ExecutionCourse> getExecutionCourses(final Enrolment enrolment, final ExecutionInterval interval) {
        final Set<ExecutionCourse> result = new HashSet<>();

        if (enrolment != null) {
            final CurricularCourse curricular = enrolment.getCurricularCourse();

            if (curricular != null) {
                result.addAll(curricular.getCompetenceCourse().getExecutionCoursesByExecutionPeriod(interval));
            }
        }

        return result;
    }

    static public ExecutionCourse getExecutionCourseUnique(final Enrolment enrolment, final ExecutionInterval interval) {
        // first consider all executions
        final Set<ExecutionCourse> all = getExecutionCourses(enrolment, interval);
        if (all.size() == 1) {
            return all.iterator().next();
        }

        // if more than one execution exists, consider the one for the given curricular course
        final CurricularCourse curricular = enrolment.getCurricularCourse();
        final List<ExecutionCourse> filtered = all.stream()
                .filter(ec -> ec.getAssociatedCurricularCoursesSet().contains(curricular)).collect(Collectors.toList());
        if (filtered.size() == 1) {
            return filtered.iterator().next();
        }

        return null;
    }

}
