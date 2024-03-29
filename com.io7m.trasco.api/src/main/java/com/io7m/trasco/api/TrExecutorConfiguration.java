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

package com.io7m.trasco.api;

import java.sql.Connection;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The configuration information required for an executor.
 *
 * @param connection An open database connection
 * @param events     A function that will receive progress events
 * @param revisions  The set of known database schema revisions
 * @param upgrade    The desired upgrade behaviour
 * @param arguments  The arguments provided to any required parameters
 * @param versionGet A function that will be executed to retrieve a schema
 *                   version number
 * @param versionSet A function that will be executed to set the schema version
 *                   number
 */

public record TrExecutorConfiguration(
  TrExecutorVersionRetrieverType versionGet,
  TrExecutorVersionUpdaterType versionSet,
  Consumer<TrEventType> events,
  TrSchemaRevisionSet revisions,
  TrExecutorUpgrade upgrade,
  TrArguments arguments,
  Connection connection)
{
  /**
   * The configuration information required for an executor.
   *
   * @param connection An open database connection
   * @param events     A function that will receive progress events
   * @param revisions  The set of known database schema revisions
   * @param upgrade    The desired upgrade behaviour
   * @param arguments  The arguments provided to any required parameters
   * @param versionGet A function that will be executed to retrieve a schema
   *                   version number
   * @param versionSet A function that will be executed to set the schema
   *                   version number
   */

  public TrExecutorConfiguration
  {
    Objects.requireNonNull(versionGet, "versionGet");
    Objects.requireNonNull(versionSet, "versionSet");
    Objects.requireNonNull(events, "events");
    Objects.requireNonNull(revisions, "revisions");
    Objects.requireNonNull(upgrade, "upgrade");
    Objects.requireNonNull(connection, "connection");
    Objects.requireNonNull(arguments, "arguments");
  }
}
