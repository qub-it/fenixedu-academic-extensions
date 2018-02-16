/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: luis.egidio@qub-it.com
 *
 * 
 * This file is part of FenixEdu Specifications.
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

package org.fenixedu.academic.domain.evaluation.season;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.treasury.IImprovementTreasuryEvent;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.util.LocalizedStringUtil;
import org.fenixedu.ulisboa.specifications.ULisboaConfiguration;
import org.fenixedu.academic.domain.evaluation.season.rule.BlockingTreasuryEventInDebt;
import org.fenixedu.academic.domain.evaluation.season.rule.EvaluationSeasonRule;
import org.fenixedu.academic.domain.evaluation.season.rule.EvaluationSeasonStatuteType;
import org.fenixedu.academic.domain.evaluation.season.rule.PreviousSeasonBlockingGrade;
import org.fenixedu.academic.domain.evaluation.season.rule.PreviousSeasonEvaluation;
import org.fenixedu.academic.domain.evaluation.season.rule.PreviousSeasonMinimumGrade;
import org.fenixedu.ulisboa.specifications.domain.exceptions.ULisboaSpecificationsDomainException;
import org.fenixedu.ulisboa.specifications.domain.services.statute.StatuteServices;
import org.fenixedu.ulisboa.specifications.util.ULisboaSpecificationsUtil;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.core.AbstractDomainObjectServices;

