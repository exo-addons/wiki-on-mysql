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

package org.exoplatform.wiki.jpa.search;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import org.exoplatform.wiki.jpa.BaseWikiIntegrationTest;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 10/21/15
 */
public class JPADataStorageSearchTest extends BaseWikiIntegrationTest {

    @Test
    public void testSearchWikiByName() throws Exception {
        // Given
        // When
        indexWiki("My Wiki", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData(null, "My Wiki", null, null)).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData("My Wiki", null, null, null)).getPageSize());
    }

    @Test
    public void testSearchPageByName() throws Exception {
        // Given
        // When
        indexPage("My name", "My title", "This is the content of my Page", "This is a comment", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData(null, "name", null, null)).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData("name", null, null, null)).getPageSize());
    }

    @Test
    public void testSearchPageByTitle() throws Exception {
        // Given
        // When
        indexPage("My name", "My title", "This is the content of my Page", "This is a comment", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData(null, "Title", null, null)).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData("Title", null, null, null)).getPageSize());
    }

    @Test
    public void testSearchPageByContent() throws Exception {
        // Given
        // When
        indexPage("My Page", "My Page", "This is the content of my Page", "This is a comment", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData(null, "content", null, null)).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData("content", null, null, null)).getPageSize());
    }

    @Test
    public void testSearchPageByComment() throws Exception {
        // Given
        // When
        indexPage("My Page", "My Page", "This is the content of my Page", "This is a comment", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData("comment", null, null, null)).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData(null, "comment", null, null)).getPageSize());
    }

    @Test
    public void testSearchAttachmentByTitle() throws NoSuchFieldException, IllegalAccessException, IOException {
        // Given
        URL fileResource = this.getClass().getClassLoader().getResource("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
        // When
        indexAttachment("Scrum @eXo - Collector", fileResource.getPath(), "www.exo.com", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData("Collector", null, null, null)).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData(null, "Collector", null, null)).getPageSize());
    }

    @Test
    public void testSearchAttachmentByContent() throws NoSuchFieldException, IllegalAccessException, IOException {
        // Given
        URL fileResource = this.getClass().getClassLoader().getResource("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
        // When
        indexAttachment("Scrum @eXo - Collector", fileResource.getPath(), "www.exo.com", "BCH", null);
        // Then
        assertEquals(1, storage.search(new WikiSearchData("Agile", null, null, null)).getPageSize());
        assertEquals(1, storage.search(new WikiSearchData(null, "Agile", null, null)).getPageSize());
    }
}