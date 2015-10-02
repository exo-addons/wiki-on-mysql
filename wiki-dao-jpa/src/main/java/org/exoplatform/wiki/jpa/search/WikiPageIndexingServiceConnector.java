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

import java.util.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addons.es.domain.Document;
import org.exoplatform.addons.es.index.elastic.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.Page;
import org.exoplatform.wiki.jpa.entity.Wiki;

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
    public Document create(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Id is null");
        }
        //Get the Page object from BD
        Page page = dao.find(Long.parseLong(id));
        if (page==null) {
            LOGGER.info("The page entity with id {} doesn't exist.", id);
            return null;
        }
        Map<String,String> fields = new HashMap<>();
        fields.put("name", page.getName());
        fields.put("content", page.getContent());
        fields.put("title", page.getTitle());
        fields.put("comment", page.getComment());
        return new Document(TYPE, id, page.getUrl(), page.getUpdateDate(), computePermissions(page), fields);
    }

    @Override
    public Document update(String id) {
        return create(id);
    }

    private String[] computePermissions(Page wiki) {
        List<String> permissions = new ArrayList<>();
        //Add the owner
        permissions.add(wiki.getOwner());
        //TODO Add the permissions
        String[] result = new String[permissions.size()];
        return permissions.toArray(result);
    }
}
