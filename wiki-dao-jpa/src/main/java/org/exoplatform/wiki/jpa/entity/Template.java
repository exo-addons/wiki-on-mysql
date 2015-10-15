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

import javax.persistence.*;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 7/16/15
 */
@Entity
@ExoEntity
@Table(name = "WIKI_TEMPLATES")
@NamedQueries({
        @NamedQuery(name = "template.getTemplatesOfWiki", query = "SELECT t FROM Template t JOIN t.wiki w WHERE w.type = :type AND w.owner = :owner"),
        @NamedQuery(name = "template.getTemplateOfWikiByName", query = "SELECT t FROM Template t JOIN t.wiki w WHERE t.name = :name AND w.type = :type AND w.owner = :owner"),
        @NamedQuery(name = "template.searchTemplatesByTitle", query = "SELECT t FROM Template t JOIN t.wiki w WHERE w.type = :type AND w.owner = :owner AND t.title like :searchText")
})
public class Template extends BasePage {

  @Id
  @Column(name = "TEMPLATE_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "WIKI_ID")
  private Wiki wiki;

  public long getId() {
    return id;
  }

  @Column(name = "NAME")
  private String name;

  @Column(name = "CONTENT")
  private String content;

  @Column(name = "SYNTAX")
  private String syntax;

  @Column(name = "TITLE")
  private String title;

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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Wiki getWiki() {
    return wiki;
  }

  public void setWiki(Wiki wiki) {
    this.wiki = wiki;
  }
}
