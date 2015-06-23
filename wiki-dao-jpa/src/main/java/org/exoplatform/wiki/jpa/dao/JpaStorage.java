/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.jpa.dao;

import java.io.InputStream;
import java.util.List;

import org.chromattic.api.ChromatticSession;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.DataStorage;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015  
 */
public class JpaStorage implements DataStorage {

  @Override
  public PageList<SearchResult> search(ChromatticSession session, WikiSearchData data) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public InputStream getAttachmentAsStream(String path, ChromatticSession session) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<SearchResult> searchRenamedPage(ChromatticSession session, WikiSearchData data) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<TemplateSearchResult> searchTemplate(ChromatticSession session,
                                                   TemplateSearchData data) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Page getWikiPageByUUID(ChromatticSession session, String uuid) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
