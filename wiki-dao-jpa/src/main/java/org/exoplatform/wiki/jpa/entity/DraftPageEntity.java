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
import java.util.Date;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * Jun 23, 2015
 */
@Entity
@ExoEntity
@Table(name = "WIKI_DRAFT_PAGES")
@NamedQueries({
        @NamedQuery(name = "wikiDraftPage.findDraftPagesByUser", query = "SELECT d FROM DraftPageEntity d WHERE d.author = :username ORDER BY d.updatedDate DESC"),
        @NamedQuery(name = "wikiDraftPage.findDraftPageByUserAndTargetPage", query = "SELECT d FROM DraftPageEntity d WHERE d.author = :username AND d.targetPage.id = :targetPageId"),
        @NamedQuery(name = "wikiDraftPage.deleteDraftPagesByUserAndTargetPage", query = "DELETE FROM DraftPageEntity d WHERE d.author = :username AND d.targetPage.id = :targetPageId"),
        @NamedQuery(name = "wikiDraftPage.deleteDraftPagesByUserAndName", query = "DELETE FROM DraftPageEntity d WHERE d.author = :username AND d.name = :draftPageName")
})
public class DraftPageEntity extends BasePage {

  @Id
  @Column(name = "DRAFT_PAGE_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "TARGET_PAGE_ID")
  private PageEntity targetPage;

  @Column(name = "TARGET_PAGE_REVISION")
  private String targetRevision;

  @Column(name = "NEW_PAGE")
  private boolean newPage;

  @Column(name = "AUTHOR")
  private String author;

  @Column(name = "NAME")
  private String name;

  @Column(name = "TITLE")
  private String title;

  @Column(name = "CONTENT")
  private String content;

  @Column(name = "SYNTAX")
  private String syntax;

  @Column(name = "CREATED_DATE")
  private Date createdDate;

  @Column(name = "UPDATED_DATE")
  private Date updatedDate;

  public PageEntity getTargetPage() {
    return targetPage;
  }

  public void setTargetPage(PageEntity targetPage) {
    this.targetPage = targetPage;
  }

  public String getTargetRevision() {
    return targetRevision;
  }

  public void setTargetRevision(String targetRevision) {
    this.targetRevision = targetRevision;
  }

  public boolean isNewPage() {
    return newPage;
  }

  public void setNewPage(boolean newPage) {
    this.newPage = newPage;
  }

  public long getId() {
    return id;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
}
