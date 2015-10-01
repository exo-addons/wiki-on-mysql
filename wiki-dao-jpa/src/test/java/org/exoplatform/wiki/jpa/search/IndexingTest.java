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

package org.exoplatform.wiki.jpa.search;

import static org.junit.Assert.assertNotEquals;

import java.util.Collections;

import org.exoplatform.wiki.jpa.BaseWikiIntegrationTest;
import org.exoplatform.wiki.jpa.entity.Page;
import org.exoplatform.wiki.jpa.entity.Permission;
import org.exoplatform.wiki.jpa.entity.PermissionType;
import org.exoplatform.wiki.jpa.entity.Wiki;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/14/15
 */
public class IndexingTest extends BaseWikiIntegrationTest {

    public void testIndexingAndSearchingOfWiki() throws NoSuchFieldException, IllegalAccessException {
        //Given
        //When
        indexWiki("RDBMS Guidelines");
        //Then
        assertEquals(1, storage.search(new WikiSearchData("RDBMS", null, null, null)).getPageSize());
    }

    public void testIndexingAndSearchingOfWikiPage() throws NoSuchFieldException, IllegalAccessException {
        //Given
        //When
        indexPage("RDBMS Guidelines", "RDBMS Guidelines", "All the guidelines you need", "Draft version");
        //Then
        assertEquals(1, storage.search(new WikiSearchData("RDBMS", null, null, null)).getPageSize());
    }

    public void testUpdatingWiki() throws NoSuchFieldException, IllegalAccessException {
        //Given
        Wiki wiki = indexWiki("RDBMS Guidelines");
        assertEquals(0, storage.search(new WikiSearchData("Liquibase", null, null, null)).getPageSize());
        //When
        wiki.setName("Liquibase Guidelines");
        wikiDAO.update(wiki);
        assertEquals(1, wikiDAO.findAll().size());
        indexingService.reindex(WikiIndexingServiceConnector.TYPE, Long.toString(wiki.getId()));
        setIndexingOperationTimestamp();
        indexingService.process();
        node.client().admin().indices().prepareRefresh().execute().actionGet();
        //Then
        assertEquals(1, storage.search(new WikiSearchData("Liquibase", null, null, null)).getPageSize());
    }

}
