package org.exoplatform.wiki.jpa;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.*;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.IDType;

import java.util.*;

/**
 * Utility class to convert JPA entity objects
 */
public class EntityConverter {

  public static Wiki convertWikiEntityToWiki(WikiEntity wikiEntity) {
    Wiki wiki = null;
    if (wikiEntity != null) {
      wiki = new Wiki();
      wiki.setId(String.valueOf(wikiEntity.getId()));
      wiki.setType(wikiEntity.getType());
      wiki.setOwner(wikiEntity.getOwner());
      PageEntity wikiHomePageEntity = wikiEntity.getWikiHome();
      if (wikiHomePageEntity != null) {
        wiki.setWikiHome(convertPageEntityToPage(wikiHomePageEntity));
      }
      // wiki.setPermissions(wikiEntity.getPermissions());
      // wiki.setDefaultPermissionsInited();
      wiki.setPreferences(wiki.getPreferences());
    }
    return wiki;
  }

  public static WikiEntity convertWikiToWikiEntity(Wiki wiki, WikiDAO wikiDAO) {
    WikiEntity wikiEntity = null;
    if (wiki != null) {
      wikiEntity = new WikiEntity();
      wikiEntity.setType(wiki.getType());
      wikiEntity.setOwner(wiki.getOwner());
      wikiEntity.setWikiHome(convertPageToPageEntity(wiki.getWikiHome(), wikiDAO));
      // wikiEntity.setPermissions(wiki.getPermissions());
    }
    return wikiEntity;
  }

  public static Page convertPageEntityToPage(PageEntity pageEntity) {
    Page page = null;
    if (pageEntity != null) {
      page = new Page();
      page.setId(String.valueOf(pageEntity.getId()));
      page.setName(pageEntity.getName());
      WikiEntity wiki = pageEntity.getWiki();
      if (wiki != null) {
        page.setWikiId(String.valueOf(wiki.getId()));
        page.setWikiType(wiki.getType());
        page.setWikiOwner(wiki.getOwner());
      }
      page.setTitle(pageEntity.getTitle());
      page.setAuthor(pageEntity.getAuthor());
      page.setContent(pageEntity.getContent());
      page.setSyntax(pageEntity.getSyntax());
      page.setCreatedDate(pageEntity.getCreatedDate());
      page.setUpdatedDate(pageEntity.getUpdatedDate());
      page.setMinorEdit(pageEntity.isMinorEdit());
      page.setComment(pageEntity.getComment());
      page.setUrl(pageEntity.getUrl());
      page.setPermissions(convertPermissionEntitiesToPermissionEntries(pageEntity.getPermissions()));
      page.setActivityId(pageEntity.getActivityId());
    }
    return page;
  }

  public static List<PermissionEntry> convertPermissionEntitiesToPermissionEntries(List<PermissionEntity> permissionEntities) {
    List<PermissionEntry> permissionEntries = new ArrayList<>();
    if(permissionEntities != null) {
      // we fill a map to prevent duplicated entries
      Map<String, PermissionEntry> permissionEntriesMap = new HashMap<>();
      for(PermissionEntity permissionEntity : permissionEntities) {
        // only permission types relevant for pages are used
        if(permissionEntity.getPermissionType().equals(PermissionType.VIEWPAGE)
                || permissionEntity.getPermissionType().equals(PermissionType.EDITPAGE)) {
          Permission newPermission = new Permission(permissionEntity.getPermissionType(), true);
          if (permissionEntriesMap.get(permissionEntity.getIdentity()) != null) {
            PermissionEntry permissionEntry = permissionEntriesMap.get(permissionEntity.getIdentity());
            Permission[] permissions = permissionEntry.getPermissions();
            // add the new permission only if it does not exist yet
            if (!ArrayUtils.contains(permissions, newPermission)) {
              permissionEntry.setPermissions((Permission[]) ArrayUtils.add(permissions,
                      newPermission));
              permissionEntriesMap.put(permissionEntity.getIdentity(), permissionEntry);
            }
          } else {
            permissionEntriesMap.put(permissionEntity.getIdentity(), new PermissionEntry(
                    permissionEntity.getIdentity(),
                    null,
                    IDType.valueOf(permissionEntity.getIdentityType()),
                    new Permission[]{newPermission}));
          }
        }
      }
      permissionEntries = new ArrayList(permissionEntriesMap.values());

      // fill missing Permission (all PermissionEntry must have all Permission Types with isAllowed to true or false)
      List<PermissionType> pagepermissionTypes = Arrays.asList(PermissionType.VIEWPAGE, PermissionType.EDITPAGE);
      for(PermissionEntry permissionEntry : permissionEntries) {
        for(PermissionType permissionType : pagepermissionTypes) {
          boolean permissionTypeFound = false;
          for(Permission permission : permissionEntry.getPermissions()) {
            if(permission.getPermissionType().equals(permissionType)) {
              permissionTypeFound = true;
              break;
            }
          }
          if(!permissionTypeFound) {
            Permission newPermission = new Permission(permissionType, false);
            permissionEntry.setPermissions((Permission[])ArrayUtils.add(permissionEntry.getPermissions(), newPermission));
          }
        }
      }
    }
    return permissionEntries;
  }