abstract public class EvaluationSeasonServices {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationSeasonServices.class);

    static private void init(final EvaluationSeason evaluationSeason, final boolean active,
            final boolean requiresEnrolmentEvaluation, final boolean supportsEmptyGrades,
            final boolean supportsTeacherConfirmation) {

        EvaluationSeasonInformation.create(evaluationSeason, active, requiresEnrolmentEvaluation, supportsEmptyGrades,
                supportsTeacherConfirmation);
        checkRules(evaluationSeason);
    }

    static private void checkRules(final EvaluationSeason season) {
        if (season.getInformation() == null) {
            throw new ULisboaSpecificationsDomainException("error.EvaluationSeason.evaluationSeasonInformation.required");
        }

        if (Strings.isNullOrEmpty(season.getCode())) {
            throw new ULisboaSpecificationsDomainException("error.EvaluationSeason.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(season.getAcronym())) {
            throw new ULisboaSpecificationsDomainException("error.EvaluationSeason.acronym.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(season.getName())) {
            throw new ULisboaSpecificationsDomainException("error.EvaluationSeason.name.required");
        }

        if (!checkNTrue(1, season.getNormal(), season.getImprovement(), season.getSpecial())
                && !season.getSpecialAuthorization()) {
            throw new ULisboaSpecificationsDomainException("error.EvaluationSeason.type.not.unique");
        }

        checkSeasonExistsForName(season, season.getName());
    }

    static private void checkSeasonExistsForName(final EvaluationSeason evaluationSeason, final LocalizedString name) {

        for (final EvaluationSeason season : findByName(name).collect(Collectors.toSet())) {
            if (season != evaluationSeason) {
                throw new ULisboaSpecificationsDomainException("error.EvaluationSeason.duplicated.name");
            }
        }
    }

    static private boolean checkNTrue(int n, boolean... args) {
        assert args.length > 0;
        int count = 0;
        for (boolean b : args) {
            if (b) {
                count++;
            }
            if (count > n) {
                return false;
            }
        }
        return count == n;
    }

    @Atomic
    static public void edit(final EvaluationSeason evaluationSeason, final String code, final LocalizedString acronym,
            final LocalizedString name, final boolean normal, final boolean improvement, final boolean special,
            final boolean specialAuthorization, final boolean active, final boolean requiresEnrolmentEvaluation,
            final boolean supportsEmptyGrades, final boolean supportsTeacherConfirmation) {

        checkSeasonExistsForName(evaluationSeason, name);

        evaluationSeason.setCode(code);
        evaluationSeason.setAcronym(acronym);
        evaluationSeason.setName(name);
        evaluationSeason.setNormal(normal);
        evaluationSeason.setImprovement(improvement);
        evaluationSeason.setSpecial(special);
        evaluationSeason.setSpecialAuthorization(specialAuthorization);

        evaluationSeason.getInformation().edit(active, requiresEnrolmentEvaluation, supportsEmptyGrades,
                supportsTeacherConfirmation);
        checkRules(evaluationSeason);
    }

    @Atomic
    static public EvaluationSeason create(final String code, final LocalizedString acronym, final LocalizedString name,
            final boolean normal, final boolean improvement, final boolean special, final boolean specialAuthorization,
            final boolean active, final boolean requiresEnrolmentEvaluation, final boolean supportsEmptyGrades,
            final boolean supportsTeacherConfirmation) {

        final EvaluationSeason evaluationSeason =
                new EvaluationSeason(acronym, name, normal, improvement, specialAuthorization, special);
        evaluationSeason.setCode(code);

        init(evaluationSeason, active, requiresEnrolmentEvaluation, supportsEmptyGrades, supportsTeacherConfirmation);
        return evaluationSeason;
    }

    static public Stream<EvaluationSeason> findAll() {
        return EvaluationSeason.all();
    }

    static public Stream<EvaluationSeason> findByCode(final String code) {
        return findAll().filter(i -> code.equalsIgnoreCase(i.getCode()));
    }

    static public Stream<EvaluationSeason> findByAcronym(final LocalizedString acronym) {
        return findAll().filter(i -> acronym.equals(i.getAcronym()));
    }

    static public Stream<EvaluationSeason> findByName(final LocalizedString name) {
        return findAll().filter(i -> name.equals(i.getName()));
    }

    static public Stream<EvaluationSeason> findBySeasonOrder(final Integer seasonOrder) {
        return findAll().filter(i -> seasonOrder.equals(getSeasonOrder(i)));
    }

    static public Stream<EvaluationSeason> findByActive(final boolean active) {
        return findAll().filter(i -> active == getActive(i));
    }

    static public LocalizedString getDescriptionI18N(final EvaluationSeason input) {
        LocalizedString result = new LocalizedString();

        if (input != null) {
            result = result.append(input.getName());
            result = result.append(" [");
            result = result.append(getTypeDescriptionI18N(input));
            result = result.append("]");
        }

        return result;
    }

    static public LocalizedString getTypeDescriptionI18N(final EvaluationSeason input) {
        return getEnrolmentEvaluationType(input).getDescriptionI18N();
    }

    static public boolean hasPreviousSeasonBlockingGrade(final EvaluationSeason season, final EnrolmentEvaluation evaluation) {
        if (evaluation != null) {

            for (final PreviousSeasonBlockingGrade rule : EvaluationSeasonRule.find(season, PreviousSeasonBlockingGrade.class)) {
                final Grade blocking = rule.getBlocking();
                final Grade inspect = evaluation.getGrade();

                if (blocking.toString().equals(inspect.toString())) {
                    return true;
                }
            }
        }

        return false;
    }

    static public boolean hasRequiredPreviousSeasonMinimumGrade(final EvaluationSeason season,
            final Collection<EnrolmentEvaluation> evaluations) {

        if (evaluations != null) {

            final EvaluationSeason previousSeason = EvaluationSeasonServices.getPreviousSeason(season);
            if (previousSeason != null) {

                for (final PreviousSeasonMinimumGrade rule : EvaluationSeasonRule.find(season,
                        PreviousSeasonMinimumGrade.class)) {
                    for (final EnrolmentEvaluation evaluation : evaluations) {
                        if (evaluation.getEvaluationSeason() == previousSeason) {

                            final Grade minimum = rule.getMinimum();
                            final Grade grade = evaluation.getGrade();
                            if (minimum.compareTo(grade) < 0) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    static public boolean isBlockingTreasuryEventInDebt(final EvaluationSeason season, final Enrolment enrolment,
            final ExecutionSemester semester) {

        if (enrolment != null && !EvaluationSeasonRule.find(season, BlockingTreasuryEventInDebt.class).isEmpty()) {

            final Registration registration = enrolment.getRegistration();
            final Person person = registration.getPerson();

            if (ULisboaConfiguration.getConfiguration().getEnrolmentsInEvaluationsDependOnAcademicalActsBlocked()
                    && TreasuryBridgeAPIFactory.implementation().isAcademicalActsBlocked(person, new LocalDate())) {
                return true;
            }

            if (season.isImprovement()) {
                final EnrolmentEvaluation evaluation =
                        enrolment.getEnrolmentEvaluation(season, semester, (Boolean) null).orElse(null);

                if (evaluation != null) {
                    final IImprovementTreasuryEvent event = TreasuryBridgeAPIFactory.implementation()
                            .getImprovementTaxTreasuryEvent(registration, semester.getExecutionYear());

                    if (event != null && event.isInDebt(evaluation)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    static public boolean hasStudentStatuteBlocking(final EvaluationSeason season, final Enrolment enrolment,
            final ExecutionSemester semester) {

        if (enrolment != null) {

            for (final EvaluationSeasonStatuteType rule : EvaluationSeasonRule.find(season, EvaluationSeasonStatuteType.class)) {
                final Set<StatuteType> blocking = rule.getStatuteTypesSet();
                final Set<StatuteType> inspect = Sets.newHashSet(
                        StatuteServices.findStatuteTypes(enrolment.getRegistration(), enrolment.getExecutionPeriod()));

                if (!Sets.intersection(blocking, inspect).isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    static public boolean isRequiredPreviousSeasonEvaluation(final EvaluationSeason season) {
        return !EvaluationSeasonRule.find(season, PreviousSeasonEvaluation.class).isEmpty();
    }

    static public boolean isRequiredEnrolmentEvaluation(final EvaluationSeason input) {
        return input != null && input.getInformation() != null && input.getInformation().getRequiresEnrolmentEvaluation();
    }

    static public boolean isSupportsEmptyGrades(final EvaluationSeason input) {
        return input != null && input.getInformation() != null && input.getInformation().getSupportsEmptyGrades();
    }

    static public boolean isSupportsTeacherConfirmation(final EvaluationSeason input) {
        return input != null && input.getInformation() != null && input.getInformation().getSupportsTeacherConfirmation();
    }

    static public boolean isForApprovedEnrolments(final EvaluationSeason season) {
        return season != null && season.isImprovement();
    }

    static public boolean isDifferentEvaluationSemesterAccepted(final EvaluationSeason season) {
        return season != null && season.isImprovement();
    }

    static public EvaluationSeason getPreviousSeason(final EvaluationSeason input) {
        return getPreviousSeason(input, true);
    }

    static private EvaluationSeason getPreviousSeason(final EvaluationSeason input, final boolean onlyActive) {
        EvaluationSeason result = null;

        for (final EvaluationSeason iter : findByActive(onlyActive).collect(Collectors.toSet())) {
            if (iter == input) {
                continue;
            }

            if (getEnrolmentEvaluationType(iter) != getEnrolmentEvaluationType(input)) {
                continue;
            }

            if (getSeasonOrder(iter) > getSeasonOrder(input)) {
                continue;
            }

            if (result != null && getSeasonOrder(result) > getSeasonOrder(iter)) {
                continue;
            }

            result = iter;
        }

        return result;
    }

    static public EvaluationSeason getNextSeason(final EvaluationSeason input) {
        return getNextSeason(input, true);
    }

    static private EvaluationSeason getNextSeason(final EvaluationSeason input, final boolean onlyActive) {
        EvaluationSeason result = null;

        for (final EvaluationSeason iter : findByActive(onlyActive).collect(Collectors.toSet())) {
            if (iter == input) {
                continue;
            }

            if (getEnrolmentEvaluationType(iter) != getEnrolmentEvaluationType(input)) {
                continue;
            }

            if (getSeasonOrder(iter) < getSeasonOrder(input)) {
                continue;
            }

            if (result != null && getSeasonOrder(result) < getSeasonOrder(iter)) {
                continue;
            }

            result = iter;
        }

        return result;
    }

    static public EvaluationSeason getFirstSeasonInChain(final EvaluationSeason input) {
        if (isFirst(input)) {
            return input;
        }

        return getFirstSeasonInChain(getPreviousSeason(input));
    }

    static public boolean isFirst(final EvaluationSeason input) {
        return getPreviousSeason(input) == null;
    }

    static public boolean isLast(final EvaluationSeason input) {
        return getNextSeason(input) == null;
    }

    static public boolean isDeletable(final EvaluationSeason evaluationSeason) {
        return evaluationSeason.getMarkSheetSet().size() == 0 && evaluationSeason.getEvaluationSet().size() == 0;
    }

    static public boolean getActive(final EvaluationSeason input) {
        return input != null && input.getInformation() != null && input.getInformation().getActive();
    }

    static public Integer getSeasonOrder(final EvaluationSeason input) {
        final EvaluationSeasonInformation information = input.getInformation();

        // bulletproof in order to be bootstrap-safe
        return information == null || information.getSeasonOrder() == null ? 0 : information.getSeasonOrder();
    }

    static public void setSeasonOrder(final EvaluationSeason evaluationSeason, final Integer order) {
        evaluationSeason.getInformation().setSeasonOrder(order);
    }

    @Atomic
    static public void orderUp(final EvaluationSeason input) {
        if (isFirst(input)) {
            return;
        }

        final EvaluationSeason neighbour = getPreviousSeason(input, false);
        final Integer temp = getSeasonOrder(neighbour);
        setSeasonOrder(neighbour, getSeasonOrder(input));
        setSeasonOrder(input, temp);
    }

    @Atomic
    static public void orderDown(final EvaluationSeason input) {
        if (isLast(input)) {
            return;
        }

        final EvaluationSeason neighbour = getNextSeason(input, false);
        final Integer temp = getSeasonOrder(neighbour);
        setSeasonOrder(neighbour, getSeasonOrder(input));
        setSeasonOrder(input, temp);
    }

    @Atomic
    static public void delete(final EvaluationSeason evaluationSeason) {
        if (!isDeletable(evaluationSeason)) {
            throw new ULisboaSpecificationsDomainException("error.EvaluationSeason.not.empty.to.delete");
        }

        AbstractDomainObjectServices.deleteDomainObject(evaluationSeason);
    }

    static private enum EnrolmentEvaluationType {

        NORMAL("label.EvaluationSeason.normal"),

        SPECIAL_SEASON("label.EvaluationSeason.special"),

        IMPROVEMENT("label.EvaluationSeason.improvement"),

        SPECIAL_AUTHORIZATION("label.EvaluationSeason.specialAuthorization");

        private String descriptionKey;

        private EnrolmentEvaluationType(final String descriptionKey) {
            this.descriptionKey = descriptionKey;
        }

        public LocalizedString getDescriptionI18N() {
            return ULisboaSpecificationsUtil.bundleI18N(this.descriptionKey);
        }
    }

    static private EnrolmentEvaluationType getEnrolmentEvaluationType(final EvaluationSeason input) {
        if (input.isSpecialAuthorization()) {
            return EnrolmentEvaluationType.SPECIAL_AUTHORIZATION;
        } else if (input.isNormal()) {
            return EnrolmentEvaluationType.NORMAL;
        } else if (input.isSpecial()) {
            return EnrolmentEvaluationType.SPECIAL_SEASON;
        } else if (input.isImprovement()) {
            return EnrolmentEvaluationType.IMPROVEMENT;
        }

        throw new ULisboaSpecificationsDomainException("error.EvaluationSeason.missing.type");
    }

    static private Integer getEnrolmentEvaluationTypePrecedence(final EvaluationSeason input) {
        return getEnrolmentEvaluationType(input).ordinal();
    }

    public static Comparator<EvaluationSeason> SEASON_ORDER_COMPARATOR = new Comparator<EvaluationSeason>() {

        @Override
        public int compare(EvaluationSeason o1, EvaluationSeason o2) {
            int result = getEnrolmentEvaluationTypePrecedence(o1).compareTo(getEnrolmentEvaluationTypePrecedence(o2));

            if (result == 0) {
                result = getSeasonOrder(o1).compareTo(getSeasonOrder(o2));
            }

            if (result == 0) {
                result = o1.compareTo(o2);
            }

            return result;
        }
    };

    @Atomic
    static public void initialize() {
        final List<EvaluationSeason> seasons = findAll().sorted(SEASON_ORDER_COMPARATOR).collect(Collectors.toList());

        for (int i = 0; i < seasons.size(); i++) {
            final EvaluationSeason iter = seasons.get(i);

            if (iter.getInformation() == null) {
                logger.info("Init " + iter.getName().getContent());
                EvaluationSeasonInformation.create(iter, false, false, true, false).setSeasonOrder(i);
            }
        }
    }

    static public Integer maxOrder() {
        int result = 0;

        for (final EvaluationSeason iter : findAll().collect(Collectors.toSet())) {
            final Integer order = getSeasonOrder(iter);
            if (order > result) {
                result = order;
            }
        }

        return result;
    }

}
