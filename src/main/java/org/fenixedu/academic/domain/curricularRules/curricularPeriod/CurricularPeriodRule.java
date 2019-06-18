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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.AbstractCurricularRuleExecutorLogic;
import org.fenixedu.academic.domain.degreeStructure.CurricularPeriodServices;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academicextensions.util.AcademicExtensionsUtil;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

abstract public class CurricularPeriodRule extends CurricularPeriodRule_Base {

    static final public Logger logger = LoggerFactory.getLogger(CurricularPeriodRule.class);

    static protected String MODULE_BUNDLE = AcademicExtensionsUtil.BUNDLE;

    protected CurricularPeriodRule() {
        super();
        setHideMessagePrefix(false);
    }

    public void messagePrefixDisabled() {
        setHideMessagePrefix(true);
    }

    public void messagePrefixEnabled() {
        setHideMessagePrefix(false);
    }

    public void init(final BigDecimal value) {
        setValue(value);
        checkRules();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getValue() == null) {
            throw new DomainException("error." + this.getClass().getSimpleName() + ".value.required");
        }
    }

    @Atomic
    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        deleteDomainObject();
    }

    abstract protected CurricularPeriodConfiguration getConfiguration();

    /**
     * If true, an executive rule overrides all other (possibly false) rules
     */
    public boolean isExecutive() {
        return false;
    }

    abstract protected DegreeCurricularPlan getDegreeCurricularPlan();

    public abstract String getLabel();

    protected DegreeModule getDegreeModule() {
        return getDegreeCurricularPlan().getRoot();
    }

    public RuleResult createTrue() {
        return RuleResult.createTrue(getDegreeModule());
    }

    public RuleResult createFalseConfiguration() {
        return createFalseConfiguration(getDegreeModule(), getMessagesPrefix());
    }

    static public RuleResult createFalseConfiguration(final DegreeModule degreeModule,
            final CurricularPeriodConfiguration configuration) {
        return createFalseConfiguration(degreeModule, getMessagesPrefix(configuration, true));
    }

    static private RuleResult createFalseConfiguration(final DegreeModule degreeModule, final String prefix) {
        return AbstractCurricularRuleExecutorLogic.createFalseConfiguration(degreeModule, prefix,
                "label.enrolmentPeriodRestrictions");
    }

    public RuleResult createFalseLabelled(final BigDecimal suffix) {
        return suffix == null ? createFalseLabelled() : createFalseLabelled(
                getMessagesSuffix("label.CurricularPeriodRule.suffix", suffix.toPlainString()));
    }

    public RuleResult createFalseLabelled() {
        return createFalseLabelled("");
    }

    public RuleResult createFalseLabelled(final String suffix) {
        final String literalMessage = getMessagesPrefix() + getLabel() + suffix;
        return RuleResult.createFalseWithLiteralMessage(getDegreeModule(), literalMessage);
    }

    public RuleResult createNA() {
        return RuleResult.createNA(getDegreeModule());
    }

    static public String getMessages(final RuleResult input) {
        if (input == null || input.getMessages() == null || input.getMessages().isEmpty()) {
            return "-";
        }

        return input.getMessages().stream().filter(i -> !Strings.isNullOrEmpty(i.getMessage())).map(i -> i.getMessage()).sorted()
                .collect(Collectors.joining("; "));
    }

    public void copyConfigurationTo(CurricularPeriodRule target) {
        target.setValue(getValue());
        target.setHideMessagePrefix(getHideMessagePrefix());
        target.setSemester(getSemester());
        target.setYearMin(getYearMin());
        target.setYearMax(getYearMax());;
    }

    private String getMessagesPrefix() {
        return getMessagesPrefix(getConfiguration(), getHideMessagePrefix());
    }

    static private String getMessagesPrefix(final CurricularPeriodConfiguration configuration, final boolean hideMessagePrefix) {
        return hideMessagePrefix || configuration == null ? "" : (BundleUtil.getString(MODULE_BUNDLE,
                "label.CurricularPeriodRule.prefix", configuration.getCurricularPeriod().getFullLabel()) + " ");
    }

    protected String getMessagesSuffix(final String key, final String... args) {
        return args == null ? "" : (" " + BundleUtil.getString(MODULE_BUNDLE, key, args));
    }

    protected Set<CurricularPeriod> getCurricularPeriodsConfigured(final int yearMin, final int yearMax,
            final boolean semesterAware) {

        final Set<CurricularPeriod> result = Sets.newHashSet();

        final DegreeCurricularPlan dcp = getDegreeCurricularPlan();
        for (int i = yearMin; i <= yearMax; i++) {
            final CurricularPeriod curricularPeriod = semesterAware ? CurricularPeriodServices.getCurricularPeriod(dcp, i,
                    getSemester()) : CurricularPeriodServices.getCurricularPeriod(dcp, i);

            if (curricularPeriod == null) {
                // if even one is not found, return false
                return null;
            } else {
                result.add(curricularPeriod);
            }
        }

        return result;
    }

    public CurricularPeriodRule cloneRule() {

        try {

            final Constructor<? extends CurricularPeriodRule> constructor = getClass().getDeclaredConstructor();
            constructor.setAccessible(true);

            final CurricularPeriodRule newInstance = constructor.newInstance();

            copyConfigurationTo(newInstance);

            return newInstance;

        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

}
