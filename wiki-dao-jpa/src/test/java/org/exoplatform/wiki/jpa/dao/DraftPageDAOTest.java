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

import org.exoplatform.wiki.jpa.BaseTest;
import org.junit.Test;

import org.exoplatform.wiki.jpa.entity.DraftPage;
import org.exoplatform.wiki.jpa.entity.Page;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
public class DraftPageDAOTest extends BaseTest {

  @Test
  public void testInsert(){
    DraftPageDAO dao = new DraftPageDAO();
    PageDAO pageDAO = new PageDAO();
    DraftPage dp = new DraftPage();
    Page page = new Page();
    page.setName("name");
    dp.setTargetPage(page);
    dao.create(dp);
    
    assertNotNull(dao.find(dp.getId()));
    assertNotNull(pageDAO.find(page.getId()));
    
    DraftPage got = dao.find(dp.getId());
    got.getTargetPage().setName("name1");
    dao.update(got);
    assertEquals("name1",page.getName());
    dao.deleteAll();
    pageDAO.deleteAll();
    
    assertNull(dao.find(dp.getId()));
  }

  @Test
  public void testFindDraftPagesByUser(){
    DraftPageDAO dao = new DraftPageDAO();
    PageDAO pageDAO = new PageDAO();
    DraftPage dp = new DraftPage();
    Page page = new Page();
    page.setName("name");
    dp.setTargetPage(page);
    dp.setAuthor("user1");
    dao.create(dp);

    assertNotNull(dao.find(dp.getId()));
    assertNotNull(pageDAO.find(page.getId()));
    List<DraftPage> user1DraftPages = dao.findDraftPagesByUser("user1");
    assertNotNull(user1DraftPages);
    assertEquals(1, user1DraftPages.size());
    List<DraftPage> user2DraftPages = dao.findDraftPagesByUser("user2");
    assertNotNull(user2DraftPages);
    assertEquals(0, user2DraftPages.size());

    dao.deleteAll();
    pageDAO.deleteAll();
  }

  @Test
  public void testFindLatestDraftPageByUser(){
    DraftPageDAO dao = new DraftPageDAO();
    PageDAO pageDAO = new PageDAO();

    Calendar calendar = Calendar.getInstance();
    Date now = calendar.getTime();
    calendar.roll(Calendar.YEAR, 1);
    Date oneYearAgo = calendar.getTime();

    Page page = new Page();
    page.setName("page1");
    DraftPage dp1 = new DraftPage();
    dp1.setTargetPage(page);
    dp1.setAuthor("user1");
    dp1.setUpdatedDate(oneYearAgo);
    dp1.setTargetRevision("1");
    dao.create(dp1);
    DraftPage dp2 = new DraftPage();
    dp2.setTargetPage(page);
    dp2.setAuthor("user1");
    dp2.setUpdatedDate(now);
    dp1.setTargetRevision("2");
    dao.create(dp2);

    assertNotNull(dao.find(dp2.getId()));
    assertNotNull(pageDAO.find(page.getId()));
    DraftPage user1DraftPage = dao.findLatestDraftPageByUser("user1");
    assertNotNull(user1DraftPage);
    assertEquals("2", user1DraftPage.getTargetRevision());
    DraftPage user2DraftPage = dao.findLatestDraftPageByUser("user2");
    assertNull(user2DraftPage);

    dao.deleteAll();
    pageDAO.deleteAll();
  }

  @Test
  public void testFindDraftPagesByUserAndTargetPage(){
    //Given
    DraftPageDAO dao = new DraftPageDAO();
    PageDAO pageDAO = new PageDAO();
    DraftPage dp = new DraftPage();
    Page page = new Page();
    page.setName("page1");
    dp.setTargetPage(page);
    Page createdPage = pageDAO.create(page);
    dp.setAuthor("user1");
    dao.create(dp);

    //When
    List<DraftPage> drafts1 = dao.findDraftPagesByUserAndTargetPage("user1", createdPage.getId());
    List<DraftPage> drafts2 = dao.findDraftPagesByUserAndTargetPage("user2", createdPage.getId());
    List<DraftPage> drafts3 = dao.findDraftPagesByUserAndTargetPage("user1", createdPage.getId() + 1);

    //Then
    assertNotNull(dao.find(dp.getId()));
    assertNotNull(pageDAO.find(page.getId()));
    assertNotNull(drafts1);
    assertEquals(1, drafts1.size());
    assertNotNull(drafts2);
    assertEquals(0, drafts2.size());
    assertNotNull(drafts3);
    assertEquals(0, drafts3.size());

    dao.deleteAll();
    pageDAO.deleteAll();
  }
}
