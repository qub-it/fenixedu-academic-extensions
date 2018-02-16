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

package org.fenixedu.academic.domain.evaluation.markSheet;

import java.util.Collection;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.ulisboa.specifications.domain.exceptions.ULisboaSpecificationsDomainException;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class CompetenceCourseMarkSheetStateChange extends CompetenceCourseMarkSheetStateChange_Base
        implements Comparable<CompetenceCourseMarkSheetStateChange> {

    protected CompetenceCourseMarkSheetStateChange() {
        super();
    }

    protected void init(final CompetenceCourseMarkSheet markSheet, final CompetenceCourseMarkSheetStateEnum state,
            final String reason, final boolean byTeacher) {

        setCompetenceCourseMarkSheet(markSheet);
        setState(state);
        setReason(reason);
        setByTeacher(byTeacher);
        setDate(new DateTime());
        setResponsible(Authenticate.getUser().getPerson());
        checkRules();
    }

    private void checkRules() {
        if (getCompetenceCourseMarkSheet() == null) {
            throw new ULisboaSpecificationsDomainException(
                    "error.CompetenceCourseMarkSheetStateChange.competenceCourseMarkSheet.required");
        }

        if (getState() == null) {
            throw new ULisboaSpecificationsDomainException("error.CompetenceCourseMarkSheetStateChange.state.required");
        }

        if (getDate() == null) {
            throw new ULisboaSpecificationsDomainException("error.CompetenceCourseMarkSheetStateChange.date.required");
        }

        if (getResponsible() == null) {
            throw new ULisboaSpecificationsDomainException("error.CompetenceCourseMarkSheetStateChange.responsible.required");
        }
    }

    @Atomic
    public void edit(final CompetenceCourseMarkSheet competenceCourseMarkSheet, final CompetenceCourseMarkSheetStateEnum state,
            final DateTime date, final java.lang.String reason, final boolean byTeacher) {
        setCompetenceCourseMarkSheet(competenceCourseMarkSheet);
        setState(state);
        setDate(date);
        setReason(reason);
        setByTeacher(byTeacher);
        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {

        if (getSnapshot() != null) {
            getSnapshot().delete();
        }

        super.setCompetenceCourseMarkSheet(null);
        super.setResponsible(null);

        ULisboaSpecificationsDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        deleteDomainObject();
    }

    @Atomic
    public static CompetenceCourseMarkSheetStateChange createEditionState(final CompetenceCourseMarkSheet markSheet,
            final boolean byTeacher, final String reason) {

        final CompetenceCourseMarkSheetStateChange result = new CompetenceCourseMarkSheetStateChange();
        result.init(markSheet, CompetenceCourseMarkSheetStateEnum.findEdition(), reason, byTeacher);
        
        if (byTeacher) {
            markSheet.checkIfIsGradeSubmissionAvailable();
        }
        
        return result;
    }

    @Atomic
    public static CompetenceCourseMarkSheetStateChange createConfirmedState(final CompetenceCourseMarkSheet markSheet,
            final boolean byTeacher, final String reason) {

        final CompetenceCourseMarkSheetStateChange result = new CompetenceCourseMarkSheetStateChange();
        result.init(markSheet, CompetenceCourseMarkSheetStateEnum.findConfirmed(), reason, byTeacher);
        return result;
    }

    @Atomic
    public static CompetenceCourseMarkSheetStateChange createSubmitedState(final CompetenceCourseMarkSheet markSheet,
            final boolean byTeacher, final String reason) {

        final CompetenceCourseMarkSheetStateChange result = new CompetenceCourseMarkSheetStateChange();
        result.init(markSheet, CompetenceCourseMarkSheetStateEnum.findSubmited(), reason, byTeacher);
        
        if (byTeacher) {
            markSheet.checkIfIsGradeSubmissionAvailable();
        }
        
        return result;
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<CompetenceCourseMarkSheetStateChange> findByCompetenceCourseMarkSheet(
            final CompetenceCourseMarkSheet competenceCourseMarkSheet) {
        return competenceCourseMarkSheet.getStateChangeSet().stream()
                .filter(i -> competenceCourseMarkSheet.equals(i.getCompetenceCourseMarkSheet()));
    }

    @Override
    public int compareTo(final CompetenceCourseMarkSheetStateChange o) {
        int c = getDate().compareTo(o.getDate());
        return c != 0 ? c : getExternalId().compareTo(o.getExternalId());
    }

    public boolean isEdition() {
        return getState().isEdition();
    }

    public boolean isSubmitted() {
        return getState().isSubmitted();
    }

    public boolean isConfirmed() {
        return getState().isConfirmed();
    }

}
