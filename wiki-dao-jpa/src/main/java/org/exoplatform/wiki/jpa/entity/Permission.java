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

import javax.persistence.*;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
@Entity
@ExoEntity
@Table(name = "WIKI_PERMISSIONS")
public class Permission {

  @Id
  @Column(name = "PERMISSION_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  /**
   * User or Group
   */
  @Column(name = "USER")
  private String user;

  @Column(name="TYPE")
  @Enumerated
  private PermissionType type;


  public Permission() {
    //Default constructor
  }

  public Permission(String user, PermissionType type) {
    this.user = user;
    this.type = type;
  }

  public long getId(){
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public PermissionType getType() {
    return type;
  }

  public void setType(PermissionType type) {
    this.type = type;
  }
}
