/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/8/15
 */
public class JPADataStorageTest extends BaseWikiIntegrationTest {

    public void testSearchWikiByName() throws Exception {
        //Given
        indexWiki("My Wiki");
        JPADataStorage storage = new JPADataStorage();
        WikiSearchData searchData = new WikiSearchData("My Wiki", null, null, null);
        //When
        PageList<SearchResult> results = storage.search(searchData);
        //Then
        assertEquals(1, results.getAll().size());
    }

    public void testSearchPageByName() throws Exception {
        //Given
        indexPage("My Page", "My Page", "This is the content of my Page", "This is a comment");
        JPADataStorage storage = new JPADataStorage();
        WikiSearchData searchData = new WikiSearchData("Page", null, null, null);
        //When
        PageList<SearchResult> results = storage.search(searchData);
        //Then
        assertEquals(1, results.getAll().size());
    }

    //TODO test search on all the fields
    //TODO test with wrong field in the configuration

}
