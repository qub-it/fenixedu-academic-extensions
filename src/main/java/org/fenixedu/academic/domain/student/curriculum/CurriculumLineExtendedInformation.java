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

import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.FenixFramework;

public class CurriculumLineExtendedInformation extends CurriculumLineExtendedInformation_Base {

    protected CurriculumLineExtendedInformation() {
        super();
        super.setBennu(Bennu.getInstance());
    }

    protected void init(final CurriculumLine curriculumLine) {
        super.setCurriculumLine(curriculumLine);
        checkRules();

    }

    private void checkRules() {
        if (getCurriculumLine() == null) {
            throw new IllegalArgumentException("error.CurriculumLineExtendedInformation.curriculumLine.cannot.be.null");
        }
    }

    static public void setupDeleteListener() {
        FenixFramework.getDomainModel().registerDeletionListener(CurriculumLine.class, line ->
        {
            if (line.getExtendedInformation() != null) {
                line.getExtendedInformation().delete();
            }
        });
    }

    private void delete() {

        super.setCurriculumLine(null);
        super.setBennu(null);

        super.deleteDomainObject();

    }

    static public CurriculumLineExtendedInformation findOrCreate(CurriculumLine curriculumLine) {
        return curriculumLine.getExtendedInformation() != null ? curriculumLine.getExtendedInformation() : create(curriculumLine);
    }

    static public CurriculumLineExtendedInformation create(CurriculumLine curriculumLine) {
        final CurriculumLineExtendedInformation result = new CurriculumLineExtendedInformation();
        result.init(curriculumLine);

        return result;
    }

}
