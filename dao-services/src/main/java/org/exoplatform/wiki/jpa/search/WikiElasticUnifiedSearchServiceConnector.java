/* 
* Copyright (C) 2003-2016 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.wiki.jpa.search;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.addons.es.client.ElasticSearchingClient;
import org.exoplatform.addons.es.search.ElasticSearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.Utils;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 2/15/16
 */
public class WikiElasticUnifiedSearchServiceConnector extends ElasticSearchServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(WikiElasticUnifiedSearchServiceConnector.class);

  private WikiService wikiService;

  public WikiElasticUnifiedSearchServiceConnector(InitParams initParams, ElasticSearchingClient client, WikiService wikiService) {
    super(initParams, client);
    this.wikiService = wikiService;
  }

  @Override
  protected String getSourceFields() {

    List<String> fields = new ArrayList<>();
    fields.add(getTitleElasticFieldName());
    fields.add("wikiType");
    fields.add("wikiOwner");
    fields.add("name");
    fields.add("pageName");

    List<String> sourceFields = new ArrayList<>();
    for (String sourceField: fields) {
      sourceFields.add("\"" + sourceField + "\"");
    }

    return StringUtils.join(sourceFields, ",");
  }

  @Override
  protected String getUrlFromJsonResult(JSONObject hitSource, SearchContext context) {

    String wikiType = (String) hitSource.get("wikiType");
    String wikiOwner = (String) hitSource.get("wikiOwner");

    // if pageName exists, it is an attachment and pageName must be used for the page url
    // otherwise, it is a page and the name must be used for the page url
    String pageName = (String) hitSource.get("pageName");
    if(StringUtils.isEmpty(pageName)) {
      pageName = (String) hitSource.get("name");
    }

    StringBuffer permalink = new StringBuffer();
    try {

      //Build the eXo Platform base URL common to all Wiki
      String portalContainerName = Utils.getPortalName();
      String portalOwner = context.getSiteName();
      String wikiWebappUri = wikiService.getWikiWebappUri();
      permalink.append("/");
      permalink.append(portalContainerName);
      permalink.append("/");
      permalink.append(portalOwner);
      permalink.append("/");
      permalink.append(wikiWebappUri);

      //Add User or Group ID to the url according to the wiki Type
      if (wikiType.equalsIgnoreCase(WikiType.GROUP.toString())) {
        permalink.append("/");
        permalink.append(PortalConfig.GROUP_TYPE);
        permalink.append(wikiOwner);
      } else if (wikiType.equalsIgnoreCase(WikiType.USER.toString())) {
        permalink.append("/");
        permalink.append(PortalConfig.USER_TYPE);
        permalink.append("/");
        permalink.append(wikiOwner);
      }

      //Add the wiki page name to the URL
      permalink.append("/");
      permalink.append(pageName);

    } catch (Exception ex) {
      LOG.info("Can not build the permalink for wiki page ", ex);
    }
    return permalink.toString();
  }

}