  public static PageEntity convertPageToPageEntity(Page page, WikiDAO wikiDAO) {
    PageEntity pageEntity = null;
    if (page != null) {
      pageEntity = new PageEntity();
      pageEntity.setName(page.getName());
      if (page.getWikiId() != null) {
        WikiEntity wiki = wikiDAO.find(Long.parseLong(page.getWikiId()));
        if (wiki != null) {
          pageEntity.setWiki(wiki);
        }
      }
      pageEntity.setTitle(page.getTitle());
      pageEntity.setAuthor(page.getAuthor());
      pageEntity.setContent(page.getContent());
      pageEntity.setSyntax(page.getSyntax());
      pageEntity.setCreatedDate(page.getCreatedDate());
      pageEntity.setUpdatedDate(page.getUpdatedDate());
      pageEntity.setMinorEdit(page.isMinorEdit());
      pageEntity.setComment(page.getComment());
      pageEntity.setUrl(page.getUrl());
      pageEntity.setPermissions(convertPermissionEntriesToPermissionEntities(page.getPermissions()));
      pageEntity.setActivityId(page.getActivityId());
    }
    return pageEntity;
  }

  public static List<PermissionEntity> convertPermissionEntriesToPermissionEntities(List<PermissionEntry> permissionEntries) {
    List<PermissionEntity> permissionEntities = null;
    if(permissionEntries != null) {
      permissionEntities = new ArrayList<>();
      for (PermissionEntry permissionEntry : permissionEntries) {
        for (Permission permission : permissionEntry.getPermissions()) {
          if (permission.isAllowed()) {
            permissionEntities.add(new PermissionEntity(
                    permissionEntry.getId(),
                    permissionEntry.getIdType().toString(),
                    permission.getPermissionType()
            ));
          }
        }
      }
    }
    return permissionEntities;
  }

  public static Attachment convertAttachmentEntityToAttachment(AttachmentEntity attachmentEntity) {
    Attachment attachment = null;
    if (attachmentEntity != null) {
      attachment = new Attachment();
      attachment.setName(attachmentEntity.getName());
      attachment.setTitle(attachmentEntity.getTitle());
      attachment.setFullTitle(attachmentEntity.getFullTitle());
      attachment.setCreator(attachmentEntity.getCreator());
      if (attachmentEntity.getCreatedDate() != null) {
        Calendar createdDate = Calendar.getInstance();
        createdDate.setTime(attachmentEntity.getCreatedDate());
        attachment.setCreatedDate(createdDate);
      }
      if (attachmentEntity.getUpdatedDate() != null) {
        Calendar updatedDate = Calendar.getInstance();
        updatedDate.setTime(attachmentEntity.getUpdatedDate());
        attachment.setUpdatedDate(updatedDate);
      }
      attachment.setContent(attachmentEntity.getContent());
      attachment.setMimeType(attachmentEntity.getMimeType());
      attachment.setWeightInBytes(attachmentEntity.getWeightInBytes());
      // attachment.setPermissions(?);
    }
    return attachment;
  }

  public static AttachmentEntity convertAttachmentToAttachmentEntity(Attachment attachment) {
    AttachmentEntity attachmentEntity = null;
    if (attachment != null) {
      attachmentEntity = new AttachmentEntity();
      attachmentEntity.setName(attachment.getName());
      attachmentEntity.setTitle(attachment.getTitle());
      attachmentEntity.setFullTitle(attachment.getFullTitle());
      attachmentEntity.setCreator(attachment.getCreator());
      if (attachment.getCreatedDate() != null) {
        attachmentEntity.setCreatedDate(attachment.getCreatedDate().getTime());
      }
      if (attachment.getUpdatedDate() != null) {
        attachmentEntity.setUpdatedDate(attachment.getUpdatedDate().getTime());
      }
      attachmentEntity.setContent(attachment.getContent());
      attachmentEntity.setMimeType(attachment.getMimeType());
      // page.setPermissions(pageEntity.getPermissions());
    }
    return attachmentEntity;
  }

