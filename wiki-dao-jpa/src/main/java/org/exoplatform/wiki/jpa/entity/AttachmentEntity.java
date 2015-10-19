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
import org.exoplatform.wiki.mow.api.PermissionType;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015  
 */
@Entity
@ExoEntity
@Table(name = "WIKI_ATTACHMENTS")
public class AttachmentEntity {
  @Id 
  @Column(name = "ATTACHMENT_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(name = "NAME")
  private String name;

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

  @Lob
  @Column(name = "CONTENT")
  private byte[] content;

  @ManyToOne

  private PageEntity page;

  @OneToMany(cascade=CascadeType.ALL)
  private List<PermissionEntity> permissions;

  public long getId(){return this.id;}

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getWeightInBytes(){
    return content==null?0:content.length;
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

  public byte[] getContent() {
    return content;
  }

  public void setContent(byte[] content) {
    this.content = content;
  }

  public PageEntity getPage() {
    return page;
  }

  public void setPage(PageEntity page) {
    this.page = page;
  }

  public List<PermissionEntity> getPermissions(){
    return permissions;
  }
  public void setPermissions(List<PermissionEntity> permission){
    this.permissions = permission;
  }
  
  public boolean hasPermission(PermissionType permissionType){
    throw new UnsupportedOperationException("Not implemented");
  }
}
