/* 
* Copyright (C) 2003-2016 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.wiki.jpa.migration;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.settings.impl.SettingServiceImpl;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.core.api.wiki.WikiImpl;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 1/26/16
 */
public class WikiMigrationSettingService {

  //Log
  private static final Log LOG = ExoLogger.getLogger(WikiMigrationSettingService.class);

  //Service
  private SettingService settingService;

  //eXo Properties
  private boolean forceJCRDeletion = false;
  private boolean forceRunMigration = false;

  public WikiMigrationSettingService(SettingService settingService) {

    //Init Service
    this.settingService = settingService;

    //Init eXo Properties
    if (StringUtils.isNotBlank(PropertyManager.getProperty(WikiMigrationContext.WIKI_RDBMS_MIGRATION_FORCE_DELETION_PROPERTY_NAME))) {
      this.forceJCRDeletion = Boolean.valueOf(PropertyManager.getProperty(WikiMigrationContext.WIKI_RDBMS_MIGRATION_FORCE_DELETION_PROPERTY_NAME));
    }
    if (StringUtils.isNotBlank(PropertyManager.getProperty(WikiMigrationContext.WIKI_RDBMS_MIGRATION_FORCE_MIGRATION_PROPERTY_NAME))) {
      this.forceRunMigration = Boolean.valueOf(PropertyManager.getProperty(WikiMigrationContext.WIKI_RDBMS_MIGRATION_FORCE_MIGRATION_PROPERTY_NAME));
    }
  }

  public boolean isForceJCRDeletion() {
    return forceJCRDeletion;
  }

  public void setForceJCRDeletion(boolean forceJCRDeletion) {
    this.forceJCRDeletion = forceJCRDeletion;
  }

  public boolean isForceRunMigration() {
    return forceRunMigration;
  }

  public void setForceRunMigration(boolean forceRunMigration) {
    this.forceRunMigration = forceRunMigration;
  }

