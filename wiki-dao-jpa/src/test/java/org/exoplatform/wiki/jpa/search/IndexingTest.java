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

import java.io.IOException;

import org.exoplatform.wiki.jpa.BaseWikiIntegrationTest;
import org.exoplatform.wiki.jpa.entity.Page;
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

    public void testReindexingWikiAndSearch() throws NoSuchFieldException, IllegalAccessException, IOException {
        //Given
        Wiki wiki = indexWiki("RDBMS Guidelines");
        wiki.setName("Liquibase Guidelines");
        wikiDAO.update(wiki);
        wiki = indexWiki("RDBMS Stats");
        wiki.setName("Liquibase Stats");
        wikiDAO.update(wiki);
        assertEquals(2, wikiDAO.findAll().size());
        //When
        indexingService.reindexAll(WikiIndexingServiceConnector.TYPE);
        setIndexingOperationTimestamp();
        indexingService.process();
        node.client().admin().indices().prepareRefresh().execute().actionGet();
        //Then
        assertEquals(2, storage.search(new WikiSearchData("Liquibase", null, null, null)).getPageSize());
    }

    public void testReindexingWikiPagesAndSearch() throws NoSuchFieldException, IllegalAccessException, IOException {
        //Given
        Page page = indexPage("RDBMS Guidelines", "RDBMS Guidelines", "All the guidelines you need", "Draft version");
        page.setName("Liquibase Guidelines");
        page.setTitle("Liquibase Guidelines");
        pageDAO.update(page);
        page = indexPage("RDBMS Stats", "RDBMS Stats", "All the stats you need", "Draft version");
        page.setName("Liquibase Stats");
        page.setTitle("Liquibase Stats");
        pageDAO.update(page);
        assertEquals(2, pageDAO.findAll().size());
        //When
        indexingService.reindexAll(WikiPageIndexingServiceConnector.TYPE);
        setIndexingOperationTimestamp();
        indexingService.process();
        node.client().admin().indices().prepareRefresh().execute().actionGet();
        //Then
        assertEquals(2, storage.search(new WikiSearchData("Liquibase", null, null, null)).getPageSize());
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

    public void testIndexingAndSearchingOfAttachment() throws NoSuchFieldException, IllegalAccessException, IOException {
        //Given
        //When
        indexAttachment("Scrum @eXo - Collector", "src/test/resources/AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf", "www.exo.com");
        //Then
        assertEquals(1, storage.search(new WikiSearchData("Collector", null, null, null)).getPageSize()); //Title
        assertEquals(1, storage.search(new WikiSearchData("Agile", null, null, null)).getPageSize()); //Content
    }

}
