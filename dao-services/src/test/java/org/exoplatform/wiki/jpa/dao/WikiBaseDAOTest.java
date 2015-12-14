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
import org.exoplatform.wiki.jpa.entity.PageAttachmentEntity;

import java.util.Date;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
public class WikiBaseDAOTest extends BaseWikiJPAIntegrationTest {

  public void testRollBackTransaction(){
    //Given
    //When
    PageAttachmentEntity attachment = new PageAttachmentEntity();
    attachment.setCreatedDate(new Date());
    attachment.setUpdatedDate(new Date());
    PageAttachmentEntity att = pageAttachmentDAO.create(attachment);
    //Then
    assertNotNull(pageAttachmentDAO.find(att.getId()));
  }

  public void testCommit(){
    //Given
    long count = pageAttachmentDAO.count();
    //When
    PageAttachmentEntity attachment = new PageAttachmentEntity();
    attachment.setCreatedDate(new Date());
    attachment.setUpdatedDate(new Date());
    PageAttachmentEntity att = pageAttachmentDAO.create(attachment);
    //Then
    assertEquals(new Long(count + 1), pageAttachmentDAO.count());
  }
}
