/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.persistence.*;

import java.util.Date;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 7/16/15
 */
@Entity
@ExoEntity
@Audited
@Table(name = "WIKI_PAGES")
@NamedQueries({
        @NamedQuery(name = "wikiPage.getAllIds", query = "SELECT p.id FROM Page p ORDER BY p.id"),
        @NamedQuery(name = "wikiPage.getPageOfWikiByName", query = "SELECT p FROM Page p JOIN p.wiki w WHERE p.name = :name AND w.type = :type AND w.owner = :owner"),
        @NamedQuery(name = "wikiPage.getChildrenPages", query = "SELECT p FROM Page p WHERE p.parentPage.id = :id")
})
public class Page extends BasePage {

  @Id
  @Column(name = "PAGE_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "WIKI_ID")
  private Wiki wiki;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "PARENT_PAGE_ID")
  private Page parentPage;

  @OneToMany(cascade = CascadeType.ALL)
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  private List<Attachment> attachments;

  @Column(name = "AUTHOR")
  private String author;

  public long getId() {
    return id;
  }

  @Column(name = "NAME")
  private String name;

  @Column(name = "OWNER")
  private String owner;

  @Column(name = "CREATED_DATE")
  private Date createdDate;

  @Column(name = "UPDATED_DATE")
  private Date updatedDate;

  @Column(name = "CONTENT")
  private String content;

  @Column(name = "SYNTAX")
  private String syntax;

  @Column(name = "TITLE")
  private String title;

  @Column(name = "COMMENT")
  private String comment;

  @Column(name = "URL")
  private String url;

  @Column(name = "IS_MINOR_EDIT")
  private boolean isMinorEdit;

  @Column(name = "ACTIVITY_ID")
  private String activityId;

  @OneToMany(cascade = CascadeType.ALL)
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  private List<Permission> permissions;

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Date getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getSyntax() {
    return syntax;
  }

  public void setSyntax(String syntax) {
    this.syntax = syntax;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isMinorEdit() {
    return isMinorEdit;
  }

  public void setMinorEdit(boolean isMinorEdit) {
    this.isMinorEdit = isMinorEdit;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public List<Permission> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<Permission> permission) {
    this.permissions = permission;
  }

  public Wiki getWiki() {
    return wiki;
  }

  public void setWiki(Wiki wiki) {
    this.wiki = wiki;
  }

  public Page getParentPage() {
    return parentPage;
  }

  public void setParentPage(Page parentPage) {
    this.parentPage = parentPage;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }
}
