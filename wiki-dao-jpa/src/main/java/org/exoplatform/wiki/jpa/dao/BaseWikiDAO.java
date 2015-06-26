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
package org.exoplatform.wiki.jpa.dao;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.exoplatform.commons.api.jpa.EntityManagerService;
import org.exoplatform.commons.api.jpa.dao.AbstractGenericDAO;
import org.exoplatform.container.PortalContainer;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 25, 2015  
 */
public abstract class BaseWikiDAO<E, ID extends Serializable> extends AbstractGenericDAO<E,ID>{
  public boolean beginTransaction(){
    if(getEntityManager().getTransaction().isActive())
      return false;
    else{
      getEntityManager().getTransaction().begin();
      return true;
    }
  }
  public boolean commit(){
    if(getEntityManager().getTransaction().isActive()){
      getEntityManager().getTransaction().commit();
      return true;    
    }
    else{      
      return false;
    }
  }
  public boolean rollback(){
    if(getEntityManager().getTransaction().isActive()){
      getEntityManager().getTransaction().rollback();
      return true;    
    }
    else{      
      return false;
    }
  }
  private EntityManager em;
  @Override
  public EntityManager getEntityManager() {
    if(this.em == null)     
      this.em = PortalContainer.getInstance()
      .getComponentInstanceOfType(EntityManagerService.class).getEntityManager();
    return this.em;
  }
  public void injectEM(EntityManager em){
    this.em = em;
  }
}
