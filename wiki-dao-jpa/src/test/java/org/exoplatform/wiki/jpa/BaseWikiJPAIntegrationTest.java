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
  protected TemplateDAO    templateDAO;
  protected EmotionIconDAO emotionIconDAO;
  private Connection       conn;
  private Liquibase        liquibase;

  public void setUp() {
    super.setUp();
    // Init Liquibase
    try {
      Class.forName("org.hsqldb.jdbcDriver");
      conn = DriverManager.getConnection("jdbc:hsqldb:mem:db1", "sa", "");
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
      liquibase = new Liquibase("db/changelog/wiki.db.changelog-test.xml", new ClassLoaderResourceAccessor(), database);
      liquibase.update((String) null);
    } catch (ClassNotFoundException | SQLException | LiquibaseException e) {
      fail(e.getMessage());
    }
    // Init DAO

    wikiDAO = PortalContainer.getInstance().getComponentInstanceOfType(WikiDAO.class);
    pageDAO = PortalContainer.getInstance().getComponentInstanceOfType(PageDAO.class);
    attachmentDAO = PortalContainer.getInstance().getComponentInstanceOfType(AttachmentDAO.class);
    draftPageDAO = PortalContainer.getInstance().getComponentInstanceOfType(DraftPageDAO.class);
    templateDAO = PortalContainer.getInstance().getComponentInstanceOfType(TemplateDAO.class);
    emotionIconDAO = PortalContainer.getInstance().getComponentInstanceOfType(EmotionIconDAO.class);
    // Clean Data
    cleanDB();
  }

  public void tearDown() {
    super.tearDown();
    // Close DB
    try {
      liquibase.rollback(1000, null);
      conn.close();
    } catch (SQLException | LiquibaseException e) {
      fail(e.getMessage());
    }
  }

  private void cleanDB() {
    emotionIconDAO.deleteAll();
    templateDAO.deleteAll();
    draftPageDAO.deleteAll();
    attachmentDAO.deleteAll();
    pageDAO.deleteAll();
    wikiDAO.deleteAll();
  }
}
