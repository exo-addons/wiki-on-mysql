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

package org.exoplatform.wiki.jpa.test.dao;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.entity.Page;

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
  }
}
