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

import org.apache.commons.lang.StringUtils;
import org.chromattic.api.ChromatticSession;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.settings.impl.SettingServiceImpl;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.JPADataStorage;
import org.exoplatform.wiki.jpa.search.AttachmentIndexingServiceConnector;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.exoplatform.wiki.mow.core.api.WikiStoreImpl;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.mow.core.api.wiki.WikiContainer;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.impl.JCRDataStorage;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.service.search.jcr.JCRWikiSearchQueryBuilder;
import org.jgroups.util.DefaultThreadFactory;
import org.picocontainer.Startable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
  private IndexingService indexingService;
  private SettingService settingService;

  private ExecutorService executorService;

  private final CountDownLatch latch;

  private List<Page> pagesWithRelatedPages = new ArrayList<>();
  private ExoContainer currentContainer;

  public MigrationService(JCRDataStorage jcrDataStorage, JPADataStorage jpaDataStorage,
                          OrganizationService organizationService, MOWService mowService,
                          IndexingService indexingService, SettingService settingService) {
    this.jcrDataStorage = jcrDataStorage;
    this.jpaDataStorage = jpaDataStorage;
    this.organizationService = organizationService;
    this.mowService = mowService;
    this.indexingService = indexingService;
    this.settingService = settingService;
    this.executorService = Executors.newSingleThreadExecutor(new DefaultThreadFactory("WIKI-MIGRATION-RDBMS", false, false));
    latch = new CountDownLatch(1);
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public void start() {

    currentContainer = ExoContainerContext.getCurrentContainer();
    try {

      RequestLifeCycle.begin(currentContainer);

      Identity userIdentity = new Identity(IdentityConstants.SYSTEM);
      ConversationState.setCurrent(new ConversationState(userIdentity));

      initMigrationSetting();

      if (WikiMigrationContext.isMigrationDone()) {
        LOG.info("No Wiki data to migrate from JCR to RDBMS");
      } else {

        LOG.info("=== Start Wiki data migration from JCR to RDBMS");

        long startTime = System.currentTimeMillis();

        // migrate
        if (!WikiMigrationContext.isPortalWikiMigrationDone()) migrateWikisOfType(PortalConfig.PORTAL_TYPE);
        if (!WikiMigrationContext.isSpaceWikiMigrationDone()) migrateWikisOfType(PortalConfig.GROUP_TYPE);
        if (!WikiMigrationContext.isUserWikiMigrationDone()) migrateUsersWikis();
        if (!WikiMigrationContext.isDraftPageMigrationDone()) migrateDraftPages();
        if (!WikiMigrationContext.isRelatedPageMigrationDone()) migrateRelatedPages();

        long endTime = System.currentTimeMillis();

        updateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_KEY, true);
        LOG.info("=== Wiki data migration from JCR to RDBMS done in " + (endTime - startTime) + " ms");

        Long errorNumber = getErrorNumber();

        if (errorNumber == 0l) {
          LOG.info("No error during migration");
        } else {
          LOG.info("Numbers of error during migration = " + errorNumber);
        }

      }

      if(WikiMigrationContext.isDeletionDone()) {
        LOG.info("No Wiki data to delete from JCR");
      } else {

        LOG.info("=== Start Wiki JCR data cleaning due to RDBMS migration");

        getExecutorService().submit(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try {

              long startTime = System.currentTimeMillis();

              RequestLifeCycle.begin(currentContainer);

              Identity userIdentity = new Identity(IdentityConstants.SYSTEM);
              ConversationState.setCurrent(new ConversationState(userIdentity));

              // indexation
              if (!WikiMigrationContext.isReindexDone()) {
                LOG.info("Start reindexation of all wiki pages");
                indexingService.reindexAll(WikiPageIndexingServiceConnector.TYPE);
                LOG.info("Start reindexation of all wiki pages attachments");
                indexingService.reindexAll(AttachmentIndexingServiceConnector.TYPE);
                updateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_REINDEX_KEY, true);
              }

              // cleanup
              if (!WikiMigrationContext.isPortalWikiCleanupDone()) deleteWikisOfType(PortalConfig.PORTAL_TYPE);
              if (!WikiMigrationContext.isSpaceWikiCleanupDone()) deleteWikisOfType(PortalConfig.GROUP_TYPE);
              if (!WikiMigrationContext.isUserWikiCleanupDone()) deleteWikisOfType(PortalConfig.USER_TYPE);
              if (!WikiMigrationContext.isEmoticonCleanupDone()) deleteEmotionIcons();
              Long errorNumber = getErrorNumber();
              if (errorNumber > 0) {
                LOG.warn("Numbers of error during migration = " + errorNumber);
                LOG.warn("Due to the error during migration, Wiki Root node and wiki(s) in error are not deleted from JCR. " +
                    "Please delete it manually after checking issues.");
              } else {
                deleteWikiRootNode();
              }

              long endTime = System.currentTimeMillis();

              updateSettingValue(WikiMigrationContext.WIKI_RDBMS_DELETION_KEY, true);
              LOG.info("=== Wiki JCR data cleaning due to RDBMS migration done in " + (endTime - startTime) + " ms");

            } catch (Exception e) {
              LOG.error("Error while cleaning Wiki JCR data to RDBMS - Cause : " + e.getMessage(), e);
            } finally {
              // reset session
              ConversationState.setCurrent(null);
              RequestLifeCycle.end();
            }

            latch.countDown();

            return null;
          }
        });

      }

    } catch(Exception e) {
      LOG.error("Error while migrating Wiki JCR data to RDBMS - Cause : " + e.getMessage(), e);
    } finally {
      // reset session
      ConversationState.setCurrent(null);
      RequestLifeCycle.end();
    }
  }

  public CountDownLatch getLatch() {
    return latch;
  }
  
  private void migrateWikisOfType(String wikiType) {
    try {

      LOG.info("  Start migration of "+wikiType+" wikis");

      // get all wikis
      List<Wiki> wikis = jcrDataStorage.getWikisByType(wikiType);

      if(wikis != null && !wikis.isEmpty()) {
        LOG.info("  Number of " + wikiType + " wikis to migrate = " + wikis.size());

        // for each wiki...
        for (Wiki jcrWiki : wikis) {
          migrateWiki(jcrWiki);
        }
      } else {
        LOG.info("  No " + wikiType + " wikis to migrate");
      }
      setWikiMigrationOfTypeDone(wikiType);
      LOG.info("    Migration of "+wikiType+" wikis done");

    } catch (Exception e) {
      LOG.error("Cannot finish the migration of " + wikiType + " wikis - Cause " + e.getMessage(), e);
    }
  }

  private void migrateUsersWikis() {
    int pageSize = 20;
    int current = 0;
    try {
      LOG.info("  Start migration of user wikis");
      ListAccess<User> allUsersListAccess = organizationService.getUserHandler().findAllUsers();
      int totalUsers = allUsersListAccess.getSize();
      LOG.info("    Number of users = " + totalUsers);
      User[] users;
      do {
        LOG.info("    Progression of users wikis migration : " + current + "/" + totalUsers);
        if (current + pageSize > totalUsers) {
          pageSize = totalUsers - current;
        }
        users = allUsersListAccess.load(current, pageSize);
        for (User user : users) {
          try {
            // get user wiki
            Wiki jcrWiki = jcrDataStorage.getWikiByTypeAndOwner(PortalConfig.USER_TYPE, user.getUserName());

            // if it exists, migrate it
            if(jcrWiki != null) {
              LOG.info("    Migration of the wiki of the user " + user.getUserName());
              migrateWiki(jcrWiki);
            } else {
              LOG.info("    No wiki for user " + user.getUserName());
            }

          } catch (Exception e) {
            LOG.error("Cannot migrate wiki of user " + user.getUserName() + " - Cause " + e.getMessage(), e);
          }
        }
        current += users.length;
      } while(users != null && users.length > 0);
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_USER_WIKI_KEY, true);
      LOG.info("    Migration of users wikis done");
    } catch (Exception e) {
      LOG.error("Cannot migrate users wikis - Cause : " + e.getMessage(), e);
    }
  }

  private void migrateWiki(Wiki jcrWiki) {
    Boolean isWikiMigrationSuccess = true;
    try {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(currentContainer);

      LOG.info("  Start migration of wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner());
      Wiki existingPortalWiki = jpaDataStorage.getWikiByTypeAndOwner(jcrWiki.getType(), jcrWiki.getOwner());
      if (existingPortalWiki != null) {
        LOG.info("  Wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner() + " has already been migrated.");
      } else {
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
        isWikiMigrationSuccess = createChildrenPagesOf(createdWiki, jcrWiki, null, 1);
        LOG.info("    Pages migrated");

        // TEMPLATES
        LOG.info("    Start migration of templates ...");
        createTemplates(jcrWiki);
        LOG.info("    Templates migrated");

        LOG.info("  Wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner() + " migrated successfully");
      }
    } catch(Exception e) {
      LOG.error("Cannot migrate wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner()
          + " - Cause : " + e.getMessage(), e);
      setErrorMigrationMixinToWikiNode(jcrWiki.getType(), jcrWiki.getOwner());
    } finally {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(currentContainer);
    }
    if (!isWikiMigrationSuccess) {
      setErrorMigrationMixinToWikiNode(jcrWiki.getType(), jcrWiki.getOwner());
    }
  }

  private void migrateDraftPages() {
    int pageSize = 20;
    int current = 0;
    try {
      LOG.info("  Start migration of draft pages");
      ListAccess<User> allUsersListAccess = organizationService.getUserHandler().findAllUsers();
      int totalUsers = allUsersListAccess.getSize();
      User[] users;
      do {
        if(current + pageSize > totalUsers) {
          pageSize = totalUsers - current;
        }
        users = allUsersListAccess.load(current, pageSize);
        for(User user : users) {
          try {
            List<DraftPage> draftPages = jcrDataStorage.getDraftPagesOfUser(user.getUserName());
            for (DraftPage jcrDraftPage : draftPages) {

              LOG.info("    Draft page " + jcrDraftPage.getName() + " of user " + user.getUserName());
              try {
                // old target id (JCR uuid - String) must be converted to new target id (PK - long)
                Page jcrPageOfDraft = jcrDataStorage.getPageById(jcrDraftPage.getTargetPageId());
                if (jcrPageOfDraft != null) {
                  Page jpaPageOfDraft = jpaDataStorage.getPageOfWikiByName(jcrPageOfDraft.getWikiType(), jcrPageOfDraft.getWikiOwner(), jcrPageOfDraft.getName());
                  jcrDraftPage.setTargetPageId(jpaPageOfDraft.getId());
                  jpaDataStorage.createDraftPageForUser(jcrDraftPage, user.getUserName());
                } else {
                  LOG.error("Cannot migrate draft page " + jcrDraftPage.getName() + " of user " + user.getUserName()
                      + " - Cause : target page " + jcrDraftPage.getTargetPageId() + " does not exist");
                }
              } catch (Exception e) {
                LOG.error("Cannot migrate draft page " + jcrDraftPage.getName() + " of user " + user.getUserName()
                    + " - Cause : " + e.getMessage(), e);
              }
            }
          } catch (Exception e) {
            LOG.error("Cannot migrate draft pages of user " + user.getUserName() + " - Cause : " + e.getMessage(), e);
          }
        }
        current += users.length;
      } while(users != null && users.length > 0);
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_DRAFT_PAGE_KEY, true);
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
          try {
            LOG.info("      Add related page " + relatedPage.getName());
            jpaDataStorage.addRelatedPage(pageWithRelatedPages, relatedPage);
          } catch(Exception e) {
            LOG.error("Cannot migrate related page " + relatedPage.getName() + " - Cause : " + e.getMessage(), e);
          }
        }
      }
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_KEY, true);
      LOG.info("  Related pages migrated");
    } catch (Exception e) {
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
      wStore.setMOWService(mowService);
      WikiContainer<WikiImpl> wikiContainer = wStore.getWikiContainer(WikiType.valueOf(wikiType.toUpperCase()));
      Collection<WikiImpl> allWikis = wikiContainer.getAllWikis();
      for(WikiImpl wiki : allWikis) {
        try {
          String wikiPath = wiki.getPath();
          if (wikiPath.startsWith("/")) {
            wikiPath = wikiPath.substring(1);
          }
          Node wikiNode = session.getRootNode().getNode(wikiPath);
          //Do not remove wiki with migration error mixin
          if (!wikiNode.isNodeType("wiki:migrationError")) {
            LOG.info("    Delete wiki " + wiki.getType() + ":" + wiki.getOwner());
            wikiNode.remove();
            session.save();
          }
        } catch(Exception e) {
          LOG.error("Cannot delete wiki " + wiki.getType() + ":" + wiki.getOwner() + " - Cause : " + e.getMessage(), e);
        }
      }
      setWikiCleanupOfTypeDone(wikiType);
      LOG.info("  Deletion of wikis of type " + wikiType + " done");
    } catch (Exception e) {
      LOG.error("Cannot delete wikis of type " + wikiType + " - Cause : " + e.getMessage(), e);
    } finally {
      /*if(session != null) {
        session.logout();
      }*/
      mowService.stopSynchronization(created);
    }
  }

  private void deleteEmotionIcons() {
    LOG.info("  Start deletion of emotion icons ...");

    boolean created = mowService.startSynchronization();

    try {
      WikiStoreImpl wStore = (WikiStoreImpl)this.mowService.getWikiStore();
      wStore.setMOWService(mowService);
      PageImpl emotionIconsPage = wStore.getEmotionIconsContainer();
      if(emotionIconsPage != null) {
        emotionIconsPage.remove();
      }
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_EMOTICON_KEY, true);
      LOG.info("  Deletion of emotion icons done");
    } catch(Exception e) {
      LOG.error("Cannot delete emotion icons - Cause : " + e.getMessage(), e);
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
      LOG.info("  Deletion of root wiki data node done");
    } catch (RepositoryException e) {
      LOG.error("Cannot delete root wiki data node - Cause : " + e.getMessage(), e);
    } finally {
      /*if(session != null) {
        session.logout();
      }*/
      mowService.stopSynchronization(created);
    }
  }


  @Override
  public void stop() {

  }

  private Boolean createChildrenPagesOf(Wiki jpaWiki, Wiki jcrWiki, Page jcrPage, int level) throws WikiException {
    Boolean isMigrationSuccess = true;
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
        boolean pageCreated;
        try {
          LOG.info(String.format("    %1$" + ((level) * 2) + "s Page %2$s", " ", childrenPage.getName()));
          RequestLifeCycle.end();
          RequestLifeCycle.begin(currentContainer);
          createPage(jpaWiki, jcrPage, childrenPage);
          pageCreated = true;
        } catch(Exception e) {
          LOG.error("Cannot create page " + jpaWiki.getType() + ":" + jpaWiki.getOwner() + ":" + childrenPage.getName()
              + " - Cause : " + e.getMessage(), e);
          pageCreated = false;
          isMigrationSuccess = false;
          //Stamp page as migration error
          setErrorMigrationMixinToPageNode(childrenPage);
        }

        if(pageCreated) {
          try {
            // check if the page has related pages, and keep it if so
            List<Page> relatedPages = jcrDataStorage.getRelatedPagesOfPage(childrenPage);
            if (relatedPages != null && !relatedPages.isEmpty()) {
              pagesWithRelatedPages.add(childrenPage);
            }
          } catch(Exception e) {
            LOG.error("Cannot get related pages of page " + jpaWiki.getType() + ":" + jpaWiki.getOwner() + ":" + childrenPage.getName()
                + " - Cause : " + e.getMessage(), e);
            isMigrationSuccess = false;
            //Stamp page as migration error
            setErrorMigrationMixinToPageNode(childrenPage);
          }

          Boolean isChildrenSuccess = createChildrenPagesOf(jpaWiki, jcrWiki, childrenPage, level + 1);
          //If the creation of this page is success return result of the creation of its child
          if (isMigrationSuccess) isMigrationSuccess = isChildrenSuccess;
        }
      }
    }
    return isMigrationSuccess;
  }

  @ExoTransactional
  private void createPage(Wiki wiki, Page jcrParentPage, Page jcrPage) throws WikiException {
    try {
      // versions
      List<PageVersion> pageVersions = jcrDataStorage.getVersionsOfPage(jcrPage);
      if (pageVersions == null || pageVersions.isEmpty()) {
        LOG.warn("Page " + jcrPage.getName() + " is not versioned, migrating the page as the only version");
        PageVersion pageOnlyVersion = new PageVersion();
        pageOnlyVersion.setAuthor(jcrPage.getAuthor());
        pageOnlyVersion.setContent(jcrPage.getContent());
        pageOnlyVersion.setCreatedDate(jcrPage.getCreatedDate());
        pageOnlyVersion.setUpdatedDate(jcrPage.getUpdatedDate());
        pageOnlyVersion.setComment(jcrPage.getComment());

        if (pageVersions == null) {
          pageVersions = new ArrayList<>();
        }
        pageVersions.add(pageOnlyVersion);
      }

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

      if (jcrParentPage == null) {
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


      // watchers
      List<String> watchers = jcrDataStorage.getWatchersOfPage(jcrPage);
      for (String watcher : watchers) {
        jpaDataStorage.addWatcherToPage(watcher, jcrPage);
      }

      // attachments
      List<Attachment> attachments = jcrDataStorage.getAttachmentsOfPage(jcrPage);
      for (Attachment attachment : attachments) {
        jpaDataStorage.addAttachmentToPage(attachment, jcrPage);
      }
    } finally {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(currentContainer);
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

  private void initMigrationSetting() {
    settingService = CommonsUtils.getService(SettingService.class);

    //Init migration state
    WikiMigrationContext.setMigrationDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_KEY));
    WikiMigrationContext.setPortalWikiMigrationDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_PORTAL_WIKI_KEY));
    WikiMigrationContext.setSpaceWikiMigrationDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_SPACE_WIKI_KEY));
    WikiMigrationContext.setUserWikiMigrationDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_USER_WIKI_KEY));
    WikiMigrationContext.setDraftPageMigrationDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_DRAFT_PAGE_KEY));
    WikiMigrationContext.setRelatedPageMigrationDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_KEY));

    //Init reindex state
    WikiMigrationContext.setReindexDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_REINDEX_KEY));

    //Init deletion state
    WikiMigrationContext.setDeletionDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_DELETION_KEY));
    WikiMigrationContext.setPortalWikiCleanupDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_PORTAL_WIKI_KEY));
    WikiMigrationContext.setSpaceWikiCleanupDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_SPACE_WIKI_KEY));
    WikiMigrationContext.setUserWikiCleanupDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_USER_WIKI_KEY));
    WikiMigrationContext.setEmoticonCleanupDone(getOrCreateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_EMOTICON_KEY));
  }

  private boolean getOrCreateSettingValue(String key) {
    try {
      if (settingService == null) LOG.info("settingService is null");
      SettingValue<?> migrationValue =  settingService.get(Context.GLOBAL, Scope.GLOBAL.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), key);
      if (migrationValue != null) {
        return Boolean.parseBoolean(migrationValue.getValue().toString());
      } else {
        updateSettingValue(key, Boolean.FALSE);
        return false;
      }
    } finally {
      Scope.GLOBAL.id(null);
    }
  }

  private void updateSettingValue(String key, Boolean status) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      settingService.set(Context.GLOBAL, Scope.GLOBAL.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), key, SettingValue.create(status));
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.GLOBAL.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  private void setWikiMigrationOfTypeDone(String wikiType) {
    if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_PORTAL_WIKI_KEY, true);
    } else if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_SPACE_WIKI_KEY, true);
    }
  }

  private void setWikiCleanupOfTypeDone(String wikiType) {
    LOG.info("Enter setWikiCleanupOfTypeDone");
    if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_PORTAL_WIKI_KEY, true);
    } else if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_SPACE_WIKI_KEY, true);
    } else if (wikiType.equals(PortalConfig.USER_TYPE)) {
      LOG.info("Set WIKI_RDBMS_CLEANUP_USER_WIKI_KEY has done");
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_USER_WIKI_KEY, true);
    }
  }

  /**
   * Add a mixin type to a wiki to do not delete it in case migration error happened
   *
   * @param wikiType
   * @param wikiOwner
   */
  private void setErrorMigrationMixinToWikiNode(String wikiType, String wikiOwner) {

    Session session = null;
    boolean created = mowService.startSynchronization();

    LOG.info("Wiki "+wikiType+":"+wikiOwner+" get issue during migration, it will be not deleted from JCR.");

    try {

      WikiImpl wiki = fetchWikiImpl(wikiType, wikiOwner);

      session = mowService.getSession().getJCRSession();

      String wikiPath = wiki.getPath();
      if (wikiPath.startsWith("/")) {
        wikiPath = wikiPath.substring(1);
      }
      Node wikiNode = session.getRootNode().getNode(wikiPath);
      wikiNode.addMixin("wiki:migrationError");
      session.save();

    } catch (Exception e) {
      LOG.error("Impossible to add ErrorMigration mixin to wiki " + wikiType + ":" + wikiOwner + " - Cause : " + e.getMessage(), e);
    } finally {
      /*if (session != null) {
        session.logout();
      }*/
      mowService.stopSynchronization(created);
    }
  }

  /**
   * Add a mixin type to a Page to kno that an error happened on it during migration
   *
   */
  private void setErrorMigrationMixinToPageNode(Page page) {

    LOG.info("Page "+page.getName()+" get issue during migration, it will be not deleted from JCR.");

    Session session = null;
    boolean created = mowService.startSynchronization();

    try {

      PageImpl pageImpl = fetchPageImpl(page);

      session = mowService.getSession().getJCRSession();

      String pagePath = pageImpl.getPath();
      //TODO check if we really need this:
      if (pagePath.startsWith("/")) {
        pagePath = pagePath.substring(1);
      }
      Node pageNode = session.getRootNode().getNode(pagePath);
      pageNode.addMixin("wiki:migrationError");

    } catch (Exception e) {
      LOG.error("Impossible to add ErrorMigration mixin to page " + page.getName() + " - Cause : " + e.getMessage(), e);
    } finally {
      /*if (session != null) {
        session.logout();
      }*/
      mowService.stopSynchronization(created);
    }
  }

  private WikiImpl fetchWikiImpl(String wikiType, String wikiOwner) throws WikiException {
    boolean created = this.mowService.startSynchronization();

    WikiImpl userWikiContainer1;
    try {
      WikiStoreImpl wStore = (WikiStoreImpl)this.mowService.getWikiStore();
      WikiImpl wiki = null;
      WikiContainer userWikiContainer;
      if(PortalConfig.PORTAL_TYPE.equals(wikiType)) {
        userWikiContainer = wStore.getWikiContainer(WikiType.PORTAL);
        wiki = userWikiContainer.getWiki(wikiOwner);
      } else if(PortalConfig.GROUP_TYPE.equals(wikiType)) {
        userWikiContainer = wStore.getWikiContainer(WikiType.GROUP);
        wiki = userWikiContainer.getWiki(wikiOwner);
      } else if(PortalConfig.USER_TYPE.equals(wikiType)) {
        userWikiContainer = wStore.getWikiContainer(WikiType.USER);
        wiki = userWikiContainer.getWiki(wikiOwner);
      }

      userWikiContainer1 = wiki;
    } finally {
      this.mowService.stopSynchronization(created);
    }

    return userWikiContainer1;
  }

  private PageImpl fetchPageImpl(Page page) throws WikiException {
    boolean created = this.mowService.startSynchronization();

    Object searchData2;
    try {
      Object wikiPage = null;
      ChromatticSession session = this.mowService.getSession();
      if(page.getId() != null && !StringUtils.isEmpty(page.getId())) {
        wikiPage = (PageImpl)session.findById(PageImpl.class, page.getId());
      } else if("WikiHome".equals(page.getName())) {
        WikiImpl searchData = this.fetchWikiImpl(page.getWikiType(), page.getWikiOwner());
        wikiPage = searchData.getWikiHome();
      } else {
        WikiSearchData searchData1 = new WikiSearchData(page.getWikiType(), page.getWikiOwner(), page.getName());
        JCRWikiSearchQueryBuilder queryBuilder = new JCRWikiSearchQueryBuilder(searchData1);
        String statement = queryBuilder.getPageConstraint();
        if(statement != null) {
          org.chromattic.api.query.QueryResult path = session.createQueryBuilder(PageImpl.class).where(statement).get().objects();
          if(path.hasNext()) {
            wikiPage = (PageImpl)path.next();
          }
        }

        if(wikiPage != null) {
          String path1 = ((PageImpl)wikiPage).getPath();
          if(path1.startsWith("/")) {
            path1 = path1.substring(1, path1.length());
          }

          wikiPage = (PageImpl)session.findByPath(PageImpl.class, path1);
        }
      }

      searchData2 = wikiPage;
    } finally {
      this.mowService.stopSynchronization(created);
    }

    return (PageImpl)searchData2;
  }

  private Long getErrorNumber() {

    Session session = null;
    boolean created = mowService.startSynchronization();

    try {
      QueryManager ex = mowService.getSession().getJCRSession().getWorkspace().getQueryManager();
      Query query = ex.createQuery("SELECT * FROM wiki:migrationError", "sql");
      if(query instanceof QueryImpl) {
        QueryImpl impl = (QueryImpl)query;
        return impl.execute().getNodes().getSize();
      } else {
        return query.execute().getNodes().getSize();
      }
    } catch (Exception e) {
      LOG.error("Impossible to retrieve number of errors during migration - Cause: ", e);
      return null;
    } finally {
      /*if(session != null) {
        session.logout();
      }
      */
      mowService.stopSynchronization(created);
    }
  }

}
