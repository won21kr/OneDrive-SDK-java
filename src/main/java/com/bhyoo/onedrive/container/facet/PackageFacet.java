package com.bhyoo.onedrive.container.facet;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import static lombok.AccessLevel.PRIVATE;

// TODO: Enhance javadoc
// TODO: merge with DriveItem if possible

/**
 * <a href="https://dev.onedrive.com/facets/package_facet.htm">https://dev.onedrive.com/facets/package_facet.htm</a>
 *
 * @author <a href="mailto:bh322yoo@gmail.com" target="_top">isac322</a>
 */
public class PackageFacet {
	@Getter @Setter(PRIVATE) @Nullable protected String type;
}
