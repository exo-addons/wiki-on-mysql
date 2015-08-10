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
package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015  
 */
@Entity
@ExoEntity
@Table(name = "WIKI_DRAFT_PAGES")
public class DraftPage extends BasePage {

    @Id
    @Column(name="DRAFT_PAGE_ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    @OneToOne(cascade=CascadeType.ALL)
    private Page targetPage;
    
    @Column(name="TARGET_REVISION")
    private int targetRevision;
    
    @Column(name="IS_NEW_PAGE")
    private boolean isNewPage;

    public Page getTargetPage() {
      return targetPage;
    }

    public void setTargetPage(Page targetPage) {
      this.targetPage = targetPage;
    }

    public int getTargetRevision() {
      return targetRevision;
    }

    public void setTargetRevision(int targetRevision) {
      this.targetRevision = targetRevision;
    }

    public boolean isNewPage() {
      return isNewPage;
    }

    public void setNewPage(boolean isNewPage) {
      this.isNewPage = isNewPage;
    }

    public long getId() {
      return id;
    }
    
    
}
