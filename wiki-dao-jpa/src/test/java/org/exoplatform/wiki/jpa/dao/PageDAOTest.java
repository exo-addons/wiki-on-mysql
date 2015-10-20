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

package org.exoplatform.wiki.jpa.dao;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.junit.Test;

import org.exoplatform.wiki.jpa.BaseWikiIntegrationTest;
import org.exoplatform.wiki.jpa.entity.Page;
import org.exoplatform.wiki.jpa.entity.Permission;
import org.exoplatform.wiki.jpa.entity.PermissionType;
import org.exoplatform.wiki.jpa.entity.Wiki;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 7/31/15
 */
public class PageDAOTest extends BaseWikiJPAIntegrationTest {

  @Test
  public void testPageOfWikiByName() {
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    Page parentPage = new Page();

    Page page = new Page();
    page.setWiki(wiki);
    page.setParentPage(parentPage);
    page.setName("page1");
    page.setTitle("Page 1");

    pageDAO.create(page);

    assertEquals(2, pageDAO.findAll().size());

    Page pageOfWikiByName = pageDAO.getPageOfWikiByName("portal", "wiki1", "page1");
    assertNotNull(pageOfWikiByName);
    assertEquals("portal", pageOfWikiByName.getWiki().getType());
    assertEquals("wiki1", pageOfWikiByName.getWiki().getOwner());
    assertEquals("page1", pageOfWikiByName.getName());
    assertEquals("Page 1", pageOfWikiByName.getTitle());
  }


  @Test
  public void testGetRemovedPages() {
    // Given
    Page parentPage = new Page();
    Page page = new Page();
    page.setName("page1");
    page.setTitle("Page 1");
    page.setParentPage(parentPage);
    page = pageDAO.create(page);
    parentPage = page.getParentPage(); //get persisted parentPage with generated ID
    assertEquals(2, pageDAO.findAll().size());
    pageDAO.delete(page);
    assertEquals(1, pageDAO.findAll().size());
    // When
    List<Page> removedPages = pageDAO.findRemovedPages(parentPage);
    // Then
    assertEquals(1, removedPages.size());
    assertEquals("page1", removedPages.get(0).getName());
    assertEquals("Page 1", removedPages.get(0).getTitle());
  }

  @Test
  public void testInsert(){
    //Given
    Page page = new Page();
    Permission per = new Permission();
    per.setUser("user");
    per.setType(PermissionType.EDITPAGE);
    List<Permission> permissions = new ArrayList<Permission>();
    permissions.add(per);
    page.setPermissions(permissions);
    
    page.setAuthor("author");
    page.setContent("content");
    page.setComment("comment");
    page.setCreatedDate(new Date());
    page.setName("name");
    page.setMinorEdit(true);
    page.setOwner("owner");
    page.setSyntax("syntax");
    page.setTitle("title");
    page.setUrl("url");
    // When
    pageDAO.create(page);
    Page got = pageDAO.find(page.getId());
    // Then
    assertNotNull(got);
    if(got == null) return;
    assertEquals("name", got.getName());
  }

  @Test
  public void testAudit(){
    //Given
    Page page = new Page();
    Permission per = new Permission();
    per.setUser("user");
    per.setType(PermissionType.EDITPAGE);
    List<Permission> permissions = new ArrayList<Permission>();
    permissions.add(per);
    page.setPermissions(permissions);
    
    page.setAuthor("author");
    page.setContent("content");
    page.setComment("comment");
    page.setCreatedDate(new Date());
    page.setName("name");
    page.setMinorEdit(true);
    page.setOwner("owner");
    page.setSyntax("syntax");
    page.setTitle("title");
    page.setUrl("url");
    // When
    pageDAO.create(page);
    int size1 = pageDAO.getAllHistory(page).size();
    int version1 = pageDAO.getCurrentVersion(page);
    Page got = pageDAO.find(page.getId());
    got.setName("name2");
    pageDAO.update(got);
    assertEquals(size1 + 1, pageDAO.getAllHistory(got).size());
    
    Page oldVersion = pageDAO.getPageAtRevision(got, version1);
    assertEquals("name", oldVersion.getName());
  }
}
