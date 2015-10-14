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


import org.exoplatform.wiki.jpa.BaseWikiIntegrationTest;
import org.exoplatform.wiki.jpa.entity.Template;
import org.exoplatform.wiki.jpa.entity.Wiki;
import org.junit.Test;

import java.util.List;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 7/31/15
 */
public class TemplateDAOTest extends BaseWikiIntegrationTest {

  @Test
  public void testGetTemplateOfWikiByName() {
    //Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    Template template = new Template();
    template.setWiki(wiki);
    template.setName("template1");
    template.setTitle("Template 1");
    template.setContent("Template 1 Content");
    templateDAO.create(template);

    //When
    Template fetchedTemplate1 = templateDAO.getTemplateOfWikiByName("portal", "wiki1", "template1");
    Template fetchedTemplate2 = templateDAO.getTemplateOfWikiByName("portal", "wiki1", "template2");
    Template fetchedTemplate1OfWiki2 = templateDAO.getTemplateOfWikiByName("portal", "wiki2", "template1");

    //Then
    assertEquals(1, templateDAO.findAll().size());
    assertNotNull(fetchedTemplate1);
    assertEquals("portal", fetchedTemplate1.getWiki().getType());
    assertEquals("wiki1", fetchedTemplate1.getWiki().getOwner());
    assertEquals("template1", fetchedTemplate1.getName());
    assertEquals("Template 1", fetchedTemplate1.getTitle());
    assertEquals("Template 1 Content", fetchedTemplate1.getContent());
    assertNull(fetchedTemplate2);
    assertNull(fetchedTemplate1OfWiki2);
  }


  @Test
  public void testGetTemplatesOfWiki() {
    //Given
    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    Template template1 = new Template();
    template1.setWiki(wiki);
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    templateDAO.create(template1);

    Template template2 = new Template();
    template2.setWiki(wiki);
    template2.setName("template2");
    template2.setTitle("Template 2");
    template2.setContent("Template 2 Content");
    templateDAO.create(template2);

    //When
    List<Template> templatesWiki1 = templateDAO.getTemplatesOfWiki("portal", "wiki1");
    List<Template> templatesWiki2 = templateDAO.getTemplatesOfWiki("portal", "wiki2");

    //Then
    assertEquals(2, templateDAO.findAll().size());
    assertNotNull(templatesWiki1);
    assertEquals(2, templatesWiki1.size());
    assertNotNull(templatesWiki2);
    assertEquals(0, templatesWiki2.size());
  }

}
