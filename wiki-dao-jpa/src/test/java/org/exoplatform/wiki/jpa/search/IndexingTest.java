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

import org.exoplatform.addons.es.index.impl.ElasticIndexingOperationProcessor;
import org.exoplatform.wiki.jpa.BaseWikiIntegrationTest;
import org.exoplatform.wiki.jpa.entity.AttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.service.search.WikiSearchData;

import java.io.IOException;

import static org.junit.Assert.assertNotEquals;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 9/14/15
 */
public class IndexingTest extends BaseWikiIntegrationTest {

  public void testReindexingWikiAndSearch() throws NoSuchFieldException, IllegalAccessException, IOException {
    // Given
    WikiEntity wiki1 = indexWiki("RDBMS Guidelines", "BCH", null);
    wiki1.setName("Liquibase Guidelines");
    wikiDAO.update(wiki1);
    WikiEntity wiki2 = indexWiki("RDBMS Stats", "BCH", null);
    wiki2.setName("Liquibase Stats");
    wikiDAO.update(wiki2);
    assertEquals(2, wikiDAO.findAll().size());
    // When
    indexingService.reindexAll(WikiIndexingServiceConnector.TYPE);
    indexingOperationProcessor.process();
    // Second time because operations were reinjected in the queue
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    // Then
    assertEquals(2, storage.search(new WikiSearchData("Liquibase", null, null, null)).getPageSize());
  }

  public void testReindexing_isProcessedAsBatch() throws NoSuchFieldException, IllegalAccessException, IOException {
    // Given
    WikiEntity wiki = new WikiEntity();
    wiki.setName("RDBMS Guidelines");
    wiki.setOwner("BCH");
    wiki = wikiDAO.create(wiki);
    assertNotEquals(wiki.getId(), 0);
    WikiEntity wiki2 = new WikiEntity();
    wiki2.setName("Liquibase Guidelines");
    wiki2.setOwner("BCH");
    wiki2 = wikiDAO.create(wiki2);
    assertNotEquals(wiki2.getId(), 0);
    WikiEntity wiki3 = new WikiEntity();
    wiki3.setName("Logs Guidelines");
    wiki3.setOwner("BCH");
    wiki3 = wikiDAO.create(wiki3);
    assertNotEquals(wiki3.getId(), 0);
    assertEquals(3, wikiDAO.findAll().size());
    ((ElasticIndexingOperationProcessor) indexingOperationProcessor).setReindexBatchSize(2);
    // When
    indexingService.reindexAll(WikiIndexingServiceConnector.TYPE);
    indexingOperationProcessor.process();
    // Second time because operations were reinjected in the queue
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    // Then
    assertEquals(3, storage.search(new WikiSearchData("Guidelines", null, null, null)).getPageSize());
  }

  public void testReindexingWikiPagesAndSearch() throws NoSuchFieldException, IllegalAccessException, IOException {
    // Given
    PageEntity page = indexPage("RDBMS Guidelines",
                                "RDBMS Guidelines",
                                "All the guidelines you need",
                                "Draft version",
                                "BCH",
                                null);
    page.setName("Liquibase Guidelines");
    page.setTitle("Liquibase Guidelines");
    pageDAO.update(page);
    page = indexPage("RDBMS Stats", "RDBMS Stats", "All the stats you need", "Draft version", "BCH", null);
    page.setName("Liquibase Stats");
    page.setTitle("Liquibase Stats");
    pageDAO.update(page);
    assertEquals(2, pageDAO.findAll().size());
    // When
    indexingService.reindexAll(WikiPageIndexingServiceConnector.TYPE);
    indexingOperationProcessor.process();
    // Second time because operations were reinjected in the queue
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    // Then
    assertEquals(2, storage.search(new WikiSearchData("Liquibase", null, null, null)).getPageSize());
  }

  public void testUpdatingWiki() throws NoSuchFieldException, IllegalAccessException {
    // Given
    WikiEntity wiki = indexWiki("RDBMS Guidelines", "BCH", null);
    assertEquals(0, storage.search(new WikiSearchData("Liquibase", null, null, null)).getPageSize());
    // When
    wiki.setName("Liquibase Guidelines");
    wikiDAO.update(wiki);
    assertEquals(1, wikiDAO.findAll().size());
    indexingService.reindex(WikiIndexingServiceConnector.TYPE, Long.toString(wiki.getId()));
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    // Then
    assertEquals(1, storage.search(new WikiSearchData("Liquibase", null, null, null)).getPageSize());
  }

  public void testReindexingAttachmentAndSearch() throws NoSuchFieldException, IllegalAccessException, IOException {
    // Given
    AttachmentEntity attachment1 = indexAttachment("Scrum @eXo - Collector",
        fileResource.getPath(),
        "www.exo.com",
        "BCH",
        null);
    attachmentDAO.create(attachment1);
    AttachmentEntity attachment2 = indexAttachment("Scrum @eXo - Collector",
        fileResource.getPath(),
        "www.exo.com",
        "BCH",
        null);
    attachmentDAO.create(attachment2);
    assertEquals(2, attachmentDAO.findAll().size());
    // When
    indexingService.reindexAll(AttachmentIndexingServiceConnector.TYPE);
    indexingOperationProcessor.process();
    // Second time because operations were reinjected in the queue
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    // Then
    assertEquals(2, storage.search(new WikiSearchData("RDBMS", null, null, null)).getPageSize());
  }

}
