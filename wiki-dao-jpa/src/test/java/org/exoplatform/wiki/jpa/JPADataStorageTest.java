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

package org.exoplatform.wiki.jpa;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.EmotionIcon;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Template;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/8/15
 */
public class JPADataStorageTest extends BaseWikiIntegrationTest {

  @Test
  public void testSearchWikiByName() throws Exception {
    //Given
    indexWiki("My Wiki");
    JPADataStorage storage = new JPADataStorage();
    WikiSearchData searchData = new WikiSearchData("My Wiki", null, null, null);
    //When
    PageList<SearchResult> results = storage.search(searchData);
    //Then
    assertEquals(1, results.getAll().size());
  }

  @Test
  public void testSearchPageByName() throws Exception {
    //Given
    indexPage("My Page", "My Page", "This is the content of my Page", "This is a comment");
    JPADataStorage storage = new JPADataStorage();
    WikiSearchData searchData = new WikiSearchData("Page", null, null, null);
    //When
    PageList<SearchResult> results = storage.search(searchData);
    //Then
    assertEquals(1, results.getAll().size());
  }

  //TODO test search on all the fields
  //TODO test with wrong field in the configuration

  @Test
  public void testParentPageOfPage() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page parentPage = new Page();
    parentPage.setWikiId(wiki.getId());
    parentPage.setWikiType(wiki.getType());
    parentPage.setWikiOwner(wiki.getOwner());
    parentPage.setName("page0");
    parentPage.setTitle("Page 0");

    Page page = new Page();
    page.setWikiId(wiki.getId());
    page.setWikiType(wiki.getType());
    page.setWikiOwner(wiki.getOwner());
    page.setName("page1");
    page.setTitle("Page 1");

    //When
    storage.createPage(wiki, wiki.getWikiHome(), parentPage);
    storage.createPage(wiki, parentPage, page);
    Page pageOfWikiByName = storage.getPageOfWikiByName("portal", "wiki1", "page1");