  public static DraftPage convertDraftPageEntityToDraftPage(DraftPageEntity draftPageEntity) {
    DraftPage draftPage = null;
    if (draftPageEntity != null) {
      draftPage = new DraftPage();
      draftPage.setId(String.valueOf(draftPageEntity.getId()));
      draftPage.setName(draftPageEntity.getName());
      draftPage.setTitle(draftPageEntity.getTitle());
      draftPage.setAuthor(draftPageEntity.getAuthor());
      draftPage.setContent(draftPageEntity.getContent());
      draftPage.setSyntax(draftPageEntity.getSyntax());
      draftPage.setCreatedDate(draftPageEntity.getCreatedDate());
      draftPage.setUpdatedDate(draftPageEntity.getUpdatedDate());
      PageEntity targetPage = draftPageEntity.getTargetPage();
      if (targetPage != null) {
        draftPage.setTargetPageId(String.valueOf(draftPageEntity.getTargetPage().getId()));
        draftPage.setTargetPageRevision(draftPageEntity.getTargetRevision());
      }
    }
    return draftPage;
  }

  public static DraftPageEntity convertDraftPageToDraftPageEntity(DraftPage draftPage, PageDAO pageDAO) {
    DraftPageEntity draftPageEntity = null;
    if (draftPage != null) {
      draftPageEntity = new DraftPageEntity();
      draftPageEntity.setName(draftPage.getName());
      draftPageEntity.setTitle(draftPage.getTitle());
      draftPageEntity.setAuthor(draftPage.getAuthor());
      draftPageEntity.setContent(draftPage.getContent());
      draftPageEntity.setSyntax(draftPage.getSyntax());
      draftPageEntity.setCreatedDate(draftPage.getCreatedDate());
      draftPageEntity.setUpdatedDate(draftPage.getUpdatedDate());
      String targetPageId = draftPage.getTargetPageId();
      if (StringUtils.isNotEmpty(targetPageId)) {
        draftPageEntity.setTargetPage(pageDAO.find(Long.valueOf(targetPageId)));
      }
      draftPageEntity.setTargetRevision(draftPage.getTargetPageRevision());
    }
    return draftPageEntity;
  }

  public static Template convertTemplateEntityToTemplate(TemplateEntity templateEntity) {
    Template template = null;
    if (templateEntity != null) {
      template = new Template();
      template.setId(String.valueOf(templateEntity.getId()));
      template.setName(templateEntity.getName());
      WikiEntity wiki = templateEntity.getWiki();
      if (wiki != null) {
        template.setWikiId(String.valueOf(wiki.getId()));
        template.setWikiType(wiki.getType());
        template.setWikiOwner(wiki.getOwner());
      }
      template.setTitle(templateEntity.getTitle());
      template.setContent(templateEntity.getContent());
      template.setSyntax(templateEntity.getSyntax());
    }
    return template;
  }

  public static TemplateEntity convertTemplateToTemplateEntity(Template template, WikiDAO wikiDAO) {
    TemplateEntity templateEntry = null;
    if (template != null) {
      templateEntry = new TemplateEntity();
      templateEntry.setName(template.getName());
      if (template.getWikiId() != null) {
        WikiEntity wiki = wikiDAO.find(Long.parseLong(template.getWikiId()));
        if (wiki != null) {
          templateEntry.setWiki(wiki);
        }
      }
      templateEntry.setTitle(template.getTitle());
      templateEntry.setContent(template.getContent());
      templateEntry.setSyntax(template.getSyntax());
    }
    return templateEntry;
  }

  public static EmotionIcon convertEmotionIconEntityToEmotionIcon(EmotionIconEntity emotionIconEntity) {
    EmotionIcon emotionIcon = null;
    if (emotionIconEntity != null) {
      emotionIcon = new EmotionIcon();
      emotionIcon.setName(emotionIconEntity.getName());
      emotionIcon.setImage(emotionIconEntity.getImage());
    }
    return emotionIcon;
  }

  public static EmotionIconEntity convertEmotionIconToEmotionIconEntity(EmotionIcon emotionIcon) {
    EmotionIconEntity emotionIconEntity = null;
    if (emotionIcon != null) {
      emotionIconEntity = new EmotionIconEntity();
      emotionIconEntity.setName(emotionIcon.getName());
      emotionIconEntity.setImage(emotionIcon.getImage());
    }
    return emotionIconEntity;
  }

  public static PermissionEntry convertPermissionEntityToPermissionEntry(PermissionEntity permissionEntity) {
    PermissionEntry permissionEntry = null;
    if (permissionEntity != null) {
      permissionEntry = new PermissionEntry();
      permissionEntry.setId(permissionEntity.getIdentity());
      permissionEntry.setIdType(IDType.valueOf(permissionEntity.getIdentityType().toUpperCase()));
      permissionEntry.setPermissions(new Permission[] { new Permission(permissionEntity.getPermissionType(), true) });
    }
    return permissionEntry;
  }

}
