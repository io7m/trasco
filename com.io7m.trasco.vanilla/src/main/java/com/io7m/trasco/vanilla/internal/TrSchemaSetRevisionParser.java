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


package com.io7m.trasco.vanilla.internal;

import com.io7m.anethum.common.ParseException;
import com.io7m.anethum.common.ParseSeverity;
import com.io7m.anethum.common.ParseStatus;
import com.io7m.blackthorne.api.BTException;
import com.io7m.blackthorne.api.BTParseError;
import com.io7m.blackthorne.api.BTParseErrorType.Severity;
import com.io7m.blackthorne.jxe.BlackthorneJXE;
import com.io7m.trasco.api.TrSchemaRevisionSet;
import com.io7m.trasco.api.TrSchemaRevisionSetParserType;
import com.io7m.trasco.vanilla.internal.v1.TrV1SchemaDeclSetParser;
import com.io7m.trasco.xml.schemas.TrSchemas;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.io7m.trasco.vanilla.internal.v1.TrV1.element;

/**
 * A parser of revision sets.
 */

public final class TrSchemaSetRevisionParser
  implements TrSchemaRevisionSetParserType
{
  private final URI source;
  private final InputStream stream;
  private final Consumer<ParseStatus> statusConsumer;

  /**
   * A parser of revision sets.
   *
   * @param inStream         The stream
   * @param inStatusConsumer A status consumer
   * @param inSource         The source
   */

  public TrSchemaSetRevisionParser(
    final URI inSource,
    final InputStream inStream,
    final Consumer<ParseStatus> inStatusConsumer)
  {
    this.source =
      Objects.requireNonNull(inSource, "source");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.statusConsumer =
      Objects.requireNonNull(inStatusConsumer, "statusConsumer");
  }

  private static ParseStatus mapParseError(
    final BTParseError error)
  {
    return ParseStatus.builder()
      .setLexical(error.lexical())
      .setSeverity(mapSeverity(error.severity()))
      .setErrorCode("parse-error")
      .setMessage(error.message())
      .build();
  }

  private static ParseSeverity mapSeverity(
    final Severity severity)
  {
    return switch (severity) {
      case ERROR -> ParseSeverity.PARSE_ERROR;
      case WARNING -> ParseSeverity.PARSE_WARNING;
    };
  }

  @Override
  public TrSchemaRevisionSet execute()
    throws ParseException
  {
    try {
      final TrSchemaRevisionSet schemas =
        BlackthorneJXE.parse(
          this.source,
          this.stream,
          Map.ofEntries(
            Map.entry(
              element("Schemas"),
              TrV1SchemaDeclSetParser::new
            )
          ),
          TrSchemas.schemas()
        );

      return schemas;
    } catch (final BTException e) {
      final var statuses =
        e.errors()
          .stream()
          .map(TrSchemaSetRevisionParser::mapParseError)
          .collect(Collectors.toList());

      for (final var status : statuses) {
        this.statusConsumer.accept(status);
      }

      throw new ParseException(e.getMessage(), statuses);
    }
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }
}
