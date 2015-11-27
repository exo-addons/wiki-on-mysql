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
package org.exoplatform.wiki.jpa.migration;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.JPADataStorage;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.impl.JCRDataStorage;
import org.picocontainer.Startable;

import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015  
 */
public class MigrationService implements Startable {

  private static final Log LOG = ExoLogger.getLogger(MigrationService.class);

  private JCRDataStorage jcrDataStorage;
  private JPADataStorage jpaDataStorage;

  public MigrationService(JCRDataStorage jcrDataStorage, JPADataStorage jpaDataStorage) {
    this.jcrDataStorage = jcrDataStorage;
    this.jpaDataStorage = jpaDataStorage;
  }

  @Override
  public void start() {
    LOG.info("=== Start Wiki data migration from JCR to RDBMS");

    long startTime = System.currentTimeMillis();

    migrateWikiOfType(PortalConfig.PORTAL_TYPE);
    migrateWikiOfType(PortalConfig.GROUP_TYPE);
    migrateWikiOfType(PortalConfig.USER_TYPE);

    long endTime = System.currentTimeMillis();

    LOG.info("=== Wiki data migration from JCR to RDBMS done in " + (endTime - startTime) + " ms");
  }

  private void migrateWikiOfType(String wikiType) {
    try {
      // get all portal wikis
      List<Wiki> wikis = jcrDataStorage.getWikisByType(wikiType);

      LOG.info("  Number of " + wikiType + " wikis to migrate = " + wikis.size());

      // for each wiki...
      for(Wiki jcrWiki : wikis) {
        LOG.info("  Start migration of wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner());
        Wiki existingPortalWiki = jpaDataStorage.getWikiByTypeAndOwner(jcrWiki.getType(), jcrWiki.getOwner());
        if(existingPortalWiki != null) {
          LOG.error("  Cannot migrate wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner() + " because it already exists.");
        } else {
          LOG.info("    Migration of wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner());
          // create the wiki
          Wiki createdWiki = jpaDataStorage.createWiki(jcrWiki);

          // PAGES
          LOG.info("    Update wiki home page");
          // get wiki home
          Page wikiHome = jcrWiki.getWikiHome();
          jpaDataStorage.updatePage(wikiHome);
          // create pages recursively
          LOG.info("    Creation of all wiki pages ...");
          createChildrenPagesOf(createdWiki, wikiHome, 1);
          LOG.info("    Pages migrated");

          // TEMPLATES
          LOG.info("    Start migration of templates ...");
          createTemplates(jcrWiki);
          LOG.info("    Templates migrated");

          LOG.info("  Wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner() + " migrated successfully");
        }
      }
    } catch (WikiException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void stop() {

  }

  private void createChildrenPagesOf(Wiki wiki, Page page, int level) throws WikiException {
    List<Page> childrenPages = jcrDataStorage.getChildrenPageOf(page);
    if(childrenPages != null) {
      for(Page childrenPage : childrenPages) {
        LOG.info(String.format("    %1$" + ((level) * 2) + "s Page %2$s", " ", childrenPage.getName()));
        createPage(wiki, page, childrenPage);

        createChildrenPagesOf(wiki, childrenPage, level+1);
      }
    }
  }

  private void createPage(Wiki wiki, Page parentPage, Page page) throws WikiException {
    // versions
    List<PageVersion> pageVersions = jcrDataStorage.getVersionsOfPage(page);

    if(pageVersions != null && !pageVersions.isEmpty()) {
      PageVersion firstVersion = pageVersions.get(pageVersions.size() - 1);

      Page newPage = new Page();
      newPage.setWikiType(wiki.getType());
      newPage.setWikiOwner(wiki.getOwner());
      newPage.setName(page.getName());
      newPage.setTitle(page.getTitle());
      newPage.setAuthor(firstVersion.getAuthor());
      newPage.setSyntax(page.getSyntax());
      newPage.setContent(firstVersion.getContent());
      newPage.setPermissions(page.getPermissions());
      newPage.setCreatedDate(firstVersion.getCreatedDate());
      newPage.setUpdatedDate(firstVersion.getUpdatedDate());
      newPage.setOwner(page.getOwner());
      newPage.setComment(firstVersion.getComment());
      // TODO minorEdit should be in PageVersion, not Page
      newPage.setMinorEdit(page.isMinorEdit());
      newPage.setActivityId(page.getActivityId());

      jpaDataStorage.createPage(wiki, parentPage, newPage);
      jpaDataStorage.addPageVersion(newPage);

      for (int i = pageVersions.size() - 2; i >= 0; i--) {
        PageVersion version = pageVersions.get(i);

        newPage.setAuthor(version.getAuthor());
        newPage.setContent(version.getContent());
        newPage.setUpdatedDate(version.getUpdatedDate());
        newPage.setComment(version.getComment());

        jpaDataStorage.updatePage(newPage);
        jpaDataStorage.addPageVersion(newPage);
      }

    }

    // attachments
    List<Attachment> attachments = jcrDataStorage.getAttachmentsOfPage(page);

    for(Attachment attachment : attachments) {
      jpaDataStorage.addAttachmentToPage(attachment, page);
    }
  }

  private void createTemplates(Wiki jcrWiki) throws WikiException {
    Map<String, Template> jcrWikiTemplates = jcrDataStorage.getTemplates(new WikiPageParams(jcrWiki.getType(), jcrWiki.getOwner(), jcrWiki.getId()));
    if(jcrWikiTemplates != null) {
      for (Template jcrTemplate : jcrWikiTemplates.values()) {
        LOG.info("      Template " + jcrTemplate.getName());
        jpaDataStorage.createTemplatePage(jcrWiki, jcrTemplate);
      }
    }
  }
}