    // Then
    assertEquals(3, pageDAO.findAll().size());
    assertNotNull(pageOfWikiByName);
    assertEquals("portal", pageOfWikiByName.getWikiType());
    assertEquals("wiki1", pageOfWikiByName.getWikiOwner());
    assertEquals("page1", pageOfWikiByName.getName());
    assertEquals("Page 1", pageOfWikiByName.getTitle());
  }

  @Test
  public void testChildrenPagesOfPage() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page parentPage = new Page();
    parentPage.setWikiId(wiki.getId());
    parentPage.setWikiType(wiki.getType());
    parentPage.setWikiOwner(wiki.getOwner());
    parentPage.setName("page0");
    parentPage.setTitle("Page 0");

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    Page page2 = new Page();
    page2.setWikiId(wiki.getId());
    page2.setWikiType(wiki.getType());
    page2.setWikiOwner(wiki.getOwner());
    page2.setName("page2");
    page2.setTitle("Page 2");

    //When
    storage.createPage(wiki, wiki.getWikiHome(), parentPage);
    storage.createPage(wiki, parentPage, page1);
    storage.createPage(wiki, parentPage, page2);
    List<Page> childrenPages = storage.getChildrenPageOf(parentPage);

    // Then
    assertEquals(4, pageDAO.findAll().size());
    assertNotNull(childrenPages);
    assertEquals(2, childrenPages.size());
  }

  @Test
  public void testDeletePage() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    //When
    storage.createPage(wiki, wiki.getWikiHome(), page1);
    assertEquals(2, pageDAO.findAll().size());
    storage.deletePage(wiki.getType(), wiki.getOwner(), page1.getName());

    //Then
    assertEquals(1, pageDAO.findAll().size());
  }

  @Test
  public void testDeletePageTree() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    Page page2 = new Page();
    page2.setWikiId(wiki.getId());
    page2.setWikiType(wiki.getType());
    page2.setWikiOwner(wiki.getOwner());
    page2.setName("page2");
    page2.setTitle("Page 2");

    //When
    storage.createPage(wiki, wiki.getWikiHome(), page1);
    storage.createPage(wiki, page1, page2);
    assertEquals(3, pageDAO.findAll().size());
    storage.deletePage(wiki.getType(), wiki.getOwner(), page1.getName());

    //Then
    assertEquals(1, pageDAO.findAll().size());
  }

  @Test
  public void testMovePage() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    Page page2 = new Page();
    page2.setWikiId(wiki.getId());
    page2.setWikiType(wiki.getType());
    page2.setWikiOwner(wiki.getOwner());
    page2.setName("page2");
    page2.setTitle("Page 2");

    //When
    storage.createPage(wiki, wiki.getWikiHome(), page1);
    storage.createPage(wiki, wiki.getWikiHome(), page2);
    assertEquals(3, pageDAO.findAll().size());
    assertEquals(2, storage.getChildrenPageOf(wiki.getWikiHome()).size());
    storage.movePage(new WikiPageParams(wiki.getType(), wiki.getOwner(), page2.getName()),
            new WikiPageParams(wiki.getType(), wiki.getOwner(), page1.getName()));

    //Then
    assertEquals(3, pageDAO.findAll().size());
    assertEquals(1, storage.getChildrenPageOf(wiki.getWikiHome()).size());
    assertEquals(1, storage.getChildrenPageOf(page1).size());
  }

  @Test
  public void testUpdatePage() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    //When
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page1);
    assertEquals(2, pageDAO.findAll().size());
    createdPage.setTitle("Page 1 updated");
    storage.updatePage(createdPage);

    //Then
    assertEquals(2, pageDAO.findAll().size());
    Page updatedPage = storage.getPageById(createdPage.getId());
    assertNotNull(updatedPage);
    assertEquals("page1", updatedPage.getName());
    assertEquals("Page 1 updated", updatedPage.getTitle());
  }

  @Test
  public void testRenamePage() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Page page1 = new Page();
    page1.setWikiId(wiki.getId());
    page1.setWikiType(wiki.getType());
    page1.setWikiOwner(wiki.getOwner());
    page1.setName("page1");
    page1.setTitle("Page 1");

    //When
    Page createdPage = storage.createPage(wiki, wiki.getWikiHome(), page1);
    assertEquals(2, pageDAO.findAll().size());
    storage.renamePage(wiki.getType(), wiki.getOwner(), page1.getName(), "newName", "New Title");

    //Then
    assertEquals(2, pageDAO.findAll().size());
    Page renamedPage = storage.getPageById(createdPage.getId());
    assertNotNull(renamedPage);
    assertEquals("newName", renamedPage.getName());
    assertEquals("New Title", renamedPage.getTitle());
  }

  @Test
  public void testGetEmotionIcons() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    EmotionIcon emotionIcon1 = new EmotionIcon();
    emotionIcon1.setName("emotionIcon1");
    emotionIcon1.setImage("image1".getBytes());
    storage.createEmotionIcon(emotionIcon1);

    EmotionIcon emotionIcon2 = new EmotionIcon();
    emotionIcon2.setName("emotionIcon2");
    emotionIcon2.setImage("image2".getBytes());
    storage.createEmotionIcon(emotionIcon2);

    //When
    List<EmotionIcon> emotionIcons = storage.getEmotionIcons();

    //Then
    assertNotNull(emotionIcons);
    assertEquals(2, emotionIcons.size());
  }

  @Test
  public void testGetEmotionIconByName() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    EmotionIcon emotionIcon1 = new EmotionIcon();
    emotionIcon1.setName("emotionIcon1");
    emotionIcon1.setImage("image1".getBytes());
    storage.createEmotionIcon(emotionIcon1);

    EmotionIcon emotionIcon2 = new EmotionIcon();
    emotionIcon2.setName("emotionIcon2");
    emotionIcon2.setImage("image2".getBytes());
    storage.createEmotionIcon(emotionIcon2);

    //When
    EmotionIcon fetchedEmotionIcon1 = storage.getEmotionIconByName("emotionIcon1");
    EmotionIcon fetchedEmotionIcon2 = storage.getEmotionIconByName("emotionIcon2");
    EmotionIcon fetchedEmotionIcon3 = storage.getEmotionIconByName("emotionIcon3");

    //Then
    assertNotNull(fetchedEmotionIcon1);
    assertEquals("emotionIcon1", fetchedEmotionIcon1.getName());
    assertTrue(Arrays.equals("image1".getBytes(), fetchedEmotionIcon1.getImage()));
    assertNotNull(fetchedEmotionIcon2);
    assertEquals("emotionIcon2", fetchedEmotionIcon2.getName());
    assertTrue(Arrays.equals("image2".getBytes(), fetchedEmotionIcon2.getImage()));
    assertNull(fetchedEmotionIcon3);
  }

  @Test
  public void testGetTemplate() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Template template1 = new Template();
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    storage.createTemplatePage(wiki, template1);

    //When
    Template fetchedTemplate1 = storage.getTemplatePage(new WikiPageParams("portal", "wiki1", null), "template1");
    Template fetchedTemplate2 = storage.getTemplatePage(new WikiPageParams("portal", "wiki1", null), "template2");
    Template fetchedTemplate1OfWiki2 = storage.getTemplatePage(new WikiPageParams("portal", "wiki2", null), "template1");

    //Then
    assertNotNull(fetchedTemplate1);
    assertEquals("template1", fetchedTemplate1.getName());
    assertNull(fetchedTemplate2);
    assertNull(fetchedTemplate1OfWiki2);
  }

  @Test
  public void testGetTemplates() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Template template1 = new Template();
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    storage.createTemplatePage(wiki, template1);

    Template template2 = new Template();
    template2.setName("template2");
    template2.setTitle("Template 2");
    template2.setContent("Template 2 Content");
    storage.createTemplatePage(wiki, template2);

    //When
    Map<String, Template> fetchedTemplateWiki1 = storage.getTemplates(new WikiPageParams("portal", "wiki1", null));
    Map<String, Template> fetchedTemplateWiki2 = storage.getTemplates(new WikiPageParams("portal", "wiki2", null));

    //Then
    assertNotNull(fetchedTemplateWiki1);
    assertEquals(2, fetchedTemplateWiki1.size());
    assertNotNull(fetchedTemplateWiki2);
    assertEquals(0, fetchedTemplateWiki2.size());
  }

  @Test
  public void testUpdateTemplate() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Template template1 = new Template();
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    storage.createTemplatePage(wiki, template1);

    Template template2 = new Template();
    template2.setName("template2");
    template2.setTitle("Template 2");
    template2.setContent("Template 2 Content");
    storage.createTemplatePage(wiki, template2);

    //When
    Template fetchedTemplate1 = storage.getTemplatePage(new WikiPageParams("portal", "wiki1", null), "template1");
    fetchedTemplate1.setTitle("Template 1 Updated");
    fetchedTemplate1.setContent("Template 1 Content Updated");
    storage.updateTemplatePage(fetchedTemplate1);
    Template fetchedTemplate1AfterUpdate = storage.getTemplatePage(new WikiPageParams("portal", "wiki1", null), "template1");

    //Then
    assertNotNull(fetchedTemplate1AfterUpdate);
    assertEquals("template1", fetchedTemplate1AfterUpdate.getName());
    assertEquals("Template 1 Updated", fetchedTemplate1AfterUpdate.getTitle());
    assertEquals("Template 1 Content Updated", fetchedTemplate1AfterUpdate.getContent());
  }

  @Test
  public void testDeleteTemplate() throws WikiException {
    //Given
    JPADataStorage storage = new JPADataStorage();

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = storage.createWiki(wiki);

    Template template1 = new Template();
    template1.setName("template1");
    template1.setTitle("Template 1");
    template1.setContent("Template 1 Content");
    storage.createTemplatePage(wiki, template1);

    Template template2 = new Template();
    template2.setName("template2");
    template2.setTitle("Template 2");
    template2.setContent("Template 2 Content");
    storage.createTemplatePage(wiki, template2);

    //When
    Map<String, Template> fetchedTemplateWiki1 = storage.getTemplates(new WikiPageParams("portal", "wiki1", null));
    storage.deleteTemplatePage("portal", "wiki1", "template2");
    Map<String, Template> fetchedTemplateWiki1AfterDeletion = storage.getTemplates(new WikiPageParams("portal", "wiki1", null));

    //Then
    assertNotNull(fetchedTemplateWiki1);
    assertEquals(2, fetchedTemplateWiki1.size());
    assertNotNull(fetchedTemplateWiki1AfterDeletion);
    assertEquals(1, fetchedTemplateWiki1AfterDeletion.size());
  }
}
