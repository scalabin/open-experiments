/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.kernel.persistence;

import com.google.inject.Scopes;

import org.osgi.framework.BundleContext;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.persistence.DataSourceService;
import org.sakaiproject.kernel.api.registry.RegistryService;
import org.sakaiproject.kernel.guice.AbstractOsgiModule;
import org.sakaiproject.kernel.persistence.dbcp.DataSourceServiceImpl;
import org.sakaiproject.kernel.persistence.geronimo.TransactionManagerProvider;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

/**
 * Configuration module for persistence bindings.
 */
public class ActivatorModule extends AbstractOsgiModule {

  public ActivatorModule(BundleContext bundleContext) {
    super(bundleContext);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    super.configure();
    
    // bind in the current context
    bind(BundleContext.class).toInstance(bundleContext);

    // bind the EntityManager to a ScopedEntityManger
    bind(export(EntityManager.class)).to(ScopedEntityManager.class)
        .in(Scopes.SINGLETON);
    // Bind in the datasource
    bind(export(DataSource.class)).toProvider(DataSourceServiceImpl.class).in(
        Scopes.SINGLETON);
    // and the transaction manager via a provider.
    bind(export(TransactionManager.class)).toProvider(TransactionManagerProvider.class)
        .in(Scopes.SINGLETON);

    // bind the services
    bind(DataSourceService.class).to(DataSourceServiceImpl.class).in(
        Scopes.SINGLETON);
    
    bind(CacheManagerService.class).toProvider(importService(CacheManagerService.class)).in(Scopes.SINGLETON);
    
    bind(RegistryService.class).toProvider(importService(RegistryService.class)).in(Scopes.SINGLETON);
    
  }
  
}
