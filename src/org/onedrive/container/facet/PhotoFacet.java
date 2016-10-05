package org.onedrive.container.facet;

import com.sun.istack.internal.Nullable;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * https://dev.onedrive.com/facets/photo_facet.htm
 * {@// TODO: Enhance javadoc}
 *
 * @author <a href="mailto:yoobyeonghun@gmail.com" target="_top">isac322</a>
 */
public class PhotoFacet {
	/**
	 * Always ensure local date time.
	 */
	@Getter @Nullable protected final ZonedDateTime takenDateTime;
	@Getter @Nullable protected final String cameraMake;
	@Getter @Nullable protected final String cameraModel;
	@Getter @Nullable protected final Double fNumber;
	@Getter @Nullable protected final Double exposureDenominator;
	@Getter @Nullable protected final Double exposureNumerator;
	@Getter @Nullable protected final Double focalLength;
	@Getter @Nullable protected final Long iso;

	protected PhotoFacet(ZonedDateTime takenDateTime, String cameraMake, String cameraModel, Double fNumber,
						 Double exposureDenominator, Double exposureNumerator, Double focalLength, Long iso) {
		this.takenDateTime = takenDateTime;
		this.cameraMake = cameraMake;
		this.cameraModel = cameraModel;
		this.fNumber = fNumber;
		this.exposureDenominator = exposureDenominator;
		this.exposureNumerator = exposureNumerator;
		this.focalLength = focalLength;
		this.iso = iso;
	}

	@Nullable
	public static PhotoFacet parse(JSONObject json) {
		if (json == null) return null;

		String timestamp = json.getString("takenDateTime");
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.parse(timestamp), ZoneId.systemDefault());

		return new PhotoFacet(
				zonedDateTime,
				json.getString("cameraMake"),
				json.getString("cameraModel"),
				json.getDouble("fNumber"),
				json.getDouble("exposureDenominator"),
				json.getDouble("exposureNumerator"),
				json.getDouble("focalLength"),
				json.getLong("iso")
		);
	}
}