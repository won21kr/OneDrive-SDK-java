package com.bhyoo.onedrive.container.facet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static lombok.AccessLevel.PRIVATE;

/**
 * <a href="https://dev.onedrive.com/facets/searchresult_facet.htm">
 *     https://dev.onedrive.com/facets/searchresult_facet.htm</a>
 *
 * @author <a href="mailto:bh322yoo@gmail.com" target="_top">isac322</a>
 */
public class SearchResultFacet {
	@Getter protected final @NotNull URI onClickTelemetryUrl;

	protected SearchResultFacet(@NotNull URI onClickTelemetryUrl) {
		this.onClickTelemetryUrl = onClickTelemetryUrl;
	}

	@SneakyThrows(URISyntaxException.class)
	public static SearchResultFacet deserialize(@NotNull JsonParser parser) throws IOException {
		@NotNull URI onClickTelemetryUrl = null;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String currentName = parser.getCurrentName();
			parser.nextToken();

			if (currentName.equals("onClickTelemetryUrl")) {
				onClickTelemetryUrl = new URI(parser.getText());
			}
			else {
				throw new IllegalStateException("Unknown attribute detected in SearchResultFacet : " + currentName);
			}
		}

		return new SearchResultFacet(onClickTelemetryUrl);
	}
}
