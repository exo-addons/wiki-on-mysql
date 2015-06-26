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

import org.exoplatform.wiki.jpa.dao.AttachmentDAO;
import org.exoplatform.wiki.jpa.entity.Attachment;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
public class TestWikiBaseDAO extends BaseTest {
  public void testRollBackTransaction(){
    AttachmentDAO dao = new AttachmentDAO();
    dao.beginTransaction();
    Attachment att = dao.create(new Attachment());
    assertNotNull(dao.find(att.getId()));
    dao.rollback();
    assertNull(dao.find(att.getId()));
  }
  public void testCommit(){
    
    AttachmentDAO dao = new AttachmentDAO();
    long count = dao.count();
    dao.beginTransaction();
    Attachment att = dao.create(new Attachment());
    dao.commit();
    assertEquals(new Long(count + 1), dao.count());
    dao.delete(att);
  }
}
