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

import java.util.Collection;

import org.chromattic.api.ChromatticSession;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiStore;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.WikiContainer;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015  
 */
public class WikiStoreImpl implements WikiStore{

  @Override
  public Collection<Wiki> getWikis() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Wiki getWiki(WikiType wikiType, String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addWiki(WikiType wikiType, String name) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public <W extends Wiki> WikiContainer<W> getWikiContainer(WikiType wikiType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PageImpl getDraftNewPagesContainer() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PageImpl createPage() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setSession(ChromatticSession chromatticSession) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ChromatticSession getSession() {
    // TODO Auto-generated method stub
    return null;
  }

}
