package org.exoplatform.wiki.jpa.mock;

import org.exoplatform.commons.api.persistence.DataInitializer;

/**
 * Mock class for DataInitializer
 * Data are initialized directly in unit tests setup
 */
public class MockDataInitializer implements DataInitializer {
  @Override
  public void initData() {
  }

  @Override
  public void initData(String datasourceName) {
  }
}
