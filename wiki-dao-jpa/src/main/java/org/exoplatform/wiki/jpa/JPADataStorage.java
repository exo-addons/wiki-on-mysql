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

import org.chromattic.api.ChromatticSession;

import org.exoplatform.commons.api.search.SearchService;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.DataStorage;
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
    //TODO Remove dependency to ChromatticSession
    public PageList<SearchResult> search(ChromatticSession chromatticSession, WikiSearchData wikiSearchData) {
        List<SearchResult> searchResults = new ArrayList<SearchResult>();
        Map<String, Collection<org.exoplatform.commons.api.search.data.SearchResult>> results;
        SearchService searchService = PortalContainer.getInstance().getComponentInstanceOfType(SearchService.class);

        //TODO add other wiki types
        List<String> types = Arrays.asList("wiki");
        results = searchService.search(null, wikiSearchData.getTitle(), null, types,
                                        (int)wikiSearchData.getOffset(), wikiSearchData.getLimit(),
                                        wikiSearchData.getSort(), wikiSearchData.getOrder());
        for (org.exoplatform.commons.api.search.data.SearchResult result : results.get("wiki")) {
            searchResults.add(toSearchResult(result));
        }
        return new ObjectPageList<SearchResult>(searchResults, searchResults.size());
    }

    private SearchResult toSearchResult(org.exoplatform.commons.api.search.data.SearchResult input) {
        SearchResult output = new SearchResult();
        output.setTitle(input.getTitle());
        return output;
    }

    @Override
    public InputStream getAttachmentAsStream(String s, ChromatticSession chromatticSession) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<SearchResult> searchRenamedPage(ChromatticSession chromatticSession, WikiSearchData wikiSearchData) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public List<TemplateSearchResult> searchTemplate(ChromatticSession chromatticSession, TemplateSearchData templateSearchData) throws Exception {
        throw new IllegalAccessException("Not implemented");
    }

    @Override
    public Page getWikiPageByUUID(ChromatticSession chromatticSession, String id) throws Exception {
        //TODO Wiki page are not identified by ID anymore
        throw new IllegalAccessException("Not implemented");
    }
}
