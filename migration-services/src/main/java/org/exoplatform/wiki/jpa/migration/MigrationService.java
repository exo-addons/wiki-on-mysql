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

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.JPADataStorage;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.WikiContainer;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.impl.JCRDataStorage;
import org.picocontainer.Startable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Startable service to migrate Wiki data from JCR to RDBMS
 * Note :
 *   Emotion icons are not handled by the migration service since :
 *     - the getEmotionIcons and getEmotionIconByName do not return the image, so can not retrieve it (bug)
 *     - the Emotion Icons are created at startup if they do not exist
 */
public class MigrationService implements Startable {

  private static final Log LOG = ExoLogger.getLogger(MigrationService.class);

  private JCRDataStorage jcrDataStorage;
  private JPADataStorage jpaDataStorage;
  private OrganizationService organizationService;
  private MOWService mowService;

  private List<Page> pagesWithRelatedPages = new ArrayList<>();

  public MigrationService(JCRDataStorage jcrDataStorage, JPADataStorage jpaDataStorage,
                          OrganizationService organizationService, MOWService mowService) {
    this.jcrDataStorage = jcrDataStorage;
    this.jpaDataStorage = jpaDataStorage;
    this.organizationService = organizationService;
    this.mowService = mowService;
  }

  @Override
  public void start() {
    LOG.info("=== Start Wiki data migration from JCR to RDBMS");

    long startTime = System.currentTimeMillis();

    try {
      RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());

      // migrate
      migrateWikiOfType(PortalConfig.PORTAL_TYPE);
      migrateWikiOfType(PortalConfig.GROUP_TYPE);
      migrateWikiOfType(PortalConfig.USER_TYPE);
      migrateDraftPages();
      migrateRelatedPages();

      // cleanup
      deleteWikisOfType(PortalConfig.PORTAL_TYPE);
      deleteWikisOfType(PortalConfig.GROUP_TYPE);
      deleteWikisOfType(PortalConfig.USER_TYPE);
      deleteEmotionIcons();
      deleteWikiRootNode();
    } finally {
      RequestLifeCycle.end();
    }

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
          Page jcrWikiHome = jcrWiki.getWikiHome();
          // remove wiki home to make the createWiki method recreate it
          jcrWiki.setWikiHome(null);
          Wiki createdWiki = jpaDataStorage.createWiki(jcrWiki);

          // PAGES
          LOG.info("    Update wiki home page");
          // create pages recursively
          LOG.info("    Creation of all wiki pages ...");
          jcrWiki.setWikiHome(jcrWikiHome);
          createChildrenPagesOf(createdWiki, jcrWiki, null, 1);
          LOG.info("    Pages migrated");

          // TEMPLATES
          LOG.info("    Start migration of templates ...");
          createTemplates(jcrWiki);
          LOG.info("    Templates migrated");

          LOG.info("  Wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner() + " migrated successfully");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void migrateDraftPages() {
    try {
      LOG.info("  Start migration of draft pages");
      ListAccess<User> allUsersListAccess = organizationService.getUserHandler().findAllUsers();
      User[] allUsers = allUsersListAccess.load(0, allUsersListAccess.getSize());
      for(User user : allUsers) {
        try {
          List<DraftPage> draftPages = jcrDataStorage.getDraftPagesOfUser(user.getUserName());
          for (DraftPage jcrDraftPage : draftPages) {
            LOG.info("    Draft page " + jcrDraftPage.getName() + " of user " + user.getUserName());
            try {
              // old target id (JCR uuid - String) must be converted to new target id (PK - long)
              Page jcrPageOfDraft = jcrDataStorage.getPageById(jcrDraftPage.getTargetPageId());
              if(jcrPageOfDraft != null) {
                Page jpaPageOfDraft = jpaDataStorage.getPageOfWikiByName(jcrPageOfDraft.getWikiType(), jcrPageOfDraft.getWikiOwner(), jcrPageOfDraft.getName());
                jcrDraftPage.setTargetPageId(jpaPageOfDraft.getId());
                jpaDataStorage.createDraftPageForUser(jcrDraftPage, user.getUserName());
              } else {
                LOG.error("Cannot migrate draft page " + jcrDraftPage.getName() + " of user " + user.getUserName()
                        + " - Cause : target page " + jcrDraftPage.getTargetPageId() + " does not exist");
              }
            } catch (WikiException e) {
              LOG.error("Cannot migrate draft page " + jcrDraftPage.getName() + " of user " + user.getUserName()
                      + " - Cause : " + e.getMessage(), e);
            }
          }
        } catch (WikiException e) {
          LOG.error("Cannot migrate draft pages of user " + user.getUserName() + " - Cause : " + e.getMessage(), e);
        }
      }
      LOG.info("  Migration of draft pages done");
    } catch (Exception e) {
      LOG.error("Cannot migrate draft pages - Cause : " + e.getMessage(), e);
    }
  }

