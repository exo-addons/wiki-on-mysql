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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.exoplatform.wiki.service.PermissionType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015  
 */
@Entity
@Table(name = "WIKI_ATTACHMENTS")
public class Attachment {
  @Id 
  @Column(name = "ATTACHMENT_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @Column(name = "WeightInBytes")
  private long weightInBytes;

  @Column(name = "Creator")
  private String creator;

  @Column(name = "CreatedDate")
  private Calendar createdDate;

  @Column(name = "UpdatedDate")
  private Calendar updatedDate;

  @Column(name = "DownloadURL")
  private String downloadURL;

  @Column(name = "Title")
  private String title;

  @Column(name = "Text")
  private String text;

  @OneToMany
  @Column(name="Permission")
  private List<AttachmentPermissionList> permission;

  public long getId(){return this.id;}

  public long getWeightInBytes(){
    return weightInBytes;
  }

  public String getCreator(){
    return creator;
  }

  public Calendar getCreatedDate(){
    return createdDate;
  }

  public Calendar getUpdatedDate(){
    return updatedDate;
  }

  public String getDownloadURL(){
    return downloadURL;
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

  public List<AttachmentPermissionList> getPermission(){
    return permission;
  }
  public void setPermission(List<AttachmentPermissionList> permission){
    this.permission = permission;
  }
  
  public boolean hasPermission(PermissionType permissionType){
    return false;
  }
}
