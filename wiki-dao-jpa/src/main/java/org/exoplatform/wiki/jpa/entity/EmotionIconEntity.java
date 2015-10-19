package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;

@Entity
@ExoEntity
@Table(name = "WIKI_EMOTION_ICONS")
@NamedQueries({
        @NamedQuery(name = "emotionIcon.getEmotionIconByName", query = "SELECT e FROM EmotionIconEntity e WHERE e.name = :name")
})
public class EmotionIconEntity {
  @Id
  @Column(name = "EMOTION_ICON_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @Column(name = "NAME")
  private String name;

  @Lob
  @Column(name = "IMAGE", length = 20971520)
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
