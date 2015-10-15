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

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.persistence.*;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015
 */
@Entity
@Table(name = "WIKI_WIKIS")
@NamedQueries({
        @NamedQuery(name = "wiki.getAllIds", query = "SELECT w.id FROM Wiki w ORDER BY w.id"),
        @NamedQuery(name = "wiki.getWikisByType", query = "SELECT w FROM Wiki w WHERE w.type = :type"),
        @NamedQuery(name = "wiki.getWikiByTypeAndOwner", query = "SELECT w FROM Wiki w WHERE w.type = :type AND w.owner = :owner")
})
public class Wiki {
  @Id
  @Column(name = "WIKI_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;


  @Column(name = "NAME")
  private String name;

  @Column(name = "OWNER")
  private String owner;

  @Column(name = "TYPE")
  private String type;

  @OneToOne
  @JoinColumn(name = "WIKI_HOME")
  private Page wikiHome;

  @OneToMany(cascade=CascadeType.ALL)
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  private List<Permission> permissions;

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Wiki setName(String name) {
    this.name = name;
    return this;
  }

  public String getOwner() {
    return owner;
  }

  public Wiki setOwner(String owner) {
    this.owner = owner;
    return this;
  }

  public String getType() {
    return type;
  }

  public Wiki setType(String type) {
    this.type = type;
    return this;
  }

  public Page getWikiHome() {
    return wikiHome;
  }

  public Wiki setWikiHome(Page wikiHome) {
    this.wikiHome = wikiHome;
    return this;
  }

  public List<Permission> getPermissions() {
    return permissions;
  }

  public Wiki setPermissions(List<Permission> permissions) {
    this.permissions = permissions;
    return this;
  }
}
