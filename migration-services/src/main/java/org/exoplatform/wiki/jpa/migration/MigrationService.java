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

import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
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
import org.jgroups.util.DefaultThreadFactory;
import org.picocontainer.Startable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;
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

  //Service
  private JCRDataStorage jcrDataStorage;
  private JPADataStorage jpaDataStorage;
  private OrganizationService organizationService;
  private MOWService mowService;
  private IndexingService indexingService;
  private ExecutorService executorService;
  private WikiMigrationSettingService settingService;

  //List of migration error
  private List<String> wikiErrorsList = new ArrayList<>();
  private List<String> pageErrorsList = new ArrayList<>();

  private final CountDownLatch latch;
  private ExoContainer currentContainer;

  public MigrationService(JCRDataStorage jcrDataStorage, JPADataStorage jpaDataStorage,
                          OrganizationService organizationService, MOWService mowService,
                          IndexingService indexingService, WikiMigrationSettingService wikiMigrationSettingService) {
    this.jcrDataStorage = jcrDataStorage;
    this.jpaDataStorage = jpaDataStorage;
    this.organizationService = organizationService;
    this.mowService = mowService;
    this.indexingService = indexingService;
    this.settingService = wikiMigrationSettingService;
    this.executorService = Executors.newSingleThreadExecutor(new DefaultThreadFactory("WIKI-MIGRATION-RDBMS", false, false));
    latch = new CountDownLatch(1);
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  public CountDownLatch getLatch() {
    return latch;
  }

  @Override
  public void start() {

    //First check to see if the JCR still contains wiki data. If not, migration is skipped
    if (!hasDataToMigrate()) {
      LOG.info("No Wiki data to migrate from JCR to RDBMS");
      return;
    }

    currentContainer = ExoContainerContext.getCurrentContainer();
    try {

      RequestLifeCycle.begin(currentContainer);

      Identity userIdentity = new Identity(IdentityConstants.SYSTEM);
      ConversationState.setCurrent(new ConversationState(userIdentity));

      //Get all the migration setting to get the status of wiki migration (what is already migrated)
      settingService.initMigrationSetting();

      //Second check to see if the migration has already been run completely
      if (WikiMigrationContext.isMigrationDone() && !settingService.isForceRunMigration()) {

        //If Wiki data are still in the JCR and the migration already run completely
        // means that the migration encounter issues
        LOG.warn("Still Wiki data in JCR due to error during migration. To finish properly the migration you can:" +
            "\n 1. Delete JCR data definitively: Set exo.wiki.migration.forceJCRDeletion to true" +
            "\n 2. Rerun the migration: Set exo.wiki.migration.forceRunMigration to true" +
            "\n\n" + getErrorReport());

      } else {

        //Let's start the migration
        LOG.info("=== Start Wiki data migration from JCR to RDBMS");

        long startTime = System.currentTimeMillis();

        // Start migration only for wiki type / pages that are not already been migrated
        if (!WikiMigrationContext.isPortalWikiMigrationDone()) migrateWikisOfType(PortalConfig.PORTAL_TYPE);
        if (!WikiMigrationContext.isSpaceWikiMigrationDone()) migrateWikisOfType(PortalConfig.GROUP_TYPE);
        if (!WikiMigrationContext.isUserWikiMigrationDone()) migrateUsersWikis();
        if (!WikiMigrationContext.isDraftPageMigrationDone()) migrateDraftPages();
        if (!WikiMigrationContext.isRelatedPageMigrationDone()) migrateRelatedPages();

        long endTime = System.currentTimeMillis();

        //Stored in the settingService the termination of the migration
        settingService.updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_MIGRATION_KEY, true);

        LOG.info("=== Wiki data migration from JCR to RDBMS done in " + (endTime - startTime) + " ms");

        Integer wikiErrorNumber = settingService.getWikiErrorsNumber();

        if (wikiErrorNumber == 0) {
          LOG.info("No error during migration");
        } else {
          LOG.info("Numbers of wiki in error during migration = " + wikiErrorNumber);
        }

      }

      if (WikiMigrationContext.isDeletionDone() && !settingService.isForceJCRDeletion()) {
        LOG.info("No Wiki data to delete from JCR");
      } else {

        //Let's start the deletion of wiki data in the JCR
        LOG.info("=== Start Wiki JCR data cleaning due to RDBMS migration");

        if (!settingService.isForceJCRDeletion()) {
          LOG.info("For information, Wiki(s) with errors during migration will not be deleted from JCR");
        } else {
          LOG.info("For information, all wiki(s) will be deleted from JCR (even Wiki(s) with errors during migration)");
        }

        //Deletion of wiki data in JCR is done as a background task
        getExecutorService().submit(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            try {

              long startTime = System.currentTimeMillis();

              RequestLifeCycle.begin(currentContainer);

              Identity userIdentity = new Identity(IdentityConstants.SYSTEM);
              ConversationState.setCurrent(new ConversationState(userIdentity));

              // start reindexation of wiki data if not already done
              if (!WikiMigrationContext.isReindexDone()) {
                LOG.info("Start reindexation of all wiki pages");
                indexingService.reindexAll(WikiPageIndexingServiceConnector.TYPE);
                LOG.info("Start reindexation of all wiki pages attachments");
                indexingService.reindexAll(AttachmentIndexingServiceConnector.TYPE);
                //Stored in the settingService the termination of the reindexation
                settingService.updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_MIGRATION_REINDEX_KEY, true);
              }

              // Init the Error migration list to do not delete wiki in error
              initWikiErrorsList();

              // Start cleanup only for wiki type / pages that are not already been deleted
              if (!WikiMigrationContext.isPortalWikiCleanupDone()) deleteWikisOfType(PortalConfig.PORTAL_TYPE);
              if (!WikiMigrationContext.isSpaceWikiCleanupDone()) deleteWikisOfType(PortalConfig.GROUP_TYPE);
              if (!WikiMigrationContext.isUserWikiCleanupDone()) deleteUsersWikis();
              if (!WikiMigrationContext.isEmoticonCleanupDone()) deleteEmotionIcons();
              Integer errorNumber = settingService.getWikiErrorsNumber();
              if (errorNumber > 0 && !settingService.isForceJCRDeletion()) {
                LOG.warn(getErrorReport());
              } else {
                //Wiki Root Node must be deleted only if all wiki has been previously deleted
                deleteWikiRootNode();
                //Same for all wiki migration settings
                settingService.removeSettingValue();
              }

              long endTime = System.currentTimeMillis();

              //Stored in the settingService the termination of the deletion
              settingService.updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_DELETION_KEY, true);
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

    } catch (Exception e) {
      LOG.error("Error while migrating Wiki JCR data to RDBMS - Cause : " + e.getMessage(), e);
    } finally {
      // reset session
      ConversationState.setCurrent(null);
      RequestLifeCycle.end();
    }

  }

  /**
   * Build an error report of the migration
   * It include a detail list of wiki and page in error
   *
   * @return an error report of the migration
   */
  private String getErrorReport() {

    String[] wikiErrors = settingService.getWikiErrorsSetting().split(";");
    String[] pageErrors = settingService.getPageErrorsSetting().split(";");

    StringBuilder errorReport = new StringBuilder();
    errorReport.append("\n ============== Wiki Migration Error report ==============\n");
    errorReport.append("\n ### Summary \n");
    errorReport.append("\n Number of wiki error: "+wikiErrors.length);
    errorReport.append("\n Number of page error: "+pageErrors.length);
    errorReport.append("\n\n ### Wiki errors list:\n");
    for (String wikiError: wikiErrors) {
      String[] wikiAttribute = wikiError.split(":", 2);
      errorReport.append("\n Wiki Type  : "+wikiAttribute[0]);
      errorReport.append("\n Wiki Owner : "+wikiAttribute[1]);
      errorReport.append("\n ---------------------------------------------------------");
    }
    errorReport.append("\n\n ### Page errors list:\n");
    for (String pageError: pageErrors) {
      Page page = settingService.stringToPage(pageError);
      errorReport.append("\n Wiki Type  : "+page.getWikiType());
      errorReport.append("\n Wiki Owner  : "+page.getWikiOwner());
      errorReport.append("\n Page Id  : "+page.getId());
      errorReport.append("\n Page Name  : "+page.getName());
      errorReport.append("\n ---------------------------------------------------------");
    }
    errorReport.append("\n\n =======================================================\n");

    return errorReport.toString();
  }

  /**
   * Check if the JCR still contains some wiki data
   *
   * @return true if the JCR still contains wiki data, false if not
   */
  private boolean hasDataToMigrate() {
    boolean hasDataToMigrate = true;

    boolean created = mowService.startSynchronization();

    try {
      Session session = mowService.getSession().getJCRSession();
      hasDataToMigrate = session.getRootNode().hasNode("exo:applications/eXoWiki");
    } catch (RepositoryException e) {
      LOG.error("Cannot get root wiki data node - Cause : " + e.getMessage(), e);
    } finally {
      mowService.stopSynchronization(created);
    }

    return hasDataToMigrate;
  }

  /**
   * Manage the migration of Portal wikis and Group wiki
   *
   * @param wikiType type of wiki to migrate (portal or group)
   */
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
      settingService.setWikiMigrationOfTypeDone(wikiType);
      LOG.info("    Migration of "+wikiType+" wikis done");

    } catch (Exception e) {
      LOG.error("Cannot finish the migration of " + wikiType + " wikis - Cause " + e.getMessage(), e);
    }
  }

  /**
   * Manage the migration of User wikis
   *
   */
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
      settingService.updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_MIGRATION_USER_WIKI_KEY, true);
      LOG.info("    Migration of users wikis done");
    } catch (Exception e) {
      LOG.error("Cannot migrate users wikis - Cause : " + e.getMessage(), e);
    }
  }

  /**
   * Manage the migration of a wiki from JCR to RDBMS and catch the potential errors
   *
   * @param jcrWiki wiki to migrate
   */
  private void migrateWiki(Wiki jcrWiki) {
    Boolean isWikiMigrationSuccess = true;
    Boolean isWikiMigrationStarted = false;
    try {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(currentContainer);

      LOG.info("  Start migration of wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner());
      Page jcrWikiHome = jcrWiki.getWikiHome();
      //Check if the migration of this wiki has already been started
      Wiki jpaWiki = jpaDataStorage.getWikiByTypeAndOwner(jcrWiki.getType(), jcrWiki.getOwner());
      if (jpaWiki != null) {
        isWikiMigrationStarted = true;
        LOG.info("  Wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner() + " has already been migrated.");
      } else {
        // remove wiki home to make the createWiki method recreate it
        jcrWiki.setWikiHome(null);
        jpaWiki = jpaDataStorage.createWiki(jcrWiki);
      }

      //Even if the wiki has already been migrated, we need to be sure that all page of this wiki
      // has been migrated also and migrate no migrated page of this wiki

      // PAGES
      // create pages recursively
      LOG.info("    Start migration of wiki pages ...");
      jcrWiki.setWikiHome(jcrWikiHome);
      isWikiMigrationSuccess = createChildrenPagesOf(jpaWiki, jcrWiki, null, 1, isWikiMigrationStarted);
      LOG.info("    Pages migrated");

      //Same for template

      // TEMPLATES
      LOG.info("    Start migration of templates ...");
      createTemplates(jcrWiki, isWikiMigrationStarted);
      LOG.info("    Templates migrated");

      LOG.info("  Wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner() + " migrated successfully");

    } catch(Exception e) {
      LOG.error("Cannot migrate wiki " + jcrWiki.getType() + ":" + jcrWiki.getOwner()
          + " - Cause : " + e.getMessage(), e);
      settingService.addWikiErrorToSetting(jcrWiki);
    } finally {
      RequestLifeCycle.end();
      RequestLifeCycle.begin(currentContainer);
    }
    if (!isWikiMigrationSuccess) {
      settingService.addWikiErrorToSetting(jcrWiki);
    }
  }

  /**
   * Manage the migration of a draft page from JCR to RDBMS and catch the potential errors
   */
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
              String targetPageId = jcrDraftPage.getTargetPageId();
              if(targetPageId != null) {
                LOG.info("    Draft page " + jcrDraftPage.getName() + " of user " + user.getUserName());
                try {
                  // old target id (JCR uuid - String) must be converted to new target id (PK - long)
                  Page jcrPageOfDraft = jcrDataStorage.getPageById(jcrDraftPage.getTargetPageId());
                  if (jcrPageOfDraft != null) {
                    Page jpaPageOfDraft = jpaDataStorage.getPageOfWikiByName(jcrPageOfDraft.getWikiType(), jcrPageOfDraft.getWikiOwner(), jcrPageOfDraft.getName());
                    if(jpaPageOfDraft != null) {
                      jcrDraftPage.setTargetPageId(jpaPageOfDraft.getId());
                    } else {
                      LOG.warn("Target page " + jcrPageOfDraft.getName() + " of draft page " + jcrDraftPage.getName() + " does not exist in JPA database");
                    }
                    jpaDataStorage.createDraftPageForUser(jcrDraftPage, user.getUserName());
                  } else {
                    LOG.error("Cannot migrate draft page " + jcrDraftPage.getName() + " of user " + user.getUserName()
                        + " - Cause : target page " + jcrDraftPage.getTargetPageId() + " does not exist");
                  }
                } catch (Exception e) {
                  LOG.error("Cannot migrate draft page " + jcrDraftPage.getName() + " of user " + user.getUserName()
                      + " - Cause : " + e.getMessage(), e);
                }
              } else {
                LOG.error("Cannot migrate draft page " + jcrDraftPage.getName() + " of user " + user.getUserName()
                    + " - Cause : target page id is null");
              }
            }
          } catch (Exception e) {
            LOG.error("Cannot migrate draft pages of user " + user.getUserName() + " - Cause : " + e.getMessage(), e);
          }
        }
        current += users.length;
      } while(users != null && users.length > 0);
      settingService.updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_MIGRATION_DRAFT_PAGE_KEY, true);
      LOG.info("  Migration of draft pages done");
    } catch (Exception e) {
      LOG.error("Cannot migrate draft pages - Cause : " + e.getMessage(), e);
    }
  }

  /**
   * Manage the migration of a related page from JCR to RDBMS and catch the potential error
   */
  private void migrateRelatedPages() {
    try {
      //Get page in error that cannot be linked
      initPageErrorsList();
      // RELATED PAGES
      LOG.info("  Start migration of related pages ...");
      for(String pageWithRelatedPagesString : settingService.getPagesWithRelatedPages()) {
        Page pageWithRelatedPages = settingService.stringToPage(pageWithRelatedPagesString);
        LOG.info("    Related pages of page " + pageWithRelatedPages.getName());
        for(Page relatedPage : jcrDataStorage.getRelatedPagesOfPage(pageWithRelatedPages)) {
          try {
            LOG.info("related page to migrate "+relatedPage.getId());
            if (pageErrorsList.contains(relatedPage.getId())) {
              LOG.info("      Cannot link related page " + relatedPage.getName() + " to " + pageWithRelatedPages.getName() + " - Cause: " + relatedPage.getName() + " encounter issues during migration and has not been migrated");
            } else {
              LOG.info("      Add related page " + relatedPage.getName());
              jpaDataStorage.addRelatedPage(pageWithRelatedPages, relatedPage);
            }
          } catch(Exception e) {
            LOG.error("Cannot migrate related page " + relatedPage.getName() + " - Cause : " + e.getMessage(), e);
          }
        }
      }
      settingService.updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_KEY, true);
      LOG.info("  Related pages migrated");
    } catch (Exception e) {
      LOG.error("Cannot migrate related pages - Cause : " + e.getMessage(), e);
    }
  }

  /**
   * Manage the deletion of portal and group wikis in the JCR
   *
   * @param wikiType type of wiki to delete
   */
  private void deleteWikisOfType(String wikiType) {
    LOG.info("  Start deletion of wikis of type " + wikiType);

    boolean created = mowService.startSynchronization();

    try {
      WikiStoreImpl wStore = (WikiStoreImpl) mowService.getWikiStore();
      wStore.setMOWService(mowService);
      WikiContainer<WikiImpl> wikiContainer = wStore.getWikiContainer(WikiType.valueOf(wikiType.toUpperCase()));
      Collection<WikiImpl> allWikis = wikiContainer.getAllWikis();
      for(WikiImpl wiki : allWikis) {
        deleteWiki(wiki);
      }
      settingService.setWikiCleanupOfTypeDone(wikiType);
      LOG.info("  Deletion of wikis of type " + wikiType + " done");
    } catch (Exception e) {
      LOG.error("Cannot delete wikis of type " + wikiType + " - Cause : " + e.getMessage(), e);
    } finally {
      mowService.stopSynchronization(created);
    }
  }

  /**
   * Manage the delete of user wikis in the JCR
   *
   */
  private void deleteUsersWikis() {
    int pageSize = 20;
    int current = 0;
    try {
      LOG.info("  Start deletion of user wikis");
      ListAccess<User> allUsersListAccess = organizationService.getUserHandler().findAllUsers();
      int totalUsers = allUsersListAccess.getSize();
      LOG.info("    Number of users = " + totalUsers);
      User[] users;
      do {
        LOG.info("    Progression of users wikis deletion : " + current + "/" + totalUsers);
        if (current + pageSize > totalUsers) {
          pageSize = totalUsers - current;
        }
        users = allUsersListAccess.load(current, pageSize);
        for (User user : users) {
          try {
            // get user wiki
            WikiImpl jcrWiki = fetchWikiImpl(PortalConfig.USER_TYPE, user.getUserName());

            // if it exists, migrate it
            if(jcrWiki != null) {
              LOG.info("    Deletion of the wiki of the user " + user.getUserName());
              deleteWiki(jcrWiki);
            } else {
              LOG.info("    No wiki for user " + user.getUserName());
            }

          } catch (Exception e) {
            LOG.error("Cannot delete wiki of user " + user.getUserName() + " - Cause " + e.getMessage(), e);
          }
        }
        current += users.length;
      } while(users != null && users.length > 0);
      settingService.updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_CLEANUP_USER_WIKI_KEY, true);
      LOG.info("    Deletion of users wikis done");
    } catch (Exception e) {
      LOG.error("Cannot Delete users wikis - Cause : " + e.getMessage(), e);
    }
  }

  /**
   * Manage the deletion of a wiki in the JCR and catch the potential errors
   *
   * @param wiki wiki to delete
   */
  private void deleteWiki(WikiImpl wiki) {

    //Do not remove wiki with error during migration
    if (!wikiErrorsList.contains(settingService.wikiToString(wiki)) || settingService.isForceJCRDeletion()) {

      boolean created = mowService.startSynchronization();

      try {
        Session session = mowService.getSession().getJCRSession();
        String wikiPath = wiki.getPath();
        if (wikiPath.startsWith("/")) {
          wikiPath = wikiPath.substring(1);
        }
        Node wikiNode = session.getRootNode().getNode(wikiPath);
        LOG.info("    Delete wiki " + wiki.getType() + ":" + wiki.getOwner());
        wikiNode.remove();
        session.save();
      } catch (Exception e) {
        LOG.error("Cannot delete wiki " + wiki.getType() + ":" + wiki.getOwner() + " - Cause : " + e.getMessage(), e);
      } finally {
        mowService.stopSynchronization(created);
      }

    } else {
      LOG.info("    Not deleted Wiki " + wiki.getType() + ":" + wiki.getOwner());
    }

  }

  /**
   * Manage the deletion of emoticon in the JCR and catch the potential errors
   *
   */
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
      settingService.updateOperationStatus(WikiMigrationContext.WIKI_RDBMS_CLEANUP_EMOTICON_KEY, true);
      LOG.info("  Deletion of emotion icons done");
    } catch(Exception e) {
      LOG.error("Cannot delete emotion icons - Cause : " + e.getMessage(), e);
    } finally {
      mowService.stopSynchronization(created);
    }
  }

  /**
   * Manage the deletion of a wiki root node in the JCR
   */
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

  /**
   * Recursive function that manage the migration of all pages in a wiki from JCR to RDBMS and catch the potential errors
   *
   * @param jpaWiki wiki in RDBMS
   * @param jcrWiki wiki in JCR
   * @param jcrPage page to migrate
   * @param level hierarchical deep of the wiki (1 is the wikihome)
   * @param isParentAlreadyMigrated boolean to know if the parent of page has been already migrated in a previous migration.
   *                                if migrated in previous relation, we need to check if the page has already been
   *                                migrated too in order to do not migrate it again.
   * @return true if the migration going well, false if not
   * @throws WikiException
   */
  private Boolean createChildrenPagesOf(Wiki jpaWiki, Wiki jcrWiki, Page jcrPage, int level, Boolean isParentAlreadyMigrated) throws WikiException {
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
        boolean pageAlreadyMigrated = false;
        try {
          LOG.info(String.format("    %1$" + ((level) * 2) + "s Page %2$s", " ", childrenPage.getName()));
          RequestLifeCycle.end();
          RequestLifeCycle.begin(currentContainer);
          pageAlreadyMigrated = createPage(jpaWiki, jcrPage, childrenPage, isParentAlreadyMigrated);
          pageCreated = true;
        } catch(Exception e) {
          LOG.error("Cannot create page " + jpaWiki.getType() + ":" + jpaWiki.getOwner() + ":" + childrenPage.getName()
              + " - Cause : " + e.getMessage(), e);
          pageCreated = false;
          isMigrationSuccess = false;
          //Stamp page as migration error
          settingService.addPageErrorToSetting(childrenPage);
        }

        if(pageCreated) {
          try {
            // check if the page has related pages, and keep it if so
            List<Page> relatedPages = jcrDataStorage.getRelatedPagesOfPage(childrenPage);
            if (relatedPages != null && !relatedPages.isEmpty()) {
              settingService.addRelatedPagesToSetting(childrenPage);
            }
          } catch(Exception e) {
            LOG.error("Cannot get related pages of page " + jpaWiki.getType() + ":" + jpaWiki.getOwner() + ":" + childrenPage.getName()
                + " - Cause : " + e.getMessage(), e);
            isMigrationSuccess = false;
            //Stamp page as migration error
            settingService.addPageErrorToSetting(childrenPage);
          }

          Boolean isChildrenSuccess = createChildrenPagesOf(jpaWiki, jcrWiki, childrenPage, level + 1, pageAlreadyMigrated);
          //If the creation of this page is success return result of the creation of its child
          if (isMigrationSuccess) isMigrationSuccess = isChildrenSuccess;
        }
      }
    }
    return isMigrationSuccess;
  }

  /**
   * Create the page in the RDBMS
   *
   * @param wiki wiki related to this page in the RDBMS
   * @param jcrParentPage the parent page of the page to migrate in the JCR
   * @param jcrPage the page to migrate in the JCR
   * @param checkPageMigrated boolean to know if the parent of page has been already  migrated in a previous migration
   *                          if migrated in previous relation, we need to check if the page has already been
   *                          migrated too in order to do not migrate it again.
   * @return
   * @throws WikiException
   */
  @ExoTransactional
  private Boolean createPage(Wiki wiki, Page jcrParentPage, Page jcrPage, Boolean checkPageMigrated) throws WikiException {

    //If this parent page already migrated in a previous migration, check first it this page has already been migrated
    if (checkPageMigrated) {
      Page page = jpaDataStorage.getPageOfWikiByName(jcrPage.getWikiType(), jcrPage.getOwner(), jcrPage.getName());
      if (page != null) {
        LOG.info("  Page " + jcrPage.getName() + " has already been migrated.");
        return true;
      }
    }

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
      try {
        List<String> watchers = jcrDataStorage.getWatchersOfPage(jcrPage);
        for (String watcher : watchers) {
          jpaDataStorage.addWatcherToPage(watcher, jcrPage);
        }
      } catch (Exception e) {
        LOG.warn("Cannot get watchers of page " + jcrPage.getName() + ", keep trying to migrate it anyway - Cause : " + e.getMessage(), e);
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

    return false;
  }

  /**
   * Create the template in the RDBMS
   *
   * @param jcrWiki wiki with template to migrate
   * @param isWikiMigrationStarted boolean to know if the wiki has been already migrated in a previous migration
   *                               if migrated in previous relation, we need to check if the template has already been
   *                               migrated too in order to do not migrate it again.
   * @throws WikiException
   */
  private void createTemplates(Wiki jcrWiki, Boolean isWikiMigrationStarted) throws WikiException {
    Map<String, Template> jcrWikiTemplates = jcrDataStorage.getTemplates(new WikiPageParams(jcrWiki.getType(), jcrWiki.getOwner(), jcrWiki.getId()));
    if(jcrWikiTemplates != null) {
      for (Template jcrTemplate : jcrWikiTemplates.values()) {
        if (isWikiMigrationStarted) {
          Template jpaTemplate = jpaDataStorage.getTemplatePage(new WikiPageParams(jcrWiki.getType(), jcrWiki.getOwner(), null), jcrTemplate.getName());
          if (jpaTemplate != null) {
            LOG.info("      Template " + jcrTemplate.getName() + " already migrated.");
            return;
          }
        }
        LOG.info("      Template " + jcrTemplate.getName() + " migrated.");
        jpaDataStorage.createTemplatePage(jcrWiki, jcrTemplate);
      }
    }
  }

  @Override
  public void stop() {

  }

  /**
   * Get the wiki with migration error from the settingService and put them in the wikiErrorsList
   */
  private void initWikiErrorsList() {
    String wikiErrors = settingService.getWikiErrorsSetting();
    if (wikiErrors != null) {
      this.wikiErrorsList = Arrays.asList(wikiErrors.split(";"));
    } else {
      this.wikiErrorsList = new ArrayList<>();
    }
  }

  /**
   * Get the page with migration error from the settingService and put them in the pageErrorsList
   */
  private void initPageErrorsList() {
    String pageErrors = settingService.getPageErrorsSetting();
    if (pageErrors != null) {
      this.pageErrorsList = Arrays.asList(pageErrors.split(";"));
    } else {
      this.pageErrorsList = new ArrayList<>();
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

}
