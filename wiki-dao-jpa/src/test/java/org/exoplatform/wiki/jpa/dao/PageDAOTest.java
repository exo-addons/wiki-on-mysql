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

import org.exoplatform.wiki.jpa.BaseTest;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.wiki.jpa.entity.Page;
import org.exoplatform.wiki.jpa.entity.Permission;
import org.exoplatform.wiki.jpa.entity.PermissionType;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 7/31/15
 */
public class PageDAOTest extends BaseTest {
  private final PageDAO dao = new PageDAO();

  @Before
  public void clean() {
    dao.deleteAll();
  }

  @Test
  public void testGetRemovedPages() {
    // Given
    Page parentPage = new Page();
    Page page = new Page();
    page.setParentPage(parentPage);
    page = dao.create(page);
    parentPage = page.getParentPage(); //get persisted parentPage with generated ID
    assertEquals(2, dao.findAll().size());
    dao.delete(page);
    assertEquals(1, dao.findAll().size());
    // When
    List<Page> removedPages = dao.findRemovedPages(parentPage);
    // Then
    assertEquals(1, removedPages.size());
    // Clean
    dao.deleteAll();
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
    page.setPermission(permissions);
    
    page.setAuthor("author");
    page.setContent("content");
    page.setComment("comment");
    page.setCreateDate(new Date());
    page.setName("name");
    page.setMinorEdit(true);
    page.setOwner("owner");
    page.setSyntax("syntax");
    page.setTitle("title");
    page.setUrl("url");
    // When
    dao.create(page);
    Page got = dao.find(page.getId());
    // Then
    assertNotNull(got);
    if(got == null) return;
    assertEquals("name", got.getName());
    // Clean
    dao.deleteAll();
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
    page.setPermission(permissions);
    
    page.setAuthor("author");
    page.setContent("content");
    page.setComment("comment");
    page.setCreateDate(new Date());
    page.setName("name");
    page.setMinorEdit(true);
    page.setOwner("owner");
    page.setSyntax("syntax");
    page.setTitle("title");
    page.setUrl("url");
    // When
    dao.create(page);
    int size1 = dao.getAllHistory(page).size();
    int version1 = dao.getCurrentVersion(page);
    Page got = dao.find(page.getId());
    got.setName("name2");
    dao.update(got);
    assertEquals(size1 + 1, dao.getAllHistory(got).size());
    
    Page oldVersion = dao.getPageAtRevision(got, version1);
    assertEquals("name", oldVersion.getName());
    // Clean
    dao.deleteAll();
  }
}
