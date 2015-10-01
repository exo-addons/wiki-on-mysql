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

import java.io.InputStream;
import java.util.*;

import org.exoplatform.commons.api.search.SearchService;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.DataStorage;
import org.exoplatform.wiki.service.PermissionEntry;
import org.exoplatform.wiki.service.PermissionType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 9/8/15
 */
public class JPADataStorage implements DataStorage {

    @Override
    public PageList<SearchResult> search(WikiSearchData wikiSearchData) {
        List<SearchResult> searchResults = new ArrayList<>();
        Map<String, Collection<org.exoplatform.commons.api.search.data.SearchResult>> results;
        SearchService searchService = PortalContainer.getInstance().getComponentInstanceOfType(SearchService.class);

        results = searchService.search(null, wikiSearchData.getTitle(), null, Collections.singleton("all"),
                (int)wikiSearchData.getOffset(), wikiSearchData.getLimit(),
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
    public Wiki getWikiByTypeAndOwner(String s, String s1, boolean b) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Wiki createWiki(String s, String s1) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Page createPage(Wiki wiki, Page page, Page page1) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Page getPageOfWikiByName(String s, String s1, String s2) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Page getPageById(String s) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Page getParentPageOf(Page page) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<Page> getChildrenPageOf(Page page) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void createTemplatePage(Wiki wiki, Template template) throws Exception {
        throw new IllegalAccessException("Not implemented");

    }

    @Override
    public void deleteTemplatePage(String s, String s1, String s2) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void deletePage(String s, String s1, String s2) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Template getTemplatePage(WikiPageParams wikiPageParams, String s) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Map<String, Template> getTemplates(WikiPageParams wikiPageParams) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void deleteDraftOfPage(Page page, String s) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void deleteDraftById(String s, String s1) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void renamePage(String s, String s1, String s2, String s3, String s4) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void movePage(WikiPageParams wikiPageParams, WikiPageParams wikiPageParams1) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<PermissionEntry> getWikiPermission(String s, String s1) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<String> getWikiDefaultPermissions(String s, String s1) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void setWikiPermission(String s, String s1, List<PermissionEntry> list) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<Page> getRelatedPagesOfPage(Page page) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Page getRelatedPage(String s, String s1, String s2) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void addRelatedPage(Page page, Page page1) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void removeRelatedPage(Page page, Page page1) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Page getExsitedOrNewDraftPageById(String s, String s1, String s2, String s3) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public DraftPage getDraft(WikiPageParams wikiPageParams, String s) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public DraftPage getLastestDraft(String s) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public DraftPage getDraft(String s, String s1) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<DraftPage> getDraftPagesOfUser(String s) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void createDraftPageForUser(DraftPage draftPage, String s) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public InputStream getAttachmentAsStream(String s) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Object findByPath(String s, String s1) {
        return null;
    }

    @Override
    public Page getHelpSyntaxPage(String s, List<ValuesParam> list, ConfigurationManager configurationManager) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Page getEmotionIconsPage() throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public String getPortalOwner() {
        return null;
    }

    @Override
    public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity identity) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public boolean hasAdminSpacePermission(String s, String s1, Identity identity) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public boolean hasAdminPagePermission(String s, String s1, Identity identity) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<PageVersion> getVersionsOfPage(Page page) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void addPageVersion(Page page) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public void updatePage(Page page) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<SearchResult> searchRenamedPage(WikiSearchData wikiSearchData) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Page getPageOfAttachment(Attachment attachment) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<TemplateSearchResult> searchTemplate(TemplateSearchData templateSearchData) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }
}
