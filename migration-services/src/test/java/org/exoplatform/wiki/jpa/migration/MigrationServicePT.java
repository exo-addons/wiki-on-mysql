package org.exoplatform.wiki.jpa.migration;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.bench.WikiDataInjector;
import org.exoplatform.wiki.jpa.JPADataStorage;
import org.exoplatform.wiki.jpa.dao.*;
import org.exoplatform.wiki.mow.api.Attachment;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.impl.JCRDataStorage;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class MigrationServicePT extends MigrationITSetup {

  @Override
  public void setUp() {
    super.setUp();

    WikiDataInjector wikiDataInjector = PortalContainer.getInstance().getComponentInstanceOfType(WikiDataInjector.class);
    HashMap<String, String> injectorParams = new HashMap<>();
    injectorParams.put("type", "data");
    injectorParams.put("q", "1,10,10");
    injectorParams.put("pre", "Public%20Wiki%20A,Public%20Wiki%20AB,Public%20Wiki%20ABC");
    injectorParams.put("wo", "intranet");
    injectorParams.put("wt", "portal");
    injectorParams.put("maxAtt", "50");
    try {
      startSessionAs("john");
      wikiDataInjector.inject(injectorParams);
    } catch (WikiException e) {
      e.printStackTrace();
    }
  }

  public void testWikiMigration() throws WikiException {
    migrationService.start();
  }
}
