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

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.ulisboa.specifications.util.ULisboaSpecificationsUtil;

public enum CompetenceCourseMarkSheetStateEnum {

    EDITION,

    SUBMITTED,

    CONFIRMED

    ;

    public LocalizedString getDescriptionI18N() {
        return ULisboaSpecificationsUtil.bundleI18N(getClass().getSimpleName() + "." + name());
    }

    static public CompetenceCourseMarkSheetStateEnum findEdition() {
        return EDITION;
    }

    static public CompetenceCourseMarkSheetStateEnum findSubmited() {
        return SUBMITTED;
    }

    static public CompetenceCourseMarkSheetStateEnum findConfirmed() {
        return CONFIRMED;
    }

    public boolean isEdition() {
        return this == EDITION;
    }

    public boolean isSubmitted() {
        return this == SUBMITTED;
    }

    public boolean isConfirmed() {
        return this == CONFIRMED;
    }

}
