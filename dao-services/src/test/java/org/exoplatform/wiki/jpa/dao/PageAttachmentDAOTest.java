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
package org.exoplatform.wiki.jpa.dao;

import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.entity.*;
import org.exoplatform.wiki.mow.api.PermissionType;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 25, 2015  
 */
public class PageAttachmentDAOTest extends BaseWikiJPAIntegrationTest {

  public void testInsertDelete() throws IOException, URISyntaxException {
    //Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity page = new PageEntity();
    page.setName("page1");
    page.setWiki(wiki);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page = pageDAO.create(page);
    URL fileResource = this.getClass().getClassLoader().getResource("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
    PageAttachmentEntity att = new PageAttachmentEntity();
    att.setContent(Files.readAllBytes(Paths.get(fileResource.toURI())));
    att.setName("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
    att.setPage(page);
    att.setCreatedDate(new Date());
    att.setUpdatedDate(new Date());
    //When
    pageAttachmentDAO.create(att);
    Long id = att.getId();
    //Then
    AttachmentEntity got = pageAttachmentDAO.find(id);
    assertNotNull(got.getContent());
    assertEquals(new File(fileResource.toURI()).length(), got.getWeightInBytes());
    //Delete
    pageAttachmentDAO.delete(att);
    assertNull(pageAttachmentDAO.find(id));
  }

  public void testUpdate() throws IOException, URISyntaxException {
    //Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity page = new PageEntity();
    page.setName("page1");
    page.setWiki(wiki);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page = pageDAO.create(page);
    URL fileResource = this.getClass().getClassLoader().getResource("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
    PageAttachmentEntity att = new PageAttachmentEntity();
    att.setContent(Files.readAllBytes(Paths.get(fileResource.toURI())));
    att.setName("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
    att.setPage(page);
    att.setCreatedDate(new Date());
    att.setUpdatedDate(new Date());
    //When
    pageAttachmentDAO.create(att);
    Long id = att.getId();
    PermissionEntity per = new PermissionEntity();
    per.setIdentity("user");
    per.setIdentityType("User");
    per.setPermissionType(PermissionType.ADMINPAGE);
    List<PermissionEntity> permissions = new ArrayList<>();
    permissions.add(per);
    att.setCreator("creator");
    att.setTitle("title");
    Date date = new Date();
    att.setUpdatedDate(date);
    //Then
    pageAttachmentDAO.update(att);
    AttachmentEntity got = pageAttachmentDAO.find(id);
    assertEquals("title", got.getTitle());
    assertEquals("creator", got.getCreator());
    assertEquals(date, got.getUpdatedDate());
  }
}
