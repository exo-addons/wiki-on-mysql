package org.exoplatform.wiki.jpa.search;

import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;

/**
 * Listener on pages creation/update/deletion to index them
 */
public class PageIndexingListener extends PageWikiListener {

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    IndexingService indexingService  = CommonsUtils.getService(IndexingService.class);
    indexingService.index(WikiPageIndexingServiceConnector.TYPE, page.getId());
  }

  @Override
  public void postUpdatePage(String wikiType, String wikiOwner, String pageId, Page page, PageUpdateType wikiUpdateType) throws WikiException {
    IndexingService indexingService  = CommonsUtils.getService(IndexingService.class);
    indexingService.reindex(WikiPageIndexingServiceConnector.TYPE, page.getId());
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    IndexingService indexingService  = CommonsUtils.getService(IndexingService.class);
    indexingService.unindex(WikiPageIndexingServiceConnector.TYPE, page.getId());
  }
}
