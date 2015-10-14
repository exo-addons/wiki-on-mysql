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

import java.util.*;

import org.exoplatform.commons.api.search.SearchService;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.*;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.DataStorage;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.WikiConstants;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/8/15
 */
public class JPADataStorage implements DataStorage {

  private WikiDAO wikiDAO;
  private PageDAO pageDAO;

  public JPADataStorage() {
    wikiDAO = new WikiDAO();
    pageDAO = new PageDAO();
  }

  @Override
  public PageList<SearchResult> search(WikiSearchData wikiSearchData) {
    List<SearchResult> searchResults = new ArrayList<>();
    Map<String, Collection<org.exoplatform.commons.api.search.data.SearchResult>> results;
    SearchService searchService = PortalContainer.getInstance().getComponentInstanceOfType(SearchService.class);

    results = searchService.search(null, wikiSearchData.getTitle(), null, Collections.singleton("all"),
            (int) wikiSearchData.getOffset(), wikiSearchData.getLimit(),
            wikiSearchData.getSort(), wikiSearchData.getOrder());
    for (String type : results.keySet()) {
      for (org.exoplatform.commons.api.search.data.SearchResult result : results.get(type)) {
        searchResults.add(toSearchResult(result));
      }
    }
    return new ObjectPageList<>(searchResults, searchResults.size());
  }

  private SearchResult toSearchResult(org.exoplatform.commons.api.search.data.SearchResult input) {
    SearchResult output = new SearchResult();
    output.setTitle(input.getTitle());
    return output;
  }

  @Override
  public Wiki getWikiByTypeAndOwner(String wikiType, String wikiOwner) throws WikiException {
    return convertWikiEntityToWiki(wikiDAO.getWikiByTypeAndOwner(wikiType, wikiOwner));
  }

  @Override
  public List<Wiki> getWikisByType(String wikiType) throws WikiException {
    List<Wiki> wikis = new ArrayList();
    for(org.exoplatform.wiki.jpa.entity.Wiki wikiEntity : wikiDAO.getWikisByType(wikiType)) {
      wikis.add(convertWikiEntityToWiki(wikiEntity));
    }
    return wikis;
  }

  @Override
  public Wiki createWiki(Wiki wiki) throws WikiException {
    Wiki createdWiki = convertWikiEntityToWiki(wikiDAO.create(convertWikiToWikiEntity(wiki)));

    // create wiki home page
    Page wikiHomePage = new Page();
    wikiHomePage.setWikiType(wiki.getType());
    wikiHomePage.setWikiOwner(wiki.getOwner());
    wikiHomePage.setName(WikiConstants.WIKI_HOME_NAME);
    wikiHomePage.setTitle(WikiConstants.WIKI_HOME_TITLE);

    Page createdWikiHomePage = createPage(createdWiki, null, wikiHomePage);
    createdWiki.setWikiHome(createdWikiHomePage);

    return createdWiki;
  }

