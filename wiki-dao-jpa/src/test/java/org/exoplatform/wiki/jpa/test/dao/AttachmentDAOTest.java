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
package org.exoplatform.wiki.jpa.test.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.wiki.jpa.dao.AttachmentDAO;
import org.exoplatform.wiki.jpa.entity.Attachment;
import org.exoplatform.wiki.jpa.entity.Permission;
import org.exoplatform.wiki.jpa.entity.PermissionType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 25, 2015  
 */
public class AttachmentDAOTest extends BaseTest{

  public void testInsertDelete(){
    //Given
    AttachmentDAO attachmentDAO = getService(AttachmentDAO.class);
    Attachment att = new Attachment();
    att.setText("abc");
    //When
    attachmentDAO.create(att);
    Long id = att.getId();
    //Then
    Attachment got = attachmentDAO.find(id);
    assertEquals("abc", got.getText());
    //Delete
    attachmentDAO.delete(att);
    assertNull(attachmentDAO.find(id));
  }

  public void testUpdate(){
    //Given
    AttachmentDAO attachmentDAO = getService(AttachmentDAO.class);
    Attachment att = new Attachment();
    att.setText("abc");
    //When
    attachmentDAO.create(att);
    Long id = att.getId();
    Permission per = new Permission();
    per.setUser("user");
    per.setType(PermissionType.ADMINPAGE);
    List<Permission> permissions = new ArrayList<Permission>();
    permissions.add(per);
    att.setPermission(permissions);
    att.setText("def");
    att.setWeightInBytes((long) 1000);
    att.setCreator("creator");
    att.setDownloadURL("http://exoplatform.com");
    att.setTitle("title");
    Date date = new Date();
    att.setUpdatedDate(date);
    //Then
    attachmentDAO.update(att);
    Attachment got = attachmentDAO.find(id);
    assertEquals("def", got.getText());
    assertEquals("http://exoplatform.com", got.getDownloadURL());
    assertEquals("title", got.getTitle());
    assertEquals("creator", got.getCreator());
    assertEquals((long) 1000, got.getWeightInBytes());
    assertEquals(date, got.getUpdatedDate());
    Permission got_per = got.getPermission().get(0);
    assertEquals("user", got_per.getUser());
  }
}
