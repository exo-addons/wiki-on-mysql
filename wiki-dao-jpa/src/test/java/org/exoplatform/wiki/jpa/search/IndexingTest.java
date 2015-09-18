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

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Date;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestController;

import org.exoplatform.addons.es.domain.OperationType;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.jpa.SecurityUtils;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.Permission;
import org.exoplatform.wiki.jpa.entity.PermissionType;
import org.exoplatform.wiki.jpa.entity.Wiki;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/14/15
 */
public class IndexingTest extends BaseTest {

    private Node node;

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
//        initMapping();
        SecurityUtils.setCurrentUser("BCH", "*:/admin");
    }

    public void tearDown() {
        node.client().admin().indices().prepareDelete("wiki").execute().actionGet();
        node.close();
    }

    public void testCreateIndex_addCreateOperation_sizeOfWikiIs1() throws NoSuchFieldException, IllegalAccessException {
        //Given
        Wiki wiki = new Wiki();
        wiki.setName("My Wiki");
        wiki.setOwner("BCH");
        wiki.setPermissions(Arrays.asList(new Permission("publisher:/developers", PermissionType.VIEWPAGE)));
        WikiDAO dao = new WikiDAO();
        wiki = dao.create(wiki);
        assertThat(dao.findAll().size(), is(1));
        assertNotEquals(wiki.getId(), 0);
        IndexingService indexingService = PortalContainer.getInstance().getComponentInstanceOfType(IndexingService.class);
        indexingService.addToIndexingQueue("wiki", Long.toString(wiki.getId()), OperationType.CREATE);
        setIndexingOperationTimestamp();
        //When
        indexingService.process();
        node.client().admin().indices().prepareRefresh().execute().actionGet();
        //Then
        assertThat(getNumberOfDocInIndex("wiki"), is(1L));
    }

    // TODO This method MUST be removed : we MUST find a way to use exo-es-search Liquibase changelogs
    @ExoTransactional
    private void setIndexingOperationTimestamp() throws NoSuchFieldException, IllegalAccessException {
        EntityManagerService emService = PortalContainer.getInstance().getComponentInstanceOfType(EntityManagerService.class);
        emService.getEntityManager()
                .createQuery("UPDATE IndexingOperation set timestamp = :now")
                .setParameter("now", new Date(0L))
                .executeUpdate();
    }

    private long getNumberOfDocInIndex(String wiki) {
        return node.client().prepareCount("wiki").execute().actionGet().getCount();
    }
}
