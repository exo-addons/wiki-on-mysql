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

import java.util.List;

import org.junit.Test;

import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.entity.DraftPage;
import org.exoplatform.wiki.jpa.entity.Page;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
public class DraftPageDAOTest extends BaseWikiJPAIntegrationTest {

  @Test
  public void testInsert(){;
    DraftPage dp = new DraftPage();
    Page page = new Page();
    page.setName("name");
    dp.setTargetPage(page);
    draftPageDAO.create(dp);
    
    assertNotNull(draftPageDAO.find(dp.getId()));
    assertNotNull(pageDAO.find(page.getId()));
    
    DraftPage got = draftPageDAO.find(dp.getId());
    got.getTargetPage().setName("name1");
    draftPageDAO.update(got);
    assertEquals("name1",page.getName());
    draftPageDAO.deleteAll();
    pageDAO.deleteAll();
    
    assertNull(draftPageDAO.find(dp.getId()));
  }

  @Test
  public void testFindDraftPagesByUser(){
    DraftPage dp = new DraftPage();
    Page page = new Page();
    page.setName("name");
    dp.setTargetPage(page);
    dp.setAuthor("user1");
    draftPageDAO.create(dp);

    assertNotNull(draftPageDAO.find(dp.getId()));
    assertNotNull(pageDAO.find(page.getId()));
    List<DraftPage> user1DraftPages = draftPageDAO.findDraftPagesByUser("user1");
    assertNotNull(user1DraftPages);
    assertEquals(1, user1DraftPages.size());
    List<DraftPage> user2DraftPages = draftPageDAO.findDraftPagesByUser("user2");
    assertNotNull(user2DraftPages);
    assertEquals(0, user2DraftPages.size());
  }
}