  @Override
  public Page createPage(Wiki wiki, Page parentPage, Page page) throws WikiException {
    org.exoplatform.wiki.jpa.entity.Wiki wikiEntity = wikiDAO.getWikiByTypeAndOwner(wiki.getType(), wiki.getOwner());
    if(wikiEntity == null) {
      throw new WikiException("Cannot create page " + wiki.getType() + ":" + wiki.getOwner() + ":"
              + page.getName() + " because wiki does not exist.");
    }

    org.exoplatform.wiki.jpa.entity.Page parentPageEntity = null;
    if(parentPage != null) {
      parentPageEntity = pageDAO.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), parentPage.getName());
      if(parentPageEntity == null) {
        throw new WikiException("Cannot create page " + wiki.getType() + ":" + wiki.getOwner() + ":"
                + page.getName() + " because parent page " + parentPage.getName() + " does not exist.");
      }
    }
    org.exoplatform.wiki.jpa.entity.Page pageEntity = convertPageToPageEntity(page);
    pageEntity.setWiki(wikiEntity);
    pageEntity.setParentPage(parentPageEntity);

    return convertPageEntityToPage(pageDAO.create(pageEntity));
  }

  @Override
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException {
    return convertPageEntityToPage(pageDAO.getPageOfWikiByName(wikiType, wikiOwner, pageName));
  }

  @Override
  public Page getPageById(String id) throws WikiException {
    return convertPageEntityToPage(pageDAO.find(Long.parseLong(id)));
  }

  @Override
  public Page getParentPageOf(Page page) throws WikiException {
    Page parentPage = null;

    org.exoplatform.wiki.jpa.entity.Page childPageEntity = null;
    if(page.getId() != null && !page.getId().isEmpty()) {
      childPageEntity = pageDAO.find(Long.parseLong(page.getId()));
    } else {
      childPageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());
    }

    if(childPageEntity != null) {
      parentPage = convertPageEntityToPage(childPageEntity.getParentPage());
    }

    return parentPage;
  }

  @Override
  public List<Page> getChildrenPageOf(Page page) throws WikiException {
    org.exoplatform.wiki.jpa.entity.Page pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());
    if(pageEntity == null) {
      throw new WikiException("Cannot get children of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
              + page.getName() + " because page does not exist.");
    }

    List<Page> childrenPages = new ArrayList<>();
    List<org.exoplatform.wiki.jpa.entity.Page> childrenPagesEntities = pageDAO.getChildrenPages(pageEntity);
    if(childrenPagesEntities != null) {
      for (org.exoplatform.wiki.jpa.entity.Page childPageEntity : childrenPagesEntities) {
        childrenPages.add(convertPageEntityToPage(childPageEntity));
      }
    }

    return childrenPages;
  }

  @Override
  public void deletePage(String wikiType, String wikiOwner, String pageName) throws WikiException {
    org.exoplatform.wiki.jpa.entity.Page pageEntity = pageDAO.getPageOfWikiByName(wikiType, wikiOwner, pageName);
    if(pageEntity == null) {
      throw new WikiException("Cannot delete page " + wikiType + ":" + wikiOwner + ":" + pageName
              + " because page does not exist.");
    }

    // delete the page and all its children pages (listeners call on delete page event is done on service layer)
    deletePageEntity(pageEntity);
  }

  /**
   * Recursively deletes a page and all its children pages
   * @param pageEntity the root page to delete
   */
  private void deletePageEntity(org.exoplatform.wiki.jpa.entity.Page pageEntity) {
    List<org.exoplatform.wiki.jpa.entity.Page> childrenPages = pageDAO.getChildrenPages(pageEntity);
    if(childrenPages != null) {
      for (org.exoplatform.wiki.jpa.entity.Page childPage : childrenPages) {
        deletePageEntity(childPage);
      }
    }

    pageDAO.delete(pageEntity);
  }

  @Override
  public void createTemplatePage(Wiki wiki, Template template) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void updateTemplatePage(Template template) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void deleteTemplatePage(String s, String s1, String s2) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Template getTemplatePage(WikiPageParams wikiPageParams, String s) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Map<String, Template> getTemplates(WikiPageParams wikiPageParams) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void deleteDraftOfPage(Page page, String s) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void deleteDraftByName(String s, String s1) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void renamePage(String s, String s1, String s2, String s3, String s4) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException {
    org.exoplatform.wiki.jpa.entity.Page pageEntity = pageDAO.getPageOfWikiByName(currentLocationParams.getType(),
            currentLocationParams.getOwner(), currentLocationParams.getPageName());
    if(pageEntity == null) {
      throw new WikiException("Cannot move page " + currentLocationParams.getType() + ":"
              + currentLocationParams.getOwner() + ":" + currentLocationParams.getPageName()
              + " because page does not exist.");
    }

    org.exoplatform.wiki.jpa.entity.Page destinationPageEntity = pageDAO.getPageOfWikiByName(newLocationParams.getType(),
            newLocationParams.getOwner(), newLocationParams.getPageName());
    if(destinationPageEntity == null) {
      throw new WikiException("Cannot move page " + currentLocationParams.getType() + ":"
              + currentLocationParams.getOwner() + ":" + currentLocationParams.getPageName() + " to page "
              + newLocationParams.getType() + ":" + newLocationParams.getOwner()
              + ":" + newLocationParams.getPageName() + " because destination page does not exist.");
    }

    pageEntity.setParentPage(destinationPageEntity);
    pageDAO.update(pageEntity);
  }

  @Override
  public List<PermissionEntry> getWikiPermission(String s, String s1) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void updateWikiPermission(String s, String s1, List<PermissionEntry> list) throws WikiException {

  }

  @Override
  public List<Page> getRelatedPagesOfPage(Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Page getRelatedPage(String s, String s1, String s2) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void addRelatedPage(Page page, Page page1) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void removeRelatedPage(Page page, Page page1) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Page getExsitedOrNewDraftPageById(String s, String s1, String s2, String s3) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public DraftPage getDraft(WikiPageParams wikiPageParams, String s) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public DraftPage getLastestDraft(String s) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public DraftPage getDraft(String s, String s1) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<DraftPage> getDraftPagesOfUser(String s) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void createDraftPageForUser(DraftPage draftPage, String s) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<TemplateSearchResult> searchTemplate(TemplateSearchData templateSearchData) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<SearchResult> searchRenamedPage(WikiSearchData wikiSearchData) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void deleteAttachmentOfPage(String s, Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Page getHelpSyntaxPage(String s, List<ValuesParam> list, ConfigurationManager configurationManager) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void createEmotionIcon(EmotionIcon emotionIcon) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<EmotionIcon> getEmotionIcons() throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public EmotionIcon getEmotionIconByName(String s) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public String getPortalOwner() throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity identity) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public boolean hasAdminSpacePermission(String s, String s1, Identity identity) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public boolean hasAdminPagePermission(String s, String s1, Identity identity) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<PageVersion> getVersionsOfPage(Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void addPageVersion(Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void restoreVersionOfPage(String s, Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void updatePage(Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<String> getPreviousNamesOfPage(Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public List<String> getWatchersOfPage(Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void addWatcherToPage(String s, Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void deleteWatcherOfPage(String s, Page page) throws WikiException {
    throw new RuntimeException("Not implemented");
  }

  private Wiki convertWikiEntityToWiki(org.exoplatform.wiki.jpa.entity.Wiki wikiEntity) {
    Wiki wiki = null;
    if(wikiEntity != null) {
      wiki = new Wiki();
      wiki.setId(String.valueOf(wikiEntity.getId()));
      wiki.setType(wikiEntity.getType());
      wiki.setOwner(wikiEntity.getOwner());
      //wiki.setWikiHome(wikiEntity.getWikiHome());
      //wiki.setPermissions(wikiEntity.getPermissions());
      //wiki.setDefaultPermissionsInited();
      wiki.setPreferences(wiki.getPreferences());
    }
    return wiki;
  }

  private org.exoplatform.wiki.jpa.entity.Wiki convertWikiToWikiEntity(Wiki wiki) {
    org.exoplatform.wiki.jpa.entity.Wiki wikiEntity = null;
    if(wiki != null) {
      wikiEntity = new org.exoplatform.wiki.jpa.entity.Wiki();
      wikiEntity.setType(wiki.getType());
      wikiEntity.setOwner(wiki.getOwner());
      wikiEntity.setWikiHome(convertPageToPageEntity(wiki.getWikiHome()));
      //wikiEntity.setPermissions(wiki.getPermissions());
    }
    return wikiEntity;
  }

  private Page convertPageEntityToPage(org.exoplatform.wiki.jpa.entity.Page pageEntity) {
    Page page = null;
    if(pageEntity != null) {
      page = new Page();
      page.setId(String.valueOf(pageEntity.getId()));
      page.setName(pageEntity.getName());
      org.exoplatform.wiki.jpa.entity.Wiki wiki = pageEntity.getWiki();
      if(wiki != null) {
        page.setWikiId(String.valueOf(wiki.getId()));
        page.setWikiType(wiki.getType());
        page.setWikiOwner(wiki.getOwner());
      }
      page.setTitle(pageEntity.getTitle());
      page.setAuthor(pageEntity.getAuthor());
      page.setContent(pageEntity.getContent());
      page.setSyntax(pageEntity.getSyntax());
      page.setCreatedDate(pageEntity.getCreatedDate());
      page.setUpdatedDate(pageEntity.getUpdatedDate());
      page.setMinorEdit(pageEntity.isMinorEdit());
      page.setComment(pageEntity.getComment());
      page.setUrl(pageEntity.getUrl());
      //page.setPermissions(pageEntity.getPermissions());
      //page.setActivityId(?);
    }
    return page;
  }

  private org.exoplatform.wiki.jpa.entity.Page convertPageToPageEntity(Page page) {
    org.exoplatform.wiki.jpa.entity.Page pageEntity = null;
    if(page != null) {
      pageEntity = new org.exoplatform.wiki.jpa.entity.Page();
      pageEntity.setName(page.getName());
      if(page.getWikiId() != null) {
        org.exoplatform.wiki.jpa.entity.Wiki wiki = wikiDAO.find(Long.parseLong(page.getWikiId()));
        if (wiki != null) {
          pageEntity.setWiki(wiki);
        }
      }
      pageEntity.setTitle(page.getTitle());
      pageEntity.setAuthor(page.getAuthor());
      pageEntity.setContent(page.getContent());
      pageEntity.setSyntax(page.getSyntax());
      pageEntity.setCreatedDate(page.getCreatedDate());
      pageEntity.setUpdatedDate(page.getUpdatedDate());
      pageEntity.setMinorEdit(page.isMinorEdit());
      pageEntity.setComment(page.getComment());
      pageEntity.setUrl(page.getUrl());
      //page.setPermissions(pageEntity.getPermissions());
      //page.setActivityId(?);
    }
    return pageEntity;
  }
}
