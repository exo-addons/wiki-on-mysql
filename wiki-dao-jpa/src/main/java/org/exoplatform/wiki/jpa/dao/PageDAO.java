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

import java.util.Date;
import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wiki.jpa.entity.Page;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 24, 2015  
 */
public class PageDAO extends GenericDAOJPAImpl<Page, Long> {

    public List<Page> findRemovedPages(Page parentPage) {
        AuditReader reader = AuditReaderFactory.get(getEntityManager());
        AuditQuery query = reader.createQuery()
                .forRevisionsOfEntity(Page.class, true, true)
                .add(AuditEntity.property("parentPage").eq(parentPage))
                .add(AuditEntity.revisionType().eq(RevisionType.DEL));
        return query.getResultList();
    }

    public List<Number> getAllHistory(Page page){
      AuditReader reader = AuditReaderFactory.get(getEntityManager());
      return reader.getRevisions(Page.class,page.getId());
    }

    public Page getPageAtRevision(Page page, int revision){
      AuditReader reader = AuditReaderFactory.get(getEntityManager());
      return reader.find(Page.class, page.getId(), revision);
    }

    public int getCurrentVersion(Page page){
      AuditReader reader = AuditReaderFactory.get(getEntityManager());
      return (Integer) reader.getRevisionNumberForDate(new Date(Long.MAX_VALUE));
    }
}
