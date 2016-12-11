package org.onedrive.container.items.pointer;

import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onedrive.RequestTool;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@// TODO: Enhance javadoc }
 * {@// TODO: this class can be static factory!. consider this later (if path is same, give caller same object to
 * save memory}
 * In OneDrive, <a href='https://dev.onedrive.com/README.htm#item-resource'>item</a> referencing can be represented by
 * both <b><tt>ID</tt></b> and <b><tt>path</tt></b>.<br>
 * Unlike <b><tt>ID</tt></b>, usage of <b><tt>path</tt></b> is not quite simple.<br>
 * All <b><tt>path</tt></b> referencing must fallow <tt>/drive/root:/{item-path}</tt> or
 * <tt>/drives/{drive-id}/root:/{item-path}</tt> form.<br>
 * <br>
 * This class helps programmer to reference file with path notation.
 *
 * @author <a href="mailto:yoobyeonghun@gmail.com" target="_top">isac322</a>
 */
public class PathPointer extends BasePointer {
	private final static Pattern pathMatcher = Pattern.compile("^/drive/root:(.*)$");
	private final static Pattern drivePathMatcher = Pattern.compile("^/drives/([a-fA-F0-9]+)/root:(.*)$");
	@Getter @Nullable private final String driveId;
	@Getter @NotNull private final String readablePath;
	@NotNull private final String path;
	@NotNull private final String rawPath;


	/*
	string escaping issue. (example : %fe%f2.txt)
	strings that matches with..
	`drivePathMatcher` : will be treated as unescaped string
	`pathMatcher` : will be treated as unescaped string
	else : will be treated as "already escaped string"

	because strings that matches with regex can be assumed that it's invoked by constructor of BaseItem.
	and otherwise user invoked.
	 */
	public PathPointer(@NotNull String anyPath) {
		String rawPath;
		Matcher matcher;

		if ((matcher = drivePathMatcher.matcher(anyPath)).matches()) {
			// decode `anyPath`
			rawPath = anyPath;
			anyPath = QueryStringDecoder.decodeComponent(anyPath);

			this.readablePath = matcher.group(2);
			this.path = anyPath;
			this.driveId = matcher.group(1);
			this.rawPath = rawPath;
		}
		else if ((matcher = pathMatcher.matcher(anyPath)).matches()) {
			// decode `anyPath`
			rawPath = anyPath;
			anyPath = QueryStringDecoder.decodeComponent(anyPath);

			this.readablePath = matcher.group(1);
			this.path = anyPath;
			this.driveId = null;
			this.rawPath = rawPath;
		}
		else if (anyPath.charAt(0) != '/') {
			throw new IllegalArgumentException("`path` doesn't start with '/'. given : " + anyPath);
		}
		else {
			// encode `anyPath`
			try {
				rawPath = new URI(null, null, anyPath, null).toASCIIString();
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("Illegal character in `anyPath` at index " + e.getIndex(), e);
			}

			this.readablePath = anyPath;
			this.path = "/drive/root:" + anyPath;
			this.driveId = null;
			this.rawPath = "/drive/root:" + rawPath;
		}
	}


	/*
	string escaping issue. (example : %fe%f2.txt)
	strings that matches with..
	`drivePathMatcher` : will be treated as unescaped string
	`pathMatcher` : will be treated as unescaped string
	else : will be treated as "already escaped string"

	because strings that matches with regex can be assumed that it's invoked by constructor of BaseItem.
	and otherwise user invoked.
	 */
	public PathPointer(@NotNull String anyPath, @Nullable String driveId) {
		String rawPath = anyPath;
		// decode `anyPath`
		anyPath = QueryStringDecoder.decodeComponent(anyPath);
		Matcher matcher;

		// anyPath is OneDrive-path notation that contains drive-id
		if ((matcher = drivePathMatcher.matcher(anyPath)).matches()) {
			this.readablePath = matcher.group(2);
			this.driveId = driveId;

			// index of path-value separator
			int separatorIdx = anyPath.indexOf(':');

			// if `driveId` is null, `anyPath`'s drive-id will be removed
			if (driveId == null) {
				this.path = "/drive/root:" + this.readablePath;
				this.rawPath = "/drive/root" + rawPath.substring(separatorIdx);
			}
			// set drive-id value in `anyPath` to `driveId`
			else {
				this.path = "/drives/" + driveId + "/root:" + this.readablePath;
				this.rawPath = "/drives/" + driveId + "/root" + rawPath.substring(separatorIdx);
			}
		}
		// anyPath is OneDrive-path notation that does not contain drive-id
		else if ((matcher = pathMatcher.matcher(anyPath)).matches()) {

			this.readablePath = matcher.group(1);
			this.driveId = driveId;

			if (driveId == null) {
				this.path = anyPath;
				this.rawPath = rawPath;
			}
			else {
				this.path = "/drives/" + driveId + "/root:" + this.readablePath;
				// `rawPath` start with '/drive/root:' and `/root:`'s start index is 6.
				this.rawPath = "/drives/" + driveId + rawPath.substring(6);
			}
		}
		// if anyPath is neither OneDrive-path nor pure absolute path
		else if (anyPath.charAt(0) != '/') {
			throw new IllegalArgumentException("`path` doesn't start with '/'. given : " + anyPath);
		}
		// if anyPath is pure absolute path
		else {
			String t;
			// encode `anyPath`
			try {
				t = new URI(null, null, rawPath, null).toASCIIString();
				anyPath = rawPath;
				rawPath = t;
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("Illegal character in `anyPath` at index " + e.getIndex(), e);
			}

			this.readablePath = anyPath;
			this.driveId = driveId;

			if (driveId == null) {
				this.path = "/drive/root:" + anyPath;
				this.rawPath = "/drive/root:" + rawPath;
			}
			else {
				this.path = "/drives/" + driveId + "/root:" + anyPath;
				this.rawPath = "/drives/" + driveId + "/root:" + rawPath;
			}
		}
	}

	private PathPointer(@Nullable String driveId, @NotNull String readablePath,
						@NotNull String path, @NotNull String rawPath) {
		this.driveId = driveId;
		this.readablePath = readablePath;
		this.path = path;
		this.rawPath = rawPath;
	}

	@NotNull
	@Override
	public URI toURI() throws URISyntaxException {
		return new URI(RequestTool.SCHEME, RequestTool.HOST, path, null);
	}

	@NotNull
	@Override
	public String toApi() {
		return path;
	}

	@NotNull
	@Override
	public String toASCIIApi() {
		return rawPath;
	}

	@NotNull
	@Override
	public String toJson() {
		return "{\"path\":\"" + path + "\"}";
	}

	@NotNull
	@Override
	public String resolveOperator(@NotNull Operator op) {
		return rawPath + ":/" + op.getString();
	}

	@NotNull
	public PathPointer resolve(@NotNull String name) {
		// raise exception if `name` is absolute path
		if (name.charAt(0) == '/') {
			throw new IllegalArgumentException("`name` doesn't starts with '/'. given : " + name);
		}

		String rawName;

		// escape `name` string
		try {
			rawName = new URI(null, null, name, null).toASCIIString();
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Illegal character in `name` at index " + e.getIndex(), e);
		}

		// if `path` end with '/'
		if (path.charAt(path.length() - 1) == '/')
			return new PathPointer(driveId, readablePath + name, path + name, rawPath + rawName);
		else
			return new PathPointer(driveId, readablePath + '/' + name, path + '/' + name, rawPath + '/' + rawName);
	}
}