  public void initMigrationSetting() {

    if (forceRunMigration) {
      initMigrationSettingToDefault();
      return;
    }

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

  private void initMigrationSettingToDefault() {
    settingService = CommonsUtils.getService(SettingService.class);

    //Init migration state
    WikiMigrationContext.setMigrationDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_KEY));
    WikiMigrationContext.setPortalWikiMigrationDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_PORTAL_WIKI_KEY));
    WikiMigrationContext.setSpaceWikiMigrationDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_SPACE_WIKI_KEY));
    WikiMigrationContext.setUserWikiMigrationDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_USER_WIKI_KEY));
    WikiMigrationContext.setDraftPageMigrationDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_DRAFT_PAGE_KEY));
    WikiMigrationContext.setRelatedPageMigrationDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_KEY));

    //Init reindex state
    WikiMigrationContext.setReindexDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_MIGRATION_REINDEX_KEY));

    //Init deletion state
    WikiMigrationContext.setDeletionDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_DELETION_KEY));
    WikiMigrationContext.setPortalWikiCleanupDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_CLEANUP_PORTAL_WIKI_KEY));
    WikiMigrationContext.setSpaceWikiCleanupDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_CLEANUP_SPACE_WIKI_KEY));
    WikiMigrationContext.setUserWikiCleanupDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_CLEANUP_USER_WIKI_KEY));
    WikiMigrationContext.setEmoticonCleanupDone(setSettingValueToDefault(WikiMigrationContext.WIKI_RDBMS_CLEANUP_EMOTICON_KEY));
  }

  public boolean getOrCreateSettingValue(String key) {
    try {
      if (settingService == null) LOG.info("settingService is null");
      SettingValue<?> migrationValue =  settingService.get(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), key);
      if (migrationValue != null) {
        return Boolean.parseBoolean(migrationValue.getValue().toString());
      } else {
        updateSettingValue(key, Boolean.FALSE);
        return false;
      }
    } finally {
      Scope.APPLICATION.id(null);
    }
  }

  public boolean setSettingValueToDefault(String key) {
    updateSettingValue(key, Boolean.FALSE);
    return false;
  }

  public void updateSettingValue(String key, Boolean status) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      settingService.set(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), key, SettingValue.create(status));
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  public void removeSettingValue() {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      settingService.remove(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY));
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  public void addWikiErrorToSetting(Wiki wikiMigrationError) {
    String wiki = wikiToString(wikiMigrationError);
    addErrorToSetting(WikiMigrationContext.WIKI_RDBMS_MIGRATION_ERROR_WIKI_LIST_SETTING, wiki);
  }

  public void addPageErrorToSetting(Page pageMigrationError) {
    String page = pageToString(pageMigrationError);
    addErrorToSetting(WikiMigrationContext.WIKI_RDBMS_MIGRATION_ERROR_PAGE_LIST_SETTING, page);
  }

  private void addErrorToSetting(String settingErrorKey, String settingErrorValue) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      String migrationErrors = getErrorsSetting(settingErrorKey);
      //Add the error to the migrationErrors String list
      if (migrationErrors == null) {
        migrationErrors = settingErrorValue;
      } else {
        migrationErrors += ";"+settingErrorValue;
      }
      SettingValue<String> errorsSetting = new SettingValue<>(migrationErrors);
      settingService.set(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), settingErrorKey, errorsSetting);
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  public String getWikiErrorsSetting() {
    return getErrorsSetting(WikiMigrationContext.WIKI_RDBMS_MIGRATION_ERROR_WIKI_LIST_SETTING);
  }

  public String getPageErrorsSetting() {
    return getErrorsSetting(WikiMigrationContext.WIKI_RDBMS_MIGRATION_ERROR_PAGE_LIST_SETTING);
  }

  private String getErrorsSetting(String settingErrorKey) {

    String migrationErrors = null;

    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();

    try {
      SettingValue settingValue = settingService.get(
          Context.GLOBAL,
          Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY),
          settingErrorKey);
      if (settingValue != null) {
        migrationErrors = (String) settingValue.getValue();
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }

    return migrationErrors;
  }

  public void addRelatedPagesToSetting(Page relatedPage) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      SettingValue<String> relatedPageSetting = new SettingValue<>(pageToString(relatedPage));
      settingService.set(Context.GLOBAL, Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY), WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_LIST_SETTING, relatedPageSetting);
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  public String getRelatedPagesSetting() {

    String relatedPage = null;

    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();

    try {
      SettingValue settingValue = settingService.get(
          Context.GLOBAL,
          Scope.APPLICATION.id(WikiMigrationContext.WIKI_MIGRATION_SETTING_GLOBAL_KEY),
          WikiMigrationContext.WIKI_RDBMS_MIGRATION_RELATED_PAGE_LIST_SETTING);
      if (settingValue != null) {
        relatedPage = (String) settingValue.getValue();
      }
    } finally {
      Scope.APPLICATION.id(null);
      settingServiceImpl.stopSynchronization(created);
    }

    return relatedPage;
  }

  public void setWikiMigrationOfTypeDone(String wikiType) {
    if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_PORTAL_WIKI_KEY, true);
    } else if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_MIGRATION_SPACE_WIKI_KEY, true);
    }
  }

  public void setWikiCleanupOfTypeDone(String wikiType) {
    if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_PORTAL_WIKI_KEY, true);
    } else if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
      updateSettingValue(WikiMigrationContext.WIKI_RDBMS_CLEANUP_SPACE_WIKI_KEY, true);
    }
  }

  public Integer getWikiErrorsNumber() {
    String wikiErrors = getWikiErrorsSetting();
    if (wikiErrors != null) return wikiErrors.split(";").length;
    return 0;
  }

  public Integer getPageErrorsNumber() {
    String pageErrors = getPageErrorsSetting();
    if (pageErrors != null) return pageErrors.split(";").length;
    return 0;
  }

  public String wikiToString(Wiki wiki) {
    return wiki.getType()+":"+wiki.getOwner();
  }

  public String wikiToString(WikiImpl wiki) {
    return wiki.getType()+":"+wiki.getOwner();
  }

  public Page stringToPage(String pageWithRelatedPages) {
    String[] pageAttribute = pageWithRelatedPages.split(":");
    Page page = new Page();
    page.setWikiType(pageAttribute[0]);
    page.setWikiOwner(pageAttribute[1]);
    page.setId(pageAttribute[2]);
    page.setName(pageAttribute[3]);
    return page;
  }

  public String pageToString(Page page) {
    return page.getWikiType()+":"+page.getWikiOwner()+":"+page.getId()+":"+page.getName();
  }

  public String[] getPagesWithRelatedPages() {
    String pageWithRelatedPages = getRelatedPagesSetting();
    if (pageWithRelatedPages != null) return pageWithRelatedPages.split(";");
    return null;
  }


}

