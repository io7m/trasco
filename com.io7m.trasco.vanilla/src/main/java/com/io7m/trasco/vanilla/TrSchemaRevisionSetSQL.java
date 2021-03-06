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


package com.io7m.trasco.vanilla;

import com.io7m.anethum.common.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;

import static com.io7m.trasco.vanilla.TrStatementExclusion.GRANTS;
import static com.io7m.trasco.vanilla.TrStatementExclusion.ROLES;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Locale.ROOT;

/**
 * Utilities for dumping raw SQL statements from sets of revisions.
 */

public final class TrSchemaRevisionSetSQL
{
  private TrSchemaRevisionSetSQL()
  {

  }

  /**
   * A function to dump the SQL statements of a revision set.
   *
   * @param input      The input file
   * @param output     The output file
   * @param exclusions The excluded content
   *
   * @throws IOException    On errors
   * @throws ParseException On errors
   */

  public static void showSQLStatements(
    final Path input,
    final Path output,
    final EnumSet<TrStatementExclusion> exclusions)
    throws IOException, ParseException
  {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(output, "output");
    Objects.requireNonNull(exclusions, "exclusions");

    final var set =
      new TrSchemaRevisionSetParsers()
        .parseFile(input);

    final var outputParent = output.getParent();
    if (outputParent != null) {
      Files.createDirectories(outputParent);
    }

    try (var writer =
           Files.newBufferedWriter(
             output,
             UTF_8,
             WRITE,
             TRUNCATE_EXISTING,
             CREATE)) {

      for (final var entry : set.revisions().entrySet()) {
        final var revision = entry.getValue();
        for (final var statement : revision.statements()) {
          if (exclude(statement.strip().toUpperCase(ROOT), exclusions)) {
            continue;
          }
          writer.append(statement);
          writer.append(';');
          writer.newLine();
        }
      }
    }
  }

  private static boolean exclude(
    final String statement,
    final EnumSet<TrStatementExclusion> exclusions)
  {
    if (statement.startsWith("CREATE ROLE")) {
      if (exclusions.contains(ROLES)) {
        return true;
      }
    }
    if (statement.startsWith("DROP ROLE")) {
      if (exclusions.contains(ROLES)) {
        return true;
      }
    }
    if (statement.startsWith("GRANT")) {
      if (exclusions.contains(GRANTS)) {
        return true;
      }
    }
    return false;
  }
}
