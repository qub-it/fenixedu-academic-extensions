/**
 * 
 */
package org.fenixedu.academic.domain.curricularRules;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.function.Consumer;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.degreeStructure.CourseLoadType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;

public class StudentScheduleListeners {

    static public Consumer<DomainObjectEvent<Enrolment>> SHIFTS_ENROLLER = new Consumer<DomainObjectEvent<Enrolment>>() {

        @Override
        public void accept(DomainObjectEvent<Enrolment> event) {
            final Enrolment enrolment = event.getInstance();
            final ExecutionInterval executionInterval = enrolment.getExecutionInterval();
            final Attends attends = enrolment.getAttendsFor(executionInterval);

            if (attends == null) {
                return;
            }

            boolean enrolInShiftIfUnique = !enrolment.getCurriculumGroup().isNoCourseGroupCurriculumGroup() && enrolment
                    .getCurricularRules(executionInterval).stream().filter(cr -> cr instanceof StudentSchoolClassCurricularRule)
                    .map(cr -> (StudentSchoolClassCurricularRule) cr).anyMatch(ssccr -> ssccr.getEnrolInShiftIfUnique());
            if (enrolInShiftIfUnique) {

                final Registration registration = enrolment.getRegistration();
                final ExecutionCourse executionCourse = attends.getExecutionCourse();

                final SchoolClass schoolClass = registration.findSchoolClass(executionInterval).orElse(null);

                for (CourseLoadType courseLoadType : executionCourse.getCourseLoadTypes()) {
                    if (registration.findEnrolledShiftFor(executionCourse, courseLoadType).isEmpty()) {

                        final Collection<Shift> shiftsOfType = executionCourse.findShiftsByLoadType(courseLoadType)
                                .filter(shift -> schoolClass == null || shift.getAssociatedClassesSet().contains(schoolClass))
                                .collect(toSet());

                        if (shiftsOfType.size() == 1) {
                            final Shift shift = shiftsOfType.iterator().next();
                            shift.enrol(registration);
                        }
                    }
                }
            }

        }

    };

}
