package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;

@Entity
@ExoEntity
@Table(name = "WIKI_EMOTICONS")
public class EmotionIcon {
  @Id
  @Column(name = "EMOTICON_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @Column(name = "NAME")
  private String name;

  @Lob
  @Column(name = "IMAGE")
  private byte[] image;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }
}
