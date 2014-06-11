/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class QProfileServiceMediumTest {

  static final QualityProfileKey XOO_PROFILE_1 = QualityProfileKey.of("P1", "xoo");
  static final QualityProfileKey XOO_PROFILE_2 = QualityProfileKey.of("P2", "xoo");
  static final RuleKey XOO_RULE_1 = RuleKey.of("xoo", "x1");

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession dbSession;
  ActiveRuleIndex index;
  QProfileService service;
  RuleActivator activator;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    service = tester.get(QProfileService.class);
    activator = tester.get(RuleActivator.class);

    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newDto(XOO_RULE_1)
      .setSeverity("MINOR").setLanguage("xoo");
    db.ruleDao().insert(dbSession, xooRule1);

    // create pre-defined profile
    db.qualityProfileDao().insert(dbSession,
      QualityProfileDto.createFor(XOO_PROFILE_1),
      QualityProfileDto.createFor(XOO_PROFILE_2));
    dbSession.commit();
    dbSession.clearCache();
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void count_by_all_profiles() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    service.activate(new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_1, XOO_RULE_1))
      .setSeverity("BLOCKER"));

    service.activate(new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_2, XOO_RULE_1))
      .setSeverity("BLOCKER"));

    dbSession.commit();

    Map<QualityProfileKey, Long> counts = service.countAllActiveRules();
    assertThat(counts).hasSize(2);
    assertThat(counts.keySet()).containsOnly(
      XOO_PROFILE_1, XOO_PROFILE_2
    );
    assertThat(counts.values()).containsOnly(1L, 1L);

  }
}
