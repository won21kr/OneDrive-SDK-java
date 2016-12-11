package org.onedrive.container;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static lombok.AccessLevel.PRIVATE;

/**
 * {@// TODO: add javadoc}
 *
 * @author <a href="mailto:yoobyeonghun@gmail.com" target="_top">isac322</a>
 */
public class Drive {
	@Getter @Setter(PRIVATE) @NotNull protected String id;
	@Getter @Setter(PRIVATE) @Nullable protected String driveType;
	@Getter @Setter(PRIVATE) @Nullable protected IdentitySet identitySet;
	@Setter(PRIVATE) @JsonProperty @NotNull private Quota quota;

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Drive && id.equals(((Drive) obj).getId());
	}

	@JsonIgnore public long getTotalCapacity() {return quota.total;}

	@JsonIgnore public long getDeleted() {return quota.deleted;}

	@JsonIgnore public long getUsedCapacity() {return quota.used;}

	@JsonIgnore public long getRemaining() {return quota.remaining;}

	@JsonIgnore public String getState() {return quota.state;}

	@SuppressWarnings("WeakerAccess")
	static private class Quota {
		public String state;
		public long total, deleted, used, remaining;
	}
}
