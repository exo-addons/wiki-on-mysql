package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;

@Entity
@ExoEntity
@Table(name = "WIKI_WATCHERS")
public class Watcher {
  public Watcher() {
  }

  public Watcher(String username) {
    this.username = username;
  }

  @Id
  @Column(name = "WATCHER_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @Column(name = "USERNAME")
  private String username;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Watcher)) return false;

    Watcher watcher = (Watcher) o;

    return username.equals(watcher.username);

  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }
}
