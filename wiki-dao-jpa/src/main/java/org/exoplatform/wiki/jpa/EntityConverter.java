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
      wiki.setPermissions(convertPermissionEntitiesToPermissionEntries(wikiEntity.getPermissions(),
              Arrays.asList(PermissionType.VIEWPAGE, PermissionType.EDITPAGE, PermissionType.ADMINPAGE, PermissionType.ADMINSPACE)));
      // wiki.setDefaultPermissionsInited();
      WikiPreferences wikiPreferences = new WikiPreferences();
      WikiPreferencesSyntax wikiPreferencesSyntax = new WikiPreferencesSyntax();
      wikiPreferencesSyntax.setDefaultSyntax(wikiEntity.getSyntax());
      wikiPreferencesSyntax.setAllowMultipleSyntaxes(wikiEntity.isAllowMultipleSyntax());
      wikiPreferences.setWikiPreferencesSyntax(wikiPreferencesSyntax);
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
      wikiEntity.setPermissions(convertPermissionEntriesToPermissionEntities(wiki.getPermissions()));
      WikiPreferences wikiPreferences = wiki.getPreferences();
      if(wikiPreferences != null) {
        WikiPreferencesSyntax wikiPreferencesSyntax = wikiPreferences.getWikiPreferencesSyntax();
        if(wikiPreferencesSyntax != null) {
          wikiEntity.setSyntax(wikiPreferencesSyntax.getDefaultSyntax());
          wikiEntity.setAllowMultipleSyntax(wikiPreferencesSyntax.isAllowMultipleSyntaxes());
        }
      }
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
      page.setOwner(pageEntity.getOwner());
      page.setAuthor(pageEntity.getAuthor());
      page.setContent(pageEntity.getContent());
      page.setSyntax(pageEntity.getSyntax());
      page.setCreatedDate(pageEntity.getCreatedDate());
      page.setUpdatedDate(pageEntity.getUpdatedDate());
      page.setMinorEdit(pageEntity.isMinorEdit());
      page.setComment(pageEntity.getComment());
      page.setUrl(pageEntity.getUrl());
      page.setPermissions(convertPermissionEntitiesToPermissionEntries(pageEntity.getPermissions(),
              Arrays.asList(PermissionType.VIEWPAGE, PermissionType.EDITPAGE)));
      page.setActivityId(pageEntity.getActivityId());
    }
    return page;
  }

  public static List<PermissionEntry> convertPermissionEntitiesToPermissionEntries(List<PermissionEntity> permissionEntities,
                                                                                   List<PermissionType> filteredPermissionTypes) {
    List<PermissionEntry> permissionEntries = new ArrayList<>();
    if(permissionEntities != null) {
      // we fill a map to prevent duplicated entries
      Map<String, PermissionEntry> permissionEntriesMap = new HashMap<>();
      for(PermissionEntity permissionEntity : permissionEntities) {
        // only permission types relevant for pages are used
        if(filteredPermissionTypes.contains(permissionEntity.getPermissionType())) {
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
      for(PermissionEntry permissionEntry : permissionEntries) {
        for(PermissionType permissionType : filteredPermissionTypes) {
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
      pageEntity.setOwner(page.getOwner());
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
      draftPage.setNewPage(draftPageEntity.isNewPage());
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
      draftPageEntity.setNewPage(draftPage.isNewPage());
      String targetPageId = draftPage.getTargetPageId();
      if (StringUtils.isNotEmpty(targetPageId)) {
        draftPageEntity.setTargetPage(pageDAO.find(Long.valueOf(targetPageId)));
      }
      draftPageEntity.setTargetRevision(draftPage.getTargetPageRevision());
    }
    return draftPageEntity;
  }

  public static PageVersion convertPageVersionEntityToPageVersion(PageVersionEntity pageVersionEntity) {
    PageVersion pageVersion = null;
    if (pageVersionEntity != null) {
      pageVersion = new PageVersion();
      pageVersion.setName(String.valueOf(pageVersionEntity.getVersionNumber()));
      pageVersion.setAuthor(pageVersionEntity.getAuthor());
      pageVersion.setContent(pageVersionEntity.getContent());
      pageVersion.setCreatedDate(pageVersionEntity.getCreatedDate());
      pageVersion.setUpdatedDate(pageVersionEntity.getUpdatedDate());
    }
    return pageVersion;
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
      template.setDescription(templateEntity.getDescription());
      template.setContent(templateEntity.getContent());
      template.setSyntax(templateEntity.getSyntax());
      template.setCreatedDate(templateEntity.getCreatedDate());
      template.setUpdatedDate(templateEntity.getUpdatedDate());
    }
    return template;
  }

  public static TemplateEntity convertTemplateToTemplateEntity(Template template, WikiDAO wikiDAO) {
    TemplateEntity templateEntity = null;
    if (template != null) {
      templateEntity = new TemplateEntity();
      templateEntity.setName(template.getName());
      if (template.getWikiId() != null) {
        WikiEntity wiki = wikiDAO.find(Long.parseLong(template.getWikiId()));
        if (wiki != null) {
          templateEntity.setWiki(wiki);
        }
      }
      templateEntity.setTitle(template.getTitle());
      templateEntity.setDescription(template.getDescription());
      templateEntity.setContent(template.getContent());
      templateEntity.setSyntax(template.getSyntax());
      templateEntity.setCreatedDate(template.getCreatedDate());
      templateEntity.setUpdatedDate(template.getUpdatedDate());
    }
    return templateEntity;
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
