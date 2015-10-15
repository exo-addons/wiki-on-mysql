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
import org.exoplatform.addons.es.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.Wiki;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/9/15
 */
public class WikiIndexingServiceConnector extends ElasticIndexingServiceConnector {
    private static final Log LOGGER = ExoLogger.getExoLogger(WikiIndexingServiceConnector.class);
    public static final String TYPE = "wiki";
    private final WikiDAO dao;

    public WikiIndexingServiceConnector(InitParams initParams, WikiDAO dao) {
        super(initParams);
        this.dao = dao;
    }

    @Override
    public Document create(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Id is null");
        }
        //Get the wiki object from BD
        Wiki wiki = dao.find(Long.parseLong(id));
        if (wiki==null) {
            LOGGER.info("The wiki entity with id {} doesn't exist.", id);
            return null;
        }
        Map<String,String> fields = new HashMap<>();
        //we just want to index the field "name"
        fields.put("name", wiki.getName());
        return new Document(TYPE, id, getUrl(wiki), getCreatedDate(wiki), computePermissions(wiki), fields);
    }

    @Override
    public Document update(String id) {
        return create(id);
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

    private Date getCreatedDate(Wiki wiki) {
        //TODO
        return null;
    }

    private String getUrl(Wiki wiki) {
        //TODO
        return null;
    }

    private String[] computePermissions(Wiki wiki) {
        List<String> permissions = new ArrayList<String>();
        //Add the owner
        permissions.add(wiki.getOwner());
        //TODO Add the permissions
        String[] result = new String[permissions.size()];
        return permissions.toArray(result);
    }
}
