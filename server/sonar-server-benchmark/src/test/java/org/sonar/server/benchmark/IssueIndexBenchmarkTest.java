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

package org.sonar.server.benchmark;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.index.IssueAuthorizationDao;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class IssueIndexBenchmarkTest {

  private static final Logger LOGGER = LoggerFactory.getLogger("benchmarkIssues");

  final static int PROJECTS = 100;
  final static int FILES_PER_PROJECT = 100;
  final static int ISSUES_PER_FILE = 100;

  @ClassRule
  public static ServerTester tester = new ServerTester();

  @Test
  public void benchmark() throws Exception {
    // initialization - feed issues/issueAuthorization with projects and hardcoded users
    indexAuthorizations();

    // index issues
    benchmarkIssueIndexing();

    // execute some queries
    benchmarkQueries();
  }

  private void indexAuthorizations() {
    IssueAuthorizationIndexer indexer = tester.get(IssueAuthorizationIndexer.class);
    List<IssueAuthorizationDao.Dto> authorizations = Lists.newArrayList();
    for (int i = 0; i < PROJECTS; i++) {
      IssueAuthorizationDao.Dto authorization = new IssueAuthorizationDao.Dto("PROJECT" + i, System.currentTimeMillis());
      authorization.addGroup("sonar-users");
      authorization.addUser("admin");
      authorizations.add(authorization);
    }
    indexer.index(authorizations);
  }

  private void benchmarkIssueIndexing() {
    LOGGER.info("Indexing issues");
    IssueIterator issues = new IssueIterator(PROJECTS, FILES_PER_PROJECT, ISSUES_PER_FILE);
    ProgressTask progressTask = new ProgressTask("issues", issues.count());
    Timer timer = new Timer("IssuesIndex");
    timer.schedule(progressTask, ProgressTask.PERIOD_MS, ProgressTask.PERIOD_MS);

    long start = System.currentTimeMillis();
    tester.get(IssueIndexer.class).index(issues);
    long end = System.currentTimeMillis();

    timer.cancel();
    long period = end - start;
    LOGGER.info(String.format("%d issues indexed in %d ms (%d/second)", issues.count.get(), period, 1000 * issues.count.get() / period));
  }

  private void benchmarkQueries() {
    benchmarkQuery("all issues", IssueQuery.builder().build());
    benchmarkQuery("project issues", IssueQuery.builder().projectUuids(Arrays.asList("PROJECT33")).build());
    benchmarkQuery("file issues", IssueQuery.builder().componentUuids(Arrays.asList("FILE333")).build());
    benchmarkQuery("various", IssueQuery.builder()
      .resolutions(Arrays.asList(Issue.RESOLUTION_FIXED))
      .assigned(true)
      .build());
    // TODO test facets
  }

  private void benchmarkQuery(String label, IssueQuery query) {
    IssueIndex index = tester.get(IssueIndex.class);
    for (int i = 0; i < 10; i++) {
      long start = System.currentTimeMillis();
      Result<Issue> result = index.search(query, new QueryContext());
      long end = System.currentTimeMillis();
      LOGGER.info("Request (" + label + "): {} docs in {} ms", result.getTotal(), end - start);
    }
  }

  private static class IssueIterator implements Iterator<IssueDoc> {
    private final int nbProjects, nbFilesPerProject, nbIssuesPerFile;
    private int currentProject = 0, currentFile = 0;
    private AtomicLong count = new AtomicLong(0L);
    private final Iterator<String> users = cycleIterator("guy", 200);
    private Iterator<String> rules = cycleIterator("squid:rule", 1000);
    private final Iterator<String> severities = Iterables.cycle(Severity.ALL).iterator();
    private final Iterator<String> statuses = Iterables.cycle(Issue.STATUSES).iterator();
    private final Iterator<String> resolutions = Iterables.cycle(Issue.RESOLUTIONS).iterator();

    IssueIterator(int nbProjects, int nbFilesPerProject, int nbIssuesPerFile) {
      this.nbProjects = nbProjects;
      this.nbFilesPerProject = nbFilesPerProject;
      this.nbIssuesPerFile = nbIssuesPerFile;
    }

    public AtomicLong count() {
      return count;
    }

    @Override
    public boolean hasNext() {
      return count.get() < nbProjects * nbFilesPerProject * nbIssuesPerFile;
    }

    @Override
    public IssueDoc next() {
      IssueDoc issue = new IssueDoc(Maps.<String, Object>newHashMap());
      issue.setKey(Uuids.create());
      issue.setFilePath("src/main/java/Foo" + currentFile);
      issue.setComponentUuid("FILE" + currentFile);
      issue.setProjectUuid("PROJECT" + currentProject);
      issue.setActionPlanKey("PLAN" + currentProject);
      issue.setAssignee(users.next());
      issue.setAuthorLogin(users.next());
      issue.setLine(RandomUtils.nextInt());
      issue.setCreationDate(new Date());
      issue.setUpdateDate(new Date());
      issue.setFuncUpdateDate(new Date());
      issue.setFuncCreationDate(new Date());
      issue.setFuncCloseDate(null);
      issue.setAttributes(null);
      issue.setDebt(1000L);
      issue.setEffortToFix(3.14);
      issue.setLanguage("php");
      issue.setReporter(users.next());
      issue.setRuleKey(rules.next());
      issue.setResolution(resolutions.next());
      issue.setStatus(statuses.next());
      issue.setSeverity(severities.next());
      issue.setMessage(RandomUtils.nextLong() + "this is the message. Not too short.");
      count.incrementAndGet();
      if (count.get() % nbIssuesPerFile == 0) {
        currentFile++;
      }
      if (count.get() % (nbFilesPerProject * nbIssuesPerFile) == 0) {
        currentProject++;
      }

      return issue;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private static Iterator<String> cycleIterator(String prefix, int size) {
    List<String> values = Lists.newArrayList();
    for (int i = 0; i < size; i++) {
      values.add(String.format("%s%d", prefix, i));
    }
    return Iterators.cycle(values);
  }

  static class ProgressTask extends TimerTask {
    public static final long PERIOD_MS = 60000L;

    private final String label;
    private final AtomicLong counter;
    private long previousCount = 0L;
    private long previousTime = 0L;

    public ProgressTask(String label, AtomicLong counter) {
      this.label = label;
      this.counter = counter;
      this.previousTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
      long currentCount = counter.get();
      long now = System.currentTimeMillis();
      LOGGER.info("{} {} indexed ({} {}/second)",
        currentCount, label, documentsPerSecond(currentCount - previousCount, now - previousTime), label);
      this.previousCount = currentCount;
      this.previousTime = now;
    }
  }

  static int documentsPerSecond(long nbIssues, long time) {
    return (int) Math.round(nbIssues / (time / 1000.0));
  }
}
