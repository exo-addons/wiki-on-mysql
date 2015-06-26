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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import junit.framework.TestCase;

import org.exoplatform.commons.api.jpa.EntityManagerService;
import org.exoplatform.wiki.jpa.dao.AttachmentDAO;
import org.exoplatform.wiki.jpa.entity.Attachment;

import com.ibm.icu.impl.Assert;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 25, 2015  
 */
public class TestAttachmentDAO extends BaseTest{

  public void testInsert(){
    AttachmentDAO attachmentDAO = new AttachmentDAO();
    Attachment att = new Attachment();
    att.setText("abc");
    attachmentDAO.create(att);
    Attachment got = attachmentDAO.find(att.getId());
    assertEquals("abc", got.getText());
  }
}
