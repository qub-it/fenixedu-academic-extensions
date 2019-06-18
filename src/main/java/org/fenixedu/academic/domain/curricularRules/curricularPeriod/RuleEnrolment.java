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
package org.fenixedu.academic.domain.curricularRules.curricularPeriod;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.services.StatuteServices;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.Atomic;

abstract public class RuleEnrolment extends RuleEnrolment_Base {

    protected RuleEnrolment() {
        super();
    }

    public BigDecimal getCredits() {
        return super.getValue();
    }

    protected void init(final CurricularPeriodConfiguration configuration, final BigDecimal credits, final Integer semester) {
        super.init(credits);
        setConfigurationEnrolment(configuration);
        setSemester(semester);
        checkRules();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getConfigurationEnrolment() == null) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".configuration.is.required");
        }
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        //add more logical tests for checking deletion rules
        //if (getXPTORelation() != null)
        //{
        //    blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error." + this.getClass().getSimpleName() + ".cannot.be.deleted"));
        //}

        if (getConfigurationEnrolment() != null) {
            blockers.add(
                    BundleUtil.getString(Bundle.APPLICATION, "error." + this.getClass().getSimpleName() + ".cannot.be.deleted"));
        }
    }

    @Override
    @Atomic
    public void delete() {
        super.setConfigurationEnrolment(null);
        getStatuteTypesSet().clear();
        super.delete();
    }

    @Override
    protected CurricularPeriodConfiguration getConfiguration() {
        return getConfigurationEnrolment();
    }

    @Override
    protected DegreeCurricularPlan getDegreeCurricularPlan() {
        return getConfigurationEnrolment().getDegreeCurricularPlan();
    }

    @Override
    public void copyConfigurationTo(CurricularPeriodRule target) {
        super.copyConfigurationTo(target);
        final RuleEnrolment ruleEnrolment = (RuleEnrolment) target;
        ruleEnrolment.setIncludeEnrolments(getIncludeEnrolments());
        ruleEnrolment.setApplyToFlunkedStudents(getApplyToFlunkedStudents());
        ruleEnrolment.getStatuteTypesSet().addAll(getStatuteTypesSet());
    }

    protected boolean hasValidStatute(final EnrolmentContext enrolmentContext) {
        return getStatuteTypesSet().isEmpty()
                || StatuteServices.findStatuteTypes(enrolmentContext.getRegistration(), enrolmentContext.getExecutionYear())
                        .stream().anyMatch(s -> getStatuteTypesSet().contains(s));
    }

    protected String getStatuteTypesLabelPrefix() {
        return !getStatuteTypesSet().isEmpty() ? "["
                + getStatuteTypesSet().stream().map(s -> s.getName().getContent()).collect(Collectors.joining(", ")) + "] " : "";
    }

    abstract public RuleResult execute(final EnrolmentContext enrolmentContext);

    static public Set<IDegreeModuleToEvaluate> getEnroledAndEnroling(final EnrolmentContext enrolmentContext) {
        return enrolmentContext.getDegreeModulesToEvaluate().stream().filter(i -> i.isLeaf() && (i.isEnroled() || i.isEnroling()))
                .collect(Collectors.toSet());
    }

    static protected Set<IDegreeModuleToEvaluate> getEnroledAndEnroling(final EnrolmentContext enrolmentContext,
            final Predicate<IDegreeModuleToEvaluate> predicate) {
        return getEnroledAndEnroling(enrolmentContext).stream().filter(predicate).collect(Collectors.toSet());
    }

}
