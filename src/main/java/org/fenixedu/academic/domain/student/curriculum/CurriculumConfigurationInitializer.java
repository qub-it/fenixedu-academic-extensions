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
package org.fenixedu.academic.domain.student.curriculum;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.function.Supplier;

import org.fenixedu.academic.FenixEduAcademicExtensionsConfiguration;
import org.fenixedu.academic.FenixeduAcademicExtensionsInitializer;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodConfiguration;
import org.fenixedu.academic.domain.curricularRules.curricularPeriod.CurricularPeriodRule;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataServices;
import org.fenixedu.academic.domain.student.RegistrationServices;
import org.fenixedu.academic.domain.student.curriculum.Curriculum.CurricularYearCalculator;
import org.fenixedu.academic.domain.student.curriculum.Curriculum.CurriculumEntryPredicate;
import org.fenixedu.academic.domain.student.curriculum.Curriculum.CurriculumGradeCalculator;
import org.fenixedu.academic.domain.student.curriculum.conclusion.RegistrationConclusionServices;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup.ConclusionProcessEnabler;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup.CurriculumSupplier;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class CurriculumConfigurationInitializer {

    static private final Logger logger = LoggerFactory.getLogger(CurriculumConfigurationInitializer.class);

    static public void init() {
        Curriculum.setCurricularYearCalculator(CURRICULAR_YEAR_CALCULATOR);
        Curriculum.setCurriculumGradeCalculator(CURRICULUM_GRADE_CALCULATOR);
        Curriculum.setCurriculumEntryPredicate(CURRICULUM_ENTRY_PREDICATE);
        CurriculumGroup.setCurriculumSupplier(GROUP_CURRICULUM_SUPPLIER);
        CurriculumGroup.setConclusionProcessEnabler(GROUP_CONCLUSION_ENABLER);
    }

    /* ======================================================================================================
     * 
     * CurricularYearCalculator
     * 
     * ======================================================================================================
     */

    static private Supplier<CurricularYearCalculator> CURRICULAR_YEAR_CALCULATOR = () -> new CurricularYearCalculator() {

        private BigDecimal approvedCredits;

        private BigDecimal remainingCredits;

        private Integer curricularYear;

        private Integer totalCurricularYears;

        @Override
        public Integer curricularYear(final Curriculum curriculum) {
            if (curricularYear == null) {
                curricularYear = calculateCurricularYear(curriculum).getResult();
            }

            return curricularYear;
        }

        @Override
        public Integer totalCurricularYears(final Curriculum curriculum) {
            if (totalCurricularYears == null) {
                totalCurricularYears = calculateTotalCurricularYears(curriculum);
            }

            return totalCurricularYears;
        }

        @Override
        public BigDecimal approvedCredits(final Curriculum curriculum) {
            if (approvedCredits == null) {

                approvedCredits = BigDecimal.ZERO;
                for (final ICurriculumEntry entry : curriculum.getCurricularYearEntries()) {
                    approvedCredits = approvedCredits.add(entry.getEctsCreditsForCurriculum());
                }
                accountForDirectIngressions(curriculum);
            }

            return approvedCredits;
        }

        @Override
        public BigDecimal remainingCredits(final Curriculum curriculum) {
            if (remainingCredits == null) {

                remainingCredits = BigDecimal.ZERO;
                for (final ICurriculumEntry entry : curriculum.getCurricularYearEntries()) {
                    if (entry instanceof Dismissal) {
                        final Dismissal dismissal = (Dismissal) entry;
                        if (dismissal.getCredits().isCredits() || dismissal.getCredits().isEquivalence()
                                || (dismissal.isCreditsDismissal() && !dismissal.getCredits().isSubstitution())) {
                            remainingCredits = remainingCredits.add(entry.getEctsCreditsForCurriculum());
                        }
                    }
                }
            }

            return remainingCredits;
        }

        private void accountForDirectIngressions(final Curriculum curriculum) {
            if (curriculum.getStudentCurricularPlan() == null) {
                //empty curriculum
                return;
            }
            if (calculateCycleType(curriculum) != null) {
                return;
            }
            //this is to prevent some oddly behavior spotted (e.g. student 57276)
            if (curriculum.getStudentCurricularPlan().getCycleCurriculumGroups().isEmpty()) {
                return;
            }
            CycleCurriculumGroup sgroup = Collections.min(curriculum.getStudentCurricularPlan().getCycleCurriculumGroups(),
                    CycleCurriculumGroup.COMPARATOR_BY_CYCLE_TYPE_AND_ID);
            CycleType cycleIter = sgroup.getCycleType().getPrevious();
            while (cycleIter != null) {
                if (curriculum.getStudentCurricularPlan().getDegreeCurricularPlan().getCycleCourseGroup(cycleIter) != null) {
                    approvedCredits = approvedCredits.add(new BigDecimal(cycleIter.getEctsCredits()));
                }
                cycleIter = cycleIter.getPrevious();
            }
        }

    };

    static public CurricularYearResult calculateCurricularYear(final Curriculum curriculum) {
        final ExecutionYear executionYear = curriculum.getExecutionYear();
        final CurricularYearResult calculated = new CurricularYearResult(executionYear);

        if (curriculum.getCurriculumModule() == null) {
            calculated.setResult(1);
            calculated.setJustificationText(AcademicExtensionsUtil.bundle(
                    "error.curricularYear.unable.to.determine.student.curricular.plan.for.execution.year",
                    executionYear.getQualifiedName()));
            return calculated;
        }

        final Registration registration = curriculum.getCurriculumModule().getRegistration();

        // should always try to check the SCP at the given execution year...
        StudentCurricularPlan scp = registration.getStudentCurricularPlan(executionYear);
        if (scp == null || CurricularPeriodServices.getCurricularPeriodConfiguration(scp.getDegreeCurricularPlan(), 1) == null) {
            // ...but let's have a failsafe, use the same SCP we were until this bug fix
            scp = curriculum.getStudentCurricularPlan();
        }

        final DegreeCurricularPlan dcp = scp.getDegreeCurricularPlan();
        RuleResult justification = null;
        for (int i = calculateTotalCurricularYears(curriculum); i > 1; i--) {

            final CurricularPeriodConfiguration configuration = CurricularPeriodServices.getCurricularPeriodConfiguration(dcp, i);
            if (configuration == null) {
                calculated.setJustification(RuleResult.createFalseWithLiteralMessage(dcp.getRoot(),
                        BundleUtil.getString(Bundle.BOLONHA, "label.enrolmentPeriodRestrictions")));

            } else {

                final RuleResult ruleResult = configuration.verifyRulesForTransition(curriculum);
                if (ruleResult.isTrue()) {
                    calculated.setResult(i);
                    if (justification == null) {
                        justification = ruleResult;
                    }
                    break;
                } else {
                    justification = ruleResult;
                }
            }
        }
        calculated.setJustification(justification);

        final Integer curricularYear = RegistrationDataServices.getOverridenCurricularYear(registration, executionYear);
        if (curricularYear != null) {
            final CurricularYearResult overriden = new CurricularYearResult(executionYear);
            overriden.setResult(curricularYear);

            overriden
                    .setJustification(RuleResult.createFalseWithLiteralMessage(curriculum.getCurriculumModule().getDegreeModule(),
                            BundleUtil.getString(AcademicExtensionsUtil.BUNDLE, "label.curricularYear.overriden",
                                    String.valueOf(overriden.getResult()), String.valueOf(calculated.getResult()),
                                    calculated.getJustificationPresentation())));
            logger.debug("[REG][{}][CURRICULAR_YEAR][OVERRIDEN][{}]", registration.getNumber(),
                    String.valueOf(overriden.getResult()));
            return overriden;
        }

        logger.debug("[REG][{}][CURRICULAR_YEAR][{}]", registration.getNumber(), String.valueOf(calculated.getResult()));
        return calculated;
    }

    static private int calculateTotalCurricularYears(final Curriculum curriculum) {
        return curriculum.getStudentCurricularPlan() == null ? 0 : curriculum.getStudentCurricularPlan().getDegreeCurricularPlan()
                .getDurationInYears(calculateCycleType(curriculum));
    }

    static private CycleType calculateCycleType(final Curriculum curriculum) {
        if (!curriculum.hasCurriculumModule()) {
            return null;
        }

        final CurriculumModule module = curriculum.getCurriculumModule();
        final CycleType cycleType = module.isCycleCurriculumGroup() ? ((CycleCurriculumGroup) module).getCycleType() : null;
        return cycleType;
    }

    static public class CurricularYearResult {

        private int curricularYear = 1;

        //TODO: replace with justificationText
        private RuleResult justification = null;

        private String justificationText = null;

        private ExecutionYear executionYear;

        public CurricularYearResult(final ExecutionYear executionYear) {
            this.setExecutionYear(executionYear);
        }

        public int getResult() {
            return curricularYear;
        }

        private void setResult(int result) {
            this.curricularYear = result;
        }

        public RuleResult getJustification() {
            return justification;
        }

        private void setJustification(final RuleResult justification) {
            this.justification = justification;
        }

        private void setJustificationText(final String justificationText) {
            this.justificationText = justificationText;
        }

        public String getJustificationPresentation() {
            return this.justificationText != null ? this.justificationText : CurricularPeriodRule.getMessages(this.justification)
                    .replace("-", AcademicExtensionsUtil.bundle("label.CurricularYearResult.empty"))
                    .replace(AcademicExtensionsUtil.bundle("label.CurricularYearResult.prefix.remove"),
                            AcademicExtensionsUtil.bundle("label.CurricularYearResult.prefix.add"));
        }

        public ExecutionYear getExecutionYear() {
            return executionYear;
        }

        public void setExecutionYear(ExecutionYear executionYear) {
            this.executionYear = executionYear;
        }
    }

    /* ======================================================================================================
     * 
     * CurriculumGradeCalculator
     * 
     * ======================================================================================================
     */

    static private Supplier<CurriculumGradeCalculator> CURRICULUM_GRADE_CALCULATOR = () -> FenixeduAcademicExtensionsInitializer
            .loadClass(null, FenixEduAcademicExtensionsConfiguration.getConfiguration().getCurriculumGradeCalculator());

    /* ======================================================================================================
     * 
     * CurriculumEntryPredicate
     * 
     * ======================================================================================================
     */

    static private Supplier<CurriculumEntryPredicate> CURRICULUM_ENTRY_PREDICATE = () -> new CurriculumEntryPredicate() {

        @Override
        public boolean test(final ICurriculumEntry input) {

            if (input instanceof CurriculumLine) {
                final CurriculumLine line = (CurriculumLine) input;

                if (CurriculumLineServices.isExcludedFromCurriculum(line)) {
                    return false;
                }
            }

            return true;
        }
    };

    /* ======================================================================================================
     * 
     * CurriculumGroup CurriculumSupplier
     * 
     * ======================================================================================================
     */

    static public Supplier<CurriculumSupplier> GROUP_CURRICULUM_SUPPLIER = () -> new CurriculumSupplier() {

        @Override
        public Curriculum get(final CurriculumGroup curriculumGroup, final DateTime when, final ExecutionYear executionYear) {

            final Curriculum result = Curriculum.createEmpty(curriculumGroup, executionYear);

            if (curriculumGroup.wasCreated(when)) {
                for (final CurriculumModule curriculumModule : curriculumGroup.getCurriculumModulesSet()) {
                    result.add(curriculumModule.getCurriculum(when, executionYear));
                }
            }

            for (final CurriculumGroup otherGroup : RegistrationConclusionServices
                    .getCurriculumGroupsForConclusion(curriculumGroup)) {

                if (otherGroup != curriculumGroup) {
                    result.add(otherGroup.getCurriculum(when, executionYear));
                }
            }

            return result;
        }

    };

    /* ======================================================================================================
     * 
     * CurriculumGroup ConclusionProcessEnabler
     * 
     * ======================================================================================================
     */

    static private Supplier<ConclusionProcessEnabler> GROUP_CONCLUSION_ENABLER = () -> new ConclusionProcessEnabler() {

        @Override
        public boolean isAllowed(final CurriculumGroup input) {
            if (input != null) {
                return input.isConcluded() || RegistrationServices.isCurriculumAccumulated(input.getRegistration());
            }

            return false;
        }
    };

}
