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
package org.fenixedu.academic.domain.student;

import java.text.MessageFormat;
import java.util.function.Supplier;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResultMessage;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.exceptions.EnrollmentDomainException;
import org.fenixedu.academic.domain.student.RegistrationRegime.RegistrationRegimeVerifier;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.I18N;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Move code to academic core
@Deprecated
abstract public class RegistrationRegimeVerifierInitializer {

    static private final Logger logger = LoggerFactory.getLogger(RegistrationRegimeVerifierInitializer.class);

    static public void init() {
        RegistrationRegime.setRegistrationRegimeVerifier(REGISTRATION_REGIME_VERIFIER);
    }

    private static Supplier<RegistrationRegimeVerifier> REGISTRATION_REGIME_VERIFIER = () -> new RegistrationRegimeVerifier() {

        public void checkEctsCredits(final Registration registration, final ExecutionYear executionYear,
                final RegistrationRegimeType type) {

            if (type == RegistrationRegimeType.PARTIAL_TIME) {

                StudentCurricularPlan plan = registration.getStudentCurricularPlan(executionYear);
                if (plan == null) {
                    plan = registration.getLastStudentCurricularPlan();
                }

                try {
                    plan.enrol(executionYear.getFirstExecutionPeriod(), CurricularRuleLevel.ENROLMENT_VERIFICATION_WITH_RULES);
                } catch (final EnrollmentDomainException e) {
                    final RuleResult ruleResult = e.getFalseResult();
                    final RuleResultMessage resultMessage =
                            ruleResult == null ? null : ruleResult.getMessages().iterator().next();
                    if (resultMessage != null) {
                        throw new DomainException(resultMessage.getMessage(), resultMessage.getArgs());
                    }
                }
            }
        }

    };

    static private String convertToString(final RuleResult ruleResult) {
        final StringBuilder builder = new StringBuilder();

        for (final RuleResultMessage message : ruleResult.getMessages()) {
            if (message.isToTranslate()) {
                builder.append(translateRuleMessage(message));
            } else {
                builder.append(message.getMessage());
            }
            builder.append("\n");
        }

        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    static private String translateRuleMessage(final RuleResultMessage message) {
        return MessageFormat.format(BundleUtil.getString("resources.ApplicationResources", I18N.getLocale(), message.getMessage())
                .replace("{0}", "'{0}'"), (Object[]) message.getArgs());

    }

}
