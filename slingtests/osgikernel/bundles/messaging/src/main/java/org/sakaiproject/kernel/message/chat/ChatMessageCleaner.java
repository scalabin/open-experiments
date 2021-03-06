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
package org.sakaiproject.kernel.message.chat;

import org.apache.commons.lang.time.DateUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.jcr.JCRConstants;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.reference interface="org.apache.sling.jcr.api.SlingRepository"
 *                name="SlingRepository" bind="bindSlingRepository"
 *                unbind="unbindSlingRepository"
 */
public class ChatMessageCleaner extends TimerTask {

  private Timer chatCleanUpTimer;

  private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageCleaner.class);
  /**
   * The JCR Repository we access to update profile.
   * 
   */
  private SlingRepository slingRepository;

  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  protected void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }

  /**
   * @param componentContext
   */
  protected void activate(ComponentContext componentContext) {
    // Start the timer that will delete this message.
    chatCleanUpTimer = new Timer();
    chatCleanUpTimer.schedule(this, 15 * 1000,
        MessageConstants.CLEAUNUP_EVERY_X_MINUTES * 1000 * 60);

    LOGGER.info("Started the chats cleanup timer.");
  }

  protected void deactivate(ComponentContext componentContext) {
    if (chatCleanUpTimer != null) {
      chatCleanUpTimer.cancel();
    }
  }

  /**
   * 
   */
  public ChatMessageCleaner() {

  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.TimerTask#run()
   */
  @Override
  public void run() {

    LOGGER.info("Starting chat messages cleanup process.");
    // need to be admin when in our own thread
    Session session = null;
    QueryManager queryManager;
    try {

      session = slingRepository.loginAdministrative(null);

      // Get the current date and substract x minutes of it.
      Date d = new Date();
      d = DateUtils.addMinutes(d, MessageConstants.CLEAUNUP_EVERY_X_MINUTES);

      // Make the format for the JCR query
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat sdfMinutes = new SimpleDateFormat("kk:mm:ss");

      String timestamp = sdf.format(d) + "T" + sdfMinutes.format(d) + ".000+01:00";

      queryManager = session.getWorkspace().getQueryManager();

      String queryPath = "/jcr:root/" + ISO9075.encodePath("_user/private")
          + "//element(*)[@" + JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY + "='"
          + MessageConstants.SAKAI_MESSAGE_RT + "' and @"
          + MessageConstants.PROP_SAKAI_TYPE + "='" + MessageConstants.TYPE_CHAT
          + "' and @" + MessageConstants.PROP_SAKAI_READ + "='false' and @"
          + JCRConstants.JCR_CREATED + " < xs:dateTime('" + timestamp + "')]";

      Query query = queryManager.createQuery(queryPath, Query.XPATH);
      QueryResult qr = query.execute();

      NodeIterator nodes = qr.getNodes();

      long i = 0;
      // Loop the found nodes and delete them
      while (nodes.hasNext()) {
        Node n = nodes.nextNode();
        n.remove();
        i++;
      }

      // need to manually save
      session.save();

      LOGGER.info("Removed {} chat messages.", i);

    } catch (RepositoryException e) {
      LOGGER.warn("Got a repository exception during clean up process.");
      throw new MessagingException(e.getMessage(), e);
    } finally {
      // need to manually logout and commit
      try {
        if (session != null)
          session.logout();
      } catch (Exception e) {
        throw new RuntimeException("Failed to logout of JCR: " + e, e);
      }
    }
  }
}
