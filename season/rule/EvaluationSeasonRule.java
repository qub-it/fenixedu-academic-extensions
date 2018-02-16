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

package org.fenixedu.academic.domain.evaluation.season.rule;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.i18n.LocalizedString.Builder;
import org.fenixedu.ulisboa.specifications.domain.exceptions.ULisboaSpecificationsDomainException;
import org.fenixedu.ulisboa.specifications.util.ULisboaSpecificationsUtil;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

abstract public class EvaluationSeasonRule extends EvaluationSeasonRule_Base {

    protected EvaluationSeasonRule() {
        super();
    }

    protected void init(final EvaluationSeason season) {
        setSeason(season);
        checkRules();
    }

    private void checkRules() {
        final EvaluationSeason season = getSeason();

        if (season == null) {
            throw new ULisboaSpecificationsDomainException("error.EvaluationSeasonRule.evaluationSeason.required");
        }

        if (season.getRulesSet().stream().anyMatch(checkDuplicate())) {
            throw new ULisboaSpecificationsDomainException("error.EvaluationSeasonRule.duplicated");
        }
    }

    protected Predicate<? super EvaluationSeasonRule> checkDuplicate() {
        return i -> i != this && i.getClass().equals(this.getClass());
    }

    static protected void checkRules(final Grade grade) {
        if (grade == null || grade.isEmpty()) {
            throw new ULisboaSpecificationsDomainException("error.EvaluationSeasonRule.grade.required");
        }
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {
        ULisboaSpecificationsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        setSeason(null);
        deleteDomainObject();
    }

    abstract public boolean isUpdatable();

    abstract public LocalizedString getDescriptionI18N();

    static protected LocalizedString getDescriptionI18N(final Class<? extends EvaluationSeasonRule> clazz, final Grade grade) {
        final Builder builder = ULisboaSpecificationsUtil.bundleI18N(clazz.getSimpleName()).builder();
        builder.append(grade.getExtendedValue(), ": ");
        builder.append(grade.getValue(), " [");
        builder.append(grade.getGradeScale().getDescription(), ", ");
        builder.append("]");
        return builder.build();
    }

    static public <T extends EvaluationSeasonRule> Set<T> find(final EvaluationSeason season, final Class<T> clazz) {
        final Set<T> result = Sets.newHashSet();

        if (season != null && clazz != null) {
            season.getRulesSet().stream().filter(i -> i.getClass() == clazz).forEach(i -> result.add((T) i));
        }

        return result;
    }

}