  private void migrateRelatedPages() {
    try {
      // RELATED PAGES
      LOG.info("  Start migration of related pages ...");
      for(Page pageWithRelatedPages : pagesWithRelatedPages) {
        LOG.info("    Related pages of page " + pageWithRelatedPages.getName());
        for(Page relatedPage : jcrDataStorage.getRelatedPagesOfPage(pageWithRelatedPages)) {
          LOG.info("      Add related page " + relatedPage.getName());
          jpaDataStorage.addRelatedPage(pageWithRelatedPages, relatedPage);
        }
      }
      LOG.info("  Related pages migrated");
    } catch (WikiException e) {
      LOG.error("Cannot migrate related pages - Cause : " + e.getMessage(), e);
    }
  }

  private void deleteWikisOfType(String wikiType) {
    LOG.info("  Start deletion of wikis of type " + wikiType);

    Session session = null;
    boolean created = mowService.startSynchronization();

    try {
      session = mowService.getSession().getJCRSession();
      WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
      WikiContainer<WikiImpl> wikiContainer = wStore.getWikiContainer(WikiType.valueOf(wikiType.toUpperCase()));
      Collection<WikiImpl> allWikis = wikiContainer.getAllWikis();
      for(WikiImpl wiki : allWikis) {
        LOG.info("    Delete wiki " + wiki.getType() + ":" + wiki.getOwner());
        String wikiPath = wiki.getPath();
        if(wikiPath.startsWith("/")) {
          wikiPath = wikiPath.substring(1);
        }
        Node wikiNode = session.getRootNode().getNode(wikiPath);
        wikiNode.remove();
        session.save();
      }
    } catch (RepositoryException e) {
      LOG.error("Cannot delete wikis of type " + wikiType + " - Cause : " + e.getMessage(), e);
    } finally {
      if(session != null) {
        session.logout();
      }
      mowService.stopSynchronization(created);
    }
  }

  private void deleteEmotionIcons() {
    boolean created = mowService.startSynchronization();

    try {
      WikiStoreImpl wStore = (WikiStoreImpl)this.mowService.getWikiStore();
      PageImpl emotionIconsPage = wStore.getEmotionIconsContainer();
      if(emotionIconsPage != null) {
        emotionIconsPage.remove();
      }
    } finally {
      mowService.stopSynchronization(created);
    }
  }

  private void deleteWikiRootNode() {
    LOG.info("  Start deletion of root wiki data node ...");

    Session session = null;
    boolean created = mowService.startSynchronization();

    try {
      session = mowService.getSession().getJCRSession();
      Node wikiRootNode = session.getRootNode().getNode("exo:applications/eXoWiki");
      if(wikiRootNode != null) {
        wikiRootNode.remove();
        session.save();
      }
    } catch (RepositoryException e) {
      LOG.error("Cannot delete root wiki data node - Cause : " + e.getMessage(), e);
    } finally {
      if(session != null) {
        session.logout();
      }
      mowService.stopSynchronization(created);
    }
  }

  @Override
  public void stop() {

  }

