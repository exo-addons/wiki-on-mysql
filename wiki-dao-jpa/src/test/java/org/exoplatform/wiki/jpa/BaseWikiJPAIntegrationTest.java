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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.wiki.jpa.dao.*;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 10/20/15
 */
public abstract class BaseWikiJPAIntegrationTest extends BaseTest {
  protected WikiDAO        wikiDAO;
  protected PageDAO        pageDAO;
  protected AttachmentDAO  attachmentDAO;
  protected DraftPageDAO   draftPageDAO;
  protected PageVersionDAO pageVersionDAO;
  protected PageMoveDAO    pageMoveDAO;
  protected TemplateDAO    templateDAO;
  protected EmotionIconDAO emotionIconDAO;

  public void setUp() {
    super.setUp();

    // make sure data are well initialized for each test
    DataInitializer dataInitializer = PortalContainer.getInstance().getComponentInstanceOfType(DataInitializer.class);
    dataInitializer.initData();

    // Init DAO
    wikiDAO = PortalContainer.getInstance().getComponentInstanceOfType(WikiDAO.class);
    pageDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageDAO.class);
    attachmentDAO = PortalContainer.getInstance().getComponentInstanceOfType(AttachmentDAO.class);
    draftPageDAO = PortalContainer.getInstance().getComponentInstanceOfType(DraftPageDAO.class);
    pageVersionDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageVersionDAO.class);
    pageMoveDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageMoveDAO.class);
    templateDAO = PortalContainer.getInstance().getComponentInstanceOfType(TemplateDAO.class);
    emotionIconDAO = PortalContainer.getInstance().getComponentInstanceOfType(EmotionIconDAO.class);
    // Clean Data
    cleanDB();
  }

  public void tearDown() {
    // Clean Data
    cleanDB();
    super.tearDown();
  }

  private void cleanDB() {
    emotionIconDAO.deleteAll();
    templateDAO.deleteAll();
    pageMoveDAO.deleteAll();
    pageVersionDAO.deleteAll();
    draftPageDAO.deleteAll();
    attachmentDAO.deleteAll();
    pageDAO.deleteAll();
    wikiDAO.deleteAll();
  }
}
