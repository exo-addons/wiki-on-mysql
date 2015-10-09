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


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.jpa.entity.Wiki;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 7/31/15
 */
public class WikiDAOTest extends BaseTest {
  private final WikiDAO dao = new WikiDAO();

  @Before
  public void setUp() {
    super.setUp();
    dao.deleteAll();
  }

  @Test
  public void testFindAllIds() {
    //Given
    dao.create(new Wiki().setName("My wiki #1"));
    dao.create(new Wiki().setName("My wiki #2"));
    //When
    List<Long> ids = dao.findAllIds();
    //Then
    assertThat(ids.size(), is(2));
  }
}