  private void createChildrenPagesOf(Wiki jpaWiki, Wiki jcrWiki, Page jcrPage, int level) throws WikiException {
    List<Page> childrenPages = new ArrayList<>();
    if(jcrPage == null) {
      Page jcrWikiHome = jcrWiki.getWikiHome();
      jcrWikiHome.setId(null);
      childrenPages.add(jcrWikiHome);
    } else {
      childrenPages = jcrDataStorage.getChildrenPageOf(jcrPage);
    }

    if (childrenPages != null) {
      for (Page childrenPage : childrenPages) {
        LOG.info(String.format("    %1$" + ((level) * 2) + "s Page %2$s", " ", childrenPage.getName()));
        createPage(jpaWiki, jcrPage, childrenPage);

        // check if the page has related pages, and keep it if so
        List<Page> relatedPages = jcrDataStorage.getRelatedPagesOfPage(childrenPage);
        if(relatedPages != null && !relatedPages.isEmpty()) {
          pagesWithRelatedPages.add(childrenPage);
        }

        createChildrenPagesOf(jpaWiki, jcrWiki, childrenPage, level + 1);
      }
    }
  }

  private void createPage(Wiki wiki, Page jcrParentPage, Page jcrPage) throws WikiException {
    // versions
    List<PageVersion> pageVersions = jcrDataStorage.getVersionsOfPage(jcrPage);

    if(pageVersions != null && !pageVersions.isEmpty()) {
      PageVersion firstVersion = pageVersions.get(pageVersions.size() - 1);

      Page jpaPage = new Page();
      jpaPage.setWikiType(wiki.getType());
      jpaPage.setWikiOwner(wiki.getOwner());
      jpaPage.setName(jcrPage.getName());
      jpaPage.setTitle(jcrPage.getTitle());
      jpaPage.setAuthor(firstVersion.getAuthor());
      jpaPage.setSyntax(jcrPage.getSyntax());
      jpaPage.setContent(firstVersion.getContent());
      jpaPage.setPermissions(jcrPage.getPermissions());
      jpaPage.setCreatedDate(firstVersion.getCreatedDate());
      jpaPage.setUpdatedDate(firstVersion.getUpdatedDate());
      jpaPage.setOwner(jcrPage.getOwner());
      jpaPage.setComment(firstVersion.getComment());
      // TODO minorEdit should be in PageVersion, not Page
      jpaPage.setMinorEdit(jcrPage.isMinorEdit());
      jpaPage.setActivityId(jcrPage.getActivityId());

      if(jcrParentPage == null) {
        // home page case
        String wikiHomeId = wiki.getWikiHome().getId();
        jpaPage.setId(wikiHomeId);
        jpaDataStorage.updatePage(jpaPage);
      } else {
        jpaPage = jpaDataStorage.createPage(wiki, jcrParentPage, jpaPage);
      }
      jpaDataStorage.addPageVersion(jpaPage);

      for (int i = pageVersions.size() - 2; i >= 0; i--) {
        PageVersion version = pageVersions.get(i);

        jpaPage.setAuthor(version.getAuthor());
        jpaPage.setContent(version.getContent());
        jpaPage.setUpdatedDate(version.getUpdatedDate());
        jpaPage.setComment(version.getComment());

        jpaDataStorage.updatePage(jpaPage);
        jpaDataStorage.addPageVersion(jpaPage);
      }

      // last update with the page itself (needed if some updates have been done without requiring a new version, like a name change for example)
      String jcrPageId = jcrPage.getId();
      jcrPage.setId(jpaPage.getId());
      jpaDataStorage.updatePage(jcrPage);
      jcrPage.setId(jcrPageId);
    }

    // watchers
    List<String> watchers = jcrDataStorage.getWatchersOfPage(jcrPage);
    for(String watcher : watchers) {
      jpaDataStorage.addWatcherToPage(watcher, jcrPage);
    }

    // attachments
    List<Attachment> attachments = jcrDataStorage.getAttachmentsOfPage(jcrPage);
    for(Attachment attachment : attachments) {
      jpaDataStorage.addAttachmentToPage(attachment, jcrPage);
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
