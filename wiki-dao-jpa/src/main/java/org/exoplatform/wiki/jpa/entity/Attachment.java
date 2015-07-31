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

import java.util.*;

import javax.persistence.*;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.wiki.service.PermissionType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015  
 */
@Entity
@ExoEntity
@Table(name = "WIKI_ATTACHMENTS")
public class Attachment {
  @Id 
  @Column(name = "ATTACHMENT_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(name = "WEIGHT_IN_BYTES")
  private Long weightInBytes;

  @Column(name = "CREATOR")
  private String creator;

  @Column(name = "CREATED_DATE")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @Column(name = "UPDATED_DATE")
  @Temporal(TemporalType.TIMESTAMP)
  private Date updatedDate;

  @Column(name = "DOWNLOAD_URL")
  private String downloadURL;

  @Column(name = "TITLE")
  private String title;

  @Column(name = "TEXT")
  private String text;

  @OneToMany
  private List<Permission> permission;

  public long getId(){return this.id;}

  public void setId(Long id) {
    this.id = id;
  }

  public long getWeightInBytes(){
    return weightInBytes;
  }

  public void setWeightInBytes(Long weightInBytes) {
    this.weightInBytes = weightInBytes;
  }

  public String getCreator(){
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public Date getCreatedDate(){
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Date getUpdatedDate(){
    return updatedDate;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getDownloadURL(){
    return downloadURL;
  }

  public void setDownloadURL(String downloadURL) {
    this.downloadURL = downloadURL;
  }

  public String getTitle(){
    return title;
  }

  public void setTitle(String title){
    this.title = title;
  }

  public String getText(){
    return text;
  }

  public void setText(String text){
    this.text = text;
  }

  public List<Permission> getPermission(){
    return permission;
  }
  public void setPermission(List<Permission> permission){
    this.permission = permission;
  }
  
  public boolean hasPermission(PermissionType permissionType){
    throw new UnsupportedOperationException("Not implemented");
  }
}
