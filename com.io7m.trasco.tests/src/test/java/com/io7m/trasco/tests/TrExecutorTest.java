/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.trasco.tests;

import com.io7m.trasco.api.TrEventExecutingSQL;
import com.io7m.trasco.api.TrEventType;
import com.io7m.trasco.api.TrEventUpgrading;
import com.io7m.trasco.api.TrException;
import com.io7m.trasco.api.TrExecutorConfiguration;
import com.io7m.trasco.api.TrSchemaRevisionSet;
import com.io7m.trasco.vanilla.TrExecutors;
import com.io7m.trasco.vanilla.TrSchemaRevisionSetParsers;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.TreeMap;

import static com.io7m.trasco.api.TrErrorCode.UNRECOGNIZED_SCHEMA_REVISION;
import static com.io7m.trasco.api.TrErrorCode.UPGRADE_DISALLOWED;
import static com.io7m.trasco.api.TrExecutorUpgrade.FAIL_INSTEAD_OF_UPGRADING;
import static com.io7m.trasco.api.TrExecutorUpgrade.PERFORM_UPGRADES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class TrExecutorTest
{
  private Path database;
  private EmbeddedConnectionPoolDataSource dataSource;
  private TrExecutors executors;
  private TrSchemaRevisionSetParsers parsers;
  private ArrayDeque<TrEventType> events;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.parsers = new TrSchemaRevisionSetParsers();
    this.executors = new TrExecutors();
    this.database = TrTestDirectories.createTempDirectory();
    this.dataSource = new EmbeddedConnectionPoolDataSource();
    this.dataSource.setDatabaseName(this.database.resolve("db").toString());
    this.dataSource.setCreateDatabase("true");
    this.dataSource.setConnectionAttributes("create=true");
    this.events = new ArrayDeque<TrEventType>();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    TrTestDirectories.deleteDirectory(this.database);
  }

  private void onEvent(
    final TrEventType event)
  {
    this.events.add(event);
  }

  /**
   * If the version number isn't retrieved, and the executor is configured not
   * to perform upgrades, then execution fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testVersionNotKnown()
    throws Exception
  {
    final var executor =
      this.executors.create(new TrExecutorConfiguration(
        connection -> Optional.empty(),
        (version, connection) -> {
          throw new IllegalStateException();
        },
        this::onEvent,
        new TrSchemaRevisionSet(new TreeMap<>()),
        FAIL_INSTEAD_OF_UPGRADING,
        this.dataSource.getConnection()
      ));

    final var ex =
      assertThrows(TrException.class, executor::execute);

    assertEquals(UPGRADE_DISALLOWED, ex.errorCode());
    assertEquals(0, this.events.size());
  }

  /**
   * If the version number isn't retrieved, and the executor is configured not
   * to perform upgrades, then execution fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testVersionNotKnownNoRevisions()
    throws Exception
  {
    final var executor =
      this.executors.create(new TrExecutorConfiguration(
        connection -> Optional.empty(),
        (version, connection) -> {
          throw new IllegalStateException();
        },
        this::onEvent,
        new TrSchemaRevisionSet(new TreeMap<>()),
        PERFORM_UPGRADES,
        this.dataSource.getConnection()
      ));

    executor.execute();
    assertEquals(0, this.events.size());
  }

  /**
   * If the database is already of a version newer than any of the revisions,
   * fail!
   *
   * @throws Exception On errors
   */

  @Test
  public void testVersionTooNew()
    throws Exception
  {
    final TrSchemaRevisionSet revisions;
    try (var stream = this.resourceOf("example-1.xml")) {
      revisions = this.parsers.parse(URI.create("urn:stdin"), stream);
    }

    final var executor =
      this.executors.create(new TrExecutorConfiguration(
        connection -> {
          return Optional.of(new BigInteger("100"));
        },
        (version, connection) -> {
          throw new IllegalStateException();
        },
        this::onEvent,
        revisions,
        PERFORM_UPGRADES,
        this.dataSource.getConnection()
      ));

    final var ex =
      assertThrows(TrException.class, executor::execute);

    assertEquals(UNRECOGNIZED_SCHEMA_REVISION, ex.errorCode());
    assertEquals(0, this.events.size());
  }

  /**
   * Upgrading from an uninitialized database works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpgradeFullOK()
    throws Exception
  {
    final TrSchemaRevisionSet revisions;
    try (var stream = this.resourceOf("example-1.xml")) {
      revisions = this.parsers.parse(URI.create("urn:stdin"), stream);
    }

    final var executor =
      this.executors.create(new TrExecutorConfiguration(
        connection -> {
          return Optional.empty();
        },
        (version, connection) -> {

        },
        this::onEvent,
        revisions,
        PERFORM_UPGRADES,
        this.dataSource.getConnection()
      ));

    executor.execute();

    assertEquals(
      new TrEventUpgrading(
        new BigInteger("-1"),
        new BigInteger("0")),
      this.events.remove()
    );
    assertEquals(
      TrEventExecutingSQL.class,
      this.events.remove().getClass()
    );
    assertEquals(
      new TrEventUpgrading(
        new BigInteger("0"),
        new BigInteger("1")),
      this.events.remove()
    );
    assertEquals(
      TrEventExecutingSQL.class,
      this.events.remove().getClass()
    );
    assertEquals(
      new TrEventUpgrading(
        new BigInteger("1"),
        new BigInteger("2")),
      this.events.remove()
    );
    assertEquals(
      TrEventExecutingSQL.class,
      this.events.remove().getClass()
    );
    assertEquals(
      new TrEventUpgrading(
        new BigInteger("2"),
        new BigInteger("3")),
      this.events.remove()
    );
    assertEquals(
      TrEventExecutingSQL.class,
      this.events.remove().getClass()
    );

    assertEquals(0, this.events.size());
  }

  /**
   * Upgrading from a known database version works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpgradePartialOK()
    throws Exception
  {
    final TrSchemaRevisionSet revisions;
    try (var stream = this.resourceOf("example-1.xml")) {
      revisions = this.parsers.parse(URI.create("urn:stdin"), stream);
    }

    try (var connection = this.dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (var statement = connection.prepareStatement(
        "create table example0 (id integer)")) {
        statement.execute();
      }
      connection.commit();
    }

    final var executor =
      this.executors.create(new TrExecutorConfiguration(
        connection -> {
          return Optional.of(BigInteger.ONE);
        },
        (version, connection) -> {

        },
        this::onEvent,
        revisions,
        PERFORM_UPGRADES,
        this.dataSource.getConnection()
      ));

    executor.execute();

    assertEquals(
      new TrEventUpgrading(
        new BigInteger("1"),
        new BigInteger("2")),
      this.events.remove()
    );
    assertEquals(
      TrEventExecutingSQL.class,
      this.events.remove().getClass()
    );
    assertEquals(
      new TrEventUpgrading(
        new BigInteger("2"),
        new BigInteger("3")),
      this.events.remove()
    );
    assertEquals(
      TrEventExecutingSQL.class,
      this.events.remove().getClass()
    );

    assertEquals(0, this.events.size());
  }

  private InputStream resourceOf(
    final String name)
    throws IOException
  {
    return TrTestDirectories.resourceStreamOf(
      TrExecutorTest.class,
      this.database,
      name
    );
  }
}
