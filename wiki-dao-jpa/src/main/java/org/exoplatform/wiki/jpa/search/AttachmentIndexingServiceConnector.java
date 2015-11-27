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
import org.exoplatform.wiki.jpa.dao.PageAttachmentDAO;
import org.exoplatform.wiki.jpa.entity.PageAttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PermissionEntity;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 10/2/15
 */
public class AttachmentIndexingServiceConnector  extends ElasticIndexingServiceConnector {
  private static final Log LOGGER = ExoLogger.getExoLogger(AttachmentIndexingServiceConnector.class);
  public static final String TYPE = "wiki-attachment";
  private final PageAttachmentDAO attachmentDAO;

  public AttachmentIndexingServiceConnector(InitParams initParams, PageAttachmentDAO attachmentDAO) {
    super(initParams);
    this.attachmentDAO = attachmentDAO;
  }

  @Override
  public Document create(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Id is null");
    }
    //Get the wiki object from BD
    PageAttachmentEntity attachment = attachmentDAO.find(Long.parseLong(id));
    if (attachment==null) {
      LOGGER.info("The attachment entity with id {} doesn't exist.", id);
      return null;
    }

    Map<String,String> fields = new HashMap<>();
    Document doc = new Document(TYPE, id, getUrl(attachment), attachment.getUpdatedDate(),
        computePermissions(attachment), fields);
    doc.addField("title", attachment.getTitle());
    doc.addField("file", attachment.getContent());
    doc.addField("name", attachment.getName());
    doc.addField("createdDate", String.valueOf(attachment.getCreatedDate().getTime()));
    doc.addField("updatedDate", String.valueOf(attachment.getUpdatedDate().getTime()));
    PageEntity page = attachment.getPage();
    doc.addField("pageName", page.getName());
    fields.put("wikiType", page.getWiki().getType());
    fields.put("wikiOwner", page.getWiki().getOwner());

    return doc;
  }

  @Override
  public Document update(String id) {
    return create(id);
  }

  private String[] computePermissions(PageAttachmentEntity attachment) {
    List<String> permissions = new ArrayList<>();
    permissions.add(attachment.getCreator());
    //Add permissions from the wiki page
    List<PermissionEntity> pagePermission = attachment.getPage().getPermissions();
    if (pagePermission != null) {
      for(PermissionEntity permission : pagePermission) {
        if (permission.getPermissionType().equals(PermissionType.VIEWPAGE)
            || permission.getPermissionType().equals(PermissionType.VIEW_ATTACHMENT)) {
          permissions.add(permission.getIdentity());
        }
      }
    }
    String[] result = new String[permissions.size()];
    return permissions.toArray(result);
  }

  private String getUrl(PageAttachmentEntity attachment) {
    return attachment.getPage().getUrl();
  }

  @Override
  public String getMapping() {

    JSONObject notAnalyzedField = new JSONObject();
    notAnalyzedField.put("type", "string");
    notAnalyzedField.put("index", "not_analyzed");

    JSONObject file = new JSONObject();
    file.put("term_vector", "with_positions_offsets");
    file.put("store", true);

    JSONObject fields = new JSONObject();
    fields.put("file", file);

    JSONObject attachment = new JSONObject();
    attachment.put("type", "attachment");
    attachment.put("fields", fields);

    JSONObject properties = new JSONObject();
    properties.put("file", attachment);
    properties.put("permissions", notAnalyzedField);
    properties.put("url", notAnalyzedField);
    properties.put("sites", notAnalyzedField);
    //Add Wiki type and owner filter
    properties.put("wikiType", notAnalyzedField);
    properties.put("wikiOwner", notAnalyzedField);

    JSONObject mappingProperties = new JSONObject();
    mappingProperties.put("properties",properties);

    JSONObject mappingJSON = new JSONObject();
    mappingJSON.put(getType(), mappingProperties);

    return mappingJSON.toJSONString();
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {

    List<String> result;

    List<Long> ids = this.attachmentDAO.findAllIds(offset, limit);
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
