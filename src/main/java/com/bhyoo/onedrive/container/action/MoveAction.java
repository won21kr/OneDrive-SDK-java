package com.bhyoo.onedrive.container.action;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// TODO: add pointer (PathPointer)
public class MoveAction {
	@Getter protected final @NotNull String from;
	@Getter protected final @NotNull String to;

	protected MoveAction(@NotNull String from, @NotNull String to) {
		this.from = from;
		this.to = to;
	}

	@SuppressWarnings("ConstantConditions")
	public static @NotNull MoveAction deserialize(@NotNull JsonParser parser) throws IOException {
		@NotNull String from = null;
		@NotNull String to = null;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String currentName = parser.getCurrentName();
			parser.nextToken();

			switch (currentName) {
				case "from":
					from = parser.getText();
					break;
				case "to":
					to = parser.getText();
					break;
				default:
					throw new IllegalStateException("Unknown attribute detected in MoveAction : " + currentName);
			}
		}

		return new MoveAction(from, to);
	}
}
