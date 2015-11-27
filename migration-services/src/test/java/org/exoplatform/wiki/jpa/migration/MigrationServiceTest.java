package org.exoplatform.wiki.jpa.migration;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.WikiPageParams;

import java.util.*;

/**
 *
 */
public class MigrationServiceTest extends MigrationITSetup {

  public void testWikiMigration() throws WikiException {
    Wiki wiki = new Wiki(PortalConfig.PORTAL_TYPE, "intranet");
    wiki = jcrDataStorage.createWiki(wiki);
    Page wikiHome = wiki.getWikiHome();
    wikiHome.setContent("Wiki Home Page updated");
    jcrDataStorage.updatePage(wikiHome);

    // Pages
    startSessionAs("john");
    Page page1 = new Page("Page1", "Page 1");
    page1.setAuthor("john");
    page1.setContent("Page 1 Content");
    Date createdDatePage1 = Calendar.getInstance().getTime();
    page1.setCreatedDate(createdDatePage1);
    page1.setUpdatedDate(createdDatePage1);
    page1.setPermissions(Collections.<PermissionEntry>emptyList());
    jcrDataStorage.createPage(wiki, wikiHome, page1);
    page1.setContent("Page 1 Content - Version 2");
    jcrDataStorage.updatePage(page1);
    jcrDataStorage.addPageVersion(page1);

    startSessionAs("mary");
    Page page2 = new Page("Page2", "Page 2");
    page2.setAuthor("mary");
    page2.setContent("Page 2 Content");
    Date createdDatePage2 = Calendar.getInstance().getTime();
    page2.setCreatedDate(createdDatePage2);
    page2.setUpdatedDate(createdDatePage2);
    page2.setPermissions(Collections.<PermissionEntry>emptyList());
    jcrDataStorage.createPage(wiki, wikiHome, page2);
    Attachment attachment = new Attachment();
    attachment.setName("attachment2");
    attachment.setTitle("Attachment 2");
    attachment.setContent("attachment-content-2".getBytes());
    attachment.setCreator("mary");
    attachment.setCreatedDate(Calendar.getInstance());
    attachment.setUpdatedDate(Calendar.getInstance());
    attachment.setMimeType("text/plain");
    jcrDataStorage.addAttachmentToPage(attachment, page2);

    // Templates
    Template template1 = new Template();
    template1.setName("Template1");
    template1.setTitle("Template 1 Title");
    template1.setDescription("Template 1 Description");
    template1.setContent("Template 1 Content");
    template1.setAuthor("john");
    Date createdDateTemplate1 = Calendar.getInstance().getTime();
    template1.setCreatedDate(createdDateTemplate1);
    template1.setUpdatedDate(createdDateTemplate1);
    jcrDataStorage.createTemplatePage(wiki, template1);

    migrationService.start();

    // check wiki
    List<Wiki> portalWikis = jpaDataStorage.getWikisByType(PortalConfig.PORTAL_TYPE);
    assertNotNull(portalWikis);
    assertEquals(1, portalWikis.size());
    Wiki wikiIntranet = portalWikis.get(0);
    assertEquals(PortalConfig.PORTAL_TYPE, wikiIntranet.getType());
    assertEquals("intranet", wikiIntranet.getOwner());
    // check wiki home page
    Page fetchedWikiHome = wikiIntranet.getWikiHome();
    assertNotNull(fetchedWikiHome);
    assertEquals("Wiki Home Page updated", wikiHome.getContent());
    // check children pages
    List<Page> fetchedWikiHomeChildrenPages = jpaDataStorage.getChildrenPageOf(wikiHome);
    assertNotNull(fetchedWikiHomeChildrenPages);
    assertEquals(2, fetchedWikiHomeChildrenPages.size());
    // check page1
    Page fetchedPage1 = jpaDataStorage.getPageOfWikiByName(wikiIntranet.getType(), wikiIntranet.getOwner(), "Page1");
    assertNotNull(fetchedPage1);
    assertEquals("Page 1 Content - Version 2", fetchedPage1.getContent());
    assertEquals(2, jpaDataStorage.getVersionsOfPage(fetchedPage1).size());
    // check page2 and attachments
    Page fetchedPage2 = jpaDataStorage.getPageOfWikiByName(wikiIntranet.getType(), wikiIntranet.getOwner(), "Page2");
    assertNotNull(fetchedPage2);
    assertEquals("Page 2 Content", fetchedPage2.getContent());
    assertEquals(1, jpaDataStorage.getVersionsOfPage(fetchedPage2).size());
    List<Attachment> attachmentsOfPage2 = jpaDataStorage.getAttachmentsOfPage(fetchedPage2);
    assertNotNull(attachmentsOfPage2);
    assertEquals(1, attachmentsOfPage2.size());
    assertTrue(Arrays.equals("attachment-content-2".getBytes(), attachmentsOfPage2.get(0).getContent()));
    // check template1
    Map<String, Template> fetchedTemplates = jpaDataStorage.getTemplates(new WikiPageParams(wikiIntranet.getType(), wikiIntranet.getOwner(), wikiIntranet.getId()));
    assertNotNull(fetchedTemplates);
    assertEquals(1, fetchedTemplates.size());
    Template fetchedTemplate1 = jpaDataStorage.getTemplatePage(new WikiPageParams(wikiIntranet.getType(), wikiIntranet.getOwner(), wikiIntranet.getId()), "Template1");
    assertNotNull(fetchedTemplate1);
    assertEquals("Template1", fetchedTemplate1.getName());
    assertEquals("Template 1 Title", fetchedTemplate1.getTitle());
    assertEquals("Template 1 Description", fetchedTemplate1.getDescription());
    assertEquals("Template 1 Content", fetchedTemplate1.getContent());
  }
}
