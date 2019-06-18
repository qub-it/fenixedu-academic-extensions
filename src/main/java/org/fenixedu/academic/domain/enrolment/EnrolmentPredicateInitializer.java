/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 *
 * 
 * This file is part of FenixEdu fenixedu-ulisboa-specifications.
 *
 * FenixEdu Specifications is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Specifications is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Specifications.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.domain.enrolment;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Supplier;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.Enrolment.EnrolmentPredicate;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.curriculum.EnrolmentEvaluationContext;
import org.fenixedu.academic.domain.evaluation.season.EvaluationSeasonServices;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.security.Authenticate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class EnrolmentPredicateInitializer {

    static private final Logger logger = LoggerFactory.getLogger(EnrolmentPredicateInitializer.class);

    protected static Collection<EnrolmentPredicate> EXTRA_NORMAL_PREDICATES = new HashSet<>();

    static public void addExtraNormalPredicate(EnrolmentPredicate predicate) {
        EXTRA_NORMAL_PREDICATES.add(predicate);
    }

    protected static Collection<EnrolmentPredicate> EXTRA_IMPROVEMENT_PREDICATES = new HashSet<>();

    static public void addExtraImprovementPredicate(EnrolmentPredicate predicate) {
        EXTRA_IMPROVEMENT_PREDICATES.add(predicate);
    }

    private static Collection<EnrolmentPredicate> EXTRA_SPECIAL_PREDICATES = new HashSet<>();

    static public void addExtraSpecialPredicate(EnrolmentPredicate predicate) {
        EXTRA_SPECIAL_PREDICATES.add(predicate);
    }

    static public void init() {
        Enrolment.setPredicateSeason(PREDICATE_SEASON);
        Enrolment.setPredicateImprovement(PREDICATE_IMPROVEMENT);
        Enrolment.setPredicateSpecialSeason(PREDICATE_SPECIAL_SEASON);
    }

    static private Supplier<EnrolmentPredicate> PREDICATE_SEASON = () -> new EnrolmentPredicate() {

        @Override
        public boolean test(final Enrolment enrolment) {

            if (enrolment.isEvaluatedInSeason(getEvaluationSeason(), getExecutionSemester())) {
                throw new DomainException("error.EvaluationSeason.enrolment.evaluated.in.this.season",
                        enrolment.getPresentationName().getContent(), getEvaluationSeason().getName().getContent());
            }

            if (getContext() == EnrolmentEvaluationContext.MARK_SHEET_EVALUATION) {
                if (enrolment.isEnroledInSeason(getEvaluationSeason(), getExecutionSemester())) {
                    throw new DomainException("error.EvaluationSeason.already.enroled.in.this.season",
                            enrolment.getPresentationName().getContent(), getEvaluationSeason().getName().getContent());
                }

                if (enrolment.isApproved() && !EvaluationSeasonServices.isForApprovedEnrolments(getEvaluationSeason())) {
                    throw new DomainException("error.EvaluationSeason.evaluation.already.approved",
                            enrolment.getPresentationName().getContent(), getEvaluationSeason().getName().getContent());
                }
            }

            if (enrolment.getEvaluationsSet().stream()
                    .anyMatch(evaluation -> evaluation.getEvaluationSeason() == getEvaluationSeason() && evaluation.isFinal()
                            && evaluation.isApproved())) {
                throw new DomainException("error.EvaluationSeason.already.approved.in.this.season",
                        enrolment.getPresentationName().getContent(), getEvaluationSeason().getName().getContent());
            }

            if (EXTRA_NORMAL_PREDICATES.stream().anyMatch(p -> !p.test(enrolment))) {
                throw new DomainException("error.EvaluationSeason.extra.normal.predicate.evaluation.failed");
            }

            return true;
        }

    };

    static private Supplier<EnrolmentPredicate> PREDICATE_IMPROVEMENT = () -> new EnrolmentPredicate() {

        @Override
        public boolean test(final Enrolment enrolment) {
            final ExecutionSemester improvementSemester = getExecutionSemester();

            final ExecutionSemester enrolmentSemester = enrolment.getExecutionPeriod();
            final String name = enrolment.getPresentationName().getContent();

            if (improvementSemester.isBefore(enrolmentSemester)) {
                throw new DomainException("error.EnrolmentEvaluation.improvement.semester.must.be.after.or.equal.enrolment",
                        name);
            }

            // qubExtension
            if (enrolment.isAnual() && !improvementSemester.isFirstOfYear()) {
                throw new DomainException(
                        "error.EnrolmentEvaluation.improvement.semester.must.be.first.of.year.for.anual.courses", name);
            }

            if (!enrolment.isApproved()) {
                throw new DomainException(
                        "curricularRules.ruleExecutors.ImprovementOfApprovedEnrolmentExecutor.degree.module.hasnt.been.approved",
                        name);
            }

            if (EnrolmentServices.getExecutionCourses(enrolment, improvementSemester).isEmpty()) {
                throw new DomainException("error.EnrolmentEvaluation.improvement.required.ExecutionCourse", name,
                        improvementSemester.getQualifiedName());
            }

            if (EXTRA_IMPROVEMENT_PREDICATES.stream().anyMatch(p -> !p.test(enrolment))) {
                throw new DomainException("error.EvaluationSeason.extra.improvement.predicate.evaluation.failed");
            }

            PREDICATE_SEASON.get().fill(getEvaluationSeason(), improvementSemester, getContext()).test(enrolment);

            return true;
        }
    };

    static private Supplier<EnrolmentPredicate> PREDICATE_SPECIAL_SEASON = () -> new EnrolmentPredicate() {

        @Override
        public boolean test(final Enrolment enrolment) {
            final ExecutionSemester specialSeasonSemester = getExecutionSemester();

            final ExecutionSemester enrolmentSemester = enrolment.getExecutionPeriod();
            if (specialSeasonSemester != enrolmentSemester) {
                throw new DomainException("error.EnrolmentEvaluation.special.season.semester.must.be",
                        enrolment.getPresentationName().getContent());
            }

            if (enrolment.isApproved()) {
                throw new DomainException(
                        "curricularRules.ruleExecutors.EnrolmentInSpecialSeasonEvaluationExecutor.degree.module.has.been.approved",
                        enrolment.getPresentationName().getContent());
            }

            if (EXTRA_SPECIAL_PREDICATES.stream().anyMatch(p -> !p.test(enrolment))) {
                throw new DomainException("error.EvaluationSeason.extra.special.predicate.evaluation.failed");
            }

            PREDICATE_SEASON.get().fill(getEvaluationSeason(), getExecutionSemester(), getContext()).test(enrolment);

            final boolean isServices =
                    AcademicAuthorizationGroup.get(AcademicOperationType.STUDENT_ENROLMENTS).isMember(Authenticate.getUser());
            return considerThisEnrolmentNormalEnrolments(enrolment)
                    || considerThisEnrolmentPropaedeuticEnrolments(enrolment, isServices)
                    || considerThisEnrolmentExtraCurricularEnrolments(enrolment, isServices)
                    || considerThisEnrolmentStandaloneEnrolments(enrolment, isServices);
        }

        private boolean considerThisEnrolmentNormalEnrolments(Enrolment enrolment) {
            if (enrolment.isBolonhaDegree() && !enrolment.isExtraCurricular() && !enrolment.isPropaedeutic()
                    && !enrolment.isStandalone()) {
                if (enrolment.getParentCycleCurriculumGroup().isConclusionProcessed()) {
                    return false;
                }
            }
            return !enrolment.parentCurriculumGroupIsNoCourseGroupCurriculumGroup() || enrolment.isPropaedeutic();
        }

        private boolean considerThisEnrolmentPropaedeuticEnrolments(Enrolment enrolment, boolean isServices) {
            return enrolment.isPropaedeutic() && isServices;
        }

        private boolean considerThisEnrolmentExtraCurricularEnrolments(Enrolment enrolment, boolean isServices) {
            return enrolment.isExtraCurricular() && isServices;
        }

        private boolean considerThisEnrolmentStandaloneEnrolments(Enrolment enrolment, boolean isServices) {
            return enrolment.isStandalone() && isServices;
        }

    };

}
