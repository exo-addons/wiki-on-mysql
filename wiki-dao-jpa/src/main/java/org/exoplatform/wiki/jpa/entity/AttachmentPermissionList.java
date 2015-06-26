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

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
@Entity
@Table(name = "WIKI_ATTACHMENT_PERMISSIONS_LIST")
public class AttachmentPermissionList {
  @Id
  @Column(name = "ATTACHMENT_PERMISSION_LIST_ID")
  private long id;
  public long getId(){
    return this.id;
  }
  @Column(name = "Key")
  private String key;
  public String getKey(){
    return key;
  }
  public void setKey(String key){
    this.key = key;
  }
  
  @ElementCollection  
  @CollectionTable(name="WIKI_ATTACHMENT_PERMISSIONS_LIST", joinColumns=@JoinColumn(name="ATTACHMENT_PERMISSION_LIST_ID"))
  @Column(name="Value")
  private List<String> values;
  
}
