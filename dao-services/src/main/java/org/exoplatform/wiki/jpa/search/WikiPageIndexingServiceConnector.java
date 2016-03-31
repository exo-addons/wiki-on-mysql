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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.addons.es.domain.Document;
import org.exoplatform.addons.es.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PermissionEntity;
import org.exoplatform.wiki.mow.api.WikiType;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/9/15
 */
public class WikiPageIndexingServiceConnector extends ElasticIndexingServiceConnector {
    private static final Log LOGGER = ExoLogger.getExoLogger(WikiPageIndexingServiceConnector.class);
    public static final String TYPE = "wiki-page";
    private final PageDAO dao;

    public WikiPageIndexingServiceConnector(InitParams initParams, PageDAO dao) {
        super(initParams);
        this.dao = dao;
    }

  @Override
  public String getMapping() {

    JSONObject notAnalyzedField = new JSONObject();
    notAnalyzedField.put("type", "string");
    notAnalyzedField.put("index", "not_analyzed");

    //Use Fast Vector Highlighter
    JSONObject fastVectorHighlighterField = new JSONObject();
    fastVectorHighlighterField.put("type", "string");
    fastVectorHighlighterField.put("term_vector", "with_positions_offsets");
    fastVectorHighlighterField.put("store", Boolean.valueOf(true));
    //Use Posting Highlighter
    JSONObject postingHighlighterField = new JSONObject();
    postingHighlighterField.put("type", "string");
    postingHighlighterField.put("index_options", "offsets");

    JSONObject properties = new JSONObject();
    properties.put("permissions", notAnalyzedField);
    properties.put("url", notAnalyzedField);
    properties.put("sites", notAnalyzedField);
    properties.put("wikiType", notAnalyzedField);
    properties.put("wikiOwner", notAnalyzedField);

    properties.put("name", postingHighlighterField);
    properties.put("title", postingHighlighterField);
    properties.put("content", fastVectorHighlighterField);
    properties.put("comment", postingHighlighterField);

    JSONObject mappingProperties = new JSONObject();
    mappingProperties.put("properties", properties);

    JSONObject mappingJSON = new JSONObject();
    mappingJSON.put(this.getType(), mappingProperties);

    return mappingJSON.toJSONString();
  }

    @Override
    public Document create(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Id is null");
        }
        //Get the Page object from BD
        PageEntity page = dao.find(Long.parseLong(id));
        if (page==null) {
            LOGGER.info("The page entity with id {} doesn't exist.", id);
            return null;
        }

        Map<String,String> fields = new HashMap<>();
        fields.put("owner", page.getOwner());
        fields.put("name", page.getName());
        fields.put("content", page.getContent());
        fields.put("title", page.getTitle());
        fields.put("createdDate", String.valueOf(page.getCreatedDate().getTime()));
        fields.put("updatedDate", String.valueOf(page.getUpdatedDate().getTime()));
        fields.put("comment", page.getComment());
        fields.put("wikiType", page.getWiki().getType());
        String wikiOwner = page.getWiki().getOwner();
        //We need to add the first "/" on the wiki owner if it's  wiki group
        if (page.getWiki().getType().toUpperCase().equals(WikiType.GROUP.name())) {
          wikiOwner = dao.validateGroupWikiOwner(wikiOwner);
        }
        fields.put("wikiOwner", wikiOwner);

        return new Document(TYPE, id, page.getUrl(), page.getUpdatedDate(), computePermissions(page), fields);
    }

    @Override
    public Document update(String id) {
        return create(id);
    }

    private String[] computePermissions(PageEntity page) {
        List<String> permissions = new ArrayList<>();
        //Add the owner
        permissions.add(page.getOwner());
        //Add permissions
        if (page.getPermissions()!=null) {
            for (PermissionEntity permission : page.getPermissions()) {
                permissions.add(permission.getIdentity());
            }
        }
        String[] result = new String[permissions.size()];
        return permissions.toArray(result);
    }

    @Override
    public List<String> getAllIds(int offset, int limit) {
        List<String> result;

        List<Long> ids = this.dao.findAllIds(offset, limit);
        if (ids==null) {
            result = new ArrayList<>(0);
        } else {
            result = new ArrayList<>(ids.size());
            for (Long id : ids) {
                result.add(String.valueOf(id));
            }
        }
        return result;
    }
}
