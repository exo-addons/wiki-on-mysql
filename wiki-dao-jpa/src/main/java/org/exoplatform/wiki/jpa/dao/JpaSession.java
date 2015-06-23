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

import javax.jcr.Node;
import javax.jcr.Session;

import org.chromattic.api.ChromatticException;
import org.chromattic.api.ChromatticSession;
import org.chromattic.api.Status;
import org.chromattic.api.event.EventListener;
import org.chromattic.api.query.QueryBuilder;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 23, 2015  
 */
public class JpaSession implements ChromatticSession {

  @Override
  public void addEventListener(EventListener arg0) throws NullPointerException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public <O> O copy(O arg0, String arg1) throws NullPointerException,
                                        IllegalArgumentException,
                                        ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O copy(Object arg0, O arg1, String arg2) throws NullPointerException,
                                                     IllegalArgumentException,
                                                     ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O create(Class<O> arg0) throws NullPointerException,
                                    IllegalArgumentException,
                                    ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O create(Class<O> arg0, String arg1) throws NullPointerException,
                                                 IllegalArgumentException,
                                                 ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> QueryBuilder<O> createQueryBuilder(Class<O> arg0) throws NullPointerException,
                                                              IllegalArgumentException,
                                                              ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O findById(Class<O> arg0, String arg1) throws NullPointerException,
                                                   ClassCastException,
                                                   ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O findByNode(Class<O> arg0, Node arg1) throws NullPointerException,
                                                   ClassCastException,
                                                   ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O findByPath(Class<O> arg0, String arg1) throws NullPointerException,
                                                     ClassCastException,
                                                     ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O findByPath(Object arg0, Class<O> arg1, String arg2) throws IllegalArgumentException,
                                                                  NullPointerException,
                                                                  ClassCastException,
                                                                  ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O findByPath(Class<O> arg0, String arg1, boolean arg2) throws NullPointerException,
                                                                   ClassCastException,
                                                                   ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <E> E getEmbedded(Object arg0, Class<E> arg1) throws NullPointerException,
                                                      IllegalArgumentException,
                                                      ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getId(Object arg0) throws NullPointerException,
                                  IllegalArgumentException,
                                  ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Session getJCRSession() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getName(Object arg0) throws NullPointerException,
                                    IllegalArgumentException,
                                    ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPath(Object arg0) throws NullPointerException,
                                    IllegalArgumentException,
                                    ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Status getStatus(Object arg0) throws NullPointerException,
                                      IllegalArgumentException,
                                      ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O insert(Class<O> arg0, String arg1) throws NullPointerException,
                                                 IllegalArgumentException,
                                                 ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O insert(Class<O> arg0, String arg1, String arg2) throws NullPointerException,
                                                              IllegalArgumentException,
                                                              ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O insert(Object arg0, Class<O> arg1, String arg2) throws NullPointerException,
                                                              IllegalArgumentException,
                                                              ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <O> O insert(Object arg0, Class<O> arg1, String arg2, String arg3) throws NullPointerException,
                                                                           IllegalArgumentException,
                                                                           ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isClosed() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String persist(Object arg0) throws NullPointerException,
                                    IllegalArgumentException,
                                    ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String persist(Object arg0, String arg1) throws NullPointerException,
                                                 IllegalArgumentException,
                                                 ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String persist(Object arg0, Object arg1) throws NullPointerException,
                                                 IllegalArgumentException,
                                                 ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String persist(Object arg0, String arg1, String arg2) throws NullPointerException,
                                                              IllegalArgumentException,
                                                              ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String persist(Object arg0, Object arg1, String arg2) throws NullPointerException,
                                                              IllegalArgumentException,
                                                              ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String persist(Object arg0, Object arg1, String arg2, String arg3) throws NullPointerException,
                                                                           IllegalArgumentException,
                                                                           ChromatticException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void remove(Object arg0) throws NullPointerException,
                                 IllegalArgumentException,
                                 ChromatticException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void save() throws ChromatticException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public <E> void setEmbedded(Object arg0, Class<E> arg1, E arg2) throws NullPointerException,
                                                                 IllegalArgumentException,
                                                                 ChromatticException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setName(Object arg0, String arg1) throws NullPointerException,
                                               IllegalArgumentException,
                                               ChromatticException {
    // TODO Auto-generated method stub
    
  }

}
