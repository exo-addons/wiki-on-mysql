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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.wiki.jpa.BaseWikiIntegrationTest;
import org.exoplatform.wiki.jpa.entity.Wiki;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 7/31/15
 */
public class WikiDAOTest extends BaseWikiIntegrationTest {
  private final WikiDAO dao = new WikiDAO();

  @Before
  public void setUp() {
    super.setUp();
    dao.deleteAll();
  }

  @After
  public void tearDown()  {
    dao.deleteAll();
  }

  @Test
  public void testFindAllIds() {
    //Given
    dao.create(new Wiki().setName("My wiki #1"));
    dao.create(new Wiki().setName("My wiki #2"));
    //When
    List<Long> ids = dao.findAllIds(0, 10);
    //Then
    assertThat(ids.size(), is(2));
  }

  @Test
  public void testFindWikiByTypeAndOwner() {
    //Given
    dao.create(new Wiki().setName("My wiki #1").setType("portal").setOwner("wiki1"));
    dao.create(new Wiki().setName("My wiki #2").setType("portal").setOwner("wiki2"));
    //When
    Wiki wiki1 = dao.getWikiByTypeAndOwner("portal", "wiki1");
    Wiki wiki2 = dao.getWikiByTypeAndOwner("group", "wiki1");
    Wiki wiki3 = dao.getWikiByTypeAndOwner("portal", "wiki3");
    //Then
    assertNotNull(wiki1);
    assertNull(wiki2);
    assertNull(wiki3);
  }

  @Test
  public void testFindWikisByType() {
    //Given
    dao.create(new Wiki().setName("My wiki #1").setType("portal").setOwner("wiki1"));
    dao.create(new Wiki().setName("My wiki #2").setType("portal").setOwner("wiki2"));
    dao.create(new Wiki().setName("My wiki #3").setType("group").setOwner("wiki3"));
    //When
    List<Wiki> portalWikis = dao.getWikisByType("portal");
    List<Wiki> groupWikis = dao.getWikisByType("group");
    List<Wiki> userWikis = dao.getWikisByType("user");
    //Then
    assertNotNull(portalWikis);
    assertEquals(2, portalWikis.size());
    assertNotNull(groupWikis);
    assertEquals(1, groupWikis.size());
    assertNotNull(userWikis);
    assertEquals(0, userWikis.size());
  }

  @Test
  public void testCreateWiki() {
    //Given
    //When
    Wiki wiki1 = dao.create(new Wiki().setType("portal").setOwner("wiki1"));
    //Then
    assertNotNull(wiki1);
    assertEquals("portal", wiki1.getType());
    assertEquals("wiki1", wiki1.getOwner());
    assertNotNull(dao.getWikiByTypeAndOwner("portal", "wiki1"));
  }
}
