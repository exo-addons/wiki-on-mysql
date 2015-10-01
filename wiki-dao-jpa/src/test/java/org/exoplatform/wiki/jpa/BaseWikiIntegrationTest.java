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

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertNotEquals;

import java.util.Collections;
import java.util.Date;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.search.SearchHit;

import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.Page;
import org.exoplatform.wiki.jpa.entity.Permission;
import org.exoplatform.wiki.jpa.entity.PermissionType;
import org.exoplatform.wiki.jpa.entity.Wiki;
import org.exoplatform.wiki.jpa.search.WikiIndexingServiceConnector;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 10/1/15
 */
public abstract class BaseWikiIntegrationTest extends BaseTest {
    protected Node node;
    protected WikiDAO wikiDAO = new WikiDAO();
    protected PageDAO pageDAO = new PageDAO();
    protected IndexingService indexingService;
    protected JPADataStorage storage;

    public void setUp() {
        //Init ES
        ImmutableSettings.Builder elasticsearchSettings = ImmutableSettings.settingsBuilder()
                .put(RestController.HTTP_JSON_ENABLE, true)
                .put(InternalNode.HTTP_ENABLED, true)
                .put("network.host", "127.0.0.1")
                .put("path.data", "target/data");
        node = nodeBuilder()
                .local(true)
                .settings(elasticsearchSettings.build())
                .node();
        node.client().admin().cluster().prepareHealth()
                .setWaitForYellowStatus().execute().actionGet();
        assertNotNull(node);
        assertFalse(node.isClosed());
        deleteAllDocumentsInES();
        deleteAllWikiInBD();
        SecurityUtils.setCurrentUser("BCH", "*:/admin");
        indexingService = PortalContainer.getInstance().getComponentInstanceOfType(IndexingService.class);
        storage = PortalContainer.getInstance().getComponentInstanceOfType(JPADataStorage.class);
    }

    private void deleteAllWikiInBD() {
        pageDAO.deleteAll();
        wikiDAO.deleteAll();
    }

    private void deleteAllDocumentsInES() {
        IndicesExistsResponse exists = node.client().admin().indices().prepareExists("wiki").execute().actionGet();
        if(exists.isExists()) {
            SearchResponse docs = node.client().prepareSearch("wiki")
                    .setQuery(QueryBuilders.matchAllQuery())
                    .setTypes(WikiIndexingServiceConnector.TYPE, WikiPageIndexingServiceConnector.TYPE)
                    .execute().actionGet();
            if (docs.getHits().getHits().length>0) {
                BulkRequestBuilder bulk = node.client().prepareBulk();
                for (SearchHit hit : docs.getHits().getHits()) {
                    bulk.add(new DeleteRequest(hit.getIndex(), hit.getType(), hit.getId()));
                }
                bulk.execute().actionGet();
                node.client().admin().indices().prepareRefresh().execute().actionGet();
            }
        }
    }

    public void tearDown() {
        node.close();
    }

    // TODO This method MUST be removed : we MUST find a way to use exo-es-search Liquibase changelogs
    @ExoTransactional
    protected void setIndexingOperationTimestamp() throws NoSuchFieldException, IllegalAccessException {
        EntityManagerService emService = PortalContainer.getInstance().getComponentInstanceOfType(EntityManagerService.class);
        emService.getEntityManager()
                .createQuery("UPDATE IndexingOperation set timestamp = :now")
                .setParameter("now", new Date(0L))
                .executeUpdate();
    }

    protected Wiki indexWiki(String name) throws NoSuchFieldException, IllegalAccessException {
        Wiki wiki = new Wiki();
        wiki.setName(name);
        wiki.setOwner("BCH");
        wiki.setPermissions(Collections.singletonList(new Permission("publisher:/developers", PermissionType.VIEWPAGE)));
        wiki = wikiDAO.create(wiki);
        assertEquals(1, wikiDAO.findAll().size());
        assertNotEquals(wiki.getId(), 0);
        indexingService.index(WikiIndexingServiceConnector.TYPE, Long.toString(wiki.getId()));
        setIndexingOperationTimestamp();
        indexingService.process();
        node.client().admin().indices().prepareRefresh().execute().actionGet();
        return wiki;
    }

    protected void indexPage(String name, String title, String content, String comment)
            throws NoSuchFieldException, IllegalAccessException {
        Page page = new Page();
        page.setName(name);
        page.setTitle(title);
        page.setContent(content);
        page.setComment(comment);
        page.setOwner("BCH");
        page.setPermissions(Collections.singletonList(new Permission("publisher:/developers", PermissionType.VIEWPAGE)));
        page = pageDAO.create(page);
        assertEquals(1, pageDAO.findAll().size());
        assertNotEquals(page.getId(), 0);
        indexingService.index(WikiPageIndexingServiceConnector.TYPE, Long.toString(page.getId()));
        setIndexingOperationTimestamp();
        indexingService.process();
        node.client().admin().indices().prepareRefresh().execute().actionGet();
    }
}
