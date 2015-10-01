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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.addons.es.domain.Document;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/8/15
 */
public class JPADataStorageTest extends BaseTest {

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
        initMapping();
        SecurityUtils.setCurrentUser("BCH", "*:/admin");
    }

    private void initMapping() {
        String mapping = "{ \"properties\" : " +
                "   {\"permissions\" : " +
                "       {\"type\" : \"string\", \"index\" : \"not_analyzed\"}" +
                "   }" +
                "}";
        CreateIndexRequestBuilder cirb = node.client().admin().indices().prepareCreate("wiki")
                .addMapping("wiki", mapping)
                .addMapping("wiki-page", mapping);
        cirb.execute().actionGet();
    }

    public void tearDown() {
        node.client().admin().indices().prepareDelete("wiki").execute().actionGet();
        node.close();
    }

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

    //TODO replace with a call to exo-es-search indexer
    private void indexWiki(String name) throws JsonProcessingException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        Document wikiDocument = new Document();
        wikiDocument.setLastUpdatedDate(new Date());
        wikiDocument.setId("1");
        wikiDocument.setType("wiki");
        Map<String, String> fields = new HashMap<>();
        fields.put("name", name);
        wikiDocument.setFields(fields);
        wikiDocument.setPermissions(new String[]{"BCH"});
        IndexRequest indexRequest = new IndexRequest("wiki", "wiki", "1");
        String jsonString = mapper.writeValueAsString(wikiDocument);
        indexRequest.source(jsonString);
        node.client().prepareBulk().add(indexRequest).execute().actionGet();
        //Forcing ES to refresh index
        node.client().admin().indices().prepareRefresh().execute().actionGet();
    }

    //TODO replace with a call to exo-es-search indexer
    private void indexPage(String name, String title, String content, String comment)
            throws JsonProcessingException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        Document wikiDocument = new Document();
        wikiDocument.setLastUpdatedDate(new Date());
        wikiDocument.setId("1");
        wikiDocument.setType("wiki-page");
        Map<String, String> fields = new HashMap<>();
        fields.put("name", name);
        fields.put("title", title);
        fields.put("content", content);
        fields.put("comment", comment);
        wikiDocument.setFields(fields);
        wikiDocument.setPermissions(new String[]{"BCH"});
        IndexRequest indexRequest = new IndexRequest("wiki", "wiki-page", "1");
        String jsonString = mapper.writeValueAsString(wikiDocument);
        indexRequest.source(jsonString);
        node.client().prepareBulk().add(indexRequest).execute().actionGet();
        //Forcing ES to refresh index
        node.client().admin().indices().prepareRefresh().execute().actionGet();
    }
}
