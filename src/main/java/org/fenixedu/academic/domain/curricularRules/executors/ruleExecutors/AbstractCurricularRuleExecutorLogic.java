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
package org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleExecutor.CurricularRuleExecutorLogic;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

abstract public class AbstractCurricularRuleExecutorLogic implements CurricularRuleExecutorLogic {

    public RuleResult createFalseConfiguration(final DegreeModule degreeModule) {
        return createFalseConfiguration(degreeModule, StringUtils.EMPTY, getCurricularRuleLabelKey());
    }

    static public RuleResult createFalseConfiguration(final DegreeModule degreeModule, final String prefix,
            final String curricularRuleLabelKey) {

        final String literalMessage =
                prefix
                        + BundleUtil.getString(Bundle.APPLICATION, "curricularRules.ruleExecutors.logic.unavailable",
                                BundleUtil.getString(Bundle.BOLONHA, curricularRuleLabelKey));
        return RuleResult.createFalseWithLiteralMessage(degreeModule, literalMessage);
    }

    abstract protected String getCurricularRuleLabelKey();

}